from pathlib import Path
import re

root = Path("buildsrc/RVTEC_Android")
java = root / "app/src/main/java/com/rvtec/manutencao/MainActivity.java"
html = root / "app/src/main/assets/index.html"
gradle = root / "app/build.gradle"

j = java.read_text(encoding="utf-8")
h = html.read_text(encoding="utf-8")
g = gradle.read_text(encoding="utf-8")

assert "class MainActivity extends Activity" in j
assert 'addJavascriptInterface(new GeminiBridge(), "RVTECNative")' in j
assert 'id="loginButton"' in h and "rvtecAuth.signInWithEmailAndPassword(email,password)" in h
assert "versionName '1.4.6'" in g

j = j.replace("import android.app.Activity;", """import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.content.SharedPreferences;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;""", 1)
j = j.replace("public class MainActivity extends Activity {", "public class MainActivity extends FragmentActivity {", 1)
j = j.replace("    private Bitmap selectedPartPhoto;", """    private Bitmap selectedPartPhoto;
    private static final String BIO_PREFS = "rvtec_biometric";
    private static final String BIO_ALIAS = "RVTEC_BIOMETRIC_KEY";
    private Executor biometricExecutor;""", 1)
j = j.replace("        super.onCreate(savedInstanceState);", "        super.onCreate(savedInstanceState);\n        biometricExecutor = ContextCompat.getMainExecutor(this);", 1)

methods = r'''
    private boolean biometricHardwareReady() {
        return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }
    private boolean hasBiometricCredentials() { return getSharedPreferences(BIO_PREFS, MODE_PRIVATE).contains("ciphertext"); }
    private SecretKey biometricKey() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if(store.containsAlias(BIO_ALIAS)) return (SecretKey)store.getKey(BIO_ALIAS,null);
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        KeyGenParameterSpec.Builder b=new KeyGenParameterSpec.Builder(BIO_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true);
        if(android.os.Build.VERSION.SDK_INT>=30)b.setUserAuthenticationParameters(0,KeyProperties.AUTH_BIOMETRIC_STRONG);
        else b.setUserAuthenticationValidityDurationSeconds(-1);
        gen.init(b.build()); return gen.generateKey();
    }
    private void biometricAvailabilityToWeb(){boolean ok=biometricHardwareReady()&&hasBiometricCredentials(); if(webView!=null)runOnUiThread(()->webView.evaluateJavascript("window.rvtecBiometricAvailability("+ok+");",null));}
    private void biometricEnrollmentResult(String msg){if(webView!=null)runOnUiThread(()->webView.evaluateJavascript("window.rvtecBiometricEnrollmentResult("+JSONObject.quote(msg)+");",null));}
    private void biometricLoginResult(String email,String pass,String msg){if(webView==null)return;String js="window.rvtecBiometricResult("+JSONObject.quote(email==null?"":email)+","+JSONObject.quote(pass==null?"":pass)+","+JSONObject.quote(msg==null?"":msg)+");";runOnUiThread(()->{if(webView!=null)webView.evaluateJavascript(js,null);});}
    private BiometricPrompt makePrompt(BiometricPrompt.AuthenticationCallback cb){return new BiometricPrompt(this,biometricExecutor,cb);}
    private BiometricPrompt.PromptInfo promptInfo(String subtitle){return new BiometricPrompt.PromptInfo.Builder().setTitle("RVTEC").setSubtitle(subtitle).setNegativeButtonText("Cancelar").build();}
    private void enrollBiometricCredentials(String email,String password){
        if(!biometricHardwareReady()){biometricEnrollmentResult("Biometria forte não configurada. Continue com e-mail e senha.");return;}
        try{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,biometricKey());
          makePrompt(new BiometricPrompt.AuthenticationCallback(){
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){try{
              Cipher ac=r.getCryptoObject().getCipher();byte[] enc=ac.doFinal((email+"\n"+password).getBytes(StandardCharsets.UTF_8));
              getSharedPreferences(BIO_PREFS,MODE_PRIVATE).edit().putString("iv",Base64.encodeToString(ac.getIV(),Base64.NO_WRAP)).putString("ciphertext",Base64.encodeToString(enc,Base64.NO_WRAP)).apply();
              biometricAvailabilityToWeb();biometricEnrollmentResult("Acesso por digital ativado. Use a digital na próxima entrada.");
            }catch(Exception e){biometricEnrollmentResult("Não foi possível ativar a digital. O acesso por senha continua disponível.");}}
            @Override public void onAuthenticationError(int code,CharSequence msg){biometricEnrollmentResult("Digital não ativada. O acesso por senha continua disponível.");}
          }).authenticate(promptInfo("Confirme sua digital para habilitar o acesso rápido"),new BiometricPrompt.CryptoObject(c));
        }catch(Exception e){biometricEnrollmentResult("Não foi possível preparar a biometria. Use e-mail e senha.");}
    }
    private void authenticateBiometricLogin(){
        if(!hasBiometricCredentials()){biometricLoginResult("","","Entre primeiro com e-mail e senha para ativar a digital.");return;}
        try{SharedPreferences prefs=getSharedPreferences(BIO_PREFS,MODE_PRIVATE);byte[] iv=Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP);
          Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,biometricKey(),new GCMParameterSpec(128,iv));
          makePrompt(new BiometricPrompt.AuthenticationCallback(){
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){try{
              byte[] enc=Base64.decode(prefs.getString("ciphertext",""),Base64.NO_WRAP);String plain=new String(r.getCryptoObject().getCipher().doFinal(enc),StandardCharsets.UTF_8);
              String[] pair=plain.split("\n",2);if(pair.length!=2)throw new Exception("Credencial inválida");biometricLoginResult(pair[0],pair[1],"Digital confirmada.");
            }catch(Exception e){biometricLoginResult("","","Não foi possível desbloquear. Entre com sua senha atual.");}}
            @Override public void onAuthenticationError(int code,CharSequence msg){biometricLoginResult("","",msg==null?"Biometria cancelada.":msg.toString());}
          }).authenticate(promptInfo("Confirme sua identidade para entrar"),new BiometricPrompt.CryptoObject(c));
        }catch(Exception e){biometricLoginResult("","","Biometria indisponível. Use e-mail e senha.");}
    }
'''
anchor = "    @Override\n    protected void onActivityResult(int requestCode, int resultCode, Intent data) {"
assert anchor in j
j = j.replace(anchor, methods + "\n" + anchor, 1)

