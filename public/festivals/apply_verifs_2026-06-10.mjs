#!/usr/bin/env node
/**
 * apply_verifs_2026-06-10.mjs — vérifications site-par-site du run hebdo 2026-06-10.
 * Met à jour les entrées curées dans festivals_registry_200.json (source canonique),
 * puis build_registry.mjs propage vers festivals_musique/hors_musique + dérive dates_status.
 * Idempotent (réécrit les mêmes champs). One-shot ; conservé pour traçabilité.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const DIR = dirname(fileURLToPath(import.meta.url));
const P = join(DIR, "festivals_registry_200.json");
const j = JSON.parse(readFileSync(P, "utf8"));
const VER = "2026-06-10";

// Mises à jour par nom exact. Champs fournis écrasent l'existant ; `noteAdd` est concaténé.
const UPDATES = {
  "Festival de Cornouaille": {
    dates_2026: "2026-07-23/2026-07-26", statut_2026: "confirmé",
    dates_source: "site officiel festival-cornouaille.bzh (vérifié 2026-06-10)",
    noteAdd: "103e édition, 23-26 juillet 2026.",
  },
  "Fête du Chant de Marin": {
    dates_2026: null, statut_2026: "hors millésime (biennale) — pas d'édition 2026",
    dates_source: "site officiel paimpol-festival.bzh (vérifié 2026-06-10)",
    noteAdd: "Biennale (années impaires) : prochaine édition 6-8 août 2027.",
  },
  "Porto Latino": {
    dates_2026: null,
    statut_2026: "à confirmer (déménagé à Bastia ; ~5-8 août 2026 selon sources)",
    dates_source: "sources web (corsenetinfos, portolatino.fr) — conflit 3-6 vs 5-8 août (vérifié 2026-06-10)",
    noteAdd: "Déménagement Saint-Florent -> Bastia (Place Vincetti) ; dates 2026 conflictuelles (3-6 ou 5-8 août), à confirmer sur site officiel. Fenêtre-limite (~05/08).",
  },
  "Le Bon Air": {
    dates_2026: "2026-05-22/2026-05-24", statut_2026: "confirmé — hors fenêtre (mai)",
    dates_source: "site officiel le-bon-air.com / billetterie DICE (vérifié 2026-06-10)",
    noteAdd: "11e édition, 22-24 mai 2026 (Friche la Belle de Mai).",
  },
  "Les 3 Éléphants": {
    dates_2026: "2026-05-21/2026-05-25", statut_2026: "confirmé — hors fenêtre (mai)",
    dates_source: "site officiel les3elephants.com (vérifié 2026-06-10)",
    noteAdd: "21-25 mai 2026, Square de Boston, prog. majoritairement gratuite.",
  },
  "La Magnifique Society": {
    dates_2026: null, statut_2026: "à confirmer (juin 2026, dates non publiées)",
    dates_source: "site officiel lamagnifiquesociety.com (vérifié 2026-06-10)",
    noteAdd: "Retour annoncé en juin 2026 après pause 2025 ; dates exactes non publiées au 2026-06-10.",
  },
  "Check In Party": {
    dates_2026: null, statut_2026: "incertain 2026 (non confirmé)",
    dates_source: "recherche web (vérifié 2026-06-10)",
    noteAdd: "Typiquement août (aérodrome St-Laurent/Guéret) ; édition 2025 annulée ; 2026 non confirmée au 2026-06-10.",
  },
};

let applied = 0;
for (const f of j.festivals) {
  const u = UPDATES[f.nom];
  if (!u) continue;
  if ("dates_2026" in u) f.dates_2026 = u.dates_2026;
  if (u.statut_2026) f.statut_2026 = u.statut_2026;
  if (u.dates_source) f.dates_source = u.dates_source;
  if (u.noteAdd) {
    const tag = `[MAJ ${VER}] ${u.noteAdd}`;
    if (!f.notes || !f.notes.includes(u.noteAdd)) f.notes = f.notes ? `${f.notes} ${tag}` : tag;
  }
  f.derniere_verification = VER;
  applied++;
}
console.log("Updates appliquées:", applied, "/", Object.keys(UPDATES).length);

// --- Nouveau festival détecté ce run (1re édition) : Festival Pagaille (Bordeaux)
// Hors fenêtre J+0->J+56 (28-29 août) mais ajouté pour enrichir le registre curé.
if (!j.festivals.some((f) => f.nom === "Festival Pagaille")) {
  j.festivals.push({
    rang: j.festivals.length + 1,
    nom: "Festival Pagaille",
    ville: "Bordeaux",
    departement: "Gironde (33)",
    genre_principal: "généraliste / rock",
    frequentation_estimee: null,
    dates_2026: "2026-08-28/2026-08-29",
    site_officiel: null,
    url_programmation: null,
    statut_2026: "confirmé — hors fenêtre (fin août)",
    difficulte: null,
    notes: "1re édition 2026, place des Quinconces. Tête d'affiche The Cure (soirée d'ouverture). Source presse spécialisée (Fnac Spectacles / Offi) — à confirmer sur site officiel.",
    derniere_verification: VER,
    dates_source: "presse spécialisée (vérifié 2026-06-10)",
  });
  console.log("Ajouté : Festival Pagaille (Bordeaux)");
}

writeFileSync(P, JSON.stringify(j, null, 2));
console.log("registry_200 -> total:", j.festivals.length);
