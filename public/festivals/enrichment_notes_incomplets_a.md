# Notes d'enrichissement — Running Orders Incomplets (lot A)

Date d'extraction : **06/06/2026**. Statut commun : `Running Order Incomplet` (line-up publié, horaires non publiés ; `start`/`end` = null partout).

## Sources line-up

| Festival | Source line-up | Granularité obtenue |
|---|---|---|
| Les Vieilles Charrues 2026 (16–19 juil., Carhaix) | Page officielle vide au fetch (rendu JS) → Sortiraparis (article 50235, « programmation complète par jour »), recoupé jds.fr | artiste + jour (61 concerts), scènes non publiées → `TBD` |
| Solidays 2026 (26–28 juin, Longchamp) | https://www.solidays.org/programmation/ (HTML officiel) | artiste + jour (63 concerts), scènes non publiées → `TBD` |
| Francofolies de La Rochelle 2026 (10–14 juil.) | https://www.francofolies.fr/programmation/ (HTML officiel, scène + jour affichés par fiche) | artiste + scène + jour (71 concerts), 7 scènes connues (Rochelle Océan listée dans les filtres mais sans artiste annoncé à date) |
| Garorock 2026 (25–28 juin, Marmande) | https://www.garorock.com/fr/programmation (HTML officiel) | artiste + jour (73 concerts dont 2 sets distincts de Secte Sound System sam+dim), jeudi 25 = warm-up ; scènes non publiées sur la page → `TBD` |

## Méthode d'enrichissement

- API Deezer via proxy `r.jina.ai` (le fetch direct renvoyait un corps vide) jusqu'à un rate limit du fetcher → bascule sur WebSearch avec restriction de domaine (musicbrainz.org, deezer.com, open.spotify.com, music.apple.com, instagram.com). Les IDs sont extraits des URLs canoniques retournées.
- Priorité aux têtes d'affiche, le reste laissé à null (« null sinon »).
- `appleMusicPlaylist` : null partout (non recherché).

## IDs vérifiés (appliqués dans tous les fichiers où l'artiste apparaît)

| Artiste | MusicBrainz | Spotify | Deezer | Apple Music | Instagram |
|---|---|---|---|---|---|
| Orelsan | 6cad3ce5-6380-4594-a8da-ae7d273b683d | 4FpJcNgOvIpSBeJgRg3OfN | 259467 | 293423557 | orelsan |
| Gims | b2fbd053-4380-412c-95d2-35c6da8f1011 | null (2 pages « GIMS » concurrentes dans les résultats → homonymie non tranchée) | 4429712 | 458659054 | null |
| Katy Perry | 122d63fc-8671-43e4-9752-34e846d62a9c | 6jJ0s89eD6GaHleKKya26X | 144227 | 64387566 | katyperry |
| Nick Cave & The Bad Seeds | 172e1f1a-504d-4488-b053-6344ba63e6d0 | 4UXJsSlnKd7ltsrHebV79Q | 1581 | 1698460 | null |
| Aya Nakamura | cf580d82-3f3e-4b86-8874-7e0fbe794f01 | 7IlRNXHjoOCgEAWN5qYksg | 8909272 | 1042004371 | ayanakamura_officiel |
| Mika | 8a9ac1cb-faae-434e-8d60-b139a3707dfc | 5MmVJVhhYKQ86izuGHzJYA | 6603 | 184932871 | null |
| Major Lazer | 75be165a-ad83-4d12-bd28-f589a15c479f | null | 282118 | null | null |
| Bigflo & Oli | f4824366-d97b-4929-a970-a94845306f4a | null | 5497121 | null | null |
| Kaytranada | e56aee57-d90e-40cf-a70d-beb70f6f3c69 | null | 4452092 | null | null |
| Zara Larsson | 134e6410-6954-45d1-bd4a-0f2d2ad5471d | null | 4331004 | null | null |
| Gazo | dae7d996-38fe-4578-bf75-1e2510f522bb | null | 8873540 | null | null |
| Tom Odell | null | null | 4044787 | null | null |
| Zaz | e3214827-bd09-4d53-a88c-893d61556352 | null | null | null | null |
| Skip the Use | 1c346a43-0fba-4702-bc1b-c198f07e3da4 | null | null | null | null |

## Pièges d'homonymie / cas particuliers

- **Solidays** : « JADE » (dimanche) et « JÄDE » (vendredi) sont deux artistes distincts sur la page officielle — conservés séparément, IDs null.
- **Gims (Spotify)** : la recherche renvoie deux pages artiste « GIMS » (039lBiQ… et 0GOx72r…) plus « Maître GIMS » (28J0oVT…) → non résolu, null.
- Collaborations one-off (Vald x Vladimir Cauchemar x Todiefor ; b2b Garorock ; « Amelie Lens presents AURA » ; « Panteros666 présente 100% Eurotrance ») : entités sans fiche dédiée → IDs null.
- Petits noms à fort risque d'homonymie (Jonas, Magnus, Rose, Billie, Jade, Helena, Marguerite, Augusta, Perceval, Venga…) : null systématique.
- **Garorock** : le site annonce « du 26 au 28 juin » mais liste un jeudi 25 juin « warm-up » (KI/KI, Benwal, Onlynumbers, Yasmin Regisford, DJ Koyla, Xuitek) → jour 1 dédié. Un lien « Vue Horaire » existe (/programmation/timetable) mais les horaires n'étaient pas exploités ici (statut incomplet demandé).
- **Vieilles Charrues** : « Fest Noz » (samedi) est un événement collectif, pas un artiste ; conservé tel qu'annoncé. 4 scènes évoquées par la presse mais sans affectation par artiste → `TBD`.
- **Francofolies** : « (French) Stories From Nashville » nettoyé en « Stories From Nashville » ; « Et si on chantait ? » est un format scénique (Mille Plateaux).

## Bilan IDs (sur les 4 fichiers, 268 concerts)

- Concerts avec au moins un ID résolu : 24 (Orelsan ×3, Gims ×4, Katy Perry, Nick Cave & The Bad Seeds, Aya Nakamura ×2, Mika ×2, Major Lazer ×2, Bigflo & Oli ×2, Kaytranada, Zara Larsson, Gazo, Tom Odell, Zaz, Skip the Use ×2).
- Champs résolus : 14 musicBrainzId uniques, 12 deezerArtistId, 6 spotifyArtistId, 6 appleMusicArtistId, 4 instagramHandle (sur les artistes uniques).
- Tout le reste : null (à compléter quand les horaires/scènes seront publiés).
