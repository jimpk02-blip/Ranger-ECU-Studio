package ca.ranger.ecu;
import android.app.*; import android.os.*; import android.bluetooth.*; import android.widget.*; import java.util.*; import java.io.*; import java.util.concurrent.*;
public class MainActivity extends Activity {
 TextView log; LinearLayout layout; volatile BluetoothSocket socket; final ExecutorService worker=Executors.newSingleThreadExecutor();
 void show(String s){runOnUiThread(()->log.append(s+"\n"));}
 void button(String title,Runnable r){Button b=new Button(this);b.setText(title);layout.addView(b);b.setOnClickListener(v->r.run());}
 public void onCreate(Bundle b){super.onCreate(b); ScrollView scroll=new ScrollView(this);layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(24,48,24,24);scroll.addView(layout);setContentView(scroll);
 TextView title=new TextView(this);title.setText("RANGER ECU STUDIO — Diagnostic");title.setTextSize(24);layout.addView(title);
 button("Connecter un adaptateur Bluetooth jumelé",()->choose());
 button("Identifier le PCM",()->send("0902","0904","0906","ATDP"));
 button("Lire les codes défaut",()->send("03","07","0A"));
 button("Lire les capteurs",()->send("010C","010D","0105","0104","010F","0142","0114","0115","0118","0119"));
 button("Déconnecter",()->{try{if(socket!=null)socket.close();}catch(Exception e){}});
 button("Partager le rapport",()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(android.content.Intent.EXTRA_TEXT,log.getText().toString());startActivity(android.content.Intent.createChooser(i,"Exporter"));});
 log=new TextView(this);layout.addView(log);show("Jumeler le Viecar dans les paramètres Android. Bluetooth Classic uniquement. Réponses OBD brutes. Aucune écriture PCM.");}
 void choose(){if(Build.VERSION.SDK_INT>=31 && checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=0){requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"},1);return;} try{BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();if(a==null||!a.isEnabled()){show("Activer Bluetooth.");return;} ArrayList<BluetoothDevice> ds=new ArrayList<>(a.getBondedDevices());String[] names=new String[ds.size()];for(int i=0;i<ds.size();i++)names[i]=ds.get(i).getName()+" "+ds.get(i).getAddress();new AlertDialog.Builder(this).setTitle("Adaptateur jumelé").setItems(names,(d,n)->worker.submit(()->connect(ds.get(n)))).show();}catch(Exception e){show(e.toString());}}
 void connect(BluetoothDevice d){try{if(socket!=null)socket.close();socket=d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket.connect();show("Bluetooth connecté");for(String c:new String[]{"ATZ","ATE0","ATL0","ATS1","ATSP0","0100","ATDP"})query(c);}catch(Exception e){show("Connexion : "+e);}}
 void send(String... cs){worker.submit(()->{try{for(String c:cs)query(c);}catch(Exception e){show("Erreur : "+e);}});}
 void query(String c)throws Exception{BluetoothSocket sck=socket;if(sck==null||!sck.isConnected())throw new IOException("Non connecté");OutputStream out=sck.getOutputStream();InputStream in=sck.getInputStream();out.write((c+"\r").getBytes("US-ASCII"));out.flush();StringBuilder s=new StringBuilder();long end=SystemClock.elapsedRealtime()+15000;while(SystemClock.elapsedRealtime()<end){if(in.available()>0){int v=in.read();if(v=='>'){show(c+" : "+s.toString().trim());return;}if(v<0)break;s.append((char)v);}else Thread.sleep(20);}sck.close();throw new IOException("Délai dépassé : "+c);}
 protected void onDestroy(){try{if(socket!=null)socket.close();}catch(Exception e){}worker.shutdownNow();super.onDestroy();}
}