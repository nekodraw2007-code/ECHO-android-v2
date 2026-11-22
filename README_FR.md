# ECHO — Communication magnétique

ECHO est une application Android expérimentale centrée sur le magnétomètre du téléphone.

## Fonctionnalités
- Lecture en temps réel du champ magnétique sur les axes X, Y et Z (µT).
- Calibration sur une moyenne de mesures réelles.
- Détection des variations magnétiques avec lissage, seuil réglable, confirmation sur plusieurs échantillons et délai anti-répétition.
- Sélection locale d'un mot lorsqu'un événement mesuré satisfait les critères.
- Utilisation de l'accéléromètre et du gyroscope pour ignorer temporairement les mesures pendant les mouvements du téléphone.
- Historique local, fonctionnement hors ligne et retour haptique.

## Limites
Les capteurs mesurent le champ magnétique terrestre et les perturbations physiques de l'environnement. Les mots affichés sont une interprétation ludique d'événements instrumentaux et ne démontrent pas une communication avec une entité.

## Compiler l'APK
Le workflow GitHub Actions « Build ECHO APK » compile automatiquement un APK debug à chaque push sur `main`. Le fichier est disponible dans les artefacts du workflow pendant 14 jours.
