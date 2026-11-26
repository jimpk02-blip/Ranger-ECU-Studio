package ca.ranger.ecu;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.SystemClock;
import android.widget.LinearLayout;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

/** Fixed read-only diagnostic operations; no free-form commands or PCM writes. */
final class UsbDiagnostic {
    private static final String ACTION = "ca.ranger.ecu.USB_DIAG_PERMISSION";
    private static final String[][] JOBS = {
        {"0900","0902","0904","0906"},
        {"03","07","0A"},
        {"010C","0105","0104","0106","0107","0108","0109","010E","0110","0111"},
        {"0100","0120","0140","0101"}
    };
    private final MainActivity ui;
    private final UsbManager manager;
    private UsbSerialDriver pending;
    private int pendingJob = -1;
    private volatile boolean destroyed;
    private volatile UsbSerialPort activePort;
    private boolean registered;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!ACTION.equals(intent.getAction()) || pending == null || destroyed) return;
            UsbSerialDriver driver = pending;
            int job = pendingJob;
            pending = null;
            pendingJob = -1;
            if (manager.hasPermission(driver.getDevice())) start(driver, job);
            else ui.messagePanel("Connexion USB", "Autorisation USB refusée ou câble débranché.");
        }
    };

    UsbDiagnostic(MainActivity ui) {
        this.ui = ui;
        manager = (UsbManager) ui.getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION);
        if (Build.VERSION.SDK_INT >= 33) ui.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else ui.registerReceiver(receiver, filter);
        registered = true;
    }

    void open() {
        LinearLayout body = ui.openPanel("Diagnostic USB OTG");
        ui.addMenuRow(body, ui.menuText("vLinker FS / FTDI — lecture seule. Brancher le câble USB-C OTG, mettre le contact et immobiliser le véhicule. Déconnecter Bluetooth avant utilisation.", 14, ui.BLUE));
        ui.addMenuRow(body, ui.menuButton("Identifier le PCM", true, () -> pick(0)));
        ui.addMenuRow(body, ui.menuButton("Lire les codes défaut", false, () -> pick(1)));
        ui.addMenuRow(body, ui.menuButton("Lire les capteurs", false, () -> pick(2)));
        ui.addMenuRow(body, ui.menuButton("Vérifier la liaison CAN", false, () -> pick(3)));
        ui.addMenuRow(body, ui.menuText("115 200 bauds / 8N1. Connexion ouverte uniquement pendant chaque lecture. Résultats dans le journal principal et dans le rapport USB. Aucune programmation.", 12, 0xff91a9be));
    }

    private boolean available() {
        if (destroyed || ui.isFinishing() || ui.isDestroyed()) return false;
        if (ui.busy.get() || ui.live || (ui.socket != null && ui.socket.isConnected())) {
            ui.messagePanel("Connexion USB", "Terminer la lecture et déconnecter Bluetooth avant le diagnostic USB.");
            return false;
        }
        return true;
    }

    private void pick(int job) {
        if (!available()) return;
        if (pending != null) { ui.messagePanel("Connexion USB", "Autorisation USB déjà demandée."); return; }
        if (manager == null) { ui.messagePanel("Connexion USB", "Service USB indisponible."); return; }
        ArrayList<UsbSerialDriver> drivers = new ArrayList<>();
        for (UsbSerialDriver d : UsbSerialProber.getDefaultProber().findAllDrivers(manager))
            if (d instanceof FtdiSerialDriver && !d.getPorts().isEmpty()) drivers.add(d);
        if (drivers.isEmpty()) {
            ui.messagePanel("Connexion USB", "Aucune interface FTDI reconnue. Vérifier le câble OTG et le branchement OBD.");
            return;
        }
        if (drivers.size() == 1) { authorize(drivers.get(0), job); return; }
        String[] labels = new String[drivers.size()];
        for (int i=0; i<labels.length; i++) {
            UsbDevice d = drivers.get(i).getDevice();
            labels[i] = String.format(Locale.ROOT,"FTDI %04X:%04X — %s",d.getVendorId(),d.getProductId(),d.getDeviceName());
        }
        ui.menu("Interface USB",labels,i -> authorize(drivers.get(i),job));
    }

    private void authorize(UsbSerialDriver driver,int job) {
        if (!available()) return;
        if (manager.hasPermission(driver.getDevice())) { start(driver,job); return; }
        pending=driver; pendingJob=job;
        try {
            Intent intent = new Intent(ACTION).setPackage(ui.getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(ui,4763,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            manager.requestPermission(driver.getDevice(),pi);
        } catch (Exception e) {
            pending=null; pendingJob=-1;
            ui.messagePanel("Connexion USB","Demande d’autorisation impossible : "+e.getMessage());
        }
    }

    private void start(UsbSerialDriver driver,int job) {
        if (!available() || job < 0 || job >= JOBS.length) return;
        ui.submit(() -> run(driver,job));
    }

    private void run(UsbSerialDriver driver,int job) {
        StringBuilder report = new StringBuilder("USB FTDI — lecture seule\n");
        UsbDeviceConnection connection = null;
        UsbSerialPort port = null;
        try {
            if (destroyed) return;
            connection = manager.openDevice(driver.getDevice());
            if(connection==null) throw new IOException("Ouverture USB refusée.");
            port = driver.getPorts().get(0);
            activePort=port;
            port.open(connection);
            if(destroyed) throw new IOException("Activité fermée.");
            port.setParameters(115200,8,UsbSerialPort.STOPBITS_1,UsbSerialPort.PARITY_NONE);
            // Only fixed adapter setup; no arbitrary command entry.
            String identity = exchange(port,"ATI",3000,report);
            if(!identity.toUpperCase(Locale.ROOT).matches("(?s).*(ELM327|VLINKER|STN|OBDLINK).*"))
                throw new IOException("Identité OBD non reconnue à 115 200 bauds.");
            for(String cmd : new String[]{"ATZ","ATE0","ATL0","ATS1","ATSP6"}) {
                String reply=exchange(port,cmd,5000,report);
                if(!cmd.equals("ATZ") && !ui.adapterOK(reply)) throw new IOException("Configuration refusée : "+cmd);
            }
            String reply=exchange(port,"0100",45000,report);
            if(!reply.replaceAll("\\s+","").toUpperCase(Locale.ROOT).contains("4100"))
                throw new IOException("Réponse PCM non confirmée.");
            String proto=exchange(port,"ATDPN",5000,report);
            if(!proto.trim().matches("(?i)A?6")) throw new IOException("CAN 11/500 non confirmé.");
            for(String cmd:JOBS[job]) exchange(port,cmd,15000,report);
            report.append("Lecture terminée — aucune écriture PCM.\n");
        } catch(Exception e) {
            report.append("Interruption : ").append(e.getMessage()).append('\n');
        } finally {
            if(port!=null) try { port.close(); } catch(Exception ignored) {}
            activePort=null;
            if(connection!=null) connection.close();
            String result=report.toString();
            ui.runOnUiThread(() -> {
                if(!destroyed && !ui.isFinishing() && !ui.isDestroyed())
                    ui.messagePanel("Rapport USB",result);
            });
        }
    }

    private String exchange(UsbSerialPort port,String cmd,int timeout,StringBuilder report) throws IOException {
        if(destroyed) throw new IOException("Activité fermée.");
        long start=SystemClock.elapsedRealtime();
        port.write((cmd+"\r").getBytes(StandardCharsets.US_ASCII),1500);
        byte[] bytes=new byte[512];
        StringBuilder data=new StringBuilder();
        while(!destroyed && SystemClock.elapsedRealtime()-start<timeout) {
            int n=port.read(bytes,200);
            for(int i=0;i<n;i++){
                if(bytes[i]=='>'){
                    String result=data.toString().trim();
                    String line=cmd+" ["+(SystemClock.elapsedRealtime()-start)+" ms] : "+result;
                    report.append(line).append('\n');
                    ui.show("USB "+line);
                    return result;
                }
                data.append((char)(bytes[i]&255));
                if(data.length()>16384) throw new IOException("Réponse USB trop longue.");
            }
        }
        throw new IOException("Délai dépassé : "+cmd);
    }

    void destroy() {
        destroyed=true;
        pending=null; pendingJob=-1;
        if(registered){ui.unregisterReceiver(receiver);registered=false;}
        UsbSerialPort p=activePort;
        if(p!=null)try{p.close();}catch(Exception ignored){}
    }
}
