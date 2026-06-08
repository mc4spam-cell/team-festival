#!/usr/bin/env node
/**
 * build_registry.mjs — Générateur des registres festivals pour team-festival
 * -------------------------------------------------------------------------
 * Source : Panorama des festivals (Ministère de la Culture), open data.
 *   API : https://data.culture.gouv.fr/api/explore/v2.1/catalog/datasets/panorama-des-festivals/records
 *   (données de référence 2018-2019 : identité fiable — nom, commune, département,
 *    domaine, site web — mais SANS fréquentation ni dates 2026.)
 *
 * Produit deux fichiers, au schéma du registre, dans le même dossier :
 *   - festivals_musique.json       (cible ~500 : 200 curés + complément open data)
 *   - festivals_hors_musique.json  (spectacle vivant + cinéma/doc/animé, ~800-1000)
 *
 * Les entrées déjà curées et vérifiées (festivals_registry_200.json et
 * festivals_registry_extension.json) sont placées EN TÊTE et ne sont pas écrasées ;
 * l'open data vient compléter, dédoublonné par nom normalisé.
 *
 * Usage :  node build_registry.mjs
 * Requiert Node >= 18 (fetch natif).
 */

import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const DIR = dirname(fileURLToPath(import.meta.url));
const API =
  "https://data.culture.gouv.fr/api/explore/v2.1/catalog/datasets/panorama-des-festivals/records";

// ---- Mapping domaine open data -> { liste, genre } ------------------------
const MUSIQUE = {
  "Musiques actuelles": "musiques actuelles",
  "Musiques Actuelles": "musiques actuelles",
  "Musiques classiques": "classique",
  "Pluridisciplinaire Musique": "pluridisciplinaire musique",
};
const HORS_MUSIQUE = {
  "Cinéma et audiovisuel": { domaine: "cinéma", genre: "cinéma / audiovisuel" },
  "Cirque et Arts de la rue": { domaine: "spectacle vivant", genre: "cirque / arts de la rue" },
  "Pluridisciplinaire Spectacle vivant": { domaine: "spectacle vivant", genre: "pluridisciplinaire spectacle vivant" },
  "Divers Spectacle vivant": { domaine: "spectacle vivant", genre: "spectacle vivant" },
  "Divers spectacle vivant": { domaine: "spectacle vivant", genre: "spectacle vivant" },
  "Divers Spectacle Vivant": { domaine: "spectacle vivant", genre: "spectacle vivant" },
  Danse: { domaine: "spectacle vivant", genre: "danse" },
  Théâtre: { domaine: "spectacle vivant", genre: "théâtre" },
  // Transdisciplinaire inclus par proximité (théâtre/musique/cirque mêlés)
  Transdisciplinaire: { domaine: "spectacle vivant", genre: "transdisciplinaire" },
};

// ---- Code département -> nom ---------------------------------------------
const DEPTS = {
  "01":"Ain","02":"Aisne","03":"Allier","04":"Alpes-de-Haute-Provence","05":"Hautes-Alpes",
  "06":"Alpes-Maritimes","07":"Ardèche","08":"Ardennes","09":"Ariège","10":"Aube","11":"Aude",
  "12":"Aveyron","13":"Bouches-du-Rhône","14":"Calvados","15":"Cantal","16":"Charente",
  "17":"Charente-Maritime","18":"Cher","19":"Corrèze","20":"Corse","2A":"Corse-du-Sud","2B":"Haute-Corse",
  "21":"Côte-d'Or","22":"Côtes-d'Armor","23":"Creuse","24":"Dordogne","25":"Doubs","26":"Drôme",
  "27":"Eure","28":"Eure-et-Loir","29":"Finistère","30":"Gard","31":"Haute-Garonne","32":"Gers",
  "33":"Gironde","34":"Hérault","35":"Ille-et-Vilaine","36":"Indre","37":"Indre-et-Loire","38":"Isère",
  "39":"Jura","40":"Landes","41":"Loir-et-Cher","42":"Loire","43":"Haute-Loire","44":"Loire-Atlantique",
  "45":"Loiret","46":"Lot","47":"Lot-et-Garonne","48":"Lozère","49":"Maine-et-Loire","50":"Manche",
  "51":"Marne","52":"Haute-Marne","53":"Mayenne","54":"Meurthe-et-Moselle","55":"Meuse","56":"Morbihan",
  "57":"Moselle","58":"Nièvre","59":"Nord","60":"Oise","61":"Orne","62":"Pas-de-Calais","63":"Puy-de-Dôme",
  "64":"Pyrénées-Atlantiques","65":"Hautes-Pyrénées","66":"Pyrénées-Orientales","67":"Bas-Rhin",
  "68":"Haut-Rhin","69":"Rhône","70":"Haute-Saône","71":"Saône-et-Loire","72":"Sarthe","73":"Savoie",
  "74":"Haute-Savoie","75":"Paris","76":"Seine-Maritime","77":"Seine-et-Marne","78":"Yvelines",
  "79":"Deux-Sèvres","80":"Somme","81":"Tarn","82":"Tarn-et-Garonne","83":"Var","84":"Vaucluse",
  "85":"Vendée","86":"Vienne","87":"Haute-Vienne","88":"Vosges","89":"Yonne","90":"Territoire de Belfort",
  "91":"Essonne","92":"Hauts-de-Seine","93":"Seine-Saint-Denis","94":"Val-de-Marne","95":"Val-d'Oise",
  "971":"Guadeloupe","972":"Martinique","973":"Guyane","974":"La Réunion","976":"Mayotte",
  "975":"Saint-Pierre-et-Miquelon","977":"Saint-Barthélemy","978":"Saint-Martin",
  "986":"Wallis-et-Futuna","987":"Polynésie française","988":"Nouvelle-Calédonie",
};

