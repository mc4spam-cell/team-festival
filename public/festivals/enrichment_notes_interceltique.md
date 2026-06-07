# Notes d'extraction & d'enrichissement — Festival Interceltique de Lorient 2026
Extraction : 06/06/2026 · 55e édition, 31 juillet – 9 août 2026, « La Cornouailles, au cœur de la Mer Celtique »

## 1. Méthode & sources
- **Site officiel** (festival-interceltique.bzh) : les pages « Tous les artistes » et les pages par lieu (`programmation-le-theatre-2026`, `programmation-lespace-jean-pierre-pichard-2026`, etc.) sont rendues par un **widget JS Fragolabs** (`<fragolabs-widget application-id="4df2c040-d48f-4519-a33b-36de47868fb9">`, script `widget.fragolabs.com/main.js`) → contenu **inaccessible en fetch statique** (vérifié via l'API WordPress `wp-json/wp/v2/pages`, qui ne contient que le shortcode du widget). Le sitemap ne contient **aucune fiche artiste** WordPress.
- Données structurantes récupérées via : article officiel « FIL 2026 : Toute la programmation ! » (17/03/2026), page billetterie officielle (liste Pass Culture par lieu), **56.agendaculturel.fr** (fiches concert datées), **infoconcert.com**, **fandefest.com**, **sortiraparis.com**, Facebook officiel du FIL.
- Contrainte technique : le tool de fetch a été **rate-limité** pendant une grande partie de la session → la collecte fine (page agrégée agendaculturel, widget Weezevent `widget.weezevent.com/ticket/6fa5d550-…`) n'a pas pu être complétée. Recommandé pour une passe 2 : re-fetch de `https://56.agendaculturel.fr/festival/festival-interceltique-de-lorient.html` et du widget Weezevent (3 widgets : tous spectacles / Horizons Celtiques / Le Théâtre).

## 2. Périmètre couvert (→ `"status": "Running Order Incomplet"`)
- **Couvert** : Stade du Moustoir, Espace Jean-Pierre Pichard, Le Théâtre (Grand Théâtre), Le Palais des Congrès — l'essentiel des spectacles payants confirmés ; Quai de la Bretagne et Le Kleub **partiellement** (3 et 5 dates confirmées).
- **Non couvert** : Salle Carnot (festoù-noz quotidiens), La Taverne Celte, Les Terrasses, La Place des Pays Celtes, animations de rue, Espace Paroles & Musiques, Pub gallois, etc. (~300 spectacles annoncés au total).
- **Spectacles connus mais non datés (non inclus faute d'horaire/date)** : « L'Irlande Symphonique » (Théâtre), « L'Orchestre Symphonique du Festival » (Théâtre), duo Perceval, soirées Théâtre des 1er, 4, 5 et 9 août, soirées Palais des 1er–2 août, plupart des plateaux Quai de la Bretagne/Kleub.

## 3. Horaires : confirmés vs estimés
**Confirmés (source)** :
- Kernowcopia (Barrett's Privateers, Skillywidden) : ven 31/07, 21h30, Le Palais (article officiel).
- Carlos Núñez « Celtic Sea » : ven 07/08, 15h00 **et** 21h00, Le Théâtre (article officiel) — 2 entrées distinctes.
- Grande Parade : dim 02/08, 10h00 (officiel).
- Bouge ton Celte ! : ven 07/08, 20h30, Espace JPP (Facebook officiel FIL).
- Agnes Obel : mar 04/08, 20h30, Espace JPP (Infoconcert).
- Championnat National des Bagadoù 1ère cat. au Moustoir : sam 01/08 (Jaimeradio/officiel) — plage 13h–18h estimée.
- Horizons Celtiques : 1, 2, 4, 5, 6 août (officiel) ; durée 2h15 (billetterie) ; début 22h estimé (usage FIL).

**Estimés (méthode = créneaux standards FIL + enchaînement même lieu)** : Théâtre 21h00 ; Palais 21h30 (1ère partie 21h30→22h30, tête d'affiche 22h45→00h00) ; Espace JPP 20h30 ; Kleub 22h30 ; Quai de la Bretagne 19h00 ; après-midi du Stade le 02/08 (Danses et Costumes 14h30 — durée 2h30 sourcée ; Triomphe des Sonneurs 17h15). **Toutes les heures de fin** sont estimées (spectacle suivant même lieu ou durée standard 1h–1h30, 2h pour les grands shows).

**Points d'incertitude assumés** :
- « Musiques et Danses des Pays Celtes » : occurrences 03/08 (soir) et 05/08 (après-midi) à l'Espace JPP d'après recoupements — à confirmer.
- Cécile Corbel « Voix Celtes » et « Sur les Routes des Celtes du Sud » tous deux le 04/08 au Palais → Corbel placée à 19h00 (estimation).
- « Voix Bretonnes » (Kerno/Lavanant) 08/08 : lieu Palais présumé (série « Voix » au Palais).
- Ordre de passage des doubles plateaux : confirmé pour la Grande soirée de l'Irlande (Leonard Barry Trio ouvre, source agendaculturel) et Légendes Celtiques (Wilkinson/Le Bigot puis Tickell) ; présumé pour Kernowcopia, Écosse (LÉDA puis Capercaillie), Celtes du Sud.
- ElectroFIL 08/08 (JPP) : line-up sourcé Fleuves, NOON, DJ Miss Blue (ordre/horaires estimés) ; participation de Perceval évoquée mais ambiguë → non incluse.

## 4. Enrichissement plateformes (best-effort)
**MusicBrainz (vérifiés via recherche site:musicbrainz.org, MBID extrait de l'URL)** :
- Yann Tiersen : `12d432a3-feb0-49b1-a107-d20751880764`
- Agnes Obel : `e3c4c4af-f83b-4168-84cb-898009dd0447`
- Denez (Prigent) : `ff7592f0-2c7f-4df6-a178-56aa5c5cf966`
- Eluveitie : `8000598a-5edb-401c-8e6d-36b167feaf38`
- Capercaillie : `3020fb04-9795-412f-8474-bf6d2ac6d29c`
- Cécile Corbel : `f71bb99b-cc5d-4c30-a42e-297d1b04e129`
- Carlos Núñez, Dervish, Mànran… : non ressortis dans les résultats → null (sans acharnement).

**Spotify (vérifiés via résultats open.spotify.com)** :
- Yann Tiersen `00sazWvoTLOqg5MFwC68Um` (un 2e profil homonyme `3wOOqxvfoC2N75TSUzXYZl` existe ; retenu le profil principal ~3,1 M auditeurs).
- Agnes Obel `1rKrEdI6GKirxWHxIUPYms` · Eluveitie `5X0N2k3qMnI8kSrGJT3kfT` · Carlos Núñez `3e09WanUMwtc9XfZRLvZ30`.

**Instagram (vérifiés)** : @yanntiersen, @agnesobel. @eluveitie inclus (handle notoire, non re-vérifié en session).

**Deezer / Apple Music / playlists** : non résolus (rate limit fetch + recherche non concluante) → tous null. Les artistes trad/locaux (Plouz et Foen, Ferla Megía, Seim, Eris, Ampouailh, Tanork, Skillywidden, Barrett's Privateers…) sont volontairement laissés à null : présence plateformes faible/ambiguë.

## 5. Bilan IDs
- 45 concerts, 6 lieux, 10 jours.
- MusicBrainz : 6 résolus (Denez, Agnes Obel, Yann Tiersen, Eluveitie, Capercaillie, Cécile Corbel).
- Spotify : 5 entrées renseignées (4 artistes, Carlos Núñez ×2).
- Instagram : 3 handles. Deezer : 0. Apple Music : 0.
