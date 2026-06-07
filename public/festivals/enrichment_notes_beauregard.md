# Notes d'extraction et d'enrichissement — Beauregard 2026

Extraction effectuée le **06/06/2026**. Source principale : https://www.festivalbeauregard.com/fr/artistes (HTML statique, cartes artistes avec jour/scène/heure de début) + fiches artistes officielles (jour, heure, liens sociaux). Recoupement presse : sortiraparis.com (confirmation du « Day Before » du mercredi 1er juillet avec Bob Sinclar, Aya Nakamura, Macklemore).

## Périmètre
- **51 concerts** : 40 sets sur les scènes Beauregard/John + 11 créneaux DJ au Studio.
- 5 jours (mer 01/07 « The Day Before » → dim 05/07).

## Anomalies d'URL (jour affiché faisant foi)
Les URLs des fiches d'Aya Nakamura, Macklemore et Bob Sinclar contiennent `2026-07-05` alors que la carte ET la fiche affichent **« Mercredi 1 Juillet »** ; la presse (sortiraparis) confirme le 01/07. Le jour affiché a été retenu. Même logique pour toutes les autres cartes (URL = simple slug, parfois incohérent).

## Heures de fin (TOUTES ESTIMÉES)
Le site ne publie aucune heure de fin. Méthode :
- **Sets suivis d'un autre set sur la même scène** : `fin = min(début du set suivant − 15 min de changement de plateau, début + 90 min)`. Le plafond de 90 min évite des durées irréalistes quand le trou entre deux sets dépasse 2 h (ex. Dynamite Shakers 17h50 → Girls In Hawaii 20h00).
- **Dernier set de la scène** : durée standard de **75 min**.
- Validation ponctuelle : la fiche officielle d'Orelsan annonce « un show de 01h30 » → 22h10–23h40, exactement ce que donne la règle.
- **Le Studio** : le site n'affiche qu'une heure par « bloc » de 2 noms (ex. VEN 21h30 : Musique Large + Fulgeance b2b Rekick ; bloc suivant 23h45). Chaque bloc a été scindé en deux sets consécutifs de durée égale (split au milieu, sans changement de plateau) ; dernier bloc de la nuit estimé à ~2h15 (JEU : fin 01h00 ; SAM : fin 01h45 ; DIM : Carlala 75 min). **Fiabilité faible** sur les heures internes des blocs Studio.
- Sets après minuit : date calendaire = lendemain, `festivalDay` = jour affiché (ex. CASSIUS « Jeudi 2 juillet à 01h00 » → start 2026-07-03T01:00, festivalDay 2).

## Enrichissement — sources et vérification
- **Instagram** : en priorité depuis les **fiches officielles du festival** (orelsan, macklemore, bobsinclar, welovepulp, cassius, thylacine_music, gihband, lamano1.9, the_lanskies, dynashakers). Le quota de fetch ayant été atteint, le reste a été vérifié via recherche web `site:instagram.com` (compte principal/vérifié à fort following). 29/51 renseignés.
- **MusicBrainz** : API ws/2 (via proxy r.jina.ai) + recherche `site:musicbrainz.org`. 30/51 renseignés.
- **Deezer / Apple Music / Spotify** : API Deezer + recherche web sur deezer.com, music.apple.com (l'ID est dans l'URL), open.spotify.com. Correspondance de nom + contexte (pays, genre, discographie) vérifiée à chaque fois.

## Cas particuliers / doutes
- **Vald x Vladimir Cauchemar x Todiefor** : projet collaboratif ponctuel sans entité streaming propre → tous les IDs à `null` (les 3 artistes ont chacun leurs propres pages).
- **Trym** : homonymie sur Deezer (2 profils « TRYM/Trym ») et Apple Music → `null` ; Spotify retenu (profil hard techno FR, top résultat).
- **L2B** : ex-« L2B Gang » ; deux pages distinctes sur Apple Music (L2B / L2B Gang) → Apple `null` (homonymie non tranchée).
- **Theodora** : MBID `e560c294-…` retenu (artiste féminine FR nommée « Theodora » sur MusicBrainz) — confiance moyenne ; Instagram non tranché (plusieurs comptes) → `null`.
- **Aya Nakamura (Spotify)** : deux pages artistes remontent (`7IlRNXHjoOCgEAWN5qYksg` et `2ZMUUiMBKFxVjRuAudGSks`) ; la première (titre canonique « Aya Nakamura | Spotify ») a été retenue.
- **Vanessa Paradis** : compte IG `vanessa.paradis` (374K, top résultat) — pas de badge vérifiable hors plateforme, confiance moyenne.
- **HOUDI** : Apple Music non trouvé de façon fiable → `null`.
- **Headcharger, Nico Moreno, La Mano 1.9, Dynamite Shakers, JOLAGREEN23, Louise Charbonnel, HOUDI, L2B, Trym** : pas d'entrée MusicBrainz identifiable avec certitude → `null`.
- **DJs du Studio** (M.A.D Brains, Fred H, Vince Vega, Musique Large, Fulgeance, Rekick, Senary, Vertuoze, HCo, Swump, Dj Claude, Holasisi, Gow With the Rythm, NE2L, Miki Ménage, Carlala) : DJs locaux/b2b, risque d'homonymie élevé et créneaux multi-artistes → tous IDs `null`.
- `appleMusicPlaylist` : aucune playlist officielle festival identifiée → `null` partout.

## Statistiques de complétude (51 concerts)
| Champ | renseignés | null |
|---|---|---|
| spotifyArtistId | 39 | 12 |
| deezerArtistId | 37 | 14 |
| appleMusicArtistId | 36 | 15 |
| musicBrainzId | 30 | 21 |
| instagramHandle | 29 | 22 |
| appleMusicPlaylist | 0 | 51 |
