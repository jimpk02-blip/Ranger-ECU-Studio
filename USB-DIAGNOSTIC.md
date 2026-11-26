# v1.3.0 — Diagnostic USB OTG (prototype)

Connexion FTDI USB Host Android avec usb-serial-for-android 3.9.0. Bluetooth existant inchangé. Menu Connexion Bluetooth / USB, quatre lectures fixes, autorisation Android, chronométrage des réponses, fermeture de la connexion après chaque opération. Aucun accès en écriture PCM, pas de terminal brut, pas de flash. Débit initial 115200 8N1 selon la documentation vLinker FS; débit non modifié. Le pilote ne garantit pas la reconnaissance de tous les VID/PID.

Tests physiques requis : absence de câble, autorisation refusée, déconnexion pendant lecture, identification ATI, CAN 11/500, PID 0100, identification PCM, lecture DTC et capteurs, retour au Bluetooth. Ne pas considérer la programmation compatible. La branche main et sa signature restent inchangées.

Sources : https://github.com/mik3y/usb-serial-for-android/tree/v3.9.0 ; https://www.manualslib.com/manual/4011371/Vgate-Vlinker-Fs.html
