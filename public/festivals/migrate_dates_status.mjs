#!/usr/bin/env node
/**
 * migrate_dates_status.mjs — Migration schéma v2 : ajoute le champ `dates_status`.
 * -----------------------------------------------------------------------------
 * Dérive `dates_status` ∈ {published_iso, published_text_fr, not_published,
 * not_verified_yet} depuis `dates_2026` + `statut_2026`, et NORMALISE les dates
 * texte FR en ISO `AAAA-MM-JJ/AAAA-MM-JJ` quand c'est possible.
 *
 * Règles (cf. SKILL §B) :
 *  - statut annulé/arrêté/remplacé/liquidation/pause  -> not_published, date->null,
 *    l'indice textuel est conservé dans `notes`.
 *  - dates_2026 ISO                                   -> published_iso.
 *  - dates_2026 texte normalisable                    -> published_iso (réécrit en ISO).
 *  - dates_2026 texte non normalisable + statut vérifié (confirmé/complet/partiel)
 *                                                     -> published_text_fr (texte gardé).
 *  - dates_2026 texte vague + statut "à vérifier/à confirmer/à actualiser/incertain"
 *                                                     -> not_verified_yet, date->null,
 *                                                        indice gardé dans `notes`.
 *  - dates_2026 null                                  -> not_verified_yet.
 *
 * Ne touche QUE les fichiers festivals_musique.json / festivals_hors_musique.json,
 * en préservant tous les champs existants. Idempotent.
 *
 * Usage : node migrate_dates_status.mjs
 */
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const DIR = dirname(fileURLToPath(import.meta.url));
const FILES = ["festivals_musique.json", "festivals_hors_musique.json"];

const ISO_RE = /^\d{4}-\d{2}-\d{2}\/\d{4}-\d{2}-\d{2}$/;
const MONTHS = {
  janvier: "01", fevrier: "02", "février": "02", mars: "03", avril: "04",
  mai: "05", juin: "06", juillet: "07", aout: "08", "août": "08",
  septembre: "09", octobre: "10", novembre: "11", decembre: "12", "décembre": "12",
};
const MONTH_ALT = Object.keys(MONTHS).map((m) => m.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("|");
const pad = (n) => String(n).padStart(2, "0");

// Statuts indiquant qu'il n'y aura pas d'édition 2026 datable sous ce nom.
const CANCEL_RE = /annul|arrêt|arret|arrêté|liquidat|\bpause\b|plus d.?.?dition|pas d.?.?dition|biennale|remplac|interdit|mobilis|supprim|retour 20\d\d/i;
// Statuts considérés comme "source publiée" (date fiable même si non ISO).
// Ancré en début de statut : "confirmé…/complet…/partiel…" — exclut "à confirmer",
// "date non confirmée", "incertain", etc.
const VERIFIED_RE = /^\s*(confirmé|complet|partiel)/i;

/** Tente de convertir une date texte FR en "AAAA-MM-JJ/AAAA-MM-JJ". null si vague. */
function toISO(raw) {
  if (!raw) return null;
  let s = raw.replace(/\([^)]*\)/g, " ").replace(/\s+/g, " ").trim().toLowerCase();
  s = s.replace(/(\d)\s*er\b/g, "$1"); // 1er -> 1
  const Y = "(\\d{4})";
  // A) "JJ-JJ mois AAAA"
  let m = s.match(new RegExp(`^(\\d{1,2})\\s*[-–au]+\\s*(\\d{1,2})\\s+(${MONTH_ALT})\\s+${Y}$`));
  if (m) {
    const mo = MONTHS[m[3]]; const y = m[4];
    return `${y}-${mo}-${pad(m[1])}/${y}-${mo}-${pad(m[2])}`;
  }
  // B) "JJ mois - JJ mois AAAA"
  m = s.match(new RegExp(`^(\\d{1,2})\\s+(${MONTH_ALT})\\s*[-–au]+\\s*(\\d{1,2})\\s+(${MONTH_ALT})\\s+${Y}$`));
  if (m) {
    const y = m[5];
    return `${y}-${MONTHS[m[2]]}-${pad(m[1])}/${y}-${MONTHS[m[4]]}-${pad(m[3])}`;
  }
  // C) "JJ mois AAAA" (jour unique)
  m = s.match(new RegExp(`^(\\d{1,2})\\s+(${MONTH_ALT})\\s+${Y}$`));
  if (m) {
    const mo = MONTHS[m[2]]; const y = m[3];
    return `${y}-${mo}-${pad(m[1])}/${y}-${mo}-${pad(m[1])}`;
  }
  return null; // vague : "mai 2026", "fin août 2026", "été 2026", "avril-mai 2026"...
}

function appendNote(e, txt) {
  e.notes = e.notes ? `${e.notes} ${txt}` : txt;
}

/** Réordonne pour insérer dates_status juste après dates_2026. */
function withStatusAfterDates(e, status) {
  const out = {};
  for (const k of Object.keys(e)) {
    out[k] = e[k];
    if (k === "dates_2026") out.dates_status = status;
  }
  if (!("dates_status" in out)) out.dates_status = status; // sécurité
  return out;
}

function migrateEntry(e0) {
  const e = { ...e0 };
  const d = e.dates_2026;
  let status;

  if (CANCEL_RE.test(e.statut_2026 || "")) {
    status = "not_published";
    if (d) { appendNote(e, `[Date évoquée avant décision: ${d}]`); e.dates_2026 = null; }
  } else if (d && ISO_RE.test(d)) {
    status = "published_iso";
  } else if (d) {
    const iso = toISO(d);
    if (iso) {
      e.dates_2026 = iso;
      status = "published_iso";
      e.dates_source = e.dates_source || "normalisé depuis texte FR (migration v2)";
    } else if (VERIFIED_RE.test(e.statut_2026 || "")) {
      status = "published_text_fr"; // texte gardé tel quel
    } else {
      status = "not_verified_yet";
      appendNote(e, `[Estimation date non vérifiée: ${d}]`);
      e.dates_2026 = null;
    }
  } else {
    status = "not_verified_yet";
  }
  return withStatusAfterDates(e, status);
}

function tally(arr, key) {
  return arr.reduce((m, f) => { const k = f[key] || "?"; m[k] = (m[k] || 0) + 1; return m; }, {});
}

export { migrateEntry, toISO };

// Exécuté seulement en invocation directe (`node migrate_dates_status.mjs`),
// pas lors d'un `import` depuis build_registry.mjs.
const invokedDirectly =
  process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1];
if (invokedDirectly) {
  let grand = {};
  for (const file of FILES) {
    const p = join(DIR, file);
    const j = JSON.parse(readFileSync(p, "utf8"));
    j.festivals = j.festivals.map(migrateEntry);
    j.meta = j.meta || {};
    j.meta.date_maj = "2026-06-10";
    j.meta.schema_version = 2;
    j.meta.par_dates_status = tally(j.festivals, "dates_status");
    writeFileSync(p, JSON.stringify(j, null, 2));
    const t = tally(j.festivals, "dates_status");
    for (const k in t) grand[k] = (grand[k] || 0) + t[k];
    console.log(file, "->", JSON.stringify(t));
  }
  console.log("TOTAL dates_status:", JSON.stringify(grand));
}
