# Veille running orders — festivals français
**Run du dimanche 7 juin 2026** · Fenêtre prioritaire : éditions du 7 juin au 2 août 2026 (18 festivals non « Complet »)

## Conditions du run
- Référentiel `festivals_registry_200.json` et `index.json` accessibles (dossier connecté). Diff effectué avec l'état du 06/06.
- **Fetch direct des sites bloqué** par l'allowlist réseau (tous domaines festivals) → vérification via recherche web croisée. Aucun contournement tenté.
- 7 running orders horodatés extraits lors de la session d'enrichissement du 06/06 ont été **validés au format de référence** (`running_order.json` : stages/days/concerts, ISO+02:00, champs streaming à null) et **passés en « Complet » dans l'index** — ils n'y étaient pas encore référencés.

## ✅ Running orders complets intégrés à l'index ce run (fichiers validés)

| Festival | Fichier | Sets | Source |
|---|---|---|---|
| Les Eurockéennes (2–5/07) | running_order_eurockeennes_2026.json | 51 | eurockeennes.fr/programmation/ + PDF planning-horaires officiel, croisé sortiraparis (art. 50509) |
| Main Square (3–5/07) | running_order_mainsquare_2026.json | 40 | sortiraparis art. 50212 + onivapp (100 % concordants), scènes via jds.fr |
| Beauregard (1–5/07) | running_order_beauregard_2026.json | 51 | festivalbeauregard.com/fr/artistes (heures de début officielles) |
| Musilac (9–12/07) | running_order_musilac_2026.json | 40 | musilac.com/programmation/horaires (RO officiel) |
| Jazz à Vienne (25/06–11/07) | running_order_jazzavienne_2026.json | 165 | PDF timeline officiel JAV26 + jazzavienne.com/fr/edition-2026 |
| La Nuit de l'Erdre (juill.) | running_order_nuitdelerdre_2026.json | 27 | lanuitdelerdre.fr (officiel) |
| Printemps de Bourges (terminé, avril) | running_order_bourges_2026.json | 112 | printemps-bourges.com/programmation-generale/ |

⚠️ Heures de fin **estimées** pour Eurockéennes, Beauregard, Bourges (non publiées) — méthode documentée dans les `enrichment_notes_*.md`. Total index : **8 « Complet »** (avec Hellfest).

## 🔴 RO publiés en ligne mais NON extractibles (domaines bloqués) — priorité absolue

| Festival | Échéance | URL du RO | Note |
|---|---|---|---|
| **Marsatac** (Marseille) | **12–14 juin (J-5 !)** | https://marsatac.com/programmation/ | Timetable annoncé publié (post FB officiel). NB : infoconcert indique 11–13/06, officiel 12–14/06. |
| **Garorock** (Marmande) | 25–28 juin | https://www.garorock.com/fr/timetable | Page timetable en ligne. |
| **Nice Jazz Fest** | 23–26 juil | https://www.nicejazzfest.fr/fr/programmation/2026 | Pages horodatées par jour (23/07 : 19h30 Cavassa → 23h00 Sting). |
| **Terres du Son** (Monts) | 10–12 juil | https://www.terresduson.com/programmation/horaires-prairie/ | Page horaires prairie en ligne. |
| Pause Guitare (Albi) | 8–12 juil | https://www.pauseguitare.net/programmation/ | Horaires Grande Scène 10–12/07 dans la presse (jds.fr) ; journées 8–9/07 sans horaires → partiel. |

## 🟡 En attente (line-up publié, horaires non publiés)

| Festival | Dates | RO attendu | Vérifié via |
|---|---|---|---|
| Solidays | 26–28 juin | ~12–19 juin | solidays.org, onivapp, jds.fr |
| Aluna (Ruoms) | 25–27 juin | ~11–18 juin | aluna-festival.fr (snippets), jds.fr, infoconcert |
| Cognac Blues Passions | 1–4 juil | ~17–24 juin | bluespassions.com (snippets), sortiraparis art. 50865 |
| Vieilles Charrues | 16–19 juil | ~2–9 juil | site officiel (rendu JS Fragolabs), festapp, agendaculturel |
| Francofolies | 10–14 juil | ~26 juin–3 juil (créneaux partiels déjà visibles sur jds.fr) | francofolies.fr, jds.fr |
| Nuits Secrètes | 10–12 juil | ~26 juin–3 juil | lesnuitssecretes.com, touslesfestivals, jds.fr |
| Les Déferlantes | 10–13 juil | ~26 juin–3 juil | fichier line-up existant (44 concerts) |
| Les Escales | 24–26 juil | ~10–17 juil | festival-les-escales.com (snippets), jds.fr |

## 🌐 Domaines à ajouter à l'allowlist (par ordre d'urgence)
1. **marsatac.com** (J-5)
2. **garorock.com** (J-18, timetable déjà en ligne)
3. www.solidays.org · aluna-festival.fr (RO imminents)
4. www.nicejazzfest.fr · www.terresduson.com · www.pauseguitare.net (RO déjà en ligne)
5. bluespassions.com · www.francofolies.fr · lesnuitssecretes.com · www.festival-les-escales.com · www.vieillescharrues.asso.fr · www.sortiraparis.com · www.jds.fr (validation croisée)

## Erreurs / limites
- `mcp__workspace__web_fetch` : tous les domaines festivals hors allowlist → bascule WebSearch conformément à la consigne ; pas d'extraction complète possible ce run (risque d'inventer des horaires à partir de snippets partiels).
- Interceltique de Lorient : fichier partiel existant (49 concerts horodatés, couverture incomplète ~300 spectacles) → reste « Incomplet », widget Fragolabs non fetchable.
- Fichiers RO nommés `running_order_<slug>_2026.json` (convention de la session du 06/06) et non `<slug>-2026.json` (seul hellfest-2026.json suit ce format) — conservés tels quels pour ne pas casser les références ; à harmoniser si l'appli l'exige.

## Synthèse
**7 nouveaux running orders complets référencés cette semaine : Eurockéennes, Main Square, Beauregard, Musilac, Jazz à Vienne, La Nuit de l'Erdre, Printemps de Bourges** (extraits le 06/06, validés et intégrés à l'index ce jour — total 8 avec Hellfest). 5 RO supplémentaires sont déjà publiés en ligne mais non extractibles faute d'allowlist (Marsatac J-5 en tête, puis Garorock, Nice Jazz Fest, Terres du Son, Pause Guitare). Prochaine vague attendue mi-juin : Solidays, Aluna, Cognac.
