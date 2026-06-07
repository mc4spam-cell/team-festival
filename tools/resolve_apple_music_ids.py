#!/usr/bin/env python3
"""
Resolve Apple Music artist IDs via the public iTunes Search API for every
shipped festival, dedup'd across festivals so each artist is queried once.

Free API, no auth. We add a small inter-call sleep just to be polite.

Output (in-place edits):
  - public/festivals/<id>.json           — source of truth
  - app/src/main/assets/festivals/<id>.json — Android mirror

Usage:
  python3 tools/resolve_apple_music_ids.py [--force] [--festival <id>]
"""
import argparse
import json
import subprocess
import sys
import time
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PUBLIC = ROOT / "public" / "festivals"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "festivals"

SHIPPED_FESTIVALS = {
    "hellfest-2026":      "hellfest-2026.json",
    "beauregard-2026":    "running_order_beauregard_2026.json",
    "bourges-2026":       "running_order_bourges_2026.json",
    "eurockeennes-2026":  "running_order_eurockeennes_2026.json",
    "interceltique-2026": "running_order_interceltique_2026.json",
    "jazzavienne-2026":   "running_order_jazzavienne_2026.json",
    "mainsquare-2026":    "running_order_mainsquare_2026.json",
    "musilac-2026":       "running_order_musilac_2026.json",
    "nuitdelerdre-2026":  "running_order_nuitdelerdre_2026.json",
}

SLEEP_BETWEEN_CALLS = 0.25


def search_artist(artist: str, retries: int = 4) -> int | None:
    qs = urllib.parse.urlencode({
        "term": artist,
        "entity": "musicArtist",
        "limit": 1,
        "country": "FR",
    })
    url = f"https://itunes.apple.com/search?{qs}"
    backoff = 0.5
    for attempt in range(retries):
        try:
            r = subprocess.run(
                ["curl", "-sS", "--max-time", "10", url],
                capture_output=True, text=True, check=True,
            )
            body = r.stdout.strip()
            if not body:
                raise ValueError("empty body")
            payload = json.loads(body)
        except Exception as e:
            if attempt == retries - 1:
                print(f"    HTTP error after {retries} tries: {e}", file=sys.stderr)
                return None
            time.sleep(backoff)
            backoff *= 2
            continue
        results = payload.get("results", [])
        if not results:
            return None
        return results[0].get("artistId")
    return None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--force", action="store_true", help="re-resolve every artist")
    ap.add_argument("--festival", help="restrict to one festival id")
    args = ap.parse_args()

    festivals = {}
    for fid, filename in SHIPPED_FESTIVALS.items():
        if args.festival and fid != args.festival:
            continue
        p = PUBLIC / filename
        if not p.exists():
            print(f"  ⚠️  missing: {p}", file=sys.stderr)
            continue
        festivals[fid] = (p, json.loads(p.read_text()))

    # Cache by artist name. If --force, start empty.
    cache: dict[str, int | None] = {}
    if not args.force:
        for _, (_, data) in festivals.items():
            for c in data["concerts"]:
                if c.get("appleMusicArtistId"):
                    cache[c["artist"]] = c["appleMusicArtistId"]

    unique_artists = sorted({c["artist"] for _, (_, d) in festivals.items() for c in d["concerts"]})
    todo = [a for a in unique_artists if a not in cache]
    print(f"{len(unique_artists)} unique artists | {len(cache)} cached | {len(todo)} to resolve")

    for i, artist in enumerate(todo, 1):
        artist_id = search_artist(artist)
        time.sleep(SLEEP_BETWEEN_CALLS)
        cache[artist] = artist_id
        mark = "✓" if artist_id else "✗"
        print(f"  [{i:>3}/{len(todo)}] {mark} {artist:<48} {artist_id or ''}")

    print()
    for fid, (path, data) in festivals.items():
        n_before = sum(1 for c in data["concerts"] if c.get("appleMusicArtistId"))
        for c in data["concerts"]:
            v = cache.get(c["artist"])
            if v is not None:
                c["appleMusicArtistId"] = v
            elif "appleMusicArtistId" not in c:
                c["appleMusicArtistId"] = None
        n_after = sum(1 for c in data["concerts"] if c.get("appleMusicArtistId"))
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
        (ASSETS / f"{fid}.json").write_bytes(path.read_bytes())
        print(f"  ✓ {fid}: AM filled {n_before} → {n_after} / {len(data['concerts'])}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
