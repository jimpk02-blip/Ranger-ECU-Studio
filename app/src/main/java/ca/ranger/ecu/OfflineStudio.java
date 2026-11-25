package ca.ranger.ecu;

import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Synthetic numeric workspace. No diagnostic commands, firmware or transport. */
final class OfflineStudio {
    private final MainActivity ui;
    private final SharedPreferences prefs;
    private static final String[] LABELS = {
        "Régime moteur (tr/min)", "Température moteur (°C)",
        "Débit d’air (g/s)", "Avance à l’allumage (°)",
        "Correction carburant B1 (%)", "Correction carburant B2 (%)"
    };
    private static final double[] BASELINE = {720, 86, 4.3, 11.5, 0, 0};
    private final EditText[] fields = new EditText[LABELS.length];
    private TextView result;

    OfflineStudio(MainActivity ui) {
        this.ui = ui;
        prefs = ui.getSharedPreferences("synthetic_studio", 0);
    }

    void open() {
        LinearLayout body = ui.openPanel("Studio hors véhicule");
        ui.addMenuRow(body, ui.menuText("SIMULATION UNIQUEMENT — valeurs fictives, sans lecture ni écriture du PCM. Les références ci-dessous sont des exemples, pas une calibration Ford.", 14, ui.BLUE));
        for (int i = 0; i < LABELS.length; i++) {
            ui.addMenuRow(body, ui.menuText(LABELS[i] + " · Référence : " + BASELINE[i], 13, ui.FG));
            EditText field = new EditText(ui);
            field.setSingleLine(true);
            field.setTextColor(ui.FG);
            field.setHintTextColor(0xff91a9be);
            field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
            field.setBackground(ui.panel(0xff152b40, 0xff315b7e));
            field.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(8));
            field.setText(prefs.getString("value_" + i, Double.toString(BASELINE[i])));
            fields[i] = field;
            ui.addMenuRow(body, field);
        }
        result = ui.menuText("Saisir les valeurs puis comparer.", 14, ui.FG);
        ui.addMenuRow(body, ui.menuButton("Comparer au profil de référence", true, () -> compare(false)));
        ui.addMenuRow(body, ui.menuButton("Sauvegarder le profil fictif", false, () -> compare(true)));
        ui.addMenuRow(body, ui.menuButton("Réinitialiser les valeurs fictives", false, () -> {
            for (int i = 0; i < fields.length; i++) fields[i].setText(Double.toString(BASELINE[i]));
            compare(false);
        }));
        ui.addMenuRow(body, result);
    }

    private void compare(boolean save) {
        double[] values = new double[fields.length];
        boolean valid = true;
        for (int i = 0; i < fields.length; i++) {
            fields[i].setError(null);
            try {
                values[i] = Double.parseDouble(fields[i].getText().toString().trim().replace(',', '.'));
                if (Double.isNaN(values[i]) || Double.isInfinite(values[i]) ||
                    Double.isInfinite(values[i] - BASELINE[i])) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                fields[i].setError("Entrer un nombre fini");
                valid = false;
            }
        }
        if (!valid) {
            result.setText("Profil non enregistré : corriger les champs indiqués.");
            return;
        }
        StringBuilder out = new StringBuilder(save ? "Profil fictif enregistré localement.\n" : "Comparaison fictive.\n");
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < values.length; i++) {
            out.append(LABELS[i]).append(" : ")
                .append(String.format(Locale.CANADA_FRENCH, "%,.3f (%+,.3f)", values[i], values[i] - BASELINE[i]))
                .append('\n');
            if (save) editor.putString("value_" + i, Double.toString(values[i]));
        }
        if (save) editor.apply();
        result.setText(out.toString());
    }
}
