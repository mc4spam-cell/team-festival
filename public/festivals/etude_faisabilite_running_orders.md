# Étude de faisabilité — Running orders des 20 plus gros festivals français

*État au 6 juin 2026. Toutes les URLs ont été vérifiées par fetch direct, sauf mention contraire.*

## Synthèse

Sur les 20 festivals étudiés, la récupération automatique du running order 2026 est :

| Statut au 6 juin 2026 | Festivals |
|---|---|
| **Complet** (scènes + horaires publiés) — 8 | Hellfest, Eurockéennes, Printemps de Bourges, Nuit de l'Erdre, Beauregard, Musilac, Jazz à Vienne, Interceltique de Lorient, Main Square |
| **Partiel** (line-up par jour, sans horaires) — 9 | Vieilles Charrues, Solidays, Francofolies, Garorock, Rock en Seine, Cabaret Vert, Déferlantes, Delta, Fête de l'Humanité |
| **Non publié / sans objet** — 2 | Electrobeach (rien publié), Lollapalooza Paris (**édition 2026 annulée**, retour 2027) |

Point structurel important : les festivals publient leur running order horodaté typiquement **entre J-15 et J-7**. Un pipeline générique doit donc prévoir un re-crawl périodique, pas une extraction unique.

## Détail par festival

| # | Festival | Source officielle | Format | Statut 2026 | Difficulté |
|---|---|---|---|---|---|
| 1 | Vieilles Charrues (Carhaix) | [programmation](https://www.vieillescharrues.asso.fr/programmation/) | Page JS + appli Fragolabs | Partiel | Difficile |
| 2 | Solidays (Paris) | [programmation](https://www.solidays.org/programmation/) | HTML statique | Partiel | Moyen |
| 3 | Hellfest (Clisson) | [lineup](https://hellfest.fr/lineup) | HTML SSR, fiches artistes avec horaires | **Complet** | Facile |
| 4 | Francofolies (La Rochelle) | [programmation](https://www.francofolies.fr/programmation/) | HTML statique, filtres jour/scène | Partiel | Moyen |
| 5 | Eurockéennes (Belfort) | [programmation](https://www.eurockeennes.fr/programmation/) + [PDF planning](https://www.eurockeennes.fr/wp-content/uploads/2026/06/planning-horaires-eurockeennes-2026.pdf) | HTML + PDF officiel | **Complet** | Facile |
| 6 | Garorock (Marmande) | [programmation](https://www.garorock.com/fr/programmation) | HTML ; timetable en JS (Greencopper) | Partiel | Moyen |
| 7 | Main Square (Arras) | [programmation](https://mainsquarefestival.fr/programmation/) | Widget SPA Fragolabs | **Complet** | Difficile |
| 8 | Rock en Seine (Saint-Cloud) | [programmation](https://www.rockenseine.com/programmation) | HTML (fetch bloqué, à vérifier) | Partiel | Moyen |
| 9 | Lollapalooza Paris | [site](https://www.lollaparis.com/) | — | **Annulé 2026** | — |
| 10 | Printemps de Bourges | [programmation](https://www.printemps-bourges.com/programmation-generale/) | HTML statique, filtre ?jour= | **Complet** (édition passée) | Facile |
| 11 | Nuit de l'Erdre (Nort-sur-Erdre) | [programmation](https://www.lanuitdelerdre.fr/programmation/) | HTML statique | **Complet** | Facile |
| 12 | Beauregard (Hérouville) | [artistes](https://www.festivalbeauregard.com/fr/artistes) | HTML statique | **Complet** | Facile |
| 13 | Musilac (Aix-les-Bains) | [horaires](https://www.musilac.com/programmation/horaires) | HTML statique | **Complet** | Facile |
| 14 | Cabaret Vert (Charleville) | [programmation](https://cabaretvert.com/programmation/) | HTML + AJAX Load More | Partiel | Moyen |
| 15 | Déferlantes (Le Barcarès) | [programmation](https://www.festival-lesdeferlantes.com/programmation/) | HTML ; horaires via appli mobile | Partiel | Difficile |
| 16 | Delta Festival (Marseille) | [programmation](https://delta-festival.com/programmation/) | Page JS, line-up en images | Partiel | Difficile |
| 17 | Interceltique de Lorient | [programmation 2026](https://www.festival-interceltique.bzh/programmation-2026/) | WordPress, grille JS, fiches via sitemap | **Complet** | Moyen |
| 18 | Jazz à Vienne | [édition 2026](https://www.jazzavienne.com/fr/edition-2026) + [PDF timeline](https://www.jazzavienne.com/sites/default/files/uploads/documents/2026-05/JAV26_Timeline_Digital.pdf) | HTML Drupal + PDF | **Complet** | Facile |
| 19 | Electrobeach (Le Barcarès) | [site](https://www.electrobeach.com/) | Page JS vide | Non publié | Difficile |
| 20 | Fête de l'Humanité | [programmation](https://fete.humanite.fr/programmation) | HTML (non vérifié, domaine bloqué) | Partiel | Moyen |

## Enseignements pour généraliser l'appli

**Trois familles techniques se dégagent :**

1. **HTML serveur propre (10 festivals)** — Hellfest, Solidays, Francofolies, Eurockéennes, Bourges, Nuit de l'Erdre, Beauregard, Musilac, Jazz à Vienne, Cabaret Vert. Un scraper par festival avec sélecteurs CSS suffit. C'est la cible prioritaire.

2. **Plateformes d'applis festival mutualisées** — plusieurs gros festivals s'appuient sur les mêmes prestataires : **Fragolabs** (Hellfest, Vieilles Charrues, Main Square) et **Greencopper** (Garorock, Beauregard). Reverse-engineer l'API JSON d'une seule de ces plateformes débloque plusieurs festivals d'un coup — c'est probablement le meilleur investissement pour la généralisation.

3. **Cas pénibles** — line-up en images (Delta), appli mobile uniquement (Déferlantes), PDF graphiques (Eurockéennes), domaines bloquant les bots (Rock en Seine, Fête de l'Huma). Prévoir rendu navigateur headless et, en dernier recours, saisie semi-manuelle.

**Calendrier de publication** : prévoir un crawler qui re-visite chaque source chaque semaine de mai à septembre, avec détection de changement. Les horaires arrivent J-15 à J-7 pour la plupart des festivals d'été.

**Alternative aux sites officiels** : l'agrégateur [Clic&Scene / touslesfestivals.com](https://www.touslesfestivals.com) et les billetteries (SeeTickets, Shotgun) relaient souvent les programmations dans un format plus homogène — utile comme source de secours ou de validation croisée.

**Point juridique** : les running orders sont des données factuelles (peu protégeables en soi), mais le scraping doit respecter les CGU des sites et la directive bases de données. Pour une appli publique, un contact direct avec les festivals (beaucoup ont un service presse réactif) ou leurs prestataires d'appli est une piste plus durable qu'un scraping silencieux.
