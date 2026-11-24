# Permanent Android signing

The package ID is ca.ranger.ecu. Each published version must increment versionCode and retain the same signing certificate. Release builds require the repository Actions secret RANGER_SIGNING_BUNDLE (password, colon, base64 PKCS12 keystore). Never commit the secret or keystore. The repository owner should retain a secure offline backup. The CI checks the expected public certificate SHA-256 fingerprint before publishing. If the secret is missing or the signature is different, the build fails rather than publishing an incompatible APK.

The initial migration from an ephemeral debug certificate requires one uninstall/reinstall, after exporting any diagnostics worth preserving. Later release versions should install in place and retain application data. Run Android APK using workflow_dispatch after adding the secret.

UI standard: all menus, nested menus and confirmation panels must retain the graphite/blue design, navigation and diagnostic context. Experimental functions must be clearly labelled. Do not introduce generic gray native application menus.