// ---- Helpers -------------------------------------------------------------
const SMALL = new Set(["de","des","du","la","le","les","et","en","sur","sous","aux","au","d","l","à","the","of"]);
const ACRO = new Set(["FIFO","FID","FFA","FEMA","NEXT","ADO","DARC","BD"]);

function titleCase(str) {
  if (!str) return str;
  return str
    .toLowerCase()
    .split(/(\s|-|')/)
    .map((tok, i) => {
      if (/^(\s|-|')$/.test(tok) || tok === "") return tok;
      const up = tok.toUpperCase();
      if (ACRO.has(up)) return up;
      if (i !== 0 && SMALL.has(tok)) return tok;
      return tok.charAt(0).toUpperCase() + tok.slice(1);
    })
    .join("");
}

function normName(n) {
  return (n || "")
    .toLowerCase()
    .normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\bfestival\b|\bles\b|\ble\b|\bla\b|\bde\b|\bdu\b|\bdes\b/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function normSite(s) {
  if (!s) return null;
  s = s.trim();
  if (!s) return null;
  if (!/^https?:\/\//i.test(s)) s = "http://" + s;
  return s;
}

async function fetchDomaine(value) {
  const out = [];
  let offset = 0;
  while (true) {
    const url =
      `${API}?select=nom_de_la_manifestation,commune_principale,departement,site_web` +
      `&where=domaine%3D%22${encodeURIComponent(value)}%22&limit=100&offset=${offset}`;
    const r = await fetch(url);
    if (!r.ok) throw new Error(`HTTP ${r.status} on ${value} @${offset}`);
    const j = await r.json();
    out.push(...j.results);
    if (offset + 100 >= j.total_count) break;
    offset += 100;
  }
  return out;
}

function loadCurated(file) {
  const p = join(DIR, file);
  if (!existsSync(p)) return [];
  return JSON.parse(readFileSync(p, "utf8")).festivals || [];
}

function toRow(rec, domaine, genre) {
  const dept = String(rec.departement || "").trim();
  return {
    domaine,
    nom: titleCase(rec.nom_de_la_manifestation),
    ville: titleCase(rec.commune_principale),
    departement: DEPTS[dept] ? `${DEPTS[dept]} (${dept})` : dept || null,
    genre_principal: genre,
    frequentation_estimee: null,
    dates_2026: null,
    site_officiel: normSite(rec.site_web),
    url_programmation: null,
    statut_2026: "à actualiser",
    difficulte: null,
    notes: "Source open data Panorama des festivals (réf. 2018-2019). Dates/fréquentation à actualiser.",
    derniere_verification: null,
    source: "panorama_culture_2018",
  };
}

// ---- Build ---------------------------------------------------------------
async function build() {
  // 1) MUSIQUE
  const curMusique = loadCurated("festivals_registry_200.json").map((f) => ({
    ...f, domaine: "musique", source: "curé_2026",
  }));
  const seenM = new Set(curMusique.map((f) => normName(f.nom)));
  const musiqueAdd = [];
  for (const [val, genre] of Object.entries(MUSIQUE)) {
    for (const rec of await fetchDomaine(val)) {
      const key = normName(rec.nom_de_la_manifestation);
      if (!key || seenM.has(key)) continue;
      seenM.add(key);
      musiqueAdd.push(toRow(rec, "musique", genre));
    }
  }
  musiqueAdd.sort((a, b) => a.nom.localeCompare(b.nom, "fr"));
  const musique = [...curMusique, ...musiqueAdd.slice(0, 500 - curMusique.length)]
    .map((f, i) => ({ rang: i + 1, ...f }));

  // 2) HORS MUSIQUE
  const curHM = loadCurated("festivals_registry_extension.json").map((f) => ({
    ...f, source: "curé_2026",
  }));
  const seenH = new Set(curHM.map((f) => normName(f.nom)));
  const hmAdd = [];
  for (const [val, meta] of Object.entries(HORS_MUSIQUE)) {
    for (const rec of await fetchDomaine(val)) {
      const key = normName(rec.nom_de_la_manifestation);
      if (!key || seenH.has(key)) continue;
      seenH.add(key);
      hmAdd.push(toRow(rec, meta.domaine, meta.genre));
    }
  }
  hmAdd.sort((a, b) => a.nom.localeCompare(b.nom, "fr"));
  const horsMusique = [...curHM, ...hmAdd].map((f, i) => ({ rang: i + 1, ...f }));

  const meta = (perim, n) => ({
    date_maj: new Date().toISOString().slice(0, 10),
    description: `Registre ${perim} team-festival. Entrées 'curé_2026' vérifiées en tête, complément 'panorama_culture_2018' (open data Ministère de la Culture).`,
    source_open_data: "https://data.culture.gouv.fr/explore/dataset/panorama-des-festivals/",
    avertissement: "Open data réf. 2018-2019 : nom/ville/département/site fiables ; fréquentation et dates 2026 à actualiser. Classement non basé sur la fréquentation pour les entrées open data.",
    total: n,
  });

  writeFileSync(
    join(DIR, "festivals_musique.json"),
    JSON.stringify({ meta: meta("musique", musique.length), festivals: musique }, null, 2)
  );
  writeFileSync(
    join(DIR, "festivals_hors_musique.json"),
    JSON.stringify({ meta: meta("hors-musique (spectacle vivant + cinéma/doc/animé)", horsMusique.length), festivals: horsMusique }, null, 2)
  );

  console.log(`OK — musique: ${musique.length} | hors-musique: ${horsMusique.length}`);
}

build().catch((e) => { console.error(e); process.exit(1); });
