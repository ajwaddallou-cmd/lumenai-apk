package de.nuskha.keyboard;
import android.content.Context;import android.util.Base64;
import java.io.*;import java.net.*;import java.security.KeyStore;
import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;import android.security.keystore.KeyProperties;
public class CleanupClient {
 static final String ENDPOINT="https://nuskha-cleanup.ajwad-dallou.workers.dev/cleanup";
 static final String PAIR_URL="https://nuskha-cleanup.ajwad-dallou.workers.dev/pair";
 static final String PREFS="nuskha_keys",PREF="device_key_wrapped",ALIAS="nuskha_devkey";
 public static boolean hasKey(Context c){return c.getSharedPreferences(PREFS,0).getString(PREF,null)!=null;}
 public static void setKey(Context c,String key)throws Exception{c.getSharedPreferences(PREFS,0).edit().putString(PREF,wrap(key)).apply();}
 public static void clearKey(Context c){c.getSharedPreferences(PREFS,0).edit().remove(PREF).apply();}
 static javax.crypto.SecretKey keystoreKey()throws Exception{
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias(ALIAS)){
   KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
   g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
   g.generateKey();
  }
  return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
 }
 static String wrap(String plain)throws Exception{
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,keystoreKey());
  byte[] ct=c.doFinal(plain.getBytes("UTF-8"));
  return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(ct,Base64.NO_WRAP);
 }
 static String unwrap(String w)throws Exception{
  String[]p=w.split(":");byte[]iv=Base64.decode(p[0],Base64.NO_WRAP),ct=Base64.decode(p[1],Base64.NO_WRAP);
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,keystoreKey(),new GCMParameterSpec(128,iv));
  return new String(c.doFinal(ct),"UTF-8");
 }
 static String getKey(Context c)throws Exception{String w=c.getSharedPreferences(PREFS,0).getString(PREF,null);return w==null?null:unwrap(w);}
 public static String clean(Context ctx,String text){
  try{
   String key=getKey(ctx);if(key==null)return "{\"error\":\"nokey\"}";
   HttpURLConnection con=(HttpURLConnection)new URL(ENDPOINT).openConnection();
   con.setRequestMethod("POST");con.setDoOutput(true);con.setConnectTimeout(15000);con.setReadTimeout(25000);
   con.setRequestProperty("Content-Type","application/json");con.setRequestProperty("Authorization","Bearer "+key);
   byte[]body=("{\"text\":"+org.json.JSONObject.quote(text)+"}").getBytes("UTF-8");
   OutputStream os=con.getOutputStream();os.write(body);os.close();
   int code=con.getResponseCode();
   InputStream is=code==200?con.getInputStream():null;
   if(is==null){con.disconnect();return code==429?"{\"error\":\"limit\"}":"{\"error\":\"http\"}";}
   BufferedReader r=new BufferedReader(new InputStreamReader(is,"UTF-8"));StringBuilder sb=new StringBuilder();String line;while((line=r.readLine())!=null)sb.append(line);r.close();con.disconnect();
   return sb.toString();
  }catch(Exception e){return "{\"error\":\"net\"}";}
 }
 public static String pair(Context ctx,String code){
  try{
   byte[] rnd=new byte[24];new java.security.SecureRandom().nextBytes(rnd);
   StringBuilder hex=new StringBuilder();for(byte b:rnd)hex.append(String.format("%02x",b));
   String devKey=hex.toString();
   HttpURLConnection con=(HttpURLConnection)new URL(PAIR_URL).openConnection();
   con.setRequestMethod("POST");con.setDoOutput(true);con.setConnectTimeout(15000);con.setReadTimeout(20000);
   con.setRequestProperty("Content-Type","application/json");
   String body="{\"code\":"+org.json.JSONObject.quote(code)+",\"key\":"+org.json.JSONObject.quote(devKey)+"}";
   OutputStream os=con.getOutputStream();os.write(body.getBytes("UTF-8"));os.close();
   int rc=con.getResponseCode();con.disconnect();
   if(rc==200){setKey(ctx,devKey);return "ok";}
   return rc==429?"limit":"invalid";
  }catch(Exception e){return "net";}
 }
}
