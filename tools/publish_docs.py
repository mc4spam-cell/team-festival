#!/usr/bin/env python3
"""
Publie tous les running orders de public/festivals/ vers docs/festivals/ (GitHub
Pages), renommés avec un id canonique, et reconstruit docs/festivals/index.json.

L'app Android pull les running orders à jour au démarrage via
https://mc4spam-cell.github.io/team-festival/festivals/index.json sans attendre
une release Play Store.

L'index publié = structure de app/src/main/assets/festivals/index.json étendue
avec les festivals nouvellement détectés dans public/festivals/, plus un champ
`derniere_verification` (ISO seconde) par festival.

Aucun appel réseau ; pur copie de fichiers + génération d'index.
N'effectue PAS le git commit/push — c'est l'étape suivante du SKILL.md.

Usage: python3 tools/publish_docs.py
"""
from __future__ import annotations
import os
import re
import json
import shutil
import glob
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
os.chdir(ROOT)

os.makedirs("docs/festivals", exist_ok=True)


def slugify(name: str) -> str:
    """Nom festival → slug stable (ASCII, lowercase, kebab-case)."""
    s = name.lower()
    s = re.sub(r"[àâä]", "a", s); s = re.sub(r"[éèêë]", "e", s)
    s = re.sub(r"[îï]", "i", s); s = re.sub(r"[ôö]", "o", s)
    s = re.sub(r"[ùûü]", "u", s); s = re.sub(r"[ç]", "c", s)
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return s


def derive_dates(days) -> str:
    if not days:
        return ""
    try:
        ds = sorted(d["date"] for d in days if d.get("date"))
        if not ds:
            return ""
        if len(ds) == 1:
            return ds[0]
        return f"{ds[0]} → {ds[-1]}"
    except Exception:
        return ""


# 1. Inventaire des running orders publiables : tout fichier dans public/festivals/
#    qui contient au moins {festival, days, concerts}. On exclut l'index, le registre
#    et les rapports markdown.
shipped = {}  # festivalId → (source filepath, data)
for src in sorted(glob.glob("public/festivals/*.json")):
    basename = os.path.basename(src)
    if basename in {"index.json", "festivals_registry_200.json",
                    "festivals_registry_extension.json", "festivals_sources.json",
                    "festivals_musique.json", "festivals_hors_musique.json",
                    ".last_registry_scrub.json"}:
        continue
    try:
        data = json.load(open(src))
    except Exception:
        continue
    if not isinstance(data, dict) or "concerts" not in data:
        continue
    stem = basename.replace(".json", "")
    if stem.startswith("running_order_"):
        stem = stem[len("running_order_"):]
    fid = stem.replace("_", "-")
    shipped[fid] = (src, data)

# 2. Copie chaque running order vers docs/festivals/<id>.json (nom canonique)
times = {}
for fid, (src, _) in shipped.items():
    dp = f"docs/festivals/{fid}.json"
    shutil.copy2(src, dp)
    # Timestamp précis (ISO seconde) — permet au sync Android de détecter qu'une 2e cron
    # a tourné dans la même journée (ex. refresh à 5h00 puis resolve à 5h33 avec descriptions).
    times[fid] = datetime.fromtimestamp(os.path.getmtime(dp)).strftime("%Y-%m-%dT%H:%M:%S")

# 3. Construit le docs/festivals/index.json
#    - Festivals déjà connus de l'app : repris depuis app/src/main/assets/festivals/index.json
#    - Festivals nouveaux (running order présent mais absent de l'asset index) :
#      métadonnées dérivées du running order JSON + couleur grise par défaut
with open("app/src/main/assets/festivals/index.json") as f:
    app_index = json.load(f)
known_ids = {e["id"] for e in app_index.get("festivals", [])}
out_list = list(app_index.get("festivals", []))
for fid, (src, data) in shipped.items():
    if fid in known_ids:
        continue
    name = data.get("festival", fid)
    out_list.append({
        "id": fid,
        "name": name,
        "shortName": name[:30],
        "dates": derive_dates(data.get("days", [])),
        "location": data.get("location", "France"),
        "color": "#6E6E6E",
    })
for entry in out_list:
    entry["derniere_verification"] = times.get(entry["id"], "0000-00-00")
out_list.sort(key=lambda e: e.get("name", ""))
with open("docs/festivals/index.json", "w") as f:
    json.dump({"festivals": out_list}, f, ensure_ascii=False, indent=2)

print(f"Published {len(shipped)} running orders + index ({len(out_list)} festivals total)")