bridge = """    private class GeminiBridge {
        @JavascriptInterface public void saveBiometricCredentials(String email,String password){if(email==null||password==null||email.trim().isEmpty()||password.isEmpty())return;runOnUiThread(()->{if(!hasBiometricCredentials())enrollBiometricCredentials(email.trim(),password);else biometricAvailabilityToWeb();});}
        @JavascriptInterface public void authenticateBiometric(){runOnUiThread(()->authenticateBiometricLogin());}
        @JavascriptInterface public void checkBiometricAvailability(){runOnUiThread(()->biometricAvailabilityToWeb());}
        @JavascriptInterface public void disableBiometric(){runOnUiThread(()->{getSharedPreferences(BIO_PREFS,MODE_PRIVATE).edit().clear().apply();try{KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);k.deleteEntry(BIO_ALIAS);}catch(Exception ignored){}biometricAvailabilityToWeb();});}
"""
assert "    private class GeminiBridge {\n" in j
j = j.replace("    private class GeminiBridge {\n", bridge, 1)
java.write_text(j, encoding="utf-8")

g = g.replace("versionCode 10", "versionCode 11", 1).replace("versionName '1.4.6'", "versionName '1.4.8'", 1)
g = g.replace("dependencies {", """dependencies {
    implementation 'androidx.biometric:biometric:1.1.0'
    implementation 'androidx.fragment:fragment:1.8.5'
    implementation 'androidx.core:core:1.15.0'""", 1)
gradle.write_text(g, encoding="utf-8")

h = h.replace('<button class="primary" id="loginButton" onclick="rvtecLogin()">Entrar</button>',
              '<button class="primary" id="loginButton" onclick="rvtecLogin()">Entrar</button><button class="secondary" id="biometricButton" style="display:none" onclick="rvtecBiometricLogin()">Entrar com digital</button>', 1)
h = h.replace("try{await rvtecAuth.signInWithEmailAndPassword(email,password);}",
              "try{await rvtecAuth.signInWithEmailAndPassword(email,password);if(window.RVTECNative&&RVTECNative.saveBiometricCredentials)RVTECNative.saveBiometricCredentials(email,password);}", 1)
js = """function rvtecBiometricLogin(){if(!window.RVTECNative){showLoginMessage('Digital disponível somente no Android.');return;}showLoginMessage('Confirme sua digital...');RVTECNative.authenticateBiometric();}
function rvtecBiometricAvailability(ok){const b=$('biometricButton');if(b)b.style.display=ok?'inline-flex':'none';}
function rvtecBiometricEnrollmentResult(msg){showLoginMessage(msg);if(window.RVTECNative&&RVTECNative.checkBiometricAvailability)RVTECNative.checkBiometricAvailability();}
async function rvtecBiometricResult(email,password,msg){if(!email||!password){showLoginMessage(msg||'Não foi possível entrar pela digital. Use sua senha.');return;}$('loginEmail').value=email;$('loginPassword').value=password;showLoginMessage('Digital confirmada. Entrando...');try{await rvtecAuth.signInWithEmailAndPassword(email,password);}catch(e){showLoginMessage(firebaseError(e)+' Entre com sua senha atual.');}}
if(window.RVTECNative&&RVTECNative.checkBiometricAvailability)RVTECNative.checkBiometricAvailability();
"""
h = h.replace("async function rvtecResetPassword(){", js + "async function rvtecResetPassword(){", 1)
html.write_text(h, encoding="utf-8")

(root / "BIOMETRIA_1.4.8_NOTAS.txt").write_text(
    "RVTEC 1.4.8 - Login biométrico Android. Mantém login Firebase por e-mail e senha. "
    "Após login válido, solicita confirmação biométrica para guardar credenciais criptografadas no Android Keystore. "
    "Na próxima abertura, oferece Entrar com digital. Se a biometria não estiver configurada ou for cancelada, "
    "o login por senha continua disponível.\n", encoding="utf-8")
