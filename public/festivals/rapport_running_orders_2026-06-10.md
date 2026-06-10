# Rapport running orders — 2026-06-10

## 0 nouveau running order complet aujourd'hui

Aucun festival de la fenêtre (1re date entre **2026-06-10 et 2026-08-05**) n'a publié de **running order horodaté complet** (artiste + scène + heure de début **et** heure de fin) ce matin. Constat identique à hier : les festivals examinés publient au mieux des **heures de début** (sans heure de fin) ou de simples **line-ups par jour**. Conformément à la consigne, aucun horaire n'a été inventé → tous restent en `Running Order Incomplet`.

## Périmètre examiné

- **2 948 entrées** chargées (2 018 musique + 930 hors-musique, convention `festivals_musique.json` / `festivals_hors_musique.json`).
- **111 festivals sélectionnés** : 1re date dans la fenêtre, `statut_2026` ≠ annulé/arrêté/hors-fenêtre, dates `dates_2026` parsables. **102 musique + 9 hors-musique.**
- **Couverture : 111/111 ont désormais un fichier dans `public/festivals/`** (objectif v2 atteint — tout festival à venir est visible dans le sélecteur de l'app, marqué « Programmation à venir »).
  - **97 nouveaux squelettes** `<slug>-2026.json` créés (`status: "Running Order Incomplet"`, `days` peuplés depuis `dates_2026`).
  - **14 festivals déjà couverts** par un fichier existant (Hellfest + 13 `running_order_*.json` legacy / squelettes antérieurs). **9 squelettes en doublon ont été supprimés** au profit du fichier legacy (qui contient le travail d'extraction antérieur) — voir note technique.

## Fetch ciblé du jour (festivals imminents)

Re-contrôle des festivals les plus proches, les plus susceptibles d'avoir publié une timetable complète depuis hier :

| Festival | Dates | Constat |
|---|---|---|
| Solidays | 26-28 juin | Line-up par jour uniquement, pas de timetable |
| La Bonne Aventure | 19-21 juin | Line-up + jour, pas d'horaires |
| Les Nuits Carrées | 18-20 juin | Line-up par jour, ni scène ni horaires |
| Festival de Carcassonne | concerts juil. | Série de concerts : date + heure de début + lieu, **pas d'heure de fin** |
| Marsatac | 12-14 juin | Line-up par jour, pas d'horaires |
| Free Music Festival | 19-20 juin | Line-up confirmé, pas de scènes/horaires |
| Les Escales | 24-26 juil | Heures de **début** seules (« Josman – ven. 24/07 – 22:15 »), fin absente |
| Guitare en Scène | 14-18 juil | Heures de **début** seules par jour, fin absente |

→ Aucun ne franchit le seuil « horaires début **et** fin » requis pour `Complet`.

## Festivals proches d'être finalisables (heures de début publiées, fin manquante)

À recontrôler les prochains jours (publient les heures de début, parfois les scènes) : **Les Escales**, **Guitare en Scène**, **Les Suds à Arles**, **Jazz à Juan**, **Festival de Colmar**, **La Roque-d'Anthéron**, **Rock en Stock**. Festival de Carcassonne publie aussi date + début + lieu pour chaque concert (format série, pas grille festival).

## Domaines à investiguer (pages JavaScript / non contournées)

Inchangé depuis hier (rendu JS, conformément à la consigne aucun curl/headless tenté) :
`garorock.com`, `delta-festival.com`, `vieillescharrues.asso.fr` (Fragolabs), `europavox.com`, `avoinezonegroove.fr`, `festival-avignon.com` (IN), `petethemonkey.com`, `programme.annecyfestival.com` (OAuth).

## Erreurs / sites injoignables (rappel hier)

TLS/connexion à recontrôler : `thiers.fr`, `thepeacocksociety.fr`, `stereoparc.fr`, `lesfeuxdelete.fr`, `festival-sully.loiret.fr`, `musiquesenstock.com`, `festivalaufilduson.fr`, `rencontresetracines.fr`, `leskampagnarts.fr`, `binicfolksbluesfestival.com`, `countryrendezvous.com`. HTTP : `festival-aix.com` (404), `festival-poupet.com` (redir), `festivalduperigordnoir.fr` (403).

## Note technique

- `derniere_verification` mise à 2026-06-10 sur les **111 festivals examinés** (102 dans `festivals_musique.json`, 9 dans `festivals_hors_musique.json`). Aucun autre champ touché ; `statut_2026` (manuel) inchangé. Aucun `running_order_file` ajouté (pas de RO horodaté extrait).
- **Dé-duplication** : la convention de slug NFKD (`<slug>-2026.json`) ne matchait pas les fichiers legacy `running_order_<slug-court>_2026.json`. 9 squelettes vides créés en doublon ont donc été supprimés pour préserver le travail antérieur : Jazz à Vienne, Eurockéennes, Nuit de l'Erdre, Main Square, Francofolies, Déferlantes, Vieilles Charrues, Delta, Interceltique. Le fichier legacy reste la source de vérité pour ces festivals.
