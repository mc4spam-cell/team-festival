#!/usr/bin/env python3
"""
Resolve Apple Music artist IDs via the public iTunes Search API and
write them back into running_order.json (root + assets copy).

Free API, no auth, ~0.2 s rate-limited. Cache by artist name so repeat runs
only query newcomers.

Usage:
    python3 tools/resolve_apple_music_ids.py [--force]
"""
import argparse
import json
import subprocess
import sys
import time
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JSON_PATH = ROOT / "running_order.json"
ASSET_PATH = ROOT / "app/src/main/assets/running_order.json"


def search_artist(artist: str, retries: int = 4) -> tuple[int | None, str | None]:
    """Return (artistId, canonicalName) or (None, None). Retries empty bodies."""
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
            result = subprocess.run(
                ["curl", "-sS", "--max-time", "10", url],
                capture_output=True, text=True, check=True,
            )
            body = result.stdout.strip()
            if not body:
                raise ValueError("empty body")
            payload = json.loads(body)
        except Exception as e:
            if attempt == retries - 1:
                print(f"    HTTP error after {retries} tries: {e}", file=sys.stderr)
                return None, None
            time.sleep(backoff)
            backoff *= 2
            continue
        results = payload.get("results", [])
        if not results:
            return None, None
        top = results[0]
        return top.get("artistId"), top.get("artistName")
    return None, None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--force", action="store_true",
                    help="re-query even artists that already have an id")
    args = ap.parse_args()

    data = json.loads(JSON_PATH.read_text())

    # Cache by artist name to avoid re-querying duplicates
    cache: dict[str, int | None] = {}
    if not args.force:
        for c in data["concerts"]:
            aid = c.get("appleMusicArtistId")
            if aid is not None:
                cache[c["artist"]] = aid

    total = len(data["concerts"])
    unique_artists = sorted({c["artist"] for c in data["concerts"]})
    print(f"{len(unique_artists)} unique artists across {total} concerts")
    print(f"{len(cache)} already cached, {len(unique_artists) - len(cache)} to query\n")

    new_lookups = 0
    for artist in unique_artists:
        if artist in cache:
            continue
        new_lookups += 1
        aid, canonical = search_artist(artist)
        cache[artist] = aid
        marker = "✓" if aid else "✗"
        match_note = f"  (matched: {canonical})" if canonical and canonical.lower() != artist.lower() else ""
        print(f"  {marker} {artist:<45} -> {aid}{match_note}")
        time.sleep(1.0)

    # Write IDs back into every concert
    for c in data["concerts"]:
        c["appleMusicArtistId"] = cache.get(c["artist"])

    JSON_PATH.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    ASSET_PATH.write_bytes(JSON_PATH.read_bytes())

    resolved = sum(1 for c in data["concerts"] if c["appleMusicArtistId"] is not None)
    print(f"\n{resolved}/{total} concerts have an Apple Music artist id")
    print(f"({new_lookups} new lookups this run)")
    print(f"\nWritten: {JSON_PATH}")
    print(f"Mirror : {ASSET_PATH}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
