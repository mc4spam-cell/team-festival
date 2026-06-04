#!/usr/bin/env python3
"""
Resolve Spotify / Deezer / Instagram artist links via the MusicBrainz API.

MusicBrainz provides a single endpoint that links each artist to their socials
("free streaming" → Spotify, Deezer / "social network" → Instagram). Free,
unauthenticated, rate-limited at 1 req/sec (we sleep 1.1s between calls).

Adds these fields to each concert in running_order.json:
  - musicBrainzId   (string, UUID)
  - spotifyArtistId (string, Spotify's 22-char base62 id)
  - deezerArtistId  (int)
  - instagramHandle (string, the part after instagram.com/)

Idempotent: only queries artists that don't have a musicBrainzId yet (use
--force to re-resolve everything).

Usage:
  python3 tools/resolve_socials.py [--force]
"""
from __future__ import annotations
import argparse
import json
import re
import subprocess
import sys
import time
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JSON_PATH = ROOT / "running_order.json"
ASSET_PATH = ROOT / "app/src/main/assets/running_order.json"

UA = "Hellfest2026App/1.0 ( https://github.com/local )"
SLEEP_BETWEEN_CALLS = 1.1   # MB allows 1 req/s per IP, be polite

SPOTIFY_ARTIST_RE = re.compile(r"open\.spotify\.com/(?:intl-[a-z]+/)?artist/([A-Za-z0-9]+)")
DEEZER_ARTIST_RE = re.compile(r"deezer\.com/(?:[a-z]{2}/)?artist/(\d+)")
INSTAGRAM_RE = re.compile(r"instagram\.com/([A-Za-z0-9._]+)/?")


def curl_json(url: str) -> dict | None:
    try:
        r = subprocess.run(
            ["curl", "-sSL", "--max-time", "15", "-H", f"User-Agent: {UA}", url],
            capture_output=True, text=True, check=True,
        )
        body = r.stdout.strip()
        if not body:
            return None
        return json.loads(body)
    except Exception as e:
        print(f"    HTTP/JSON error: {e}", file=sys.stderr)
        return None


def search_mbid(artist: str) -> str | None:
    qs = urllib.parse.urlencode({"query": artist, "limit": 1, "fmt": "json"})
    data = curl_json(f"https://musicbrainz.org/ws/2/artist/?{qs}")
    if not data:
        return None
    artists = data.get("artists", [])
    if not artists:
        return None
    top = artists[0]
    # Score 100 = exact name match; require >=85 to avoid wrong matches
    if top.get("score", 0) < 85:
        return None
    return top.get("id")


def lookup_socials(mbid: str) -> dict[str, str | int | None]:
    data = curl_json(f"https://musicbrainz.org/ws/2/artist/{mbid}?inc=url-rels&fmt=json")
    out: dict[str, str | int | None] = {
        "spotifyArtistId": None,
        "deezerArtistId": None,
        "instagramHandle": None,
    }
    if not data:
        return out
    for rel in data.get("relations", []):
        url = (rel.get("url") or {}).get("resource") or ""
        if not url:
            continue
        if out["spotifyArtistId"] is None:
            m = SPOTIFY_ARTIST_RE.search(url)
            if m:
                out["spotifyArtistId"] = m.group(1)
        if out["deezerArtistId"] is None:
            m = DEEZER_ARTIST_RE.search(url)
            if m:
                out["deezerArtistId"] = int(m.group(1))
        if out["instagramHandle"] is None:
            m = INSTAGRAM_RE.search(url)
            if m:
                handle = m.group(1)
                if handle and handle.lower() not in {"explore", "p", "reel"}:
                    out["instagramHandle"] = handle
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--force", action="store_true", help="re-resolve even cached artists")
    args = ap.parse_args()

    data = json.loads(JSON_PATH.read_text())
    concerts = data["concerts"]

    # Cache by artist name (artists play once but defensive)
    cache: dict[str, dict] = {}
    if not args.force:
        for c in concerts:
            if c.get("musicBrainzId"):
                cache[c["artist"]] = {
                    "musicBrainzId": c["musicBrainzId"],
                    "spotifyArtistId": c.get("spotifyArtistId"),
                    "deezerArtistId": c.get("deezerArtistId"),
                    "instagramHandle": c.get("instagramHandle"),
                }

    unique_artists = sorted({c["artist"] for c in concerts})
    todo = [a for a in unique_artists if a not in cache]
    print(f"{len(unique_artists)} artists total, {len(cache)} cached, {len(todo)} to resolve\n")

    for artist in todo:
        mbid = search_mbid(artist)
        time.sleep(SLEEP_BETWEEN_CALLS)
        if not mbid:
            print(f"  ✗ {artist:<40} (no MBID match)")
            cache[artist] = {
                "musicBrainzId": None, "spotifyArtistId": None,
                "deezerArtistId": None, "instagramHandle": None,
            }
            continue
        socials = lookup_socials(mbid)
        time.sleep(SLEEP_BETWEEN_CALLS)
        cache[artist] = {"musicBrainzId": mbid, **socials}
        marks = "".join([
            "S" if socials["spotifyArtistId"] else "·",
            "D" if socials["deezerArtistId"] else "·",
            "I" if socials["instagramHandle"] else "·",
        ])
        print(f"  ✓ {artist:<40} [{marks}]")

    for c in concerts:
        entry = cache.get(c["artist"], {})
        c["musicBrainzId"] = entry.get("musicBrainzId")
        c["spotifyArtistId"] = entry.get("spotifyArtistId")
        c["deezerArtistId"] = entry.get("deezerArtistId")
        c["instagramHandle"] = entry.get("instagramHandle")

    JSON_PATH.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    ASSET_PATH.write_bytes(JSON_PATH.read_bytes())

    def stat(field: str) -> int:
        return sum(1 for c in concerts if c.get(field))
    print(f"\nResolved: MB={stat('musicBrainzId')}  Spotify={stat('spotifyArtistId')}  Deezer={stat('deezerArtistId')}  Instagram={stat('instagramHandle')}")
    print(f"Total concerts: {len(concerts)}")
    print(f"\nWritten: {JSON_PATH}")
    print(f"Mirror : {ASSET_PATH}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
