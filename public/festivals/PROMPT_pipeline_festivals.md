# Prompt — Pipeline complet « registres festivals team-festival »

> À coller en une seule fois. Contexte : application **team-festival**, dossier de travail
> `/Users/mc/team-festival/public/festivals/`. Date de référence du run : adapter si besoin.

---

Tu es mon agent data pour l'app **team-festival**. Objectif : construire et tenir à jour deux
registres JSON de festivals français, **vérifiés**, et savoir répondre à des requêtes par fenêtre
de dates. Travaille dans `/Users/mc/team-festival/public/festivals/`. Demande l'accès au dossier
si nécessaire. Crée une todo-list et coche au fur et à mesure.

## 0. Périmètre & règles
- Domaines RETENUS : **musique**, **cinéma / documentaire / animation**, **spectacle vivant**
  (théâtre, danse, cirque, arts de la rue), et **arts numériques** par proximité (ex. Fête des
  Lumières). EXCLUS : BD, photographie, littérature, gastronomie, pop-culture (Japan Expo…).
- Deux listes séparées : `festivals_musique.json` et `festivals_hors_musique.json`.
- Schéma commun par entrée : `rang, domaine, nom, ville, departement ("Nom (NN)"),
  genre_principal, frequentation_estimee (int|null), dates_2026 ("AAAA-MM-JJ/AAAA-MM-JJ"|null),
  site_officiel, url_programmation, statut_2026, difficulte, notes, derniere_verification,
  dates_source, source ("curé" | "panorama_culture_2018")`.
- Honnêteté des données : ne JAMAIS inventer une fréquentation ou une date. `null` + note si
  inconnu. `frequentation_estimee = null` pour les événements pro/sur accréditation (Cannes,
  Annecy, Sunny Side). Marque clairement ce qui est « à confirmer ».

## 1. Univers à grande échelle (open data)
- Source : **Panorama des festivals**, API Opendatasoft du Ministère de la Culture :
  `https://data.culture.gouv.fr/api/explore/v2.1/catalog/datasets/panorama-des-festivals/records`
  (réf. 2018-2019 : identité fiable — nom, commune, département, domaine, site — mais SANS
  fréquentation ni dates). Récupère via requêtes `?select=...&where=domaine="..."&limit=100&offset=...`
  (paginer ; max 100/req ; garder les URLs courtes). Filtre les domaines :
  - Musique : `Musiques actuelles`, `Musiques classiques`, `Pluridisciplinaire Musique`.
  - Hors-musique : `Cinéma et audiovisuel`, `Cirque et Arts de la rue`,
    `Pluridisciplinaire Spectacle vivant`, `Divers Spectacle vivant`, `Danse`, `Théâtre`,
    `Transdisciplinaire`.
- Normalise : commune et noms en casse propre (ST→Saint, STE→Sainte), code département → nom
  (table INSEE complète, DOM/TOM inclus), genre déduit du domaine, site préfixé `http(s)://`.
- Dédoublonne par nom normalisé (sans « festival/le/la/de… », sans accents).
- **Volume : pas de plafond artificiel.** Prends TOUT l'univers in-périmètre disponible dans la
  source, pour chaque liste. Ordre de grandeur attendu : musique ≈ 1 980 entrées (musiques
  actuelles + classiques + pluridisciplinaire musique), hors-musique ≈ 800-930 (cinéma/audiovisuel
  + spectacle vivant + transdisciplinaire). N'écarte une entrée que si elle est hors-périmètre ou
  doublon — jamais pour « tenir un quota ». Indique le total réellement obtenu par liste.
- Croise si possible une 2ᵉ source ouverte (ex. dataset `festivals-global` sur opendatasoft) pour
  élargir au-delà du Panorama et combler les manques, toujours dédoublonné.
- Livre aussi un script générateur reproductible **`build_registry.mjs`** (Node ≥18, `fetch`
  natif) qui régénère les deux fichiers à la demande, place les entrées curées en tête, et
  complète avec l'open data dédoublonné (sans cap).

