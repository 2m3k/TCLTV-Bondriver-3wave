package app.tcltv3wave;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.security.SecureRandom;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private TextView status,details,network,tunerState;
 private final android.os.Handler networkHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable networkRefresh=new Runnable(){public void run(){updateNetwork();networkHandler.postDelayed(this,5000);}};
 private ProgressBar progress;
 private Button setup,start,reboot,refresh,restore;
 private File stage; private PhonePairing pairing; private AlertDialog pairingDialog;private android.net.nsd.NsdManager.RegistrationListener phoneAdvertisement;private TextView pairingInfo;private Button pairButton;
 private boolean destroyed;
 private volatile boolean tunerCheckRunning;
 private int openSection=0;
 private boolean userChoseSection=false;
 private boolean autoSectionDone=false;
 private volatile boolean tunerAnyRunning=false;
 private LinearLayout[] sectionArr;
 private Button[] tileArr;
 // Material You style palette (dark, teal seed)
 private static final int COL_BG=Color.rgb(13,18,22);
 private static final int COL_CARD=Color.rgb(27,35,41);
 private static final int COL_BTN=Color.rgb(38,50,59);
 private static final int COL_BTN_FOCUS=Color.rgb(52,72,84);
 private static final int COL_PRIMARY=Color.rgb(11,110,79);
 private static final int COL_PRIMARY_FOCUS=Color.rgb(16,143,104);
 private static final int COL_DANGER=Color.rgb(96,34,32);
 private static final int COL_DANGER_FOCUS=Color.rgb(133,44,40);
 private static final int COL_TEXT=Color.rgb(227,235,238);
 private static final int COL_MUTED=Color.rgb(192,208,215);
 private static final int COL_ACCENT=Color.rgb(125,218,190);
 private static final int COL_OK=Color.rgb(105,222,155);
 private static final int COL_OFF=Color.rgb(140,155,163);
 private static final int COL_WAIT=Color.rgb(255,196,86);
 private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+0.5f); }
 private GradientDrawable shape(int color,int radiusDp,int strokeColor,int strokeDp) {
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radiusDp));
  if(strokeDp>0)d.setStroke(dp(strokeDp),strokeColor);
  return d;
 }
 private LinearLayout card(LinearLayout parent) {
  LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);
  c.setBackground(shape(COL_CARD,20,0,0));c.setPadding(dp(20),dp(16),dp(20),dp(16));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(8));parent.addView(c,lp);
  return c;
 }
 private TextView section(LinearLayout parent,String label) {
  TextView v=text(label,14,COL_ACCENT);v.setPadding(dp(4),dp(10),0,dp(2));
  parent.addView(v);return v;
 }
 private TextView note(LinearLayout parent,String value) {
  TextView v=text(value,14,COL_MUTED);v.setPadding(dp(4),dp(2),dp(4),dp(6));parent.addView(v);return v;
 }
 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  ScrollView scroll=new ScrollView(this);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(48),dp(18),dp(48),dp(18));root.setBackgroundColor(COL_BG);scroll.addView(root);setContentView(scroll);
  TextView title=text("32S5400 3波対応版",26,Color.WHITE);title.setTypeface(null,android.graphics.Typeface.BOLD);root.addView(title);
  root.addView(text("テレビで別のアプリを使いながら、PCで異なる地上波・BS・CS局を同時受信",14,COL_MUTED));
  // ---- Status dashboard card ----
  LinearLayout dash=card(root);dash.setPadding(dp(20),dp(8),dp(20),dp(8));
  LinearLayout.LayoutParams dashLp=(LinearLayout.LayoutParams)dash.getLayoutParams();dashLp.setMargins(0,dp(16),0,dp(4));
  TextView dashLabel=text("いまの状態",14,COL_ACCENT);dash.addView(dashLabel);
  LinearLayout dashCols=new LinearLayout(this);dashCols.setOrientation(LinearLayout.HORIZONTAL);dash.addView(dashCols);
  LinearLayout dashL=new LinearLayout(this);dashL.setOrientation(LinearLayout.VERTICAL);dashL.setPadding(0,0,dp(10),0);dashCols.addView(dashL,new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout dashR=new LinearLayout(this);dashR.setOrientation(LinearLayout.VERTICAL);dashR.setPadding(dp(10),0,0,0);dashCols.addView(dashR,new LinearLayout.LayoutParams(0,-2,1));
  network=text("有線LAN IP：確認中…",17,COL_ACCENT);network.setTextIsSelectable(true);network.setLineSpacing(0,1.15f);network.setPadding(0,dp(6),0,0);dashL.addView(network);
  tunerState=text("チューナー状態：確認中…",19,COL_TEXT);tunerState.setTypeface(null,android.graphics.Typeface.BOLD);tunerState.setPadding(0,dp(6),0,0);dashR.addView(tunerState);
  status=text("初めて使う場合は「1. セットアップ」を選んでください。",17,Color.WHITE);status.setLineSpacing(0,1.25f);status.setPadding(0,dp(8),0,dp(4));dashR.addView(status);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setVisibility(View.GONE);dash.addView(progress);
  pairingInfo=text("",26,COL_ACCENT);pairingInfo.setFocusable(true);pairingInfo.setVisibility(View.GONE);root.addView(pairingInfo);
  // ---- Category tiles (2x2) + collapsible sections ----
  final LinearLayout[] sections=new LinearLayout[4];
  final Button[] tiles=new Button[4];
  LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);root.addView(grid);
  LinearLayout grow1=new LinearLayout(this);grow1.setOrientation(LinearLayout.HORIZONTAL);grid.addView(grow1);
  LinearLayout grow2=new LinearLayout(this);grow2.setOrientation(LinearLayout.HORIZONTAL);grid.addView(grow2);
  String[] catNames={"セットアップ","ふだんの操作","IPTV・スマートフォン","メンテナンス"};
  for(int i=0;i<4;i++){
   sections[i]=new LinearLayout(this);sections[i].setOrientation(LinearLayout.VERTICAL);
   final int idx=i;
   tiles[i]=tileButton(idx<2?grow1:grow2,catNames[i],()->{userChoseSection=true;showSection(sections,tiles,idx);});
  }
  for(final Button t:tiles)t.setOnFocusChangeListener((v,f)->refreshTiles(tiles));
  for(int i=0;i<4;i++){root.addView(sections[i]);sections[i].setVisibility(i==0?View.VISIBLE:View.GONE);}
  refreshTiles(tiles);
  sectionArr=sections;tileArr=tiles;
  // ---- Setup section ----
  setup=button(sections[0],"1. セットアップ",1,()->perform("対応環境を確認し、セットアップしています…",()->{
   extract();String output=root("sh "+quote(new File(stage,"install.sh").getAbsolutePath())+" "+quote(stage.getAbsolutePath()),90);
   TclPolicy.configure(this,true);NetworkService.enable(this,true);return "セットアップが完了しました。次に「2. テレビを再起動」してください。\n"+output;
  }));
  reboot=button(sections[0],"2. テレビを再起動",0,()->new AlertDialog.Builder(this).setTitle("テレビを再起動します").setMessage("再起動後、配信機能を自動的に開始します。").setNegativeButton("戻る",null).setPositiveButton("再起動",(d,w)->perform("テレビを再起動しています…",()->root("sync; reboot",15))).show());
  refresh=button(sections[0],"3. 状態を確認",0,()->perform("状態を確認しています…",()->{
   String output=root("/data/local/tmp/tsctl --status; ip -4 addr show eth0",15);
   boolean first=output.contains(" running=1 "),second=output.contains(" tuner1_ready=1 ");
   return "チューナー1："+(first?"配信中":"セットアップ後に再起動してください")+"\nチューナー2："+(second?"配信中":"選局待ち（PCの視聴ソフトで選局してください）")+"\n"+output;
  }));
  // ---- Daily operations ----
  start=button(sections[1],"配信を開始・復旧",0,()->perform("配信機能を確認しています…",()->root("sh /data/adb/service.d/91-tcl-pvr.sh; /data/local/tmp/tsctl --status",90)));
  button(sections[1],"番組表XMLを今すぐ更新（視聴優先）",0,()->{status.setText(NetworkService.refreshEpg());details.setText("");});
  button(sections[1],"アプリの更新を確認",0,this::checkAppUpdate);
  // ---- IPTV / phone ----
  button(sections[2],"画面消灯中の通信維持を有効にする",0,()->perform("TCLの常駐許可を設定しています…",()->{TclPolicy.configure(this,true);NetworkService.enable(this,true);return "画面消灯中の通信維持とIPTVを有効にしました。";}));
  button(sections[2],"IPTVのM3U・番組表XMLを表示",0,this::showIptv);
  pairButton=button(sections[2],"スマートフォンを接続（確認コードを表示）",0,this::pairPhone);
  button(sections[2],"通信維持を停止する",0,()->perform("通信維持を停止しています…",()->{NetworkService.enable(this,false);TclPolicy.configure(this,false);return "通信維持を停止しました。";}));
  note(sections[2],"通信維持中は深い待機に入らず消費電力が増えます。AC給電中の自動消灯も抑制しますが、リモコンで画面は消せます。");
  // ---- Maintenance ----
  details=text("",14,COL_MUTED);details.setTextIsSelectable(true);details.setPadding(dp(4),dp(4),0,dp(8));sections[3].addView(details);
  restore=button(sections[3],"セットアップ前の状態に戻す",2,()->new AlertDialog.Builder(this).setTitle("元の状態に戻しますか？").setMessage("保存済みの設定を復元します。反映にはテレビの再起動が必要です。").setNegativeButton("戻る",null).setPositiveButton("復元",(d,w)->perform("設定を復元しています…",()->{extract();NetworkService.enable(this,false);TclPolicy.configure(this,false);return root("sh "+quote(new File(stage,"restore.sh").getAbsolutePath()),60);})).show());
  note(sections[3],"対応環境：TCL G06 日本向け・Magisk導入済み\n初回はMagiskのroot許可が必要です。");
  tiles[0].requestFocus();
 }
 private Button tileButton(LinearLayout parent,String label,Runnable click) {
  Button b=new Button(this);b.setText(label);b.setTextSize(19);b.setAllCaps(false);b.setFocusable(true);b.setMinHeight(dp(50));b.setTextColor(Color.WHITE);
  b.setBackground(shape(COL_BTN,16,0,0));
  b.setOnClickListener(v->click.run());
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(3),dp(3),dp(3),dp(3));parent.addView(b,lp);return b;
 }
 private void refreshTiles(Button[] tiles) {
  for(int i=0;i<tiles.length;i++){
   boolean open=i==openSection,foc=tiles[i].hasFocus();
   int bg=open?COL_PRIMARY:(foc?COL_BTN_FOCUS:COL_BTN);
   tiles[i].setBackground(shape(bg,16,(foc||open)?Color.WHITE:0,(foc||open)?2:0));
  }
 }
 private void showSection(LinearLayout[] sections,Button[] tiles,int idx) {
  openSection=idx;
  for(int i=0;i<sections.length;i++)sections[i].setVisibility(i==idx?View.VISIBLE:View.GONE);
  refreshTiles(tiles);
  for(int i=0;i<sections[idx].getChildCount();i++){View c=sections[idx].getChildAt(i);if(c instanceof Button){c.requestFocus();break;}}
 }
 private void showSectionQuiet(int idx) {
  if(sectionArr==null||idx==openSection)return;
  openSection=idx;
  for(int i=0;i<sectionArr.length;i++)sectionArr[i].setVisibility(i==idx?View.VISIBLE:View.GONE);
  refreshTiles(tileArr);
 }
 private void showIptv(){StringBuilder urls=new StringBuilder(NetworkService.state+"\n"+NetworkService.epgStatus()+"\n\n");String code=NetworkService.accessCode(this);try{java.util.Enumeration<java.net.NetworkInterface> interfaces=java.net.NetworkInterface.getNetworkInterfaces();while(interfaces.hasMoreElements()){java.net.NetworkInterface iface=interfaces.nextElement();if(!iface.isUp()||iface.isLoopback())continue;java.util.Enumeration<java.net.InetAddress> addresses=iface.getInetAddresses();while(addresses.hasMoreElements()){java.net.InetAddress address=addresses.nextElement();if(!(address instanceof java.net.Inet4Address))continue;String base="http://"+address.getHostAddress()+":40560/iptv/"+code+"/playlist.m3u?tuner=";urls.append(iface.getName()).append(" · チューナー1\n").append(base).append("0\n\nチューナー2\n").append(base).append("1\n\n番組表XML（両チューナー共通）\n").append(base.substring(0,base.indexOf("playlist.m3u"))).append("epg.xml\n\n");}}}catch(Exception ignored){}urls.append("先に通信維持を有効にしてください。URLをIPTVアプリやVLCへ登録します。同じチューナーはBonDriver・スマホと同時使用しないでください。URLは専用の接続キーを含むため公開しないでください。");TextView text=text(urls.toString(),16,Color.WHITE);text.setTextIsSelectable(true);text.setPadding(dp(20),dp(20),dp(20),dp(20));ScrollView scroll=new ScrollView(this);scroll.addView(text);new AlertDialog.Builder(this).setTitle("IPTV / M3U / XMLTV").setView(scroll).setPositiveButton("閉じる",null).show();}
 private static final String BUNDLED_DAEMON="v28";
 private void checkAppUpdate() {
  status.setText("アプリの更新を確認しています…");details.setText("");
  worker.submit(()->{
   String current="?";String tag=null,url=null;String error=null;
   try {
    current=getPackageManager().getPackageInfo(getPackageName(),0).versionName;
    java.net.HttpURLConnection conn=(java.net.HttpURLConnection)new java.net.URL("https://api.github.com/repos/2m3k/TCLTV-Bondriver-3wave/releases?per_page=1").openConnection();
    conn.setConnectTimeout(10000);conn.setReadTimeout(10000);conn.setRequestProperty("User-Agent","tcltv3wave-update-check");
    if(conn.getResponseCode()!=200)throw new java.io.IOException("HTTP "+conn.getResponseCode());
    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
    try(java.io.InputStream in=conn.getInputStream()){while((n=in.read(buf))>0)out.write(buf,0,n);}
    conn.disconnect();
    org.json.JSONArray releases=new org.json.JSONArray(out.toString("UTF-8"));
    if(releases.length()>0){org.json.JSONObject rel=releases.getJSONObject(0);tag=rel.getString("tag_name");url=rel.getString("html_url");}
   } catch(Exception e) { error=e.getMessage(); }
   final String cur=current,ftag=tag,furl=url,ferr=error;
   runOnUiThread(()->{
    if(destroyed)return;
    status.setText("アプリの更新確認が終わりました");
    AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("アプリの更新確認");
    if(ferr!=null||ftag==null){dialog.setMessage("更新情報を取得できませんでした。インターネット接続を確認してください。\n現在のバージョン："+cur+"\n同梱デーモン："+BUNDLED_DAEMON);dialog.setPositiveButton("閉じる",null);}
    else {
     boolean newer=compareVersion(ftag,cur)>0;
     dialog.setMessage("現在のバージョン："+cur+"\n最新のリリース："+ftag+"\n同梱デーモン："+BUNDLED_DAEMON+"\n"+(newer?"新しいバージョンがあります。リリースページから新しいAPKを入手できます。":"最新です。"));
     dialog.setNegativeButton("閉じる",null);
     if(newer)dialog.setPositiveButton("リリースページを開く",(d,w)->{try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(furl)));}catch(Exception ignored){}});
    }
    dialog.show();
   });
  });
 }
 private static int compareVersion(String a,String b) {
  int[] x=versionParts(a),y=versionParts(b);
  for(int i=0;i<3;i++){if(x[i]!=y[i])return x[i]<y[i]?-1:1;}
  return 0;
 }
 private static int[] versionParts(String v) {
  int[] parts=new int[3];if(v==null)return parts;
  String t=v.startsWith("v")?v.substring(1):v;String[] seg=t.split("\\.");
  for(int i=0;i<seg.length&&i<3;i++){int num=0;for(int j=0;j<seg[i].length();j++){char c=seg[i].charAt(j);if(c<'0'||c>'9')break;num=num*10+(c-'0');}parts[i]=num;}
  return parts;
 }
 private void pairPhone(){if(pairing!=null){pairing.close();pairing=null;stopPhoneAdvertisement();pairingInfo.setVisibility(View.GONE);pairButton.setText("スマートフォンを接続（確認コードを表示）");status.setText("スマートフォンの接続受付を終了しました");return;}pairButton.setEnabled(false);status.setText("スマートフォン用の設定を準備しています…");worker.submit(()->{try{byte[] data=PhonePairing.export(this);runOnUiThread(()->{if(destroyed)return;pairButton.setEnabled(true);try{pairing=new PhonePairing(data,message->runOnUiThread(()->{if(!destroyed){status.setText(message);pairing=null;stopPhoneAdvertisement();pairingInfo.setVisibility(View.GONE);pairButton.setText("スマートフォンを接続（確認コードを表示）");}}));advertisePhone();pairingInfo.setText("確認コード  "+pairing.code+"\nスマホにTVのIPとこの6桁を入力してください。\n2分間・1回のみ有効です。TVで他のアプリを開くと無効になります。");pairingInfo.setVisibility(View.VISIBLE);pairButton.setText("スマートフォンの接続受付を終了");pairingInfo.requestFocus();status.setText("スマートフォンの接続を待っています");}catch(Exception e){status.setText("接続の受付を開始できません。もう一度お試しください。");}});}catch(Exception e){runOnUiThread(()->{if(!destroyed){pairButton.setEnabled(true);status.setText(e.getMessage());}});}});}
 private void advertisePhone(){stopPhoneAdvertisement();android.net.nsd.NsdManager nsd=(android.net.nsd.NsdManager)getSystemService(NSD_SERVICE);android.net.nsd.NsdServiceInfo service=new android.net.nsd.NsdServiceInfo();service.setServiceName("TCL TV");service.setServiceType("_tcltv._tcp.");service.setPort(40559);service.setAttribute("protocol","1");phoneAdvertisement=new android.net.nsd.NsdManager.RegistrationListener(){public void onServiceRegistered(android.net.nsd.NsdServiceInfo i){runOnUiThread(()->{if(pairing==null)stopPhoneAdvertisement();});}public void onRegistrationFailed(android.net.nsd.NsdServiceInfo i,int e){}public void onServiceUnregistered(android.net.nsd.NsdServiceInfo i){}public void onUnregistrationFailed(android.net.nsd.NsdServiceInfo i,int e){}};try{nsd.registerService(service,android.net.nsd.NsdManager.PROTOCOL_DNS_SD,phoneAdvertisement);}catch(Exception ignored){phoneAdvertisement=null;}}
 private void stopPhoneAdvertisement(){if(phoneAdvertisement==null)return;try{((android.net.nsd.NsdManager)getSystemService(NSD_SERVICE)).unregisterService(phoneAdvertisement);}catch(Exception ignored){}phoneAdvertisement=null;}
 private void updateNetwork() {
  StringBuilder wired=new StringBuilder(),wifi=new StringBuilder();
  try {
   java.util.Enumeration<java.net.NetworkInterface> interfaces=java.net.NetworkInterface.getNetworkInterfaces();
   while(interfaces!=null&&interfaces.hasMoreElements()) {
    java.net.NetworkInterface iface=interfaces.nextElement();String name=iface.getName();boolean ethernet=name.matches("eth[0-9]+")||name.matches("en[A-Za-z0-9_.-]+");boolean wireless=name.matches("wlan[0-9]+")||name.matches("wl[A-Za-z0-9_.-]+");
    if(!iface.isUp()||iface.isLoopback()||(!ethernet&&!wireless))continue;
    StringBuilder target=ethernet?wired:wifi;java.util.Enumeration<java.net.InetAddress> addresses=iface.getInetAddresses();
    while(addresses.hasMoreElements()) {java.net.InetAddress address=addresses.nextElement();if(address instanceof java.net.Inet4Address&&!address.isLoopbackAddress()) {if(target.length()>0)target.append(" / ");target.append(address.getHostAddress());}}
   }
   network.setText("有線LAN IP："+(wired.length()==0?"未接続・IPv4未取得":wired.toString())+"\nWi-Fi IP："+(wifi.length()==0?"未接続・IPv4未取得":wifi.toString())+"\n接続先IP（PCセットアップ時は自動取得できます）\n"+NetworkService.state+"\n"+NetworkService.epgStatus());
  }catch(Exception error) {network.setText("IPを取得できません。TVのネットワーク設定を確認してください。");}
 }
 private long lastTunerCheck;
 private void updateTunerState() {
  long now=System.currentTimeMillis();
  if(tunerCheckRunning||now-lastTunerCheck<60000)return;
  tunerCheckRunning=true;lastTunerCheck=now;
  worker.submit(()->{
   CharSequence line;
   try {
    String output=root("/data/local/tmp/tsctl --status",15);
    boolean first=output.contains(" running=1 "),second=output.contains(" tuner1_ready=1 ");
    tunerAnyRunning=first||second;
    android.text.SpannableStringBuilder sb=new android.text.SpannableStringBuilder();
    sb.append("チューナー1：");
    int s1=sb.length();sb.append("●");sb.setSpan(new android.text.style.ForegroundColorSpan(first?COL_OK:COL_OFF),s1,s1+1,0);
    sb.append(first?" 配信中":" 停止中");
    sb.append("\nチューナー2：");
    int s2=sb.length();sb.append("●");sb.setSpan(new android.text.style.ForegroundColorSpan(second?COL_OK:COL_WAIT),s2,s2+1,0);
    sb.append(second?" 配信中":" 選局待ち");
    line=sb;
   } catch(Exception error) { line="チューナー状態：未確認（セットアップ後に表示されます）"; }
   final CharSequence result=line;
   runOnUiThread(()->{tunerCheckRunning=false;if(!destroyed){tunerState.setText(result);if(!userChoseSection&&!autoSectionDone){autoSectionDone=true;showSectionQuiet(tunerAnyRunning?1:0);}}});
  });
 }
 @Override protected void onResume() {super.onResume();networkHandler.removeCallbacks(networkRefresh);networkHandler.post(networkRefresh);updateTunerState();}
 @Override protected void onPause(){networkHandler.removeCallbacks(networkRefresh);super.onPause();}
 @Override protected void onStop(){if(pairing!=null){pairing.close();pairing=null;}stopPhoneAdvertisement();if(pairingInfo!=null)pairingInfo.setVisibility(View.GONE);if(pairButton!=null)pairButton.setText("スマートフォンを接続（確認コードを表示）");super.onStop();}
 private TextView text(String value,int size,int color) { TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v; }
 // style: 0=normal, 1=primary, 2=danger
 private Button button(LinearLayout parent,String label,int style,Runnable click) {
  final int base=style==1?COL_PRIMARY:(style==2?COL_DANGER:COL_BTN);
  final int focus=style==1?COL_PRIMARY_FOCUS:(style==2?COL_DANGER_FOCUS:COL_BTN_FOCUS);
  Button button=new Button(this);button.setText(label);button.setTextSize(18);button.setAllCaps(false);button.setFocusable(true);button.setMinHeight(dp(52));button.setTextColor(Color.WHITE);
  button.setBackground(shape(base,16,0,0));
  button.setOnFocusChangeListener((v,hasFocus)->button.setBackground(shape(hasFocus?focus:base,16,hasFocus?Color.WHITE:0,hasFocus?2:0)));
  button.setOnClickListener(v->click.run());
  LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.setMargins(0,dp(3),0,dp(3));parent.addView(button,layout);return button;
 }
 private void busy(boolean value) { progress.setVisibility(value?View.VISIBLE:View.GONE);for(Button b:new Button[]{setup,start,reboot,refresh,restore})b.setEnabled(!value); }
 private void perform(String message,Callable<String> action) {
  busy(true);status.setText(message);details.setText("");
  worker.submit(()->{String result;boolean ok;
   try { result=action.call();ok=true; } catch(Exception error) { result="処理を完了できませんでした。\n"+error.getMessage();ok=false; }
   final String output=result;final boolean success=ok;
   runOnUiThread(()->{if(destroyed)return;busy(false);String[] lines=output.split("\n",2);status.setText(lines[0]);details.setText(lines.length>1?lines[1]:"");updateTunerState();if(!success)setup.requestFocus();});
  });
 }
 private static String quote(String value) { return "'"+value.replace("'","'\"'\"'")+"'"; }
 private String root(String command,int timeout) throws Exception {
  Process process=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
  if(!process.waitFor(timeout,TimeUnit.SECONDS)) { process.destroyForcibly();throw new IOException("時間切れです。Magiskの許可とテレビの状態を確認してください。"); }
  ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
  try(InputStream stream=process.getInputStream()) {while((count=stream.read(buffer))>0)if(output.size()<32768)output.write(buffer,0,Math.min(count,32768-output.size()));}
  String text=output.toString("UTF-8").trim();if(process.exitValue()!=0)throw new IOException(text.isEmpty()?"root権限を許可してください。":text);return text;
 }
 private void extract() throws Exception {
  stage=new File(getFilesDir(),"setup-assets");if(!stage.isDirectory()&&!stage.mkdirs())throw new IOException("保存先を作成できません。");
  for(String name:new String[]{"install.sh","restore.sh","libtsd.so","tsctl","90-tsd-inject.sh","91-tcl-pvr.sh","tcl-pvr-net.sh"}) {
   try(InputStream input=getAssets().open(name);OutputStream output=new FileOutputStream(new File(stage,name))) {byte[] b=new byte[16384];int n;while((n=input.read(b))>0)output.write(b,0,n);}
  }
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);StringBuilder token=new StringBuilder();for(byte b:bytes)token.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  try(OutputStream output=new FileOutputStream(new File(stage,"token"))) {output.write((token.toString()+"\n").getBytes("UTF-8"));}
 }
 @Override protected void onDestroy() {destroyed=true;worker.shutdown();super.onDestroy();}
}
