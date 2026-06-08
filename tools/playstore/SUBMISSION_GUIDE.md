# Ma Team HF — Play Console submission guide

Step-by-step to get the app from "signed AAB on disk" to "live on Play Store".

## 0. Pre-requisites

- [ ] **Compte Google Play Console** créé et payé (25 USD unique) : https://play.google.com/console
- [ ] **AAB signé prêt** : `app/build/outputs/bundle/release/app-release.aab` (16 Mo)
- [ ] **Politique de confidentialité hébergée publiquement** (cf. section 1)
- [ ] **Screenshots du Pixel** (cf. section 2) — au moins 2, idéalement 4-8
- [ ] **Compte Google de test** dédié (pour la review Google — cf. section 4)

## 1. Héberger la privacy policy

Le contenu est dans `tools/playstore/privacy_policy.md`. Trois options simples :

**a) GitHub Pages (recommandé, free, durable)**
1. Crée un repo GitHub public (ex. `mateamhf-legal`)
2. Copie le `.md` à la racine en `index.md`
3. Settings → Pages → Source = `main` branch → Save
4. URL publique : `https://<ton-user>.github.io/mateamhf-legal/`

**b) Notion public page**
1. Colle le markdown dans une page Notion
2. Share → Publish to web → copie l'URL

**c) Gist GitHub** (le plus rapide, moins joli)
1. https://gist.github.com → nouveau gist public → colle le `.md`
2. Sauvegarde → l'URL "Raw" est ta privacy policy URL

**Note** Le contenu y fait référence à `mc4spam@gmail.com` pour les demandes RGPD. Si tu veux une adresse dédiée, crée-en une (ex. `legal+mateamhf@…`) et remplace dans le markdown avant publication.

## 2. Préparer les screenshots

