# Notes d'enrichissement — Running Orders Incomplets (lot B)

Date d'extraction : **06/06/2026**. Outils : WebFetch + WebSearch uniquement (pas de navigateur).

## Fichiers produits

| Fichier | Concerts | Artistes uniques | Jours connus | Scènes connues |
|---|---|---|---|---|
| running_order_rockenseine_2026.json | 58 | 58 | Oui (5 jours) | Non (TBD) |
| running_order_cabaretvert_2026.json | 94 | 84 | Oui (4 jours) | Oui (5 scènes, partiellement déduites) |
| running_order_deferlantes_2026.json | 44 | 40 | Oui (3 jours) | Non (TBD) |
| running_order_delta_2026.json | 107 | 107 | Non (festivalDay null) | Non (TBD) |
| running_order_fetehumanite_2026.json | 21 | 21 | Non (festivalDay null) | 4 scènes listées mais affectation inconnue → TBD |

## Sources par festival

- **Rock en Seine** : rockenseine.com bloqué au fetch (comme prévu). Line-up jour par jour repris de Sortiraparis (article 64022, maj 21/04/2026), qui publie les 5 listes quotidiennes (en-têtes typographiés « 2025 » dans l'article, mais contexte clairement 2026). « Kingfishhr » dans l'article = orthographié **Kingfishr** dans le JSON. Chaque jour se termine par « … » → line-up non exhaustif.
- **Cabaret Vert** : cabaretvert.com/programmation/ (HTML, liste « Load More » partielle) + page d'accueil (line-up complet par jour). Les sous-pages /programmation/zanzibar/, /greenfloor/, /razorback/, /zion-club/, /illuminations/ **redirigent vers la home** (contenu AJAX non rendu). Affectation aux scènes **déduite de l'ordre des blocs** de la page programmation (ordre des filtres : Zanzibar → Illuminations → Greenfloor → Razorback → Zion Club). Zanzibar (têtes d'affiche) et Zion Club (bloc reggae/dub) sont sûrs ; **Greenfloor/Razorback à confirmer** (la soirée Teletech du jeudi pourrait être sur Razorback). Le dernier bloc (Aibohphobia, Goya, Amper, Navyblu, Heb, Louise XIV, Damaghead, Thomas Schmahl, Jeri) ne correspond à aucun filtre de scène → stage « TBD » (probable scène locale/émergente). Résidents Zion Club (Asher Selector, Matilda Dona, Kiraden ×4 jours ; Young Kulcha ×2) = entrées multiples.
- **Les Déferlantes** : festival-lesdeferlantes.com/programmation/ — line-up par jour (blocs 11/07, 12/07, 13/07 dans l'ordre des onglets), aucune scène publiée. Athmos, Xea, Secte Sound System et Theo1337 jouent 2 jours → 2 entrées chacun.
- **Delta Festival** : line-up officiel récupéré via **Shotgun** (shotgun.live, billetterie officielle, ~100 noms) + article handsupelectro.fr (05/02/2026) pour Cassius, Meduza, Ebony, PH4, RDD. touslesfestivals.com affichait « Programmation à venir ». Aucune répartition jour/scène publiée → festivalDay null. Doublons Shotgun fusionnés (Lily/Lilly Palmer ; Deize Tigrona ×2). « medusa » (profil Shotgun ambigu) non retenu, remplacé par Meduza (presse).
- **Fête de l'Humanité** : fete.humanite.fr bloqué au fetch. Liste info-festival.net (27/04/2026) : 11 nouveaux (UB40, Oxmo Puccino, Josman, Gauvain Sers, Aïta Mon Amour, Arøne, Kutu, Rori, Naza, Sam Sauvage, Trinix) + déjà confirmés (Louane, Soprano, Superbus, The Limiñanas, Oumou Sangaré, Maureen, Théa, Yuri Buenaventura, Houdi, 2L). NB : l'article mentionne par erreur « Parc de la Courneuve » et « 11-14 septembre » ; dates/lieu retenus : 11-13/09/2026, Plessis-Pâté (recoupé jds.fr / agendaculturel). Scènes connues (Grande Scène, Commune, Zéphyr, Zébrock) listées dans `stages`, mais aucune affectation artiste→scène publiée → TBD. Patti Smith, Eddy de Pretto, Hoshi etc. cités par certains agrégateurs n'ont **pas** été retenus (probable reliquat 2025, non recoupé).

## Enrichissement (têtes d'affiche en priorité)

- **MusicBrainz (vérifié le 06/06/2026 via site:musicbrainz.org)** — 17 affectations sur 13 artistes : The Cure (69ee3720), Tyler, The Creator (f6beac20), Lorde (8e494408), Nick Cave & The Bad Seeds (172e1f1a, ×2 fichiers), Deftones (7527f6c2, ×2), Turnstile (7b748dac, ×2), Kygo (ba0e7638), Gims (b2fbd053, ×2), Aya Nakamura (cf580d82), Armin van Buuren (477b8c0c), Kungs (a125cd83, ×2), UB40 (7113aab7).
- **Deezer (vérifié via site:deezer.com)** — The Cure 381, Deftones 535 (×2), Martin Garrix 3968561, Aya Nakamura 8909272. L'API api.deezer.com était inaccessible (rate limit du proxy WebFetch) ; le reste → null.
- **Apple Music / iTunes** — API search inaccessible pendant la session (rate limit) → tous null.
- **Spotify** — pas d'API publique interrogeable sans auth : IDs renseignés **de mémoire, non vérifiés en ligne ce jour**, uniquement pour 8 artistes très connus (The Cure, Tyler, Lorde, Deftones ×2, Kygo, Sean Paul, Martin Garrix, Armin van Buuren). À revalider.
- **Instagram** — handles renseignés quand quasi certains (souvent confirmés par les slugs Shotgun, ex. kungsmusic, mosimannofficial, lilly_palmer, officialrebelion…) ; sinon null. Non vérifiés un par un sur instagram.com.
- Homonymies non résolues laissées à null (ex. Miki [FR vs autre], Theodora, Josman Deezer/Spotify, Speed, Goya, Ciel, STV, William, Paloma, Zed…).

## Bilan IDs (hors instagramHandle)

| Fichier | appleMusic | musicBrainz | spotify | deezer |
|---|---|---|---|---|
| Rock en Seine | 0 | 6 | 4 | 2 |
| Cabaret Vert | 0 | 5 | 2 | 1 |
| Déferlantes | 0 | 4 | 3 | 2 |
| Delta | 0 | 1 | 0 | 0 |
| Fête de l'Humanité | 0 | 1 | 0 | 0 |
| **Total** | **0** | **17** | **9** | **5** |

## Limites / TODO

1. Horaires (start/end) : non publiés pour les 5 festivals → null partout, status « Running Order Incomplet ».
2. Rock en Seine : répartition par scène attendue plus tard ; listes quotidiennes encore incomplètes (« … »).
3. Cabaret Vert : confirmer Greenfloor vs Razorback quand le site rendra les pages scènes.
4. Delta / Fête de l'Huma : injecter festivalDay dès publication des affiches par jour.
5. Compléter l'enrichissement (Deezer/iTunes en masse) quand le quota WebFetch sera réinitialisé.
