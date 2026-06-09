# Rapport running orders — 2026-06-09

## 1 nouveau running order complet aujourd'hui

- **Festival de la Paille** (Métabief, 24-25 juil. 2026) — running order horodaté complet extrait du **planning horaire PDF officiel**.
  Source : https://www.festivalpaille.fr/ (PDF officiel) — 18 sets, 2 scènes (Grande Scène, Scène Mont d'Or), 2 jours, heures début **et** fin pour chaque set.
  Fichier créé : `festival-de-la-paille-2026.json` (`status: "Complet"`). Le set « SURPRISE » non horodaté et les DJ camping ont été exclus (pas d'horaires publiés — aucune invention).

## Périmètre examiné

**115 festivals** sélectionnés (1re date entre 2026-06-09 et 2026-08-04, statut ≠ annulé/arrêté/hors-fenêtre), **111 fetchés** ce matin (les 4 déjà horodatés complets — Eurockéennes, Main Square, Beauregard, Musilac — non retouchés). Résultat : **1 complet**, le reste sans running order horodaté exhaustif.

Les 8 fichiers déjà présents en `status: "Running Order Incomplet"` (Garorock, Solidays, Francofolies, Déferlantes, Cabaret Vert, Rock en Seine, Fête de l'Humanité, Vieilles Charrues) ont été re-contrôlés : **aucun n'a publié les horaires** depuis hier — laissés en l'état.

## Festivals proches d'être finalisables (heures de début publiées, fin manquante)

Ces festivals publient les **heures de début** (parfois scènes) mais **pas les heures de fin** → restent « Running Order Incomplet », à recontrôler dans les prochains jours :

| Festival | Dates | Constat |
|---|---|---|
| Les Escales (St-Nazaire) | 24-26 juil | Scènes + heures de début ; pas d'heures de fin |
| Guitare en Scène | 14-18 juil | Artiste + scène + jour + heure de début ; fin absente |
| Les Suds à Arles | 13-19 juil | Date + lieu + début + durée (ex. 20:30 > 60 min) ; pas d'heure de fin explicite, prog en cours |
| Jazz à Juan | 9-19 juil | 19 sets, heures de début seules (1 lieu) |
| Festival de Colmar | 5-15 juil | Concerts avec heure de début ; fin en PDF |
| La Roque-d'Anthéron (piano) | 16 juil+ | Concerts avec heure de début ; pas de fin |
| Rock en Stock | 24-26 juil | Programmation par jour, heures de début seules |

## Domaines à investiguer (pages JavaScript bloquantes)

Contenu nav/footer uniquement, rendu JS — non contournés (conformément à la consigne) :

- `programme.annecyfestival.com` (Annecy animation) — redirige vers login OAuth, pas de contenu public
- `garorock.com` — programmation + timetable rendues en JS
- `delta-festival.com` — page programmation placeholder / SPA
- `vieillescharrues.asso.fr` — Fragolabs, schedule JS (récurrent)
- `europavox.com` — programmation sur site séparé
- `avoinezonegroove.fr` — page « analyzing… » (anti-bot/JS)
- `festival-avignon.com` (IN) — filtres interactifs, schedule non rendu
- `petethemonkey.com` → `.info` — SPA vide

## Erreurs / sites injoignables ce matin

Fetch en échec (connexion refusée, DNS, TLS, 403/404/503) — à recontrôler :

- TLS cert invalide : `thiers.fr` (La Pamparina), `thepeacocksociety.fr`, `stereoparc.fr`
- Connexion refusée / DNS : `lesfeuxdelete.fr`, `festival-sully.loiret.fr`, `musiquesenstock.com`, `festivalaufilduson.fr`, `rencontresetracines.fr`, `leskampagnarts.fr`, `binicfolksbluesfestival.com`, `countryrendezvous.com`, `electrobeach.com`
- HTTP : `lamagnifiquesociety.com` (503), `festival-aix.com` (404), `festival-poupet.com` (boucle de redirection), `festivalduperigordnoir.fr` (403)

## Note technique

`Festival de la Paille` provient de `festivals_registry_200.json` ; entrée enrichie avec `running_order_file: "festival-de-la-paille-2026.json"`. `derniere_verification` mise à jour à 2026-06-09 sur les 115 festivals examinés (106 musique + 9 hors-musique). `statut_2026` non modifié (champ manuel).
