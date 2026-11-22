# ECHO — Paranormal Investigation System (Android)

Prototype Android natif Kotlin, destiné aux appareils Android 8.0 (API 26) et plus.

## Fonctions incluses dans cette version du code
- Scanner magnétique : magnétomètre matériel, mesure du vecteur X/Y/Z et magnitude en µT.
- Calibration manuelle et seuil réglable; événements déclenchés par la variation mesurée.
- Accéléromètre et gyroscope (valeurs réelles affichées).
- Spirit Talk : dictionnaire local de mots, déclenchement lié aux événements de capteur, et émission de test manuelle.
- Spirit Box : bruit blanc, rose, brun et balayage de tonalité synthétisés localement (pas de réception radio).
- EVP : enregistrement microphone en AAC/M4A, sauvegardé dans l'espace privé de l'app.
- Caméra : ouvre l'application caméra du système pour capturer une photo.
- Journal d'événements local (préférences de l'application).
- UI en français, navigation par onglets.

Les capteurs non présents sur l'appareil sont indiqués comme indisponibles. Les mots générés ne sont pas des preuves de communication paranormale : ce prototype associe des mots à des événements physiques mesurés, dont l'origine peut être environnementale.

## Construire l'APK avec Android Studio
1. Sur un PC Windows, Mac ou Linux, installe Android Studio depuis le site officiel Android Developer.
2. Décompresse `ECHO_Android_Project_v0.2.zip`.
3. Android Studio > **Open** > sélectionne le dossier `ECHO` (celui qui contient `settings.gradle`).
4. Attends la synchronisation Gradle. Si Android Studio propose d'installer Android SDK Platform 35 et les outils de build, accepte.
5. Dans le menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. À la fin, clique sur **locate** dans la notification. L'APK de debug se trouve généralement dans `ECHO/app/build/outputs/apk/debug/app-debug.apk`.
7. Transfère `app-debug.apk` sur la tablette (USB, Drive ou autre transfert de fichiers).
8. Sur la tablette, ouvre le fichier APK. Autorise, si Android le demande, l'installation depuis cette source (par exemple le navigateur ou le gestionnaire de fichiers), puis confirme **Installer**.
9. Lance ECHO. Autorise le microphone lorsque tu veux utiliser EVP. Les autres capteurs ne demandent généralement pas d'autorisation utilisateur.

## Limites de cette version
- Ce ZIP contient les sources et la configuration du projet; aucun APK précompilé n'est fourni, car Android SDK/Gradle ne sont pas installés dans l'environnement de génération.
- La caméra s'appuie sur l'application caméra externe (photo), pas encore sur une prévisualisation vidéo intégrée.
- EVP enregistre l'audio mais n'inclut pas encore l'éditeur de spectrogramme ni les filtres audio de post-traitement.
- Le journal est local à l'application; export/backup de sessions et synchronisation non implémentés.
- Spirit Box est un générateur sonore local, pas un tuner radio.