Sur le Pixel branché en USB :

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB shell screencap -p > screen-01.png   # captures la timeline
# Répéter pour : Friends page, Mon RO, ConcertSheet ouvert, NoGroupScreen, Login
```

Play Console exige PNG/JPG, min 320 px de large, max 3840. Le Pixel 10 Pro XL sort en 1280×2856 — parfait.

Minimum demandé : 2 screenshots téléphone. Idéal : 4-8 montrant les screens-clés.

**Feature graphic** (bannière en haut de la fiche Play Store) : 1024×500 PNG/JPG. Tu peux composer à la main dans Figma/Canva en mettant le logo + un fond sombre + "Ma Team HF — édition 2026". Pas obligatoire pour publier en internal testing, obligatoire en production.

## 3. Créer l'app dans la Play Console

1. https://play.google.com/console → bouton **"Create app"**
2. App name : `Ma Team HF`
3. Default language : `Français (France)` (tu peux ajouter EN après)
4. App or game : `App`
5. Free or paid : `Free`
6. Coche les déclarations (politiques développeur + export laws)
7. Create app

## 4. Compléter la fiche obligatoire (Dashboard "Set up your app")

La Console te liste 8-10 tâches dans un dashboard. À faire dans l'ordre :

### a) Privacy policy
- Coller l'URL de la section 1

### b) App access
- "All or some functionality is restricted" (parce que login Google requis)
- **Provide test credentials** : crée un compte Google de test (ex. `test.mateamhf@gmail.com`), donne username + password à Google pour qu'ils puissent passer le login pendant la review

### c) Ads
- "No, my app does not contain ads"

### d) Content rating
- Questionnaire ~5 min : pas de violence/sex/gambling → IARC va te donner "PEGI 3" probablement
- Pour la catégorie, choisir "Music & Audio" ou "Lifestyle"

### e) Target audience and content
- Target age group : "13+ ans" (cohérent avec privacy policy section 10)

### f) News app
- "No"

### g) COVID-19 contact tracing
- "No"

### h) Data safety form (LE PLUS LONG)
Déclare les données collectées + leurs finalités. Pour notre cas :
- **Personal info** : email, name → collected → shared with no one → purpose "App functionality"
- **App activity** : "In-app actions" (les P1) → collected → shared with no one → purpose "App functionality"
- **App info and performance** : crash logs → NOT collected (on n'a pas Crashlytics)
- **Tous les autres** : NOT collected

Le formulaire prend ~20 min, fais-le tranquille.

### i) Government apps
- "No"

### j) Financial features
- "No"

## 5. Configurer le release (Internal testing d'abord)

1. Menu gauche → **Release → Testing → Internal testing**
2. **Create new release**
3. **App signing** : Play Console te propose de gérer la clé pour toi ("Play App Signing"). 
   - **Option recommandée** : Active Play App Signing. Tu uploads ton AAB qui est **signé avec ta clé d'upload** (= celle qu'on vient de générer), et Google re-signe avec **leur** clé pour la distribution. Avantage : si tu perds ta clé, tu peux la régénérer (sinon, c'est game over).
   - Confirme.
4. **Upload AAB** : `app/build/outputs/bundle/release/app-release.aab`
5. **Release name** : auto, ex. `1 (1.0.0)`
6. **Release notes** :
   ```
   Première version publique de Ma Team HF.
   - Planifie tes concerts au Hellfest 2026 avec ta team
   - Synchronisation temps-réel des P1 entre tes potes
   - Notifications de rappel avant chaque concert P1
   - Liens directs Apple Music / Spotify / Deezer / Instagram par artiste
   ```
7. **Internal testers** : Menu gauche → Testing → Internal testing → onglet "Testers" → "Create email list" → ajoute jusqu'à 100 adresses Gmail
8. Onglet "Testers" → copy le lien "Opt-in URL" → envoie-le à tes potes pour qu'ils acceptent de tester
9. Bouton **Review release** → **Start rollout to Internal testing**

**Disponible chez tes testeurs en ~5-10 min** (pas de review Google sur internal testing).

## 6. Ajouter le SHA-1 de release dans Firebase

Pour que Google Sign-In marche en build release, il faut enregistrer le SHA-1 de la clé d'upload dans Firebase (en plus du SHA-1 debug déjà présent).

**Si tu as activé Play App Signing à l'étape 5.3** :
1. Play Console → Setup → App integrity → onglet "App signing"
2. Tu vois **deux** fingerprints :
   - "App signing key certificate" → SHA-1 = la clé GOOGLE qui resigne tes APKs
   - "Upload key certificate" → SHA-1 = TA clé d'upload (celle qu'on a générée)
3. **Copie les deux**

**Sinon** (signing manuel, sans Play App Signing) :
- SHA-1 = celui de ta clé locale : `4A:40:22:3F:DA:5C:EC:B7:4F:6B:A8:4F:F3:44:B2:54:D7:6F:67:C7`

Puis :
1. Firebase Console → projet `ma-team-hf-2026` (nom historique du projet Firebase, inchangé) → ⚙ Project settings → onglet "General"
2. Section "Your apps" → app Android → bouton "Add fingerprint"
3. Colle le ou les SHA-1
4. Re-télécharge `google-services.json` et écrase l'ancien dans `app/`
5. Rebuild : `./gradlew :app:bundleRelease`
6. Upload la nouvelle AAB dans la Console

## 7. Quand tu es prêt pour la prod publique

Internal testing = privé (100 testeurs max sur invitation). Pour passer en prod :
1. Menu → **Release → Production**
2. Create release → re-upload la même AAB
3. Cette fois, Google va faire une review (entre 1h et 7 jours, souvent 1-3 jours)

**Risques de rejet à anticiper** :
- Privacy policy incomplète ou URL morte
- Login Google sans creds de test fournis
- Branding qui pourrait être confondu avec Hellfest officiel (logo "MA TEAM HF" garde une distance suffisante normalement)
- Permissions injustifiées (les nôtres sont toutes documentées dans la privacy policy)

## 8. Distribution interne RAPIDE (sans passer par Play Console)

Si tu veux juste partager à 2-10 amis sans attendre :

```bash
# APK signé prêt à envoyer
ls -la /Users/mc/team-festival/app/build/outputs/apk/release/app-release.apk
```

Envoie le fichier `.apk` (52 Mo) par Signal/Telegram/Drive. Tes amis :
1. Tap le `.apk`
2. Autorise "Installer depuis cette source"
3. "Installer quand même" (Play Protect)
4. Premier lancement → Google Sign-In → Create or Join team

**Différence majeure vs debug APK** : signé avec ta clé release stable, donc les mises à jour fonctionneront in-place pour toujours, et Play Protect est moins agressif.
