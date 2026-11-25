package ca.ranger.ecu;
import android.content.Intent;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.LinearLayout;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;

/** Read-only local file inventory. No vehicle communication. */
final class CalibrationArchive {
    static final int REQUEST = 4762;
    private static final long LIMIT = 64L * 1024 * 1024;
    private final MainActivity ui;
    CalibrationArchive(MainActivity ui) { this.ui = ui; }

    void open() {
        LinearLayout body = ui.openPanel("Dossier calibration");
        ui.addMenuRow(body, ui.menuText("FICHIERS RÉELS — inventaire en lecture seule. Le fichier sélectionné n'est ni modifié ni envoyé au PCM.", 14, ui.BLUE));
        ui.addMenuRow(body, ui.menuButton("Sélectionner un fichier Ford", true, () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { ui.startActivityForResult(intent, REQUEST); }
            catch (android.content.ActivityNotFoundException e) { ui.messagePanel("Import", "Sélecteur de fichiers indisponible."); }
        }));
        ui.addMenuRow(body, ui.menuText(ui.getSharedPreferences("calibration_inventory",0).getString("last","Aucun fichier inspecté."), 13, ui.FG));
        ui.addMenuRow(body, ui.menuText("Référence diagnostique : PYFG0A2.HEX / CVN F412F74F. Un nom identique ne prouve pas la compatibilité. SHA-256 identifie le fichier, mais ne valide pas sa calibration ni son CVN.", 12, 0xff91a9be));
    }
    void inspect(Uri uri) {
        if (!"content".equals(uri.getScheme())) {
            ui.messagePanel("Import refusé", "Sélectionner un document.");
            return;
        }
        new Thread(() -> {
            try {
                String name = "Document";
                try (Cursor c = ui.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null,null,null)) {
                    if(c != null && c.moveToFirst() && !c.isNull(0)) name = c.getString(0);
                }
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                long size = 0;
                ByteArrayOutputStream prefix = new ByteArrayOutputStream();
                try (InputStream in = ui.getContentResolver().openInputStream(uri)) {
                    if(in == null) throw new IOException("Lecture indisponible");
                    byte[] buf = new byte[32768];
                    int n;
                    while((n=in.read(buf))!=-1) {
                        if(n==0) continue;
                        size += n;
                        if(size>LIMIT) throw new IOException("Limite de 64 Mio dépassée");
                        sha.update(buf,0,n);
                        int take=Math.min(n, 128-prefix.size());
                        if(take>0) prefix.write(buf,0,take);
                    }
                }
                if(size==0) throw new IOException("Fichier vide");
                String preview = new String(prefix.toByteArray(), java.nio.charset.StandardCharsets.US_ASCII).trim();
                String type = preview.startsWith(":") ? "Intel HEX possible (structure non validée)" :
                    preview.startsWith("S0") || preview.startsWith("S1") || preview.startsWith("S2") || preview.startsWith("S3") ? "Motorola S-record possible (structure non validée)" : "Format non identifié";
                StringBuilder hash = new StringBuilder();
                for(byte v : sha.digest()) hash.append(String.format(Locale.ROOT,"%02x",v & 255));
                String report = "Fichier : " + name + "\nTaille : " + size + " octets\nFormat : " + type +
                    "\nSHA-256 : " + hash + "\nCompatibilité PCM : NON ÉTABLIE\nIntégrité Ford/CVN : NON VÉRIFIÉE\nAucune donnée envoyée au véhicule.";
                ui.getSharedPreferences("calibration_inventory",0).edit().putString("last",report).apply();
                ui.runOnUiThread(() -> { if(!ui.isFinishing() && !ui.isDestroyed()) ui.messagePanel("Inventaire du fichier",report); });
            } catch(Exception e) {
                ui.runOnUiThread(() -> { if(!ui.isFinishing() && !ui.isDestroyed()) ui.messagePanel("Import impossible",String.valueOf(e.getMessage())); });
            }
        },"calibration-inventory").start();
    }
}
