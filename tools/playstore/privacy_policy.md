# Politique de confidentialité — Ma Team HF

_Dernière mise à jour : 21 mai 2026._

Ma Team HF (« l'application ») est une application Android indépendante non affiliée à Hellfest Productions, destinée à un usage privé entre amis participant au festival Hellfest Open Air. Elle permet de planifier ensemble les concerts à voir et de partager ses priorités au sein d'un groupe restreint.

Cette politique décrit les données traitées, les finalités, la durée de conservation et tes droits.

**Responsable du traitement** : Marc (MC), `mc4spam@gmail.com`. Application développée à titre personnel, sans entité juridique.

## 1. Données collectées

Au moment de la connexion via Google :
- **Identifiant Google (UID Firebase)** — chaîne opaque, utilisée comme clé unique
- **Adresse email** — affichée à toi seul, jamais à tes coéquipiers
- **Nom d'affichage Google (« displayName »)** — visible par les autres membres de tes teams
- **URL de la photo de profil Google** — affichée par défaut à côté de ton nom

Au cours de l'usage :
- **Liste de tes priorités P1** (artistes que tu prévois de voir) — visible par les autres membres de tes teams
- **Notes (--, -, +, ++) et priorités P1/P2/P3** — restent strictement sur ton téléphone, jamais envoyées au serveur
- **Identifiants des teams dont tu es membre** — utilisés pour la synchronisation
- **Code de jointure** d'une team créée par toi (chaîne aléatoire de type `XXXX-XXXX`)
- **Horodatages techniques** (création de compte, dernière connexion, dernière mise à jour de pick)

## 2. Finalités

- **Authentification** : t'identifier de façon stable d'une session à l'autre, d'un appareil à l'autre
- **Synchronisation entre amis** : afficher dans ta timeline qui de tes coéquipiers a marqué chaque concert en P1
- **Notifications de rappel** : t'alerter 15 minutes avant le début d'un concert que tu as marqué P1 — déclenché localement sur ton téléphone, sans envoi au serveur

## 3. Sous-traitants

- **Google Firebase** (Google Ireland Ltd., siège européen) — héberge l'authentification (Firebase Authentication) et les données de team (Cloud Firestore) en région `europe-west` (Belgique). Voir [politique Google](https://policies.google.com/privacy).
- **Aucune autre tierce partie** : pas de tracking analytique, pas de pub, pas de SDK marketing.

## 4. Stockage

- Les données Firebase sont hébergées en région Belgique (UE).
- Les données locales (priorités, notes, picks Apple Music, préférences UI) sont stockées dans le sandbox privé de l'application sur ton appareil et ne quittent jamais ton téléphone — sauf sauvegarde manuelle via le menu "Sauvegarder mes picks" qui produit un fichier JSON dont tu choisis l'emplacement.

## 5. Visibilité

- Tes priorités P1 sont visibles par les autres membres des teams où tu es présent — uniquement par eux.
- Les autres données (notes, priorités P2/P3, ratings) restent strictement locales.
- L'administrateur d'une team peut voir la liste de ses membres.
- Personne d'autre que toi ne voit ton adresse email.

## 6. Conservation

- Tant que tu utilises l'application, tes données restent dans Firebase.
- Si tu te déconnectes via le menu, ta session locale est effacée mais ton profil reste sur Firebase pour une éventuelle reconnexion.
- Pour **supprimer définitivement** ton compte et toutes tes données : envoie un mail à `mc4spam@gmail.com` depuis l'adresse Google liée à ton compte, avec en objet "Suppression Ma Team HF". Suppression effective sous 7 jours.

## 7. Tes droits (RGPD)

Tu peux à tout moment :
- **Accéder** à tes données : envoie un mail à `mc4spam@gmail.com`
- **Rectifier** ton displayName ou ta photo : ça se passe côté Google (Compte Google → "Vos infos")
- **Supprimer** ton compte : cf. section 6
- **Quitter une team** : menu ⋮ ▸ Changer de team ▸ swipe sur la team (à venir, sinon mail)

## 8. Sécurité

- Communications app↔Firebase chiffrées en TLS 1.3
- Règles de sécurité Firestore qui empêchent un membre de lire les données d'un groupe auquel il n'appartient pas (cf. règles publiées le 21 mai 2026)
- Pas d'accès admin permanent côté développeur — seul un mail explicite déclenche une action manuelle

## 9. Notifications

L'application demande la permission `POST_NOTIFICATIONS` pour t'envoyer un rappel 15 min avant un concert marqué P1. Cette notification est planifiée localement par l'OS et ne transite par aucun serveur.

## 10. Mineurs

Cette application n'est pas conçue pour les utilisateurs de moins de 13 ans. Pas de collecte intentionnelle de données de mineurs.

## 11. Modifications

Toute modification matérielle de cette politique sera notifiée dans l'application au prochain lancement.

## 12. Contact

`mc4spam@gmail.com`
