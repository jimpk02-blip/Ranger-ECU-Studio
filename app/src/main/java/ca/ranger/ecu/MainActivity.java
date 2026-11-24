package ca.ranger.ecu;
import android.app.*; import android.os.*; import android.bluetooth.*; import android.widget.*; import java.util.*; import java.io.*; import java.util.concurrent.*;
public class MainActivity extends Activity {
 TextView log; ScrollView journal; LinearLayout layout, commandRow; int commandCount=0; volatile boolean live=false, can=false; final java.util.concurrent.atomic.AtomicBoolean busy=new java.util.concurrent.atomic.AtomicBoolean(); TextView status; volatile BluetoothSocket socket; final ExecutorService worker=Executors.newSingleThreadExecutor();
 void show(String s){runOnUiThread(()->{boolean atBottom=journal.getScrollY()+journal.getHeight()>=log.getBottom()-dp(24);log.append(new java.text.SimpleDateFormat("HH:mm:ss",Locale.CANADA_FRENCH).format(new Date())+"  "+s+"\n");if(atBottom)journal.post(()->journal.fullScroll(android.view.View.FOCUS_DOWN));});}
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density); }
 void button(String title,Runnable r){if(commandCount++%2==0){commandRow=new LinearLayout(this);commandRow.setOrientation(0);layout.addView(commandRow,new LinearLayout.LayoutParams(-1,-2));}Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(12);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(6),dp(4),dp(6),dp(4));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(2),dp(2),dp(2),dp(2));commandRow.addView(b,p);b.setOnClickListener(v->r.run());}
 public void onCreate(Bundle b){super.onCreate(b); LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(8),dp(8),dp(8),0);layout=new LinearLayout(this);layout.setOrientation(1);root.addView(layout,new LinearLayout.LayoutParams(-1,-2));setContentView(root);
 TextView title=new TextView(this);title.setText("RANGER ECU STUDIO — Diagnostic avancé");title.setTextSize(20);layout.addView(title);
 button("Connecter un adaptateur Bluetooth jumelé",()->choose());
 button("Identifier le PCM",()->send("0902","0904","0906","ATDP"));
 button("Lire les codes défaut",()->send("03","07","0A"));
 button("Lire les capteurs",()->send("0101","0103","010C","010D","0105","0104","010F","0142","0106","0107","0108","0109","010E","0110","0111","0113","0114","0115","0118","0119"));
 button("Diagnostic perte de puissance / O2",()->send("0100","0120","0140","0101","03","07","020200","0103","0104","0105","0106","0107","0108","0109","010B","010C","010E","0110","0111","0113","0114","0115","0116","0117","0118","0119","011A","011B","06"));
 button("Diagnostic TPMS / réseau",()->{show("TPMS : géré par le SJB Ford. Les pressions ne sont pas des PID OBD-II génériques. Identification du réseau moteur uniquement ; accès SJB à confirmer.");send("ATI","ATDP","ATDPN","0900","0904","0906");});
 button("Déconnecter",()->{live=false;try{if(socket!=null)socket.close();}catch(Exception e){}setStatus("Déconnecté");});
 button("Partager le rapport",()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(android.content.Intent.EXTRA_TEXT,log.getText().toString());startActivity(android.content.Intent.createChooser(i,"Exporter"));});
 button("Mesures en direct",()->toggleLive());button("Effacer le journal (pas le PCM)",()->log.setText(""));status=new TextView(this);status.setText("Déconnecté");status.setTextSize(13);root.addView(status,new LinearLayout.LayoutParams(-1,-2));
 TextView heading=new TextView(this);heading.setText("Journal de connexion et diagnostic");heading.setTextSize(15);heading.setPadding(dp(6),dp(10),dp(6),dp(5));root.addView(heading,new LinearLayout.LayoutParams(-1,-2));journal=new ScrollView(this);journal.setFillViewport(true);journal.setBackgroundColor(0xff202020);log=new TextView(this);log.setTextColor(0xffeeeeee);log.setTextSize(13);log.setPadding(dp(8),dp(8),dp(8),dp(8));log.setTextIsSelectable(true);journal.addView(log,new ScrollView.LayoutParams(-1,-2));root.addView(journal,new LinearLayout.LayoutParams(-1,0,1));TextView footer=new TextView(this);String version="1.1.4";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}footer.setText("Ranger ECU Studio — v"+version);footer.setGravity(android.view.Gravity.CENTER);footer.setPadding(0,dp(8),0,dp(8));footer.setTextSize(13);root.addView(footer,new LinearLayout.LayoutParams(-1,-2));show("Jumeler le Viecar dans les paramètres Android. Bluetooth Classic uniquement. Valeurs décodées et réponses brutes. Aucune écriture PCM.");}
 public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==1 && results.length>0 && results[0]==0)choose();}
 void choose(){if(Build.VERSION.SDK_INT>=31 && checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=0){requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"},1);return;} try{BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();if(a==null||!a.isEnabled()){show("Activer Bluetooth.");return;} ArrayList<BluetoothDevice> ds=new ArrayList<>(a.getBondedDevices());String[] names=new String[ds.size()];for(int i=0;i<ds.size();i++)names[i]=ds.get(i).getName()+" "+ds.get(i).getAddress();new AlertDialog.Builder(this).setTitle("Adaptateur jumelé").setItems(names,(d,n)->chooseProtocol(ds.get(n))).show();}catch(Exception e){show(e.toString());}}
 void chooseProtocol(BluetoothDevice d){String[] labels={"CAN 11 bits / 500 kbit/s (Ranger confirmé)","Automatique (45 s)","Ford J1850 PWM (essai)","ISO 9141-2 (essai)","Adaptateur seulement (sans interrogation PCM)"};String[] protocols={"6","0","1","3",""};new AlertDialog.Builder(this).setTitle("Protocole OBD-II").setItems(labels,(dialog,n)->submit(()->connect(d,protocols[n]))).show();}
 void connect(BluetoothDevice d,String protocol){try{can=protocol.equals("6");setStatus("Connexion en cours…");if(socket!=null)socket.close();socket=d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket.connect();show("Bluetooth connecté");setStatus("Bluetooth connecté — PCM non confirmé");for(String c:new String[]{"ATZ","ATE0","ATL0","ATS1","ATRV"})query(c);if(protocol.isEmpty()){show("Adaptateur prêt. Aucun test PCM effectué.");return;}query("ATSP"+protocol);show("Recherche PCM : "+(protocol.equals("0")?"automatique (45 s)":protocol)+" — attendre la réponse.");String answer=query("0100");if(answer.replaceAll("\\s+","").contains("4100")){show("Réponse PCM 41 00 reçue.");setStatus("PCM répond — protocole "+protocol);}else show("Aucune réponse PCM positive confirmée. Essayer un autre protocole.");query("ATDP");String p=query("ATDPN");can=p.trim().matches("(?i)A?[6789]");}catch(Exception e){setStatus("Échec de connexion");show("Connexion : "+e);}}
 void setStatus(String s){runOnUiThread(()->status.setText(s));}
 void submit(Runnable task){if(!busy.compareAndSet(false,true)){show("Une lecture est déjà en cours.");return;}worker.submit(()->{try{task.run();}finally{busy.set(false);}});}
 void send(String... cs){submit(()->{try{for(String c:cs)query(c);}catch(Exception e){show("Erreur : "+e);}});}
 void toggleLive(){if(live){live=false;show("Arrêt des mesures demandé.");return;}if(busy.get()){show("Terminer la lecture en cours.");return;}live=true;submit(()->{try{show("Mesures en direct — véhicule stationné. Appuyer à nouveau pour arrêter.");while(live){for(String c:new String[]{"010C","0105","0106","0107","0108","0109","010E","0114","0115","0118","0119"}){if(!live)break;query(c);}if(live)Thread.sleep(2000);}}catch(Exception e){show("Mesures : "+e);}finally{live=false;show("Mesures terminées.");}});}
 String query(String c)throws Exception{BluetoothSocket sck=socket;if(sck==null||!sck.isConnected())throw new IOException("Non connecté");OutputStream out=sck.getOutputStream();InputStream in=sck.getInputStream();out.write((c+"\r").getBytes("US-ASCII"));out.flush();StringBuilder s=new StringBuilder();long end=SystemClock.elapsedRealtime()+(c.equals("0100")?45000:15000);while(SystemClock.elapsedRealtime()<end){if(in.available()>0){int v=in.read();if(v=='>'){String raw=s.toString().trim();show(c+" : "+raw);decode(c,raw);return s.toString();}if(v<0)break;s.append((char)v);}else Thread.sleep(20);}show(c+" : réponse partielle avant délai : "+(s.length()==0?"(aucun octet)":s.toString().trim()));sck.close();setStatus("Déconnecté — délai dépassé");throw new IOException("Délai dépassé : "+c+". Reconnecter et choisir un autre protocole.");}

 String fmt(double n){return String.format(Locale.CANADA_FRENCH,"%.2f",n);}
 void decode(String cmd,String raw){
  try{
   String upper=raw.toUpperCase(Locale.ROOT);
   if(cmd.equals("03")||cmd.equals("07")||cmd.equals("0A")){
    if(!can){show("Décodage DTC automatique réservé au CAN; voir réponse brute.");return;}
    String sid=cmd.equals("03")?"43":cmd.equals("07")?"47":"4A";
    boolean found=false;
    for(String line:upper.split("[\\r\\n]+")){
     String h=line.replaceAll("\\s+","");
     if(!h.startsWith(sid)||!h.matches("[0-9A-F]+")||h.length()<4||h.length()%2!=0)continue;
     found=true;int count=Integer.parseInt(h.substring(2,4),16);
     if(h.length()<4+4*count){show("Liste DTC fragmentée — voir réponse brute.");continue;}
     if(count==0)show("Aucun DTC dans cette réponse.");
     for(int j=0;j<count;j++){int k=4+j*4;int a=Integer.parseInt(h.substring(k,k+2),16),b=Integer.parseInt(h.substring(k+2,k+4),16);String code=""+ "PCBU".charAt(a>>6)+((a>>4)&3)+String.format(Locale.ROOT,"%01X%02X",a&15,b);show("  "+code+" — "+description(code));}
    }
    if(!found)show("Réponse DTC non décodable (ou service non pris en charge).");return;
   }
   if(!cmd.matches("01[0-9A-F]{2}"))return;
   int pid=Integer.parseInt(cmd.substring(2),16);String prefix="41"+cmd.substring(2);
   for(String line:upper.split("[\\r\\n]+")){
    String h=line.replaceAll("\\s+","");
    if(!h.startsWith(prefix)||!h.matches("[0-9A-F]+")||h.length()<6||h.length()%2!=0)continue;
    int a=Integer.parseInt(h.substring(4,6),16), b=h.length()>=8?Integer.parseInt(h.substring(6,8),16):-1;
    String v=null;
    switch(pid){
     case 1: v="Check Engine : "+((a&128)!=0?"allumé":"éteint")+" ; DTC confirmés : "+(a&127);break;
     case 3: v="Boucle carburant B1 : "+loop(a)+(b>=0?" ; B2 : "+loop(b):"");break;
     case 4:v="Charge calculée : "+fmt(a*100.0/255)+" %";break;
     case 5:v="Liquide de refroidissement : "+(a-40)+" °C";break;
     case 6:case 7:case 8:case 9:v=new String[]{"STFT B1","LTFT B1","STFT B2","LTFT B2"}[pid-6]+" : "+fmt((a-128)*100.0/128)+" %";break;
     case 11:v="Pression admission : "+a+" kPa";break;
     case 12:if(b>=0)v="Régime : "+fmt((256*a+b)/4.0)+" tr/min";break;
     case 13:v="Vitesse : "+a+" km/h";break;
     case 14:v="Avance allumage : "+fmt(a/2.0-64)+" °";break;
     case 15:v="Température admission : "+(a-40)+" °C";break;
     case 16:if(b>=0)v="MAF : "+fmt((256*a+b)/100.0)+" g/s";break;
     case 17:v="Papillon : "+fmt(a*100.0/255)+" %";break;
     case 19:v="Sondes O2 présentes (masque) : "+String.format(Locale.ROOT,"%02X",a);break;
     case 20:case 21:case 22:case 23:case 24:case 25:case 26:case 27:if(b>=0)v="O2 B"+(pid<24?1:2)+"S"+((pid-20)%4+1)+" : "+fmt(a/200.0)+" V";break;
     case 66:if(b>=0)v="Tension module : "+fmt((256*a+b)/1000.0)+" V";break;
    }
    if(v!=null)show("  → "+v);
   }
  }catch(RuntimeException e){show("Décodage incomplet — réponse brute conservée.");}
 }
 String loop(int a){switch(a){case 0:return "non disponible";case 1:return "ouverte (température)";case 2:return "fermée";case 4:return "ouverte (charge/décélération)";case 8:return "ouverte (défaut)";case 16:return "fermée (défaut retour)";default:return "état "+a;}}
 String description(String code){switch(code){case "P0158":return "Tension élevée O2 B2S2";case "P0161":return "Circuit chauffage O2 B2S2";case "P0060":return "Résistance chauffage O2 B2S2";case "P0430":return "Efficacité catalyseur banc 2 sous le seuil";case "P0420":return "Efficacité catalyseur banc 1 sous le seuil";case "P0171":return "Mélange pauvre banc 1";case "P0174":return "Mélange pauvre banc 2";case "P0172":return "Mélange riche banc 1";case "P0175":return "Mélange riche banc 2";case "P0300":return "Ratés multiples ou aléatoires";default:return "Description non intégrée";}}

 protected void onDestroy(){live=false;try{if(socket!=null)socket.close();}catch(Exception e){}worker.shutdownNow();super.onDestroy();}
}