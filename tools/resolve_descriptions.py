#!/usr/bin/env python3
"""
Resolve a short French description (1–2 sentences) for every artist across every
shipped running order in public/festivals/, stored in a `description` field on
each concert.

Source chain per artist (first hit wins):
  A. Concert carries a musicBrainzId — precise path, avoids wrong-page matches:
       MusicBrainz (inc=url-rels) → Wikidata Q-id → frwiki title → French
       Wikipedia REST summary lead, with the shorter Wikidata `descriptions.fr`
       as fallback.
  B. No musicBrainzId (e.g. festivals du registre that never went through the
     socials resolver) — naive French Wikipedia REST summary by artist name,
     skipping disambiguation pages and too-short extracts.

NEVER writes `"description": null`. Either the value is a non-empty string, or
the key is absent. Inherited `description: null` from older cron versions is
deleted when encountered.

Idempotent: any artist already carrying a non-empty `description` is skipped, so
only artists added since the last run are queried.

Network goes through `curl` (system trust store) — no SSL_CERT_FILE needed here,
unlike the urllib-based resolve_apple_music_ids.py / resolve_socials.py.
MusicBrainz is rate-limited at 1 req/s; we sleep 1.1 s after that call.

Writes are atomic (tmp file → JSON re-validate → os.replace) so a crash mid-write
can never leave a corrupt running order on disk. The Android asset mirror
app/src/main/assets/festivals/<id>.json is refreshed only when it already exists
(the ~10 app festivals); registry festivals have no mirror and ship via docs/.

Usage:
  python3 tools/resolve_descriptions.py [--force] [--festival <id>]
    --force     : re-resolve everyone, even artists already described
    --festival  : restrict to a single festival id (default = all shipped)
"""
from __future__ import annotations
import argparse
import glob
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PUBLIC = ROOT / "public" / "festivals"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "festivals"

UA = "MaTeamHF2026App/1.0 ( https://github.com/local )"
SLEEP_MB = 1.1    # MusicBrainz: 1 req/s per IP
SLEEP_WIKI = 0.2  # be polite to Wikidata / Wikipedia
MAX_LEN = 320     # hard cap on the stored description

# Non-running-order files in public/festivals/ to skip (index, registries, reports).
SKIP_BASENAMES = {
    "index.json", "festivals_registry_200.json", "festivals_registry_extension.json",
    "festivals_sources.json", "festivals_musique.json", "festivals_hors_musique.json",
    ".last_registry_scrub.json",
}

RE_WIKIDATA = re.compile(r"wikidata\.org/(?:wiki|entity)/(Q\d+)")


# ── HTTP ──────────────────────────────────────────────────────────────────────
def curl_json(url: str) -> dict | None:
    try:
        r = subprocess.run(
            ["curl", "-sSL", "--max-time", "20", "-H", f"User-Agent: {UA}", url],
            capture_output=True, text=True, check=True,
        )
        body = r.stdout.strip()
        if not body:
            return None
        return json.loads(body)
    except Exception as e:
        print(f"    HTTP/JSON error: {e}", file=sys.stderr)
        return None


# ── Text shaping ─────────────────────────────────────────────────────────────
def trim_sentences(text: str, max_sentences: int = 2) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    parts = re.split(r"(?<=[.!?])\s+", text)
    out = " ".join(parts[:max_sentences]).strip()
    if len(out) > MAX_LEN:
        out = out[:MAX_LEN].rsplit(" ", 1)[0].rstrip(",;:") + "…"
    return out


# ── Source A: precise MBID → Wikidata → frwiki chain ─────────────────────────
def wikidata_qid(mbid: str) -> str | None:
    data = curl_json(f"https://musicbrainz.org/ws/2/artist/{mbid}?inc=url-rels&fmt=json")
    if not data:
        return None
    for rel in data.get("relations", []):
        url = (rel.get("url") or {}).get("resource") or ""
        if m := RE_WIKIDATA.search(url):
            return m.group(1)
    return None


def wikidata_fr(qid: str) -> tuple[str | None, str | None]:
    qs = urllib.parse.urlencode({
        "action": "wbgetentities", "ids": qid, "format": "json",
        "props": "sitelinks|descriptions", "languages": "fr", "sitefilter": "frwiki",
    })
    data = curl_json(f"https://www.wikidata.org/w/api.php?{qs}")
    if not data:
        return None, None
    ent = (data.get("entities") or {}).get(qid) or {}
    title = ((ent.get("sitelinks") or {}).get("frwiki") or {}).get("title")
    desc = ((ent.get("descriptions") or {}).get("fr") or {}).get("value")
    return title, desc


def wikipedia_fr_summary(title: str) -> dict | None:
    t = urllib.parse.quote(title.replace(" ", "_"), safe="")
    return curl_json(f"https://fr.wikipedia.org/api/rest_v1/page/summary/{t}")


def resolve_via_mbid(mbid: str) -> str | None:
    qid = wikidata_qid(mbid)
    time.sleep(SLEEP_MB)
    if not qid:
        return None
    title, wd_desc = wikidata_fr(qid)
    time.sleep(SLEEP_WIKI)
    if title:
        data = wikipedia_fr_summary(title)
        time.sleep(SLEEP_WIKI)
        if data and data.get("type") != "disambiguation":
            extract = (data.get("extract") or "").strip()
            if len(extract) >= 30:
                return trim_sentences(extract)
    if wd_desc:
        d = wd_desc.strip()
        d = d[0].upper() + d[1:] if d else d
        if d and d[-1] not in ".!?":
            d += "."
        return d or None
    return None


