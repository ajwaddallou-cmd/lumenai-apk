package de.nuskha.keyboard;
import android.inputmethodservice.InputMethodService;import android.view.*;import android.webkit.*;import android.speech.*;import android.os.*;import android.content.*;import java.util.*;
public class NuskhaIME extends InputMethodService {
 WebView web; SpeechRecognizer sr;
 public View onCreateInputView(){web=new WebView(this);web.setBackgroundColor(0xff161718);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.addJavascriptInterface(new Bridge(),"Nuskha");web.setLayoutParams(new ViewGroup.LayoutParams(-1,(int)(444*getResources().getDisplayMetrics().density)));web.loadUrl("file:///android_asset/app.html?keyboard=1");return web;}
 void state(String s){web.evaluateJavascript("nativeStatus("+org.json.JSONObject.quote(s)+")",null);}
 public class Bridge{
 @JavascriptInterface public void insert(String s){new Handler(getMainLooper()).post(()->{if(getCurrentInputConnection()!=null)getCurrentInputConnection().commitText(s,1);});}
 @JavascriptInterface public void eraseN(int n){new Handler(getMainLooper()).post(()->{if(getCurrentInputConnection()!=null)getCurrentInputConnection().deleteSurroundingTextInCodePoints(Math.max(1,n),0);});}
 @JavascriptInterface public void erase(){new Handler(getMainLooper()).post(()->{if(getCurrentInputConnection()!=null)getCurrentInputConnection().deleteSurroundingTextInCodePoints(1,0);});}
 @JavascriptInterface public void voice(){new Handler(getMainLooper()).post(()->startVoice());}@JavascriptInterface public void stopVoice(){new Handler(getMainLooper()).post(()->{if(sr!=null)sr.stopListening();});}
 @JavascriptInterface public int cleanHasKey(){return CleanupClient.hasKey(NuskhaIME.this)?1:0;}
 @JavascriptInterface public void cleanSetKey(String k){try{CleanupClient.setKey(NuskhaIME.this,k);}catch(Exception e){}}
 @JavascriptInterface public void cleanClearKey(){CleanupClient.clearKey(NuskhaIME.this);}
 @JavascriptInterface public String cleanPair(String code){return CleanupClient.pair(NuskhaIME.this,code);}
 @JavascriptInterface public void clean(String t){new Thread(()->{final String r=CleanupClient.clean(NuskhaIME.this,t);new Handler(getMainLooper()).post(()->web.evaluateJavascript("nativeClean("+org.json.JSONObject.quote(r)+")",null));}).start();}
 }
 void startVoice(){
 if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){state("Mikrofon bitte in der Nuskha-App erlauben.");return;}
 if(!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)){state("On-Device-Spracherkennung fehlt. Kein Cloud-Fallback.");return;}
 if(sr!=null)sr.destroy();sr=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
 sr.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){state("Ich höre zu. Auf dem Gerät.");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){state("Wird erkannt …");}public void onError(int e){state("Spracherkennung gestoppt ("+e+").");}public void onResults(Bundle b){ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())web.evaluateJavascript("nativeResult("+org.json.JSONObject.quote(a.get(0))+")",null);}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){} });
 Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"de-DE");i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);sr.startListening(i);
 }
 @Override public void onFinishInput(){super.onFinishInput();if(sr!=null){sr.destroy();sr=null;}}
 @Override public void onDestroy(){if(sr!=null)sr.destroy();if(web!=null)web.destroy();super.onDestroy();}
}
