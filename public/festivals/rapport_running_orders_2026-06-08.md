# Rapport running orders — 2026-06-08

## 0 nouveau running order complet aujourd'hui

Aucun running order horodaté **complet** (artiste + scène + jour + heures début **et** fin) n'a été publié par les festivals examinés. Aucun fichier `running_order_*.json` détaillé n'a donc été créé/modifié ce run (règle : ne jamais inventer d'horaires).

## Festivals examinés (édition ≤ 8 semaines, statut ≠ Complet, non annulés)

13 festivals examinés, triés par date d'édition. Les 4 festivals déjà « Complet » (Eurockéennes, Main Square, Beauregard, Musilac) n'ont pas été retouchés.

| Festival | Dates | Constat | Statut |
|---|---|---|---|
| Marsatac | 12-14 juin | Listes d'artistes par jour, sans horaires ni scènes | Incomplet |
| Garorock | 25-28 juin | Prog par jour ; vue « Timetable » annoncée mais pas en ligne | Incomplet |
| Aluna Festival | 25-28 juin | Affiche par jour + scène, aucun horaire | Incomplet |
| Solidays | 26-28 juin | Liste artistes par jour, aucun horaire | Incomplet |
| Cognac Blues Passions | 1-4 juil | Têtes d'affiche, pas de scènes ni horaires | Incomplet |
| Pause Guitare | 8-12 juil | Artistes + dates, pas d'horaires | Incomplet |
| Francofolies La Rochelle | 10-14 juil | Artistes par jour et scène, aucun horaire | Incomplet |
| Les Déferlantes | 10-13 juil | Affiche par jour, pas de timetable | Incomplet |
| Les Nuits Secrètes | 10-12 juil | Quelques créneaux épars (ex. MIKA sam 11 21:00) ; non exhaustif | Incomplet |
| Terres du Son | 10-12 juil | Affiche par jour ; pages Prairie/Village sans horaires extractibles | Incomplet |
| Les Vieilles Charrues | 16-19 juil | Page JS (Fragolabs), seuls nav/footer récupérés | Incomplet |
| Les Escales (St-Nazaire) | 24-26 juil | **Heures de DÉBUT par artiste publiées** ; fin + scènes absentes | Incomplet |
| Chalon dans la rue | 23-26 juil | Prog en ligne le 5 juin hors horaires/lieux | Incomplet |

## Point notable

**Les Escales** a publié les **heures de début** de chaque artiste (vendredi 24 → dimanche 26 juillet). Il manque les **heures de fin** et l'affectation aux scènes pour produire un running order complet — à recontrôler dans les prochains jours, c'est le plus proche d'être finalisable.

## Domaines à investiguer (pages JS bloquantes)

- `vieillescharrues.asso.fr` — rendu JavaScript (Fragolabs). Seuls nav/footer récupérés via WebFetch. Piste API Fragolabs à explorer manuellement.

## Erreurs rencontrées

Aucune erreur de fetch. Toutes les pages ont répondu ; le contenu était lisible sauf Vieilles Charrues (JS).

## Note technique

`Chalon dans la rue` provient de `festivals_registry_extension.json` et n'a pas d'entrée dans `index.json` (réservé aux 200 festivals de musique) — non répercuté dans l'index, documenté ici uniquement.
