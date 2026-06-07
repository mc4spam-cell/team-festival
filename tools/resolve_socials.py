#!/usr/bin/env python3
"""
Resolve streaming + social links for every artist across every shipped festival
via the MusicBrainz API. One MusicBrainz call yields every URL relationship the
community has curated for an artist, so we extract all 9 providers in one shot:

  Music   : Spotify, Deezer, Apple Music, Qobuz, Tidal
  Social  : Instagram, Facebook, X (Twitter), TikTok

The MusicBrainzId is stored too so we never re-query an artist that already
resolved on a prior run (artists frequently appear in 2+ festivals).

Free, unauthenticated API, rate-limited at 1 req/sec — we sleep 1.1 s between
calls.

Output (in-place edits):
  - public/festivals/<id>.json           — source of truth
  - app/src/main/assets/festivals/<id>.json — Android mirror

Usage:
  python3 tools/resolve_socials.py [--force] [--festival <id>]

  --force     : re-resolve everyone, even artists already cached
  --festival  : restrict to a single festival id (default = all shipped 9)
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
PUBLIC = ROOT / "public" / "festivals"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "festivals"

UA = "MaTeamHF2026App/1.0 ( https://github.com/local )"
SLEEP_BETWEEN_CALLS = 1.1  # MusicBrainz: 1 req/s per IP

# Which festival ids ship to the Android app, with the matching source filename
# in public/festivals/. Keep this list in lockstep with assets/festivals/index.json.
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

# URL extraction regexes — designed to be loose on path but strict on host.
RE_SPOTIFY   = re.compile(r"open\.spotify\.com/(?:intl-[a-z]+/)?artist/([A-Za-z0-9]+)")
RE_DEEZER    = re.compile(r"deezer\.com/(?:[a-z]{2}/)?artist/(\d+)")
RE_APPLE     = re.compile(r"music\.apple\.com/(?:[a-z]{2}/)?artist/(?:[^/]+/)?(\d+)")
RE_QOBUZ     = re.compile(r"qobuz\.com/.*?/interpreter/[^/]+/(\d+)")
RE_TIDAL     = re.compile(r"tidal\.com/(?:browse/)?artist/(\d+)")
RE_INSTAGRAM = re.compile(r"instagram\.com/([A-Za-z0-9._]+)/?")
RE_FACEBOOK  = re.compile(r"facebook\.com/(?!people/|pages/)([A-Za-z0-9.\-_]+)/?")
RE_TWITTER   = re.compile(r"(?:twitter\.com|x\.com)/(?!i/|home|search)([A-Za-z0-9_]+)/?")
RE_TIKTOK    = re.compile(r"tiktok\.com/@([A-Za-z0-9._]+)/?")

# Anything appearing in these path components is a navigation artefact, not a handle
SOCIAL_NOISE = {"explore", "p", "reel", "share", "i", "home", "search", "settings"}


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


def search_mbid(artist: str) -> str | None:
    qs = urllib.parse.urlencode({"query": artist, "limit": 1, "fmt": "json"})
    data = curl_json(f"https://musicbrainz.org/ws/2/artist/?{qs}")
    if not data:
        return None
    artists = data.get("artists", [])
    if not artists:
        return None
    top = artists[0]
    if top.get("score", 0) < 85:
        return None
    return top.get("id")


EMPTY: dict[str, str | int | None] = {
    "musicBrainzId":      None,
    "spotifyArtistId":    None,
    "deezerArtistId":     None,
    "appleMusicArtistId": None,
    "qobuzArtistId":      None,
    "tidalArtistId":      None,
    "instagramHandle":    None,
    "facebookHandle":     None,
    "twitterHandle":      None,
    "tiktokHandle":       None,
}


def lookup_links(mbid: str) -> dict[str, str | int | None]:
    out: dict[str, str | int | None] = {**EMPTY, "musicBrainzId": mbid}
    data = curl_json(f"https://musicbrainz.org/ws/2/artist/{mbid}?inc=url-rels&fmt=json")
    if not data:
        return out
    for rel in data.get("relations", []):
        url = (rel.get("url") or {}).get("resource") or ""
        if not url:
            continue
        if out["spotifyArtistId"] is None and (m := RE_SPOTIFY.search(url)):
            out["spotifyArtistId"] = m.group(1)
        if out["deezerArtistId"] is None and (m := RE_DEEZER.search(url)):
            out["deezerArtistId"] = int(m.group(1))
        if out["appleMusicArtistId"] is None and (m := RE_APPLE.search(url)):
            out["appleMusicArtistId"] = int(m.group(1))
        if out["qobuzArtistId"] is None and (m := RE_QOBUZ.search(url)):
            out["qobuzArtistId"] = m.group(1)
        if out["tidalArtistId"] is None and (m := RE_TIDAL.search(url)):
            out["tidalArtistId"] = m.group(1)
        if out["instagramHandle"] is None and (m := RE_INSTAGRAM.search(url)):
            h = m.group(1)
            if h and h.lower() not in SOCIAL_NOISE:
                out["instagramHandle"] = h
        if out["facebookHandle"] is None and (m := RE_FACEBOOK.search(url)):
            h = m.group(1)
            if h and h.lower() not in SOCIAL_NOISE:
                out["facebookHandle"] = h
        if out["twitterHandle"] is None and (m := RE_TWITTER.search(url)):
            h = m.group(1)
            if h and h.lower() not in SOCIAL_NOISE:
                out["twitterHandle"] = h
        if out["tiktokHandle"] is None and (m := RE_TIKTOK.search(url)):
            h = m.group(1)
            if h and h.lower() not in SOCIAL_NOISE:
                out["tiktokHandle"] = h
    return out


def load_all_festivals() -> dict[str, tuple[Path, dict]]:
    """fid → (path, parsed json). Sorted alphabetically for reproducibility."""
    out: dict[str, tuple[Path, dict]] = {}
    for fid, filename in SHIPPED_FESTIVALS.items():
        p = PUBLIC / filename
        if not p.exists():
            print(f"  ⚠️  {p} missing — skipping {fid}", file=sys.stderr)
            continue
        out[fid] = (p, json.loads(p.read_text()))
    return out


def collect_cache(all_festivals: dict[str, tuple[Path, dict]], force: bool) -> dict[str, dict]:
    """Aggregate already-resolved artists across every festival → name → fields dict."""
    cache: dict[str, dict] = {}
    if force:
        return cache
    for _, (_, data) in all_festivals.items():
        for c in data.get("concerts", []):
            artist = c["artist"]
            if not c.get("musicBrainzId"):
                continue
            # If we've already seen this artist in another festival, keep the richer record
            existing = cache.get(artist) or {}
            merged = {**existing}
            for k in EMPTY:
                v_new = c.get(k)
                if v_new and not merged.get(k):
                    merged[k] = v_new
            cache[artist] = merged
    return cache


def apply_to_festival(data: dict, cache: dict[str, dict]) -> None:
    for c in data["concerts"]:
        entry = cache.get(c["artist"])
        if not entry:
            continue
        for k in EMPTY:
            if entry.get(k) is not None:
                c[k] = entry[k]
            elif k not in c:
                c[k] = None


def mirror_to_assets(fid: str, public_path: Path) -> None:
    target = ASSETS / f"{fid}.json"
    target.write_bytes(public_path.read_bytes())


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

    unique_artists: set[str] = set()
    for _, (_, data) in all_festivals.items():
        for c in data["concerts"]:
            unique_artists.add(c["artist"])
    todo = sorted(unique_artists - set(cache.keys()))

    print(f"{len(unique_artists)} unique artists across {len(all_festivals)} festivals")
    print(f"  cached (skipped):  {len(cache)}")
    print(f"  to resolve:        {len(todo)}")
    if todo:
        eta_s = len(todo) * 2 * SLEEP_BETWEEN_CALLS
        print(f"  estimated time:    ~{int(eta_s/60)} min {int(eta_s%60)} s\n")

    for i, artist in enumerate(todo, 1):
        mbid = search_mbid(artist)
        time.sleep(SLEEP_BETWEEN_CALLS)
        if not mbid:
            cache[artist] = {**EMPTY}
            print(f"  [{i:>3}/{len(todo)}] ✗ {artist:<48} (no MBID)")
            continue
        links = lookup_links(mbid)
        time.sleep(SLEEP_BETWEEN_CALLS)
        cache[artist] = links
        marks = "".join([
            "S" if links["spotifyArtistId"]    else "·",
            "D" if links["deezerArtistId"]     else "·",
            "A" if links["appleMusicArtistId"] else "·",
            "Q" if links["qobuzArtistId"]      else "·",
            "T" if links["tidalArtistId"]      else "·",
            "I" if links["instagramHandle"]    else "·",
            "F" if links["facebookHandle"]     else "·",
            "X" if links["twitterHandle"]      else "·",
            "K" if links["tiktokHandle"]       else "·",
        ])
        print(f"  [{i:>3}/{len(todo)}] ✓ {artist:<48} [{marks}]")

    print()
    for fid, (path, data) in all_festivals.items():
        apply_to_festival(data, cache)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
        mirror_to_assets(fid, path)
        n = len(data["concerts"])
        stats = {k: sum(1 for c in data["concerts"] if c.get(k)) for k in EMPTY}
        print(f"  ✓ {fid}: {n} concerts | MB={stats['musicBrainzId']} "
              f"S={stats['spotifyArtistId']} D={stats['deezerArtistId']} "
              f"A={stats['appleMusicArtistId']} Q={stats['qobuzArtistId']} "
              f"T={stats['tidalArtistId']} | "
              f"I={stats['instagramHandle']} F={stats['facebookHandle']} "
              f"X={stats['twitterHandle']} K={stats['tiktokHandle']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
