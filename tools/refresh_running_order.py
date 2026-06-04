#!/usr/bin/env python3
"""
Refresh running_order.json from the latest Hellfest PDF.

Steps:
  1. Scrape https://www.hellfest.fr/news/running-order for the PDF URL.
  2. Download the PDF, compare SHA-256 to last fetch — skip if unchanged.
  3. Parse with pdfplumber: 4 pages × 6 stages, time pairs + artist text.
  4. Look up existing concerts by (festivalDay, stage, start) — preserve
     user-curated fields (id, appleMusicPlaylist, appleMusicArtistId, custom
     artist casing) so manual work is never clobbered.
  5. Diff and write new JSON + asset copy + cached PDF + hash file.

After running this, also run resolve_apple_music_ids.py to fill IDs for any
newly-added artists.

Usage:
  python3 tools/refresh_running_order.py [--force] [--dry-run] [--local PATH]
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import subprocess
import sys
from collections import defaultdict
from datetime import date, timedelta
from pathlib import Path

try:
    import pdfplumber
except ImportError:
    sys.exit("pdfplumber required. Install with: pip3 install --user pdfplumber")

ROOT = Path(__file__).resolve().parent.parent
JSON_PATH = ROOT / "running_order.json"
ASSET_PATH = ROOT / "app/src/main/assets/running_order.json"
PDF_CACHE = ROOT / "tools/.last_running_order.pdf"
HASH_FILE = ROOT / "tools/.last_pdf_hash"

HELLFEST_NEWS_URL = "https://www.hellfest.fr/news/running-order"
PDF_URL_RE = re.compile(r'https://[^"]+RO_\d{4}_web_FR_[a-f0-9]+\.pdf')

STAGE_ORDER = ["MAINSTAGE_01", "MAINSTAGE_02", "WARZONE", "VALLEY", "TEMPLE", "ALTAR"]
STAGE_HEADER_TEXT = {
    "MAINSTAGE_01": ("MAINSTAGE", "01"),
    "MAINSTAGE_02": ("MAINSTAGE", "02"),
    "WARZONE":      ("WARZONE",),
    "VALLEY":       ("VALLEY",),
    "TEMPLE":       ("TEMPLE",),
    "ALTAR":        ("ALTAR",),
}
STAGE_SHORT = {"MAINSTAGE_01": "ms1", "MAINSTAGE_02": "ms2", "WARZONE": "wz",
               "VALLEY": "va", "TEMPLE": "te", "ALTAR": "al"}

DAY_DATES = ["2026-06-18", "2026-06-19", "2026-06-20", "2026-06-21"]
TZ_OFFSET = "+02:00"


# ---------------------------------------------------------------------------
# PDF discovery & download
# ---------------------------------------------------------------------------

def find_pdf_url() -> str:
    html = _curl(HELLFEST_NEWS_URL).decode("utf-8", errors="replace")
    m = PDF_URL_RE.search(html)
    if not m:
        raise SystemExit(f"PDF URL not found at {HELLFEST_NEWS_URL}")
    return m.group(0)


def _curl(url: str) -> bytes:
    r = subprocess.run(["curl", "-sSL", "--max-time", "30", url],
                       capture_output=True, check=True)
    return r.stdout


def download_pdf(url: str, dest: Path) -> Path:
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(_curl(url))
    return dest


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


# ---------------------------------------------------------------------------
# PDF parsing
# ---------------------------------------------------------------------------

def detect_stage_bounds(page) -> list[tuple[str, float, float]]:
    """Read the stage header row on the page and compute column x-boundaries
    as midpoints between adjacent header centers."""
    # Get all words, then keep those whose text matches one of our header tokens.
    all_words = page.extract_words(keep_blank_chars=False)
    centers: dict[str, float] = {}
    for stage, tokens in STAGE_HEADER_TEXT.items():
        # Find a run of words matching the tokens in order, on the same y line.
        candidates = [w for w in all_words if w["text"] == tokens[0] and w["top"] < page.height * 0.2]
        for first in candidates:
            ws = [first]
            cur_y = first["top"]
            for t in tokens[1:]:
                follow = next((w for w in all_words
                               if w["text"] == t
                               and abs(w["top"] - cur_y) < 2
                               and 0 < (w["x0"] - ws[-1]["x1"]) < 12), None)
                if not follow:
                    ws = None
                    break
                ws.append(follow)
            if ws:
                centers[stage] = (ws[0]["x0"] + ws[-1]["x1"]) / 2
                break
    missing = [s for s in STAGE_ORDER if s not in centers]
    if missing:
        raise SystemExit(f"Could not locate stage headers on page: {missing}")
    ordered = [(s, centers[s]) for s in STAGE_ORDER]
    # Boundaries = midpoints between adjacent centers. For the leftmost stage,
    # mirror the right-side distance to exclude the left time-ruler column.
    bounds = []
    for i, (s, c) in enumerate(ordered):
        if i == 0:
            half = (ordered[1][1] - c) / 2
            lo = c - half
        else:
            lo = (ordered[i - 1][1] + c) / 2
        if i == len(ordered) - 1:
            half = (c - ordered[i - 1][1]) / 2
            hi = c + half
        else:
            hi = (ordered[i + 1][1] + c) / 2
        bounds.append((s, lo, hi))
    return bounds


def stage_for(word, bounds) -> str | None:
    xc = (word["x0"] + word["x1"]) / 2
    for s, lo, hi in bounds:
        if lo <= xc < hi:
            return s
    return None


_TIME_RE = re.compile(r"^\d{2}:\d{2}$")


def parse_pdf(pdf_path: Path) -> list[dict]:
    """Extract concerts as dicts: {stage, festivalDay, start_hm, end_hm, artist_raw}."""
    concerts = []
    is_time = lambda w: bool(_TIME_RE.match(w["text"]))
    is_chrome = lambda w: is_time(w) or w["text"].strip() in (">", "<")

    with pdfplumber.open(pdf_path) as pdf:
        if len(pdf.pages) != 4:
            print(f"  WARNING: expected 4 pages, got {len(pdf.pages)}")
        for page_idx, p in enumerate(pdf.pages):
            bounds = detect_stage_bounds(p)
            # Filter: skip header zone (above y = first body row) and left ruler.
            body_top = max(b[1] for b in [(0, 100, 0)]) and 100  # placeholder; real filter below
            body_top = max(
                (w["top"] for w in p.extract_words() if w["text"] == STAGE_HEADER_TEXT["ALTAR"][0]),
                default=100,
            ) + 15
            words = [w for w in p.extract_words(keep_blank_chars=False)
                     if w["top"] > body_top and w["x0"] > 30]

            buckets: dict[int, dict[str, list]] = defaultdict(lambda: defaultdict(list))
            for w in words:
                if is_time(w):
                    s = stage_for(w, bounds)
                    if s:
                        buckets[round(w["top"] / 3) * 3][s].append(w)

            cards = []
            for _yb, by_stage in buckets.items():
                for s, ws in by_stage.items():
                    if len(ws) >= 2:
                        ws.sort(key=lambda w: w["x0"])
                        cards.append({
                            "stage": s,
                            "y_actual": min(w["top"] for w in ws),
                            "start_hm": ws[0]["text"],
                            "end_hm": ws[-1]["text"],
                            "festivalDay": page_idx + 1,
                        })

            # Compute per-stage "artist zone upper bound" = previous card's time-row bottom
            cards_by_stage = defaultdict(list)
            for c in cards:
                cards_by_stage[c["stage"]].append(c)
            for stage_cards in cards_by_stage.values():
                stage_cards.sort(key=lambda c: c["y_actual"])
                prev_bottom = 0.0
                for c in stage_cards:
                    c["artist_zone_top"] = prev_bottom + 3
                    # Time row is ~10 px tall; clamp lookback to MAX_LOOKBACK to avoid headers
                    c["artist_zone_top"] = max(c["artist_zone_top"], c["y_actual"] - 55)
                    prev_bottom = c["y_actual"] + 11

            non_chrome = [w for w in words if not is_chrome(w) and stage_for(w, bounds)]
            for c in cards:
                col = [w for w in non_chrome
                       if stage_for(w, bounds) == c["stage"]
                       and c["artist_zone_top"] < w["top"] < c["y_actual"] - 3]
                by_y: dict[int, list] = defaultdict(list)
                for w in col:
                    by_y[round(w["top"] / 3) * 3].append(w)
                rows = [
                    " ".join(w["text"] for w in sorted(by_y[y], key=lambda w: w["x0"]))
                    for y in sorted(by_y)
                ]
                c["artist_raw"] = " ".join(rows).strip()
                concerts.append(c)
    return concerts


# ---------------------------------------------------------------------------
# Normalization & JSON assembly
# ---------------------------------------------------------------------------

import unicodedata


def _ascii_key(s: str) -> str:
    """Fold to ASCII-uppercase-no-space for fuzzy match (Á==A, é==e, etc.)."""
    folded = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return folded.upper().replace(" ", "")


def normalize_artist(raw: str, existing_index: dict[str, str]) -> str:
    """Apply char fixups, then preserve existing JSON name if same artist."""
    s = raw.replace("«", '"').replace("»", '"').replace("’", "'").replace("‘", "'")
    s = s.strip()
    # Lookup with ASCII-folded key so 'Skáld' matches 'SKALD' from extraction.
    hit = existing_index.get(_ascii_key(s))
    if hit:
        return hit
    return s.title() if (s.isupper() and any(ch.isalpha() for ch in s)) else s


def make_id(day: int, stage: str, artist: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", artist.lower()).strip("-")
    return f"d{day}-{STAGE_SHORT[stage]}-{slug}"


def hm_to_iso(festival_day: int, hm: str, is_after_midnight: bool) -> str:
    base = date.fromisoformat(DAY_DATES[festival_day - 1])
    if is_after_midnight:
        base = base + timedelta(days=1)
    return f"{base.isoformat()}T{hm}:00{TZ_OFFSET}"


def to_full_concert(parsed: dict, existing_by_slot: dict[tuple, dict]) -> dict:
    day = parsed["festivalDay"]
    stage = parsed["stage"]
    start_hm = parsed["start_hm"]
    end_hm = parsed["end_hm"]
    # End is past midnight iff start hour >= 18 and end hour < 12.
    start_h = int(start_hm.split(":")[0])
    end_h = int(end_hm.split(":")[0])
    start_after_midnight = start_h < 6
    end_after_midnight = (start_h >= 18 and end_h < 12) or start_after_midnight
    start_iso = hm_to_iso(day, start_hm, start_after_midnight)
    end_iso = hm_to_iso(day, end_hm, end_after_midnight)

    slot_key = (day, stage, start_hm)
    existing = existing_by_slot.get(slot_key, {})

    artist_existing_index: dict[str, str] = {}
    for c in existing_by_slot.values():
        artist_existing_index[_ascii_key(c["artist"])] = c["artist"]
    artist = normalize_artist(parsed["artist_raw"], artist_existing_index)

    # Drop playlist/ID if artist changed in this slot
    artist_changed = existing and existing.get("artist") and existing["artist"] != artist
    return {
        "id": existing.get("id") or make_id(day, stage, artist),
        "artist": artist,
        "stage": stage,
        "festivalDay": day,
        "start": start_iso,
        "end": end_iso,
        "appleMusicPlaylist": None if artist_changed else existing.get("appleMusicPlaylist"),
        "appleMusicArtistId": None if artist_changed else existing.get("appleMusicArtistId"),
    }


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--force", action="store_true", help="re-parse even if PDF hash is unchanged")
    ap.add_argument("--local", help="parse a local PDF file instead of downloading")
    ap.add_argument("--dry-run", action="store_true", help="don't write JSON / hash / cache")
    args = ap.parse_args()

    if args.local:
        pdf_path = Path(args.local)
    else:
        url = find_pdf_url()
        print(f"PDF URL : {url}")
        pdf_path = download_pdf(url, PDF_CACHE)

    h = sha256(pdf_path)
    prev = HASH_FILE.read_text().strip() if HASH_FILE.exists() else None
    if h == prev and not args.force:
        print(f"PDF unchanged (sha256={h[:12]}…). Use --force to re-parse anyway.")
        return 0
    print(f"PDF hash: {h[:16]}  (was {prev[:16] if prev else '<none>'})")

    parsed = parse_pdf(pdf_path)
    print(f"Parsed  : {len(parsed)} concerts from {pdf_path}")

    existing = json.loads(JSON_PATH.read_text())
    existing_by_slot = {
        (c["festivalDay"], c["stage"], c["start"][11:16]): c
        for c in existing["concerts"]
    }
    new_concerts = [to_full_concert(p, existing_by_slot) for p in parsed]
    new_concerts.sort(key=lambda c: (c["festivalDay"], c["start"], c["stage"]))

    # Diff
    def slot(c):
        return (c["festivalDay"], c["stage"], c["start"][11:16])

    def label(c):
        return f"D{c['festivalDay']} {c['stage']:<13} {c['start'][11:16]}->{c['end'][11:16]}  {c['artist']!r}"

    old_by_slot = existing_by_slot
    new_by_slot = {slot(c): c for c in new_concerts}
    added_slots = sorted(new_by_slot.keys() - old_by_slot.keys())
    removed_slots = sorted(old_by_slot.keys() - new_by_slot.keys())
    changed = []
    for s in sorted(new_by_slot.keys() & old_by_slot.keys()):
        old_c, new_c = old_by_slot[s], new_by_slot[s]
        if (old_c["artist"], old_c["end"][11:16]) != (new_c["artist"], new_c["end"][11:16]):
            changed.append((old_c, new_c))

    print("\n=== Diff vs current JSON ===")
    print(f"  + {len(added_slots)} added slots")
    print(f"  - {len(removed_slots)} removed slots")
    print(f"  ~ {len(changed)} modified slots (artist or end time changed)")
    for s in added_slots[:20]:
        print(f"    + {label(new_by_slot[s])}")
    for s in removed_slots[:20]:
        print(f"    - {label(old_by_slot[s])}")
    for old_c, new_c in changed[:20]:
        print(f"    ~ {label(old_c)}  →  {label(new_c)}")

    if args.dry_run:
        print("\n--dry-run: not writing.")
        return 0

    out = {**existing, "concerts": new_concerts}
    JSON_PATH.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n")
    ASSET_PATH.write_bytes(JSON_PATH.read_bytes())
    if not args.local:
        HASH_FILE.write_text(h)
    print(f"\nWrote {len(new_concerts)} concerts → {JSON_PATH}")
    print(f"Mirror → {ASSET_PATH}")

    # New-artist hint
    new_artists = [c["artist"] for c in new_concerts
                   if c["appleMusicArtistId"] is None]
    if new_artists:
        print(f"\n{len(new_artists)} artists without Apple Music ID. Run:")
        print(f"  python3 {Path(__file__).parent}/resolve_apple_music_ids.py")
    return 0


if __name__ == "__main__":
    sys.exit(main())