## 2. Couche curée & vérifiée (haut de gamme)
- En tête de chaque liste, des entrées « curé » riches et **vérifiées en ligne** (dates 2026,
  site officiel, fréquentation estimée, statut). Ce n'est PAS une limite : la couche curée vient
  s'ajouter à l'univers open data complet du point 1, pas le remplacer ni le plafonner. Vise à
  curer le plus d'entrées notables possible (idéalement toutes celles à forte fréquentation ou
  bien documentées), et continue d'enrichir au fil des runs. Inclure au minimum :
  - Musique : les plus gros par fréquentation (Vieilles Charrues, Hellfest, Solidays, Eurockéennes,
    Garorock, Main Square, Rock en Seine, Francofolies, Jazz à Vienne, etc.) — sans s'arrêter à un
    nombre fixe.
  - Hors-musique : Cannes, Annecy, Deauville, Gérardmer, Clermont-Ferrand court métrage, Lumière,
    FFA Angoulême, Cabourg, Reims Polar, La Rochelle FEMA, FIPADOC, Lussas, Sunny Side ;
    Avignon IN + OFF, Aurillac, Chalon dans la rue, Marionnettes Charleville (Temps d'M 2026),
    Printemps des Comédiens, Festival de Marseille, Montpellier Danse, Festival d'Anjou,
    Festival d'Automne, Cirque de Demain, Biennale du Cirque, Fête des Lumières.
- Signale les cas particuliers détectés (annulés/déplacés/biennales) plutôt que de les inventer.

## 3. Documentation fine des dates 2026 (vérif site par site)
- Pour TOUTES les entrées dont `dates_2026` est vague (« juillet 2026 », « fin août », mois
  multiples) ou absente : vérifie sur la **source officielle** (site / billetterie) la date exacte
  de l'édition 2026, et remplace par une plage **ISO `AAAA-MM-JJ/AAAA-MM-JJ`**.
- Mets à jour `statut_2026` (`confirmé` / `annulé 2026` / `incertain` / `à confirmer` /
  `hors fenêtre`), `derniere_verification` et `dates_source`.
- Si un site est rendu en JavaScript (fetch vide), bascule sur la recherche web (qui remonte la
  date affichée par le site officiel / la billetterie). Ne fabrique jamais de date.
- Détecte et marque explicitement : annulations 2026, arrêts définitifs, changements de ville,
  changements de nom, éditions « réduites », et festivals hors millésime (biennales).

## 4. Requête par fenêtre temporelle
- Implémente un filtre « festivals en cours ou démarrant dans les N jours » à partir d'aujourd'hui :
  parse les `dates_2026` ISO, garde ceux dont `début ≤ aujourd'hui+N` et `fin ≥ aujourd'hui`.
  Exclus les `annulé/incertain/arrêté`. Sépare « dates précises » et « à confirmer ».
- Démontre-le pour **N = 56 jours** : sors la liste chronologique (date, domaine, nom, ville,
  « EN COURS » si déjà démarré), regroupée par mois, + un récap des exclus (annulés / hors fenêtre).

## 5. Livrables & contrôle qualité
- Fichiers : `festivals_musique.json`, `festivals_hors_musique.json`, `build_registry.mjs`.
  (Les logos des plateformes sont déjà présents dans le projet — ne pas les régénérer.)
- Valide chaque JSON (parse OK), affiche les comptes par domaine / par source / par statut,
  et un échantillon de 5 lignes par fichier.
- Termine par un récap honnête : volumes obtenus, limites de la source open data (pas de
  fréquentation, données 2018-2019), et liste des entrées « à confirmer ».
- Ne touche pas aux fichiers curés existants sans préserver leurs champs ; en cas de mise à jour
  de dates, conserve l'historique via `dates_source` + `derniere_verification`.
