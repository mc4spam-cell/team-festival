# Notes d'extraction & d'enrichissement — Eurockéennes 2026 (extraction 06/06/2026)

## Sources & méthode
- **Jours par artiste** : https://www.eurockeennes.fr/programmation/ (HTML serveur, 52 entrées artistes).
- **Horaires (start)** : PDF officiel `planning-horaires-eurockeennes-2026.pdf`. L'extraction texte mélange les colonnes ; chaque paire artiste/horaire a été croisée avec la grille scène-par-scène de **sortiraparis.com** (article 50509, maj 30/04/2026). Les deux sources concordent à 100 % sur les 52 créneaux ; le créneau KT Gorique & Nash (ven. 17h30, La Plage) est aussi confirmé par FestMates/presse.
- **Heures de fin (end)** : NON publiées par le festival → **estimées** (45 min à 1h45 selon rang/scène, bornées par le set suivant et les heures de fermeture du PDF : jeu. 00h30, ven. ~02h, sam. ~03h, dim. ~01h). À considérer comme indicatives.
- Outils : WebFetch + WebSearch uniquement (APIs JSON Deezer/iTunes/MusicBrainz inaccessibles : proxy workspace en allowlist, puis rate-limit Cowork sur web_fetch ; l'enrichissement a donc été fait via WebSearch ciblé site:open.spotify.com / site:deezer.com / site:music.apple.com / site:musicbrainz.org / site:instagram.com).

## Particularités du running order
- **Jeudi 2/07** : soirée spéciale punk/rock sur la seule Grande Scène (4 concerts, portes 17h, fermeture 00h30).
- **Mosimann** clôt la Grande Scène samedi à 01h30 (confirmé sortiraparis ; inhabituel mais cohérent avec le PDF).
- **Green Line Marching Band** : fanfare « déambulatoire » ; rattachée au Chapiteau Greenroom (classement sortiraparis), point de départ à 16h45.
- **Fullmix Kinshasa (Mboka Ya Bisengo)** : 3 h de show au sein de La Loggia dimanche : Master Virus 18h30, puis DJ Ninikah & DJ Queeny D en b2b 21h30 → modélisés comme 2 entrées simultanées (21h30-23h00) pour conserver les 52 artistes listés par le festival. Le site écrit parfois « DJ Queeny DI ».
- Sets après minuit : date calendaire = lendemain, `festivalDay` = jour affiché (Vald x VC x Todiefor 00h45, Shaârghot 01h00, Nico Moreno 00h00, Enhancer 00h30, Mosimann/Coilguns 01h30…). Fuseau : +02:00 (CEST).
- Le PDF mentionne aussi une « Terrasse musicale à La Plage » (CLT Soundsystem, Mighty Worm Rockin' DJ's, ACN, Le Jeune Club, Seul Amour, Rebequita, Santo, Fadavelas, SSSound) **sans horaires exploitables** et absente de la liste artistes officielle → exclue du JSON.

## IDs laissés à null (et pourquoi)
- **Vald x Vladimir Cauchemar x Todiefor**, **KT Gorique & Nash** : projets/duos one-off sans entité propre sur les plateformes → tout à null (les artistes individuels existent, mais l'affiche est l'entité commune).
- **Madame Ose Bashung** : spectacle/cabaret hommage à Bashung (pas d'entité streaming trouvée).
- **Master Virus, DJ Ninikah, DJ Queeny D** : DJs de Kinshasa ; homonymies non résolues sur Spotify/Deezer (« Master Virus » trouvé mais correspondance avec le DJ congolais non vérifiable) → null.
- **Green Line Marching Band** : pas d'entité streaming ; Instagram vérifié (@greenlinemarchingband, « mobile rock band »).
- **MusicBrainz** : 20/52 résolus seulement. Null notamment pour Anetha, Nico Moreno, Coilguns, Ecca Vandal (résultats contradictoires : 2 MBID candidats), Shaârghot, The Lumineers, Ino Casablanca, KYBBA, et la plupart des émergents (Marie Jay, A6el, Mathis Akengin, Dove Ellis, The Sophs, GANS, Copycat, Cardinals, Madra Salach, FRS Taga, L2B, Man/Woman/Chainsaw, Upchuck, Joe Yorke, Enhancer, Bertrand Belin→résolu, etc.) — l'API MB étant injoignable, seuls les MBID exposés dans les résultats de recherche ont été retenus.
- **Spotify** : null pour Alonzo et Enhancer (page artiste non remontée par la recherche, homonymie « President »-like non prise au hasard).
- **Apple Music** : null quand seul un lien album/chanson (pas artiste) est apparu : Mathis Akengin, Joe Yorke, FRS Taga, Marie Jay, Copycat, THK.
- **Deezer** : null pour Cardinals, GANS, Joe Yorke, Madra Salach, Jehnny Beth, Dove Ellis, Mathis Akengin (pas de page artiste sûre).
- **Instagram** : renseigné uniquement quand vérifié via résultat de recherche ou fiche artiste officielle (offspring, orelsan, ayanakamura_officiel, jehnnybeth, frs_taga, nicomoreno_music, greenlinemarchingband). Les fiches artistes du festival contiennent les liens IG mais le rate-limit web_fetch a empêché de toutes les récupérer.

## Doutes résiduels
- **Trym** : AppleMusicId 187278791 et DeezerId 4219770 correspondent au slug/nom « TRYM » mais les IDs semblent anciens pour un artiste récent → homonymie possible (ex. Trym Torson) ; MBID laissé null pour cette raison.
- **Upchuck** : DeezerId 1069689 ancien pour un groupe formé en 2018, mais la page est remontée en tête sur une requête « Upchuck Atlanta » → conservé.
- **THK feat Brass Orchestra** : IDs du groupe THK (Tetra Hydro K) utilisés ; l'affiche inclut un brass orchestra invité.
- **Ben Harper & The Innocent Criminals** : Deezer possède deux entités (« & » id 16738, « And » id 214997) ; 16738 retenu.
- Heures de **fin** estimées (voir plus haut) ; les heures de **début** sont quant à elles sourcées.
