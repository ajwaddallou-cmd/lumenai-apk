package de.nuskha.keyboard;
import android.app.Activity;import android.os.Bundle;import android.webkit.*;import android.content.*;
public class MainActivity extends Activity {
 WebView w;
 public void onCreate(Bundle b){super.onCreate(b);w=new WebView(this);w.setBackgroundColor(0xff10182b);w.getSettings().setJavaScriptEnabled(true);w.getSettings().setDomStorageEnabled(true);w.getSettings().setAllowFileAccess(false);w.addJavascriptInterface(new Bridge(),"Nuskha");setContentView(w);w.loadUrl("file:///android_asset/app.html");}
 void deliver(String json){w.evaluateJavascript("nativeClean("+org.json.JSONObject.quote(json)+")",null);}
 public class Bridge {
 @JavascriptInterface public void setup(){runOnUiThread(()->startActivity(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)));}
 @JavascriptInterface public void microphone(){runOnUiThread(()->requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},7));}
 @JavascriptInterface public int cleanHasKey(){return CleanupClient.hasKey(MainActivity.this)?1:0;}
 @JavascriptInterface public void cleanSetKey(String k){try{CleanupClient.setKey(MainActivity.this,k);}catch(Exception e){}}
 @JavascriptInterface public void cleanClearKey(){CleanupClient.clearKey(MainActivity.this);}
 @JavascriptInterface public String cleanPair(String code){return CleanupClient.pair(MainActivity.this,code);}
 @JavascriptInterface public void clean(String t){new Thread(()->{final String r=CleanupClient.clean(MainActivity.this,t);runOnUiThread(()->deliver(r));}).start();}
 }
}
