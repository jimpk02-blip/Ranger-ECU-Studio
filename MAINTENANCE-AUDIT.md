# v1.4.0 — rapport de maintenance Bluetooth

Conserve USB et Bluetooth de v1.3.0. Le service OBD-II 04 existant reste soumis à confirmation de réparation et contrôle vitesse/régime nuls. Aucun nouveau service PCM modifiant son état, aucune écriture mémoire ni terminal arbitraire. Le rapport sauvegarde les réponses avant/après, la réponse brute du service, le délai, et distingue réception positive 44 de vérification de suivi. Une réponse 44 ne valide ni KAM ni flash. Si les défauts persistent, réparer avant tout effacement; ne pas utiliser le service uniquement pour tester.

Tests physiques : rapport vide; annulation; refus si moteur tourne; après réparation confirmée, vérifier réponse et sauvegarde du rapport. Aucune exécution réelle dans le développement. Compilation et signature par CI.
