# Notes d'extraction & d'enrichissement — Main Square Festival 2026
*Extraction le 06/06/2026 — uniquement via WebFetch / WebSearch.*

## Sources horaires
- **Sortiraparis** (article 50212, maj 03/04/2026) : seule source avec la **grille horaire complète des 3 scènes** (heures de début). Base du running order.
- **Onivapp.com** (26/05/2026, "horaires confirmés d'après le site officiel") : liste fusionnée Main Stage + Vauban — **100 % concordante** avec Sortiraparis sur les heures de début (vendredi 16:45→00:45 ; samedi 15:00→00:00 ; dimanche 15:00→22:45).
- **jds.fr** (page festival + actu Bastion du 30/03/2026) : confirme la **répartition artistes/scènes/jours** (dont les 12 artistes du Bastion), sans horaires.
- Site officiel `mainsquarefestival.fr/programmation` : widget JS Fragolabs **vide** au fetch (y compris via r.jina.ai). Le endpoint `app.fragolabs.com/c5c674a7-...` ne sert qu'une page de téléchargement d'app (App Store / Google Play) — aucune API exploitable.

## Fiabilité des horaires
- **Heures de début Main Stage / Vauban : 2 sources concordantes** → fiabilité élevée.
- **Heures de début Bastion : 1 seule source horaire** (Sortiraparis) ; lineup corroboré par jds.fr → fiabilité moyenne.
- **Toutes les heures de fin sont ESTIMÉES** (aucune source ne les publie) : 45 min pour les ouvertures/découvertes, 60-75 min en milieu de soirée, 90 min pour les têtes d'affiche, bornées par le set suivant sur la même scène ; fins de nuit alignées sur la presse ("dernier show vers 2h" le vendredi, Main Stage jusqu'à ~1h15 le samedi).
- Sets après minuit (Paul Kalkbrenner 00:45, Mercure Nitro 00:00, Marshmello 00:00) : date calendaire du lendemain, `festivalDay` du jour affiché.

## Divergences relevées
- **Linka Moja** : Sortiraparis (grille horaire) → Main Stage 16:45 ; jds.fr le liste en Vauban. Grille horaire retenue.
- **Stuck in Yesterday** : orthographié « Stuck in Yerterday » par Sortiraparis (coquille) ; graphie jds.fr retenue.
- **Supermodel** : noté « SUPERMODEL* » chez Sortiraparis (astérisque non expliqué).

## Enrichissement (IDs vérifiés par recherche site:musicbrainz.org / deezer.com / music.apple.com / open.spotify.com)
| Artiste | MBID | Deezer | Apple | Spotify | Instagram |
|---|---|---|---|---|---|
| Katy Perry | ✅ | ✅ 144227 (API) | ✅ 64387566 | ✅ | katyperry |
| Twenty One Pilots | ✅ | ✅ 647650 (API) | ✅ 349736311 | ✅ | twentyonepilots |
| Orelsan | ✅ | ✅ 259467 (API) | ✅ 293423557 | ✅ | orelsan |
| Marshmello | ✅ | ✅ 7890702 (API) | ✅ 980795202 | ✅ | marshmello |
| Charlotte Cardin | ✅ | ✅ 5417036 (API) | ✅ 784665014 | ✅ | charlottecardin |
| Paul Kalkbrenner | ✅ | ✅ 6377 | ❌ non recherché | ❌ 2 pages homonymes non départagées | paulkalkbrenner |
| Jessie Murph | ❌ non confirmé | ✅ 119596482 (API) | ✅ 1548067332 | ✅ | jessiemurph |
| Cassius | ✅ (duo FR) | ✅ 2049 | ❌ | ✅ 4sf3QZW8… (confiance moyenne, bio duo FR associée) | ❌ |
| The Warning | ✅ | ✅ 7716640 | ❌ | ❌ | ❌ |
| Asaf Avidan | ✅ | ✅ 403152 | ❌ | ❌ | asafavidan |
| Renée Rapp | ✅ | ✅ 171528407 | ✅ 1626560688 | ✅ | reneerapp |
| Yamê | ❌ (MBID candidat « YAME » homonyme techno non départagé) | ✅ 107030802 | ❌ | ❌ | ❌ |
| L2B | ❌ | ✅ 13790723 (trio Champigny ex-L2B Gang) | ❌ | ❌ | ❌ |
| Balu Brigada | ❌ | ✅ 11303670 | ❌ | ❌ | ❌ |
| Zaho | ❌ | ✅ 161909 (chanteuse Zaho — l'affiche dit « Zaho », pas Zaho de Sagazan) | ❌ | ❌ | ❌ |
| Vald x Vladimir Cauchemar x Todiefor | — | — | — | — | set collaboratif : IDs à null volontairement |
| Miki, Keo, Luiza, Voilà, Don West, Perceval, Supermodel | null | null | null | null | **homonymie non résolue** → null |
| Acts Bastion (12) + Linka Moja, Midnight Generation, Eve La Marka, Nono La Grinta, Radio Free Alice, Ours Samplus | null | null | null | null | artistes émergents non vérifiables dans le budget de requêtes |

- Les handles Instagram fournis sont les comptes officiels notoires des têtes d'affiche (non extraits de MusicBrainz relations, faute de quota de requêtes — les appels API directs Deezer/MusicBrainz/iTunes renvoyaient un corps vide via WebFetch, et le proxy r.jina.ai puis WebFetch lui-même ont été rate-limités en cours d'enrichissement ; bascule sur WebSearch site:domaine).
- `appleMusicPlaylist` : null partout (aucune playlist officielle vérifiée).

## Bilan
- 40 concerts, 3 scènes, 3 jours — running order **complet** (tous les starts renseignés, pas de statut « Running Order Incomplet »).
- IDs résolus : 5 artistes "full" (4-5 IDs), 10 partiels, 25 sets entièrement à null (émergents/homonymes).
