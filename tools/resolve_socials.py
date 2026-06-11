#!/usr/bin/env python3
"""
Resolve streaming + social links for every artist across every shipped festival
via the MusicBrainz API. One MusicBrainz call yields every URL relationship the
community has curated for an artist, so we extract all providers in one shot:

  Music   : Spotify, Deezer, Apple Music, Qobuz, Tidal, YouTube Music
  Social  : Instagram, Facebook, X (Twitter), TikTok

On top of MusicBrainz we resolve one extra YouTube field that MB never stores:
  - youtubeMusicChannelId : the artist channel id (UC…) — works for both
    music.youtube.com/channel/<id> and youtube.com/channel/<id>. Pulled from
    MB url-rels; if MB only has an @handle / /user / /c URL we fetch that page
    once and read its canonical channel id.
  - youtubeVideoId        : a representative video (11-char id) taken from the
    artist's OWN uploads — only resolved when we have a youtubeMusicChannelId,
    so we never blind-search and risk attaching the wrong artist's video. The
    uploads playlist of a channel UC… is UU… (same suffix); we sample the first
    few entries (falling back to the channel /videos tab) and, via the YouTube
    oEmbed endpoint, PREFER a video that allows embedding and is not a Vevo
    upload — lyric videos, Topic auto-generated tracks, official lives, the
    band's own uploads — over Vevo clips (often embed-disabled / branded), so
    the chosen video can actually be played inline in the app.

The MusicBrainzId is stored too so we never re-query an artist that already
resolved on a prior run (artists frequently appear in 2+ festivals).

Free, unauthenticated APIs, MusicBrainz rate-limited at 1 req/sec — we sleep
1.1 s between calls.

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
OEMBED_SLEEP = 0.3  # polite gap between YouTube oEmbed embeddability probes

# Daily safety cap on NEW artists resolved per run, to stay well under any
# per-day request ceiling (MB courtesy + YouTube scraping). Only never-resolved
# artists land in the to-do list, so the budget is always spent on the artists
# that have no id yet; any overflow rolls to the next day's run. `--force` and
# `--max 0` lift the cap (used for one-off backfills).
MAX_PER_RUN = 100

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
    "poney-club-2026":    "poney-club-2026.json",
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
# YouTube: a channel id (UC…) is shared between youtube.com and music.youtube.com.
RE_YTMUSIC   = re.compile(r"music\.youtube\.com/channel/(UC[A-Za-z0-9_-]+)")
RE_YTCHANNEL = re.compile(r"youtube\.com/channel/(UC[A-Za-z0-9_-]+)")
RE_YTHANDLE  = re.compile(r"youtube\.com/(@[A-Za-z0-9._-]+|user/[A-Za-z0-9._-]+|c/[A-Za-z0-9._-]+)")
RE_YTVIDEO   = re.compile(r'"videoId":"([A-Za-z0-9_-]{11})"')

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


# Desktop UA + a pre-accepted consent cookie so YouTube serves the real page
# instead of the EU consent interstitial (which carries no videoIds / channelId).
YT_UA = ("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
         "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Safari/537.36")
YT_HEADERS = ["-H", f"User-Agent: {YT_UA}", "-H", "Cookie: CONSENT=YES+1",
              "-H", "Accept-Language: fr-FR,fr;q=0.9,en;q=0.8"]


def curl_text(url: str, headers: list[str] | None = None) -> str | None:
    try:
        r = subprocess.run(
            ["curl", "-sSL", "--max-time", "20", *(headers or []), url],
            capture_output=True, text=True, check=True,
        )
        return r.stdout or None
    except Exception as e:
        print(f"    HTTP error: {e}", file=sys.stderr)
        return None


def youtube_video_candidates(channel_id: str, limit: int = 6) -> list[str]:
    """First `limit` unique videoIds from the channel's own uploads, newest-first.

    The uploads playlist of any channel UC… is UU… (identical suffix). We read
    the playlist page, falling back to the channel /videos tab. Sampling from
    the artist's own channel avoids the wrong-video risk of a blind search.
    """
    uploads = "UU" + channel_id[2:]
    seen: list[str] = []
    for url in (f"https://www.youtube.com/playlist?list={uploads}",
                f"https://www.youtube.com/channel/{channel_id}/videos"):
        html = curl_text(url, YT_HEADERS)
        if html:
            for m in RE_YTVIDEO.finditer(html):
                vid = m.group(1)
                if vid not in seen:
                    seen.append(vid)
                if len(seen) >= limit:
                    return seen
        if seen:
            break
    return seen


def youtube_oembed(video_id: str) -> tuple[int, dict | None]:
    """(http_status, json) from YouTube's oEmbed endpoint for a video.

    oEmbed returns 200 + JSON when the video is embeddable, and 401 ("Embedding
    disabled by request") when it is not (404 for private/removed). This is the
    canonical, cheap embeddability probe. The JSON carries `author_name` (the
    channel name) which we use to spot Vevo uploads.
    """
    target = f"https://www.youtube.com/watch?v={video_id}"
    url = "https://www.youtube.com/oembed?format=json&url=" + urllib.parse.quote(target, safe="")
    try:
        r = subprocess.run(
            ["curl", "-s", "-w", "\n%{http_code}", "--max-time", "15", *YT_HEADERS, url],
            capture_output=True, text=True, check=True,
        )
        out = r.stdout or ""
        nl = out.rfind("\n")
        code = int((out[nl + 1:] if nl >= 0 else out).strip() or 0)
        body = out[:nl] if nl >= 0 else ""
        data = None
        if code == 200 and body:
            try:
                data = json.loads(body)
            except Exception:
                data = None
        return code, data
    except Exception:
        return 0, None


def youtube_video_from_channel(channel_id: str) -> str | None:
    """A representative, embeddable video id from the channel's own uploads.

    Among the first few uploads (newest-first) we prefer, in order:
      1. an embeddable video (oEmbed 200) that is NOT a Vevo upload — lyric
         videos, Topic auto-generated tracks, official lives, the band's own
         uploads;
      2. failing that, the first embeddable video (even if Vevo);
      3. failing that, the first upload overall, so the field is still
         populated when nothing clean is available (non-regression).
    """
    candidates = youtube_video_candidates(channel_id)
    if not candidates:
        return None
    first_embeddable: str | None = None
    for vid in candidates:
        code, data = youtube_oembed(vid)
        time.sleep(OEMBED_SLEEP)
        if code != 200:
            continue  # embedding disabled / private / removed
        if first_embeddable is None:
            first_embeddable = vid
        author = ((data or {}).get("author_name") or "").lower()
        if "vevo" not in author:
            return vid  # embeddable AND not Vevo → best pick
    return first_embeddable or candidates[0]


def youtube_channel_from_handle(handle_url: str) -> str | None:
    """Fetch a @handle / /user / /c channel page and read its canonical UC id."""
    html = curl_text(handle_url, YT_HEADERS)
    if not html:
        return None
    m = RE_YTCHANNEL.search(html)
    return m.group(1) if m else None


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
    "musicBrainzId":         None,
    "spotifyArtistId":       None,
    "deezerArtistId":        None,
    "appleMusicArtistId":    None,
    "qobuzArtistId":         None,
    "tidalArtistId":         None,
    "youtubeMusicChannelId": None,
    "youtubeVideoId":        None,
    "instagramHandle":       None,
    "facebookHandle":        None,
    "twitterHandle":         None,
    "tiktokHandle":          None,
}


def lookup_links(mbid: str) -> dict[str, str | int | None]:
    out: dict[str, str | int | None] = {**EMPTY, "musicBrainzId": mbid}
    data = curl_json(f"https://musicbrainz.org/ws/2/artist/{mbid}?inc=url-rels&fmt=json")
    if not data:
        return out
    yt_handle_url: str | None = None  # fallback if MB only stores @handle / /user / /c
    for rel in data.get("relations", []):
        url = (rel.get("url") or {}).get("resource") or ""
        if not url:
            continue
        # YouTube channel id (UC…) — music.youtube.com first, then plain youtube.com.
        if out["youtubeMusicChannelId"] is None:
            if (m := RE_YTMUSIC.search(url)) or (m := RE_YTCHANNEL.search(url)):
                out["youtubeMusicChannelId"] = m.group(1)
            elif yt_handle_url is None and RE_YTHANDLE.search(url):
                yt_handle_url = url
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
    # MB had a YouTube @handle / /user / /c but no channel id — resolve it once.
    if out["youtubeMusicChannelId"] is None and yt_handle_url:
        cid = youtube_channel_from_handle(yt_handle_url)
        time.sleep(SLEEP_BETWEEN_CALLS)
        if cid:
            out["youtubeMusicChannelId"] = cid
    # Representative video — only when we have the artist's own channel.
    if out["youtubeMusicChannelId"]:
        vid = youtube_video_from_channel(out["youtubeMusicChannelId"])
        time.sleep(SLEEP_BETWEEN_CALLS)
        if vid:
            out["youtubeVideoId"] = vid
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
    ap.add_argument("--max", type=int, default=MAX_PER_RUN,
                    help=f"max NEW artists resolved this run (default {MAX_PER_RUN}; "
                         "0 = unlimited). Overflow rolls to the next run.")
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

    # Daily cap: never-resolved artists (no id yet) are already the only ones in
    # `todo`; we just bound how many we attempt per run so a big batch can't blow
    # past request limits. The rest roll to tomorrow. --force lifts the cap.
    cap = 0 if args.force else max(0, args.max)
    deferred = 0
    if cap and len(todo) > cap:
        deferred = len(todo) - cap
        todo = todo[:cap]

    print(f"{len(unique_artists)} unique artists across {len(all_festivals)} festivals")
    print(f"  cached (skipped):  {len(cache)}")
    print(f"  to resolve now:    {len(todo)}"
          + (f"  (cap {cap} — {deferred} deferred to next run)" if deferred else ""))
    if todo:
        eta_s = len(todo) * 3 * SLEEP_BETWEEN_CALLS  # ~3 calls/artist (search + rels + uploads)
        print(f"  estimated time:    ~{int(eta_s/60)} min {int(eta_s%60)} s\n")

    for i, artist in enumerate(todo, 1):
        mbid = search_mbid(artist)
        time.sleep(SLEEP_BETWEEN_CALLS)
        if not mbid:
            cache[artist] = {**EMPTY}
            print(f"  [{i:>3}/{len(todo)}] ✗ {artist:<48} (no MBID)")
            continue
        links = lookup_links(mbid)  # MB providers + YouTube channel + video
        time.sleep(SLEEP_BETWEEN_CALLS)
        cache[artist] = links
        marks = "".join([
            "S" if links["spotifyArtistId"]       else "·",
            "D" if links["deezerArtistId"]        else "·",
            "A" if links["appleMusicArtistId"]    else "·",
            "Q" if links["qobuzArtistId"]         else "·",
            "T" if links["tidalArtistId"]         else "·",
            "Y" if links["youtubeMusicChannelId"] else "·",
            "V" if links["youtubeVideoId"]        else "·",
            "I" if links["instagramHandle"]       else "·",
            "F" if links["facebookHandle"]        else "·",
            "X" if links["twitterHandle"]         else "·",
            "K" if links["tiktokHandle"]          else "·",
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
              f"T={stats['tidalArtistId']} "
              f"Y={stats['youtubeMusicChannelId']} V={stats['youtubeVideoId']} | "
              f"I={stats['instagramHandle']} F={stats['facebookHandle']} "
              f"X={stats['twitterHandle']} K={stats['tiktokHandle']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
