package ca.ranger.ecu;
import android.app.*; import android.os.*; import android.bluetooth.*; import android.widget.*; import java.util.*; import java.io.*; import java.util.concurrent.*;
public class MainActivity extends Activity {
 TextView log; ScrollView journal; android.widget.FrameLayout screen; final ArrayDeque<android.view.View> panels=new ArrayDeque<>(); LinearLayout layout, commandRow; int commandCount=0; volatile boolean live=false, can=false, pcmReady=false; final java.util.concurrent.atomic.AtomicBoolean busy=new java.util.concurrent.atomic.AtomicBoolean(); TextView status; boolean advancedPermission=false; final int BLUE=0xff50b9ff, FG=0xffe7eef7; volatile BluetoothSocket socket; final ExecutorService worker=Executors.newSingleThreadExecutor();
 void show(String s){runOnUiThread(()->{boolean atBottom=journal.getScrollY()+journal.getHeight()>=log.getBottom()-dp(24);log.append(new java.text.SimpleDateFormat("HH:mm:ss",Locale.CANADA_FRENCH).format(new Date())+"  "+s+"\n");if(atBottom)journal.post(()->journal.fullScroll(android.view.View.FOCUS_DOWN));});}
 android.graphics.drawable.GradientDrawable panel(int color,int stroke){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(9));d.setStroke(dp(1),stroke);return d;}
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density); }
 void button(String title,Runnable r){if(commandCount++%2==0){commandRow=new LinearLayout(this);commandRow.setOrientation(0);layout.addView(commandRow,new LinearLayout.LayoutParams(-1,-2));}Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(FG);b.setBackground(panel(0xff152b40,0xff315b7e));b.setMinHeight(dp(42));b.setMinimumHeight(dp(48));b.setPadding(dp(6),dp(4),dp(6),dp(4));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(2),dp(2),dp(2),dp(2));commandRow.addView(b,p);b.setOnClickListener(v->r.run());}

 TextView menuText(String label,int size,int color){TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(color);return t;}
 Button menuButton(String label,boolean accent,Runnable action){
  Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(accent?0xff0b1420:FG);
  b.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);b.setPadding(dp(14),dp(12),dp(12),dp(12));b.setMinHeight(dp(48));b.setBackground(panel(accent?BLUE:0xff152b40,accent?BLUE:0xff315b7e));
  b.setOnClickListener(v->action.run());return b;
 }
 void addMenuRow(LinearLayout body,android.view.View v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));body.addView(v,p);}
 LinearLayout openPanel(String title){
  if(!panels.isEmpty())panels.peek().setVisibility(android.view.View.GONE);
  FrameLayout cover=new FrameLayout(this);cover.setBackgroundColor(0xdd0b1420);cover.setClickable(true);cover.setFocusable(true);
  FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-1);cp.topMargin=dp(76);cp.bottomMargin=dp(36);screen.addView(cover,cp);panels.push(cover);
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(12),dp(14),dp(10));card.setBackground(panel(0xff0e1b28,0xff315b7e));FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-1,-1);fp.setMargins(dp(10),dp(8),dp(10),dp(8));cover.addView(card,fp);
  LinearLayout head=new LinearLayout(this);head.setGravity(android.view.Gravity.CENTER_VERTICAL);
  TextView name=menuText(title,19,BLUE);name.setTypeface(null,android.graphics.Typeface.BOLD);head.addView(name,new LinearLayout.LayoutParams(0,-2,1));
  Button x=menuButton("×",false,()->closePanel());x.setContentDescription("Retour");x.setGravity(android.view.Gravity.CENTER);x.setPadding(0,0,0,0);head.addView(x,new LinearLayout.LayoutParams(dp(44),dp(44)));card.addView(head);
  TextView hint=menuText("RANGER ECU STUDIO  /  MENU",10,0xff91a9be);hint.setPadding(0,dp(4),0,dp(12));card.addView(hint);
  ScrollView sc=new ScrollView(this);sc.setFillViewport(false);LinearLayout body=new LinearLayout(this);body.setOrientation(1);sc.addView(body,new ScrollView.LayoutParams(-1,-2));card.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  Button back=menuButton(panels.size()>1?"‹ Retour":"‹ Fermer le menu",false,()->closePanel());card.addView(back,new LinearLayout.LayoutParams(-1,-2));
  return body;
 }
 void closePanel(){if(panels.isEmpty())return;screen.removeView(panels.pop());if(!panels.isEmpty())panels.peek().setVisibility(android.view.View.VISIBLE);}
 void closeMenus(){while(!panels.isEmpty())screen.removeView(panels.pop());}
 @Override public void onBackPressed(){if(!panels.isEmpty())closePanel();else super.onBackPressed();}
 void menu(String title,String[] labels,java.util.function.IntConsumer selected){
  LinearLayout body=openPanel(title);
  for(int i=0;i<labels.length;i++){final int n=i;final android.view.View before=panels.peek();Button row=menuButton(labels[i],false,()->{selected.accept(n);if(!panels.isEmpty()&&panels.peek()==before)closeMenus();});addMenuRow(body,row);}
 }
 void messagePanel(String title,String message){LinearLayout body=openPanel(title);TextView t=menuText(message,14,FG);t.setPadding(dp(4),dp(8),dp(4),dp(12));addMenuRow(body,t);addMenuRow(body,menuButton("Compris",true,()->closePanel()));}

 public void onCreate(Bundle b){super.onCreate(b); LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(10),dp(8),dp(10),0);root.setBackgroundColor(0xff0b1420);layout=new LinearLayout(this);layout.setOrientation(1);root.addView(layout,new LinearLayout.LayoutParams(-1,-2));screen=new FrameLayout(this);screen.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(screen);
 LinearLayout brand=new LinearLayout(this);brand.setGravity(android.view.Gravity.CENTER_VERTICAL);brand.setPadding(0,0,0,dp(8));ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);brand.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));LinearLayout words=new LinearLayout(this);words.setOrientation(1);words.setPadding(dp(10),0,0,0);TextView title=new TextView(this);title.setText("RANGER ECU STUDIO");title.setTextColor(FG);title.setTextSize(20);title.setTypeface(null,android.graphics.Typeface.BOLD);words.addView(title);TextView sub=new TextView(this);sub.setText("FORD RANGER  •  DIAGNOSTIC CAN");sub.setTextColor(BLUE);sub.setTextSize(10);words.addView(sub);brand.addView(words,new LinearLayout.LayoutParams(0,-2,1));Button settings=new Button(this);settings.setText("⚙");settings.setTextSize(21);settings.setTextColor(BLUE);settings.setContentDescription("Paramètres avancés");settings.setPadding(0,0,0,0);settings.setBackground(panel(0xff152b40,0xff315b7e));brand.addView(settings,new LinearLayout.LayoutParams(dp(44),dp(44)));settings.setOnClickListener(v->advancedMenu());layout.addView(brand);
 button("Connexion Bluetooth",()->choose());
 button("Identifier le PCM",()->send("0902","0904","0906","ATDP"));
 button("Lire les codes défaut",()->send("03","07","0A"));
 button("Lire les capteurs",()->send("0101","0103","010C","010D","0105","0104","010F","0142","0106","0107","0108","0109","010E","0110","0111","0113","0114","0115","0118","0119"));
 button("Puissance / O2",()->send("0100","0120","0140","0101","03","07","020200","0103","0104","0105","0106","0107","0108","0109","010B","010C","010E","0110","0111","0113","0114","0115","0116","0117","0118","0119","011A","011B","06"));
 button("TPMS / réseau",()->{show("TPMS : géré par le SJB Ford. Les pressions ne sont pas des PID OBD-II génériques. Identification du réseau moteur uniquement ; accès SJB à confirmer.");send("ATI","ATDP","ATDPN","0900","0904","0906");});
 button("Déconnecter",()->{live=false;pcmReady=false;try{if(socket!=null)socket.close();}catch(Exception e){}setStatus("Déconnecté");});
 button("Partager le rapport",()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(android.content.Intent.EXTRA_TEXT,log.getText().toString());startActivity(android.content.Intent.createChooser(i,"Exporter"));});
 button("Mesures en direct",()->toggleLive());button("Vider le journal",()->log.setText(""));button("Maintenance PCM",()->maintenanceMenu());button("Comparer avant / après",()->compareKam());status=new TextView(this);status.setText("Déconnecté");status.setTextSize(12);status.setTextColor(BLUE);status.setPadding(dp(8),dp(7),dp(8),dp(7));status.setBackground(panel(0xff102437,0xff315b7e));root.addView(status,new LinearLayout.LayoutParams(-1,-2));
 TextView heading=new TextView(this);heading.setText("Journal de connexion et diagnostic");heading.setTextSize(14);heading.setTextColor(BLUE);heading.setPadding(dp(6),dp(10),dp(6),dp(5));root.addView(heading,new LinearLayout.LayoutParams(-1,-2));journal=new ScrollView(this);journal.setFillViewport(true);journal.setBackground(panel(0xff0e1b28,0xff263e54));log=new TextView(this);log.setTextColor(0xffeeeeee);log.setTextSize(12);log.setTypeface(android.graphics.Typeface.MONOSPACE);log.setPadding(dp(8),dp(8),dp(8),dp(8));log.setTextIsSelectable(true);journal.addView(log,new ScrollView.LayoutParams(-1,-2));root.addView(journal,new LinearLayout.LayoutParams(-1,0,1));TextView footer=new TextView(this);String version="1.2.10";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}footer.setText("Ranger ECU Studio — v"+version+"  •  Mise à jour test");footer.setGravity(android.view.Gravity.CENTER);footer.setPadding(0,dp(8),0,dp(8));footer.setTextSize(12);footer.setTextColor(0xff91a9be);root.addView(footer,new LinearLayout.LayoutParams(-1,-2));show("Connexion directe Ranger CAN 11/500. Jumeler le Viecar dans Android au besoin. Maintenance : effacement DTC après réparation uniquement. Reset KAM et programmation verrouillés.");}
 public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==1 && results.length>0 && results[0]==0)chooseDevice(advancedPermission);else if(requestCode==1)show("Autorisation Bluetooth refusée.");}
 void choose(){chooseDevice(false);}
 void advancedMenu(){
  String saved=getSharedPreferences("connection",MODE_PRIVATE).getString("device_name","Aucun");
  menu("Paramètres avancés",new String[]{"Adaptateur mémorisé : "+saved+" — changer","Autres protocoles OBD-II","Oublier l’adaptateur mémorisé"},n->{
   if(n==0)chooseDeviceList(false);
   if(n==1)chooseDevice(true);
   if(n==2){getSharedPreferences("connection",MODE_PRIVATE).edit().clear().apply();show("Adaptateur mémorisé supprimé.");}
  });
 }
 void chooseDeviceList(boolean advanced){pickDevice(advanced,true);}
 void chooseDevice(boolean advanced){pickDevice(advanced,false);}
 void pickDevice(boolean advanced,boolean forceList){
  if(busy.get()){show("Une opération est déjà en cours.");return;}
  if(Build.VERSION.SDK_INT>=31 && checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=0){advancedPermission=advanced;requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"},1);return;}
  try{
   BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();
   if(a==null||!a.isEnabled()){show("Activer Bluetooth.");return;}
   ArrayList<BluetoothDevice> ds=new ArrayList<>(a.getBondedDevices());
   if(ds.isEmpty()){show("Aucun adaptateur jumelé. Jumeler le Viecar dans Android.");return;}
   String saved=getSharedPreferences("connection",MODE_PRIVATE).getString("device_address","");
   if(!forceList&&!advanced&&!saved.isEmpty())for(BluetoothDevice d:ds){if(saved.equals(d.getAddress())){show("Adaptateur mémorisé : "+d.getName());submit(()->connect(d,"6"));return;}}
   String[] names=new String[ds.size()];
   for(int i=0;i<ds.size();i++)names[i]=ds.get(i).getName()+" "+ds.get(i).getAddress();
   menu("Adaptateur Bluetooth jumelé",names,n->{BluetoothDevice device=ds.get(n);if(advanced)chooseProtocol(device);else submit(()->connect(device,"6"));});
  }catch(Exception e){show("Bluetooth : "+e);}
 }
 void chooseProtocol(BluetoothDevice d){String[] labels={"CAN 11 bits / 500 kbit/s (Ranger confirmé)","Automatique (45 s)","Ford J1850 PWM (essai)","ISO 9141-2 (essai)","Adaptateur seulement (sans interrogation PCM)"};String[] protocols={"6","0","1","3",""};menu("Protocole OBD-II",labels,n->submit(()->connect(d,protocols[n])));}
 void connect(BluetoothDevice d,String protocol){try{pcmReady=false;can=protocol.equals("6");setStatus("Connexion en cours…");if(socket!=null)socket.close();socket=d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket.connect();show("Bluetooth connecté");setStatus("Bluetooth connecté — PCM non confirmé");for(String c:new String[]{"ATZ","ATE0","ATL0","ATS1","ATRV"})query(c);if(protocol.isEmpty()){show("Adaptateur prêt. Aucun test PCM effectué.");return;}query("ATSP"+protocol);show("Recherche PCM : "+(protocol.equals("0")?"automatique (45 s)":protocol)+" — attendre la réponse.");String answer=query("0100");if(answer.replaceAll("\\s+","").contains("4100")){pcmReady=true;show("Réponse PCM 41 00 reçue.");setStatus("PCM répond — protocole "+protocol);if(protocol.equals("6")){getSharedPreferences("connection",MODE_PRIVATE).edit().putString("device_address",d.getAddress()).putString("device_name",d.getName()==null?"Adaptateur Bluetooth":d.getName()).apply();show("Adaptateur mémorisé pour les prochaines connexions.");}}else show("Aucune réponse PCM positive confirmée. Essayer un autre protocole.");query("ATDP");String p=query("ATDPN");can=p.trim().matches("(?i)A?[6789]");}catch(Exception e){pcmReady=false;setStatus("Échec de connexion");show("Connexion : "+e);}}
 void setStatus(String s){runOnUiThread(()->{status.setText(s);status.setTextColor(s.startsWith("PCM répond")?0xff57d9a4:s.contains("Échec")?0xffff8e86:BLUE);});}
 void submit(Runnable task){if(!busy.compareAndSet(false,true)){show("Une lecture est déjà en cours.");return;}worker.submit(()->{try{task.run();}finally{busy.set(false);}});}
 void send(String... cs){submit(()->{try{for(String c:cs)query(c);}catch(Exception e){show("Erreur : "+e);}});}
 void toggleLive(){if(live){live=false;show("Arrêt des mesures demandé.");return;}if(busy.get()){show("Terminer la lecture en cours.");return;}live=true;submit(()->{try{show("Mesures en direct — véhicule stationné. Appuyer à nouveau pour arrêter.");while(live){for(String c:new String[]{"010C","0105","0106","0107","0108","0109","010E","0114","0115","0118","0119"}){if(!live)break;query(c);}if(live)Thread.sleep(2000);}}catch(Exception e){show("Mesures : "+e);}finally{live=false;show("Mesures terminées.");}});}
 String query(String c)throws Exception{BluetoothSocket sck=socket;if(sck==null||!sck.isConnected())throw new IOException("Non connecté");OutputStream out=sck.getOutputStream();InputStream in=sck.getInputStream();out.write((c+"\r").getBytes("US-ASCII"));out.flush();StringBuilder s=new StringBuilder();long end=SystemClock.elapsedRealtime()+(c.equals("0100")?45000:15000);while(SystemClock.elapsedRealtime()<end){if(in.available()>0){int v=in.read();if(v=='>'){String raw=s.toString().trim();show(c+" : "+raw);decode(c,raw);return s.toString();}if(v<0)break;s.append((char)v);}else Thread.sleep(20);}show(c+" : réponse partielle avant délai : "+(s.length()==0?"(aucun octet)":s.toString().trim()));sck.close();pcmReady=false;setStatus("Déconnecté — délai dépassé");throw new IOException("Délai dépassé : "+c+". Reconnecter et choisir un autre protocole.");}



 void maintenanceMenu(){
  menu("Maintenance PCM",new String[]{
   "Effacer les DTC après réparation",
   "Assistant KAM — mesures avant / après",
   "Reset KAM électronique — verrouillé",
   "Identification et sauvegarde OBD-II",
   "Afficher la dernière sauvegarde",
   "Configuration Ford As-Built — non disponible",
   "Reprogrammation PCM — verrouillée",
   "Autotest local — sans véhicule"
  },n->{
   if(n==0)confirmClear();
   else if(n==1)kamMenu();
   else if(n==2)messagePanel("Reset KAM Ford","La commande spécifique à la calibration n'est pas validée. L'effacement OBD-II ne constitue pas un Reset KAM garanti. Utiliser un outil Ford compatible; l'assistant permet de comparer les mesures.");
   else if(n==3)submit(()->{try{saveMaintenance("identification",new String[]{"ATI","ATRV","ATDP","ATDPN","0900","0902","0904","0906","0100","0120","0140","0101","03","07","020200","020400","020500","020C00","020D00"});show("Sauvegarde OBD-II terminée. Ce n'est pas une sauvegarde de la cartographie ni de l'As-Built.");}catch(Exception e){show("Sauvegarde incomplète : "+e);}});
   else if(n==4)show(getSharedPreferences("maintenance",MODE_PRIVATE).getString("latest","Aucune sauvegarde enregistrée."));
   else if(n==7)parserSelfTest();
   else messagePanel("Fonction non disponible","Accès aux modules Ford et compatibilité matérielle à vérifier. Aucune commande de programmation n'est envoyée.");
  });
 }
 boolean connectedForMaintenance(){BluetoothSocket x=socket;return pcmReady&&can&&x!=null&&x.isConnected();}
 void confirmClear(){
  if(!connectedForMaintenance()){show("Maintenance : connecter d'abord le PCM en CAN.");return;}
  if(busy.get()){show("Arrêter les mesures en direct et terminer la lecture en cours.");return;}
  LinearLayout body=openPanel("Effacement DTC après réparation");
  TextView info=menuText("Cette opération efface les données diagnostiques OBD-II et remet les moniteurs à non prêts. Elle ne répare pas une panne et ne garantit pas un Reset KAM. Les défauts persistants reviendront.",14,FG);addMenuRow(body,info);
  CheckBox repaired=new CheckBox(this);repaired.setText("Je confirme que les défauts concernés ont été réparés et que le véhicule est stationné, moteur arrêté, contact ON.");repaired.setTextColor(FG);repaired.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));addMenuRow(body,repaired);
  Button confirm=menuButton("Effacer après réparation",true,()->{closeMenus();submit(()->clearDtc());});confirm.setEnabled(false);confirm.setAlpha(0.45f);repaired.setOnCheckedChangeListener((box,checked)->{confirm.setEnabled(checked);confirm.setAlpha(checked?1f:0.45f);});addMenuRow(body,confirm);
 }
 boolean zeroPid(String raw,String prefix,int bytes){
  String h=raw.trim().replaceAll("\\s+","").toUpperCase(Locale.ROOT);
  StringBuilder expected=new StringBuilder(prefix);for(int i=0;i<bytes;i++)expected.append("00");return h.equals(expected.toString());
 }
 void requireStopped()throws Exception{
  if(!connectedForMaintenance())throw new IOException("Connexion CAN au PCM non confirmée.");
  if(!zeroPid(query("010C"),"410C",2)||!zeroPid(query("010D"),"410D",1))throw new IOException("Régime ou vitesse non nuls, ou réponse incertaine. Opération annulée.");
 }
 void saveMaintenance(String label,String[] commands)throws Exception{
  StringBuilder saved=new StringBuilder(label+" — "+new Date()+"\n");
  for(String cmd:commands)saved.append(cmd).append(" : ").append(query(cmd)).append('\n');
  if(!getSharedPreferences("maintenance",MODE_PRIVATE).edit().putString("latest",saved.toString()).commit())throw new IOException("Sauvegarde locale impossible.");
 }
 void clearDtc(){
  boolean attempted=false;
  try{
   requireStopped();
   saveMaintenance("Avant effacement DTC",new String[]{"0101","03","07","020200","020400","020500","020C00","020D00"});
   requireStopped();
   show("Sauvegarde locale effectuée. Effacement OBD-II après réparation.");
   attempted=true;
   String response=query("04");
   boolean ack=false;
   for(String line:response.split("[\\r\\n]+"))if(line.trim().equalsIgnoreCase("44"))ack=true;
   if(!ack){show("Effacement non confirmé. Ne pas répéter automatiquement. Vérifier les DTC.");return;}
   show("Réponse positive OBD-II reçue. Vérification des défauts et moniteurs.");
   query("0101");query("03");query("07");
   show("Contrôle terminé. Aucun Reset KAM Ford confirmé.");
  }catch(Exception e){show((attempted?"Résultat de l'effacement incertain; vérifier les DTC : ":"Effacement non envoyé : ")+e);}
 }

 void kamMenu(){
  String[] actions={"1. Enregistrer avant réinitialisation","2. Procédure de réinitialisation externe","3. Enregistrer après réinitialisation","4. Enregistrer au retour du problème"};
  menu("Assistant KAM — lecture seule",actions,n->{
   if(n==1){messagePanel("Reset KAM non disponible","Aucune commande Ford validée pour cette calibration. Utiliser un outil de diagnostic Ford compatible ou la procédure de déconnexion de la borne négative documentée par Ford, moteur arrêté et clé retirée. Ne jamais relier les bornes de la batterie. La réinitialisation peut affecter le ralenti et les moniteurs OBD. Après l'opération, reconnecter l'adaptateur et comparer à température et régime similaires.");return;}
   captureKam(n==0?"avant":n==2?"apres":"retour");
  });
 }
 int[] pidBytes(String raw,String prefix,int n){
  if(raw==null)return null;
  String h=raw.trim().replaceAll("\\s+","").toUpperCase(Locale.ROOT);
  if(!h.startsWith(prefix)||!h.matches("[0-9A-F]+")||h.length()!=prefix.length()+2*n)return null;
  int[] out=new int[n];for(int i=0;i<n;i++)out[i]=Integer.parseInt(h.substring(prefix.length()+2*i,prefix.length()+2*i+2),16);return out;
 }
 double value(String cmd,String raw){
  int n=Arrays.asList("010C","0110","0114","0115","0118","0119","0103").contains(cmd)?2:1;
  int[] a=pidBytes(raw,"41"+cmd.substring(2),n);if(a==null)return Double.NaN;
  switch(cmd){
   case "0105":return a[0]-40;
   case "010C":return (256*a[0]+a[1])/4.0;
   case "0106":case "0107":case "0108":case "0109":return (a[0]-128)*100.0/128;
   case "010E":return a[0]/2.0-64;
   case "0110":return (256*a[0]+a[1])/100.0;
   case "0114":case "0115":case "0118":case "0119":return a[0]/200.0;
   default:return a[0];
  }
 }
 Map<String,String> parseCapture(String text){
  Map<String,String> m=new LinkedHashMap<>();
  if(text!=null)for(String l:text.split("\\n")){int k=l.indexOf(" = ");if(k==4)m.put(l.substring(0,k),l.substring(k+3).trim());}return m;
 }
 boolean comparable(Map<String,String> m){
  double t=value("0105",m.get("0105")),r=value("010C",m.get("010C")),v=value("010D",m.get("010D")),loop=value("0103",m.get("0103"));
  return t>=80&&t<=110&&r>=500&&r<=1000&&v==0&&loop==2;
 }
 void captureKam(String phase){
  submit(()->{try{
   if(!connectedForMaintenance())throw new IOException("Connecter le PCM CAN avant la capture.");
   show("=== KAM : mesure "+phase+" (lecture seule) ===");
   StringBuilder data=new StringBuilder(new Date().toString()+"\n");
   for(String c:new String[]{"0101","03","07","0103","0105","010C","010D","0106","0107","0108","0109","010E","0110","0114","0115","0118","0119"}){
    String raw=query(c);data.append(c).append(" = ").append(raw.replace('\r',' ').replace('\n',' ')).append('\n');
   }
   Map<String,String> m=parseCapture(data.toString());
   for(String c:new String[]{"0105","010C","010D","0103","0107","0109"})if(!Double.isFinite(value(c,m.get(c))))throw new IOException("Donnée essentielle manquante : "+c);
   android.content.SharedPreferences prefs=getSharedPreferences("kam_test",MODE_PRIVATE);
   boolean good=comparable(m);
   android.content.SharedPreferences.Editor ed=prefs.edit().putString(phase+"_latest",data.toString());
   if(good||!comparable(parseCapture(prefs.getString(phase,""))))ed.putString(phase,data.toString());
   if(!ed.commit())throw new IOException("Sauvegarde locale impossible.");
   show(good?"Mesure comparable sauvegardée (moteur chaud, ralenti, boucle fermée).":"Mesure sauvegardée comme relevé non comparable. Une référence valide antérieure est conservée.");
   show("Aucun reset envoyé.");
  }catch(Exception e){show("Capture incomplète : "+e);}});
 }
 void compareKam(){
  android.content.SharedPreferences p=getSharedPreferences("kam_test",MODE_PRIVATE);
  String[] phases={"avant","apres","retour"};
  List<Map<String,String>> samples=new ArrayList<>();
  StringBuilder report=new StringBuilder("=== COMPARAISON KAM — mesures réelles ===\n");
  for(String phase:phases){String raw=p.getString(phase,"");Map<String,String> m=parseCapture(raw);samples.add(m);report.append(phase.toUpperCase(Locale.ROOT)).append(" : ").append(raw.isEmpty()?"absente":raw.split("\\n",2)[0]).append(" | ").append(comparable(m)?"conditions comparables":"conditions non validées").append('\n');}
  String[] cmds={"0105","010C","0103","0106","0107","0108","0109","010E","0110","0119"};
  String[] names={"Temp. °C","Régime tr/min","État boucle","STFT B1 %","LTFT B1 %","STFT B2 %","LTFT B2 %","Avance °","MAF g/s","B2S2 V"};
  report.append("\nAvant | Après | Retour\n");
  for(int i=0;i<cmds.length;i++){
   report.append(names[i]).append(" : ");
   for(int j=0;j<3;j++){if(j>0)report.append(" | ");double d=value(cmds[i],samples.get(j).get(cmds[i]));report.append(Double.isFinite(d)?fmt(d):"—");}
   report.append('\n');
  }
  Map<String,String> a=samples.get(0),b=samples.get(1);
  if(comparable(a)&&comparable(b)&&Math.abs(value("0105",a.get("0105"))-value("0105",b.get("0105")))<=10&&Math.abs(value("010C",a.get("010C"))-value("010C",b.get("010C")))<=150){
   for(String cmd:new String[]{"0107","0109"}){double x=value(cmd,a.get(cmd)),y=value(cmd,b.get(cmd));if(Double.isFinite(x)&&Double.isFinite(y))report.append(cmd.equals("0107")?"Variation LTFT B1 : ":"Variation LTFT B2 : ").append(fmt(y-x)).append(" points\n");}
  }else report.append("Comparer à température et régime similaires avant de conclure.\n");
  report.append("Une baisse des LTFT ne démontre pas à elle seule un Reset KAM complet.\n");
  show(report.toString());
 }
 void parserSelfTest(){
  boolean ok=Math.abs(value("0107","41 07 8F")-11.71875)<0.001&&Math.abs(value("0109","41 09 8C")-9.375)<0.001&&value("010C","41 0C 0B 41")==720.25&&value("0105","41 05 7E")==86&&Double.isNaN(value("0107","NO DATA"));
  show("Autotest local du décodeur KAM : "+(ok?"RÉUSSI":"ÉCHEC")+" — données fictives, aucun message envoyé au véhicule.");
 }
 String fmt(double n){return String.format(Locale.CANADA_FRENCH,"%.2f",n);}
 void decode(String cmd,String raw){
  try{
   String upper=raw.toUpperCase(Locale.ROOT);
   if(cmd.equals("03")||cmd.equals("07")||cmd.equals("0A")){
    if(!can){show("Décodage DTC automatique réservé au CAN; voir réponse brute.");return;}
    String sid=cmd.equals("03")?"43":cmd.equals("07")?"47":"4A";
    boolean found=false;
    StringBuilder joined=new StringBuilder();int next=0;
    for(String line:upper.split("[\\r\\n]+")){
     java.util.regex.Matcher m=java.util.regex.Pattern.compile("^\\s*([0-9A-F]+):\\s*([0-9A-F\\s]+)$").matcher(line);
     if(m.matches()&&Integer.parseInt(m.group(1),16)==next){joined.append(m.group(2).replaceAll("\\s+",""));next++;}
     else if(next>0)break;
    }
    if(next>0)upper=joined.toString();
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
     case 3: v="Système carburant 1 : "+loop(a)+(b>=0?" ; système 2 : "+loop(b):"");break;
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