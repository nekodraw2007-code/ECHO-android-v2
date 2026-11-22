# ECHO — installation et compilation depuis une tablette Android

## Important
Cette archive est le **projet source Android**, pas encore un APK installable. GitHub Actions fabrique l'APK sur ses serveurs. Il faut un dépôt GitHub et une connexion Internet. Aucun ordinateur n'est nécessaire.

## Contenu à importer
À la racine de cette archive se trouvent `settings.gradle`, `build.gradle`, `gradle.properties`, `app/` et `.github/`. Il faut importer le **contenu** de ce dossier dans la racine du dépôt GitHub, pas déposer le ZIP lui-même et ne pas créer un dossier ECHO imbriqué.

## Méthode la plus simple : créer un dépôt neuf
1. Dans le navigateur de la tablette, connectez-vous à GitHub.
2. Ouvrez https://github.com/new
3. Repository name : `ECHO-android-v2`.
4. Choisissez Public (ou Private si vous préférez), cochez `Add a README file`, puis `Create repository`.
5. Sur la page du nouveau dépôt, utilisez `Add file` > `Upload files`.
6. Décompressez cette archive avec l'application Fichiers de la tablette.
7. Sélectionnez et téléversez tous les éléments contenus dans le dossier extrait, y compris le dossier caché `.github`. Ne téléversez pas seulement le ZIP.
8. Vérifiez qu'à la racine GitHub vous voyez `app`, `.github`, `settings.gradle`, `build.gradle` et `gradle.properties`.
9. En bas de la page, dans `Commit changes`, entrez `Add ECHO Android project`, puis validez `Commit changes`.

## Lancer la compilation
1. Ouvrez l'onglet `Actions` du dépôt.
2. Dans la colonne de gauche, sélectionnez `Build ECHO APK`.
3. Appuyez sur `Run workflow`, choisissez `main` dans `Branch`, puis appuyez sur le bouton vert `Run workflow`.
4. Attendez que l'exécution passe au vert avec le statut `Success`.
5. Ouvrez cette exécution et descendez à la section `Artifacts`.
6. Téléchargez `ECHO-APK` : c'est un ZIP qui contient `app-debug.apk`.
7. Décompressez ce ZIP, ouvrez `app-debug.apk`, autorisez l'installation depuis cette source si Android le demande, puis installez.

## Si `Build ECHO APK` n'apparaît pas
- Le fichier doit être visible dans GitHub à l'adresse `.github/workflows/build-apk.yml`.
- Il doit être sur la branche par défaut (`main`) pour que `workflow_dispatch` soit disponible.
- Vérifiez que le dépôt autorise GitHub Actions : `Settings` > `Actions` > `General` > `Allow all actions and reusable workflows` (ou une politique autorisant les actions utilisées).
- Attendez quelques instants puis rechargez l'onglet `Actions`.

## Fonctionnalités et limites
- Magnétomètre réel si l'appareil en possède un : champ X/Y/Z et magnitude en µT, référence et seuil de variation.
- Accéléromètre et gyroscope réels si disponibles.
- Spirit Talk : mots déclenchés par des seuils de variations mesurées. Cela ne démontre pas une communication paranormale.
- Spirit Box : sons synthétiques locaux (bruits et tonalités), pas un récepteur radio.
- EVP : enregistrement microphone AAC/M4A dans le stockage privé de l'application.
- Caméra : lance l'application caméra du système pour prendre une photo ; ce n'est pas encore une caméra vidéo intégrée avec vision nocturne.
- Journal d'événements stocké localement.

Les mesures magnétiques sont sensibles aux aimants, métaux, haut-parleurs, câbles et appareils électriques. Le téléphone ne peut pas établir l'origine paranormale d'une variation.
