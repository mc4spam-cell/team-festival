# Notes d'enrichissement — Running order Musilac 2026
Extraction : 06/06/2026. Source principale : https://www.musilac.com/programmation/horaires (HTML statique, 4 blocs jour sans libellé, ordre Jeu→Dim confirmé).

## Vérification des dates et des jours
- Dates 2026 : Jeu 09.07, Ven 10.07, Sam 11.07, Dim 12.07 (onglets de la page horaires + infoconcert.com "du 09 au 12 juillet 2026").
- Attribution des blocs jours recoupée par : presse (France Bleu : "Katy Perry annoncée le jeudi 9 juillet" ; jds.fr ; musiquealliance.fr : Gorillaz/Jamiroquai samedi, Charlotte Cardin/Bigflo & Oli dimanche) et par les fiches artistes officielles qui affichent le jour (Katy Perry "Jeu. 09.07 / 22:00 > 23:30", Sahëlie "Tremplin | Ven. 10.07", Malaka "Tremplin | Jeu. 09.07", etc.).
- Sets après minuit (Sam Quealy, Yelle fin, LB aka LABAT, Lost Frequencies fin, Deki Alem, Gorillaz fin, Cassius, Bigflo & Oli fin) : date calendaire = lendemain, festivalDay = jour affiché. Cassius (Dim) joue 00:45–01:45 le lundi 13/07.

## Méthode et contraintes
- Fiches officielles musilac.com consultées pour les 10 artistes du jeudi + Sahëlie : elles fournissent l'Instagram officiel et l'ID Deezer (widget). Un rate limit du tool de fetch (persistant ~1h) a bloqué les 29 autres fiches → enrichissement complété via WebSearch (site:musicbrainz.org, site:open.spotify.com, site:music.apple.com, site:deezer.com, Instagram).
- MusicBrainz API et api.deezer.com/itunes inaccessibles en direct (réponses vides / proxy sandbox bloqué) ; r.jina.ai a fonctionné ponctuellement (iTunes API : Katy Perry 64387566 vérifié).

## Confiances et cas particuliers
- "Flora Fishbach" : slug de fiche musilac = "iliona" (item CMS recyclé) mais nom affiché partout Flora Fishbach = Fishbach (MBID 92aa0d60..., nouvelle identité Apple Music 1812398144 "Flora Fishbach", ancienne fiche "Fishbach" 1038504532 non utilisée).
- Tremplin AURA (lauréats régionaux, ouverture 17:10) : Malaka (jeu), Sahëlie (ven), Monika (sam), Arkange (dim) — confirmé par jds.fr/odsradio. Quasi aucune présence streaming → IDs null.
- Monika : IG "monika.wav" (électropunk, Insolent Records) — confiance moyenne.
- Arkange : chanteuse lyonnaise (EP "In Somnis") ; IG non vérifié → null.
- Rallye : Deezer 51907722 (fiche officielle) ; candidats Spotify 36buOdDVz6p3QbVatFi4iX / Apple 1436716608 NON retenus (homonymie non résolue → null).
- Lancelot : Spotify 0cjANFdMCgGD2q55ME1iyt et Apple 1625659341 vérifiés via tracks "Guéri"/"Pas assez" (label Six et Sept).
- Danyl : rappeur franco-algérien (album ZMIG 01/2026, IG danylchulo) ; Apple 960029524 (page listant ZMIG) ; Spotify 5Hq9W3lm1N9KRCf35RBMab et Deezer 75818912 confiance moyenne-haute (croisés via kworb/album ZMIG).
- Romsii : Apple 1443682592 obtenu via Shazam (adamid = Apple Music ID) ; Spotify ambigu (2 pages) → null ; IG "whoisromii" relié via "Nototune".
- Myra : identité confirmée par recoupement EP "Après la pluie"/album "YAPI" sur Spotify, Apple et Deezer (182425).
- MBID via Wikidata (P434) pour Last Train (4f527225...), Léonie Pernet (0de4c4c7...), Lambrini Girls (4eeea7eb...).
- Sam Quealy, Bianca Costa, Deki Alem, Yuston XIII, Camille Yembe, Styleto, Gildaa, Dinaa, LB aka LABAT, Zélie, Danyl, Romsii : présents dans MusicBrainz incertain/introuvable via recherche → MBID null.
- Deezer IDs des fiches officielles (jeudi) : Katy Perry 144227, Zélie 5435777, Rallye 51907722, Lancelot 175066527, Sam Quealy 133632762, Lambrini Girls 114111122, Feu! Chatterton 5623138, Yelle 9009, Last Train 5080091. Malaka/Sahëlie : pas de lien Deezer sur leur fiche.

## Bilan
- 40 concerts, 3 scènes, 4 jours. 200 champs d'IDs (5×40) : 164 résolus, 36 null.
- Par champ : MusicBrainz 21/40, Spotify 34/40, Apple Music 34/40, Deezer 36/40, Instagram 39/40.
- appleMusicPlaylist : null partout (aucune playlist officielle par artiste identifiée).
