# Notes d'extraction et d'enrichissement — Printemps de Bourges 2026 (50e édition)

Extraction réalisée le **06/06/2026** depuis https://www.printemps-bourges.com/programmation-generale/ (HTML statique, filtres `?jour=2026-04-14` à `?jour=2026-04-19`, 6 pages fetchées via WebFetch).

## Périmètre
Programmation générale payante uniquement (onglet « TOUS » de la programmation), telle qu'affichée sur les 6 pages-jour. Cela inclut les sessions iNOUïS du 22 (12h30 et soirées) qui figurent sur ces pages. **112 concerts, 10 salles, 6 jours.**

## Plateaux partagés
Le site ne publie qu'un horaire **par plateau** (spectacle), pas par artiste. Conformément à la consigne, chaque artiste d'un plateau a reçu un concert distinct avec le **même créneau start/end** que le plateau. Tous les plateaux multi-artistes sont concernés (ex. Le W chaque soir, Le 22, Auditorium 17h, etc.). L'ordre de passage réel et les horaires individuels ne sont pas publiés.

## Heures de fin (estimées — non publiées)
Méthode d'estimation, appliquée uniformément :
- **Spectacle à 1 artiste** : start + 90 min.
- **Plateau multi-artistes (salles classiques)** : start + 45 min × nb d'artistes (sets + changements).
- **Le 22 Est & Ouest** (deux scènes alternées dans le même lieu, sessions iNOUïS et soirées club) : start + 30 min × nb d'artistes.
- Aucun chevauchement avec le plateau suivant de la même salle n'a été constaté après calcul.
- Sets finissant après minuit : date calendaire du lendemain dans `end`, `festivalDay` inchangé (ex. Le 22 jeu/ven/sam ; samedi 22:00 → fin estimée 02:00 le 19/04). `end` > `start` partout.

## Salles ("stages")
Le W ; Palais d'Auron Daniel Colling ; Le 22 Est & Ouest ; L'Auditorium ; Palais Jacques Coeur ; MCB Pina Bausch et MCB Gabriel Monnet (deux salles de la Maison de la Culture, distinguées) ; Cathédrale ; Antre Peaux : Le Nadir ; Théâtre Jacques Coeur. Couleurs attribuées arbitrairement.

## Enrichissement des IDs
Sources : recherches web restreintes aux domaines musicbrainz.org, open.spotify.com, deezer.com, music.apple.com (l'accès direct aux APIs JSON — MusicBrainz WS, api.deezer.com, itunes.apple.com — était bloqué dans cet environnement, y compris via proxy ; le shell n'a pas d'accès réseau). Vérification par correspondance de nom dans les titres/URLs des résultats.

**26 artistes enrichis** (priorité aux têtes d'affiche) : Patti Smith (Quartet), Abd Al Malik, Feu! Chatterton, Imany, Charlie Winston, Oxmo Puccino, Charlotte Cardin, Ofenbach, Helena, Superbus, Suzane, Camille, Slift, Vladimir Cauchemar, Mosimann, Magic System, Deluxe, Biga*Ranx, Philippe Katerine, Dominique A, Yael Naim, Josman, Maureen, Donovan, GIMS.

Comptage des champs résolus (sur 112 concerts) :
- musicBrainzId : 19 résolus / 93 null
- spotifyArtistId : 24 / 88 null
- deezerArtistId : 23 / 89 null
- appleMusicArtistId : 21 / 91 null
- instagramHandle : 12 / 100 null
- appleMusicPlaylist : 0 (non recherché, null partout)

## Doutes et choix signalés
- **Patti Smith Quartet** : enrichi avec les IDs de Patti Smith (le « Quartet » n'a pas de profils propres).
- **GIMS / Spotify** : deux profils trouvés (« GIMS » `0GOx72r5AAEKRGQFn3xqXK` et « Maître Gims » `4xGSmbmXOgsCmLUcC7jLpp`). Retenu celui titré « GIMS ». À revérifier.
- **Philippe Katerine / Deezer** : deux profils (281496 et 259588732) ; retenu 281496 (profil historique). Spotify : retenu `61NKNrhSMTYg2q0f3vS46e` (page concerts active) parmi deux candidats.
- **Yael Naim / Spotify** : deux IDs candidats sans départage fiable → null.
- **Suzane / Apple Music** : ID candidat trop ancien pour l'artiste (homonymie probable) → null.
- **Helena, Deluxe, Magic System, Camille, Maureen, Donovan** : champs partiellement null faute de résultat non ambigu (homonymies fréquentes : Helena, Deluxe, Camille, Maureen, Aurore, Ebony, Noor, Augusta…).
- **Instagram** : handles fournis de mémoire pour des artistes très connus (non revérifiés en ligne ce jour) : thisispattismith, gims, charlottecardin, ofenbachmusic, feuchatterton, magicsystemofficiel, oxmopuccino, dominiqueaofficiel, bigaranx, slift_band, deluxefamily, suzaneofficiel, vladimircauchemar. Fiabilité élevée mais non garantie.
- **Mardi 14 et dimanche 19** : un seul plateau payant chacun sur la page (soirée d'ouverture Palais d'Auron ; clôture GIMS au W) — cohérent avec la page, pas une omission.
- La fiche W du mercredi mentionne « Vanessa Paradis » dans l'URL billetterie mais l'artiste ne figure pas dans la liste affichée du plateau → non inclus (probable annulation/changement de programmation).
- Les artistes marqués « (Talent iNOUïS) » sur le site conservent cette mention dans le champ artist.
