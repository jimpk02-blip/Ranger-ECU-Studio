# Dossier calibration — lecture seule

Inventaire des fichiers réels via Android Storage Access Framework. Aucun fichier synthétique, aucune connexion PCM requise. Lit un flux jusqu'à 64 Mio et calcule SHA-256; enregistre seulement le dernier rapport local. Ne sauvegarde pas une image complète du PCM, ne valide pas la signature Ford, le CVN ni la compatibilité. Détection de format indicative. Aucun accès aux cartographies ni commande de programmation.

Source : https://www.motorcraftservice.com/Diagnostic/HelmSupport?categoryId=286&channelId=46&country=usa&language=en-us
FJDS couvre la programmation de modules Ford depuis 1996 jusqu'à certains modèles 2018. Interface J2534 compatible et couverture exacte du PCM à vérifier. Viecar ELM327 non validé pour lire/flasher la mémoire.

Tests requis : import document, fichier vide, limite de taille, annulation, SHA-256 comparé à une référence indépendante, réouverture du dernier rapport. Aucun trafic OBD attribuable au module. Branche stable et signature inchangées. Aucun flash ni éditeur de valeurs réelles implémenté.