# ── Source B: naive frwiki-by-name fallback (no MBID) ────────────────────────
def resolve_via_name(name: str) -> str | None:
    data = wikipedia_fr_summary(name)
    time.sleep(SLEEP_WIKI)
    if not data or data.get("type") == "disambiguation":
        return None
    extract = (data.get("extract") or "").strip()
    if len(extract) < 30:
        return None
    return trim_sentences(extract)


# ── Festival I/O ─────────────────────────────────────────────────────────────
def festival_id(path: Path) -> str:
    stem = path.name[:-len(".json")] if path.name.endswith(".json") else path.name
    if stem.startswith("running_order_"):
        stem = stem[len("running_order_"):]
    return stem.replace("_", "-")


def load_all_festivals() -> dict[str, tuple[Path, dict]]:
    out: dict[str, tuple[Path, dict]] = {}
    for src in sorted(glob.glob(str(PUBLIC / "*.json"))):
        p = Path(src)
        if p.name in SKIP_BASENAMES:
            continue
        try:
            data = json.loads(p.read_text())
        except Exception:
            continue
        if not isinstance(data, dict) or "concerts" not in data:
            continue
        out[festival_id(p)] = (p, data)
    return out


def collect_cache(all_festivals: dict[str, tuple[Path, dict]], force: bool) -> dict[str, str]:
    cache: dict[str, str] = {}
    if force:
        return cache
    for _, (_, data) in all_festivals.items():
        for c in data.get("concerts", []):
            d = c.get("description")
            if isinstance(d, str) and d.strip():
                cache.setdefault(c["artist"], d)
    return cache


def mbid_by_artist(all_festivals: dict[str, tuple[Path, dict]]) -> dict[str, str]:
    out: dict[str, str] = {}
    for _, (_, data) in all_festivals.items():
        for c in data.get("concerts", []):
            if c.get("musicBrainzId"):
                out.setdefault(c["artist"], c["musicBrainzId"])
    return out


def apply_to_festival(data: dict, cache: dict[str, str]) -> bool:
    """Write resolved descriptions; never leave/create a null. Returns True if changed."""
    changed = False
    for c in data["concerts"]:
        desc = cache.get(c["artist"])
        if desc:
            if c.get("description") != desc:
                c["description"] = desc
                changed = True
        elif "description" in c and c["description"] is None:
            del c["description"]  # nettoie les null hérités — on n'écrit JAMAIS null
            changed = True
    return changed


def atomic_write_json(path: Path, data: dict) -> None:
    tmp = path.with_name(path.name + ".tmp")
    tmp.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    try:
        json.loads(tmp.read_text())  # validation obligatoire avant remplacement
    except Exception as e:
        tmp.unlink(missing_ok=True)
        raise RuntimeError(f"VALIDATION JSON KO sur {path}: {e}. Fichier non modifié.")
    os.replace(tmp, path)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--force", action="store_true", help="re-resolve every artist")
    ap.add_argument("--festival", help="restrict to one festival id")
    args = ap.parse_args()

    all_festivals = load_all_festivals()
    if args.festival:
        if args.festival not in all_festivals:
            print(f"Unknown festival id: {args.festival}", file=sys.stderr)
            return 1
        all_festivals = {args.festival: all_festivals[args.festival]}

    cache = collect_cache(all_festivals, args.force)
    mbids = mbid_by_artist(all_festivals)

    unique: set[str] = set()
    for _, (_, data) in all_festivals.items():
        for c in data["concerts"]:
            unique.add(c["artist"])

    todo = sorted(a for a in unique if a not in cache)
    with_mbid = sum(1 for a in todo if a in mbids)

    print(f"{len(unique)} unique artists across {len(all_festivals)} festivals")
    print(f"  described (skipped): {len(cache)}")
    print(f"  to resolve:          {len(todo)} ({with_mbid} via MBID, {len(todo) - with_mbid} by name)")
    if todo:
        eta_s = with_mbid * (SLEEP_MB + 2 * SLEEP_WIKI) + (len(todo) - with_mbid) * SLEEP_WIKI
        print(f"  estimated time:      ~{int(eta_s / 60)} min {int(eta_s % 60)} s\n")

    n_found = 0
    for i, artist in enumerate(todo, 1):
        desc = resolve_via_mbid(mbids[artist]) if artist in mbids else resolve_via_name(artist)
        if desc:
            cache[artist] = desc
            n_found += 1
            preview = desc if len(desc) <= 60 else desc[:57] + "…"
            print(f"  [{i:>4}/{len(todo)}] ✓ {artist:<40} {preview}")
        else:
            print(f"  [{i:>4}/{len(todo)}] ✗ {artist:<40} (no FR bio)")

    n_written = 0
    for fid, (path, data) in all_festivals.items():
        if apply_to_festival(data, cache):
            atomic_write_json(path, data)
            asset = ASSETS / f"{fid}.json"
            if asset.exists():  # mirror only the ~10 app festivals
                asset.write_bytes(path.read_bytes())
            n_written += 1

    print(f"\ndescriptions: {n_found} new, {len(todo) - n_found} not found, {n_written} files updated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
