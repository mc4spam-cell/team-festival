# Notes d'extraction & d'enrichissement — Jazz à Vienne 2026

**Date d'extraction : 06/06/2026**

## Sources
1. **PDF timeline officiel** : `JAV26_Timeline_Digital.pdf` (jazzavienne.com, dossier 2026-05 ; le site pointe aussi vers une version 2026-06 identique) — source principale, programme complet jour par jour.
2. **Site web** : https://www.jazzavienne.com/fr/edition-2026 (Drupal 11, filtre `?date=AAAA-MM-JJ`) — utilisé pour vérifier le mapping jour↔contenu et les horaires de fin publiés.

## Méthode de mapping des jours (PDF)
Dans l'extraction texte du PDF, les libellés de jours apparaissent **après** le bloc de contenu correspondant (artefact de mise en page en colonnes). Le mapping a été vérifié par recoupement avec le site sur 3 dates ancres :
- 25/06 : Stefano Di Battista / Erik Truffaz (soirée d'ouverture, Scène Sacem) ✓
- 27/06 : Soirée Blues (Samantha Fish / Fantastic Negrito / Chicago Blues Summer) + Swing System 12h-12h50 + Big band CAPI 13h10-14h ✓
- 11/07 : All Night (Vulfpeck, Fearless Flyers, Ludivine Issambourg, Souleance, ubaq) + Big Band Groove 12h30-13h20 + SuperMega SuperCool Révolution 16h-17h ✓

Le bloc « Lun. 22.06 / Mar. 23.06 — Création jeune public Ronja (Vincent Peirani) » est **hors périmètre** (avant-festival, réservé aux scolaires) et n'a pas été inclus.

## Périmètre retenu (165 entrées)
- Inclus : tous les sets musicaux horodatés, toutes scènes (Théâtre Antique, Scène de Cybèle, Kiosque, Jardin de Cybèle, Le Club, Jazz Ô Musée au musée gallo-romain, église Saint-Pierre, concerts hors-les-murs : stade nautique, place Simone Veil, second line, belvédère de Pipet, Verrière des Cordeliers, spectacles musicaux Jazz for Kids).
- **Doubles sets comptés séparément** (ex. combos Kiosque « 21h & 22h15 », DJ sets « 23h15 & 01h » = 2 entrées chacun).
- Exclus : expositions, ateliers (sérigraphie, badges, moulage, Posca), jeux, bibliothèque en plein air, lectures « Lettres sur Cour », émission « Radio Jazz à Vienne » (18h15 quotidienne), petit-déjeuner, salon du disque, balade musique & danse, Poésie Couchée, mise en graphie.
- Le chiffre officiel « 167 concerts » dépend du mode de comptage du festival ; avec les critères ci-dessus on obtient 165 sets horodatés. L'écart probable vient d'événements borderline exclus ici (ex. Radio JAV en public, lectures musicales).

## Heures de fin — méthode d'estimation
Très peu d'heures de fin sont publiées. Règles appliquées (signalées ici, non distinguées dans le JSON) :
- **Fin publiée par le site** (reprises telles quelles) : Big band INSA 12h30-13h30, Vincent Schmidt & Natan Niddam 16h-17h, Swing System 12h-12h50, Big band CAPI 13h10-14h, Big Band Groove 12h30-13h20, SuperMega SuperCool Révolution 16h-17h, Rêves cosmiques 16h-16h30, Minos 15h-16h, Ballade avec Miles (non retenu : atelier).
- **Théâtre Antique** : fin d'un set = début du set suivant sur la même scène ; dernier set du soir = +90 min.
- **Le Club (minuit)** : +75 min (durée standard).
- **Scène de Cybèle / Kiosque / Jazz Ô Musée / hors-les-murs** : +60 min par défaut ; DJ sets +90 min ; spectacles jeune public +45 min ; second line +90 min ; Ōazin +75 min ; LGMX +90 min ; Jazz On The Water +75 min ; rendez-vous du tout-monde +90 min ; Live House Collective (All Night, 22h30) : fin estimée 01h30.
- **Sets après minuit** : date calendaire = lendemain, `festivalDay` = jour affiché (ex. Le Club 00h du « jeudi 25 » → 2026-06-26T00:00, festivalDay 1 ; Ludivine Issambourg 01h30 et Souleance 03h15 de l'All Night → 2026-07-12, festivalDay 17).

## Enrichissement des artistes
Contraintes rencontrées : rate limit sévère du tool WebFetch (les appels directs aux API Deezer/MusicBrainz ont échoué : corps vide ou HTTP 429). Bascule sur **WebSearch restreint aux domaines** musicbrainz.org / deezer.com / music.apple.com / open.spotify.com / instagram.com : une requête par artiste, correspondance de nom vérifiée sur le titre des résultats.

**31 entrées Théâtre Antique enrichies (priorité 1)** : Erik Truffaz, Stefano Di Battista, Too Many Zooz, Deluxe, Fantastic Negrito, Samantha Fish, Sun Ra Arkestra, Jeff Mills, Kokoroko, Groundation, Molly Johnson, Imany, Vincent Peirani, Beirut, Kyoto Jazz Massive, Cerrone, Terence Blanchard (& Ravi Coltrane → IDs de Blanchard, tête d'affiche), Marcus Miller, Fatoumata Diawara, Angélique Kidjo, Lakecia Benjamin, De La Soul, Big Freedia, Jon Batiste, Melissa Aldana, Maria Schneider, Samara Joy, The Fearless Flyers, Vulfpeck, Ludivine Issambourg, Souleance.

**Champs laissés à null malgré l'enrichissement (non vérifiables sans ambiguïté)** :
- Spotify d'Angélique Kidjo et de Jon Batiste : deux profils Spotify homonymes chacun dans les résultats (doublons de plateforme) → null.
- Deezer : Stefano Di Battista, Sun Ra Arkestra, Marcus Miller, Maria Schneider (pas de profil solo confirmé dans les résultats).
- MusicBrainz : Deluxe, Marcus Miller, The Fearless Flyers, Ludivine Issambourg, Souleance (MBID non remonté).
- Instagram : Vulfpeck, Kyoto Jazz Massive, Terence Blanchard, Lakecia Benjamin (handle non confirmé). Pour Beirut, le compte retourné est @zachcondon (leader unique du projet).

**Têtes d'affiche TA restées entièrement à null (5 sets)** : Chicago Blues Summer (plateau ad hoc), VERB (Génération Spedidam), The Getdown, Buena Vista All Stars (plateau hommage, ≠ Buena Vista Social Club), ubaq — homonymie/projet non résolu → null.

Les 134 autres entrées (Cybèle, Kiosque, Club, Jazz Ô Musée, hors-les-murs) sont à null, le temps imparti ayant été consacré à la complétude des horaires (consigne : priorité absolue).

Cas d'homonymie identifiés parmi les non-enrichis : « AMG », « Kiefer », « Soba », « TREK », « Buck », « Intermed », « Just Friends », « Jéroboam », « Indawa », « Tatanka ».

Projets ad hoc / ensembles pédagogiques (pas d'IDs streaming pertinents, null par construction) : big bands et combos des conservatoires (CRR Lyon/Saint-Étienne, ENM Villeurbanne, Conservatoire de Vienne, CAPI, INSA), jams (Périscope, JazzUp, LGTDZ), restitutions de stage, Chicago Blues Summer (plateau), Buena Vista All Stars (plateau hommage), Jazz On The Water, Radio JAV.

## Doutes / points d'attention
- « The Honnet Brothers 21h & 22h15 » : le PDF indique **Scène de Cybèle** (et non le Kiosque comme les autres soirs) — repris tel quel.
- Jour 4 (dim 28/06) et jour 11 (dim 05/07) : pas de concert au Théâtre Antique (« Super Dimanches » gratuits en ville) — cohérent avec le PDF et la page « Les Super Dimanches » du site.
- Horaires de début non publiés au-delà du créneau pour certains événements continus (expos, ateliers) : exclus du périmètre.
- L'All Night du 11/07 est annoncé [COMPLET] sur le site.
