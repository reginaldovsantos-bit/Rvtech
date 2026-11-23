from pathlib import Path
import re

ROOT = Path("RVTEC_Android")
JAVA = ROOT / "app/src/main/java/com/rvtec/manutencao/MainActivity.java"
HTML = ROOT / "app/src/main/assets/index.html"
GRADLE = ROOT / "app/build.gradle"

def once(text, pattern, replacement, label, flags=0):
    if callable(replacement):
        repl = replacement
    else:
        def repl(match):
            return re.sub(r"\\([1-9])", lambda backref: match.group(int(backref.group(1))), replacement)
    out, n = re.subn(pattern, repl, text, count=1, flags=flags)
    if n != 1:
        raise SystemExit(f"Patch biometria abortado: {label}; ocorrências={n}")
    return out

j = JAVA.read_text(encoding="utf-8")
h = HTML.read_text(encoding="utf-8")
g = GRADLE.read_text(encoding="utf-8")

# BiometricPrompt requires FragmentActivity, not android.app.Activity.
if "import androidx.fragment.app.FragmentActivity;" not in j:
    j = j.replace("package com.rvtec.manutencao;\\n", "package com.rvtec.manutencao;\\n\\nimport androidx.fragment.app.FragmentActivity;\\n", 1)
j = j.replace("public class MainActivity extends Activity {", "public class MainActivity extends FragmentActivity {", 1)

# Confirma que a fonte-base é a aplicação Android nativa com WebView e bridge Gemini.
for token in ["class MainActivity", "class GeminiBridge", 'addJavascriptInterface(new GeminiBridge(), "RVTECNative")']:
    if token not in j:
        raise SystemExit(f"Fonte Android incompatível: {token}. Início da fonte:\\n" + "\\n".join(j.splitlines()[:80]))
for token in ['id="loginButton"', "async function rvtecLogin", "rvtecAuth.signInWithEmailAndPassword"]:
    if token not in h:
        raise SystemExit(f"Tela de login incompatível: {token}")

if "androidx.biometric.BiometricPrompt" not in j:
    j = j.replace("package com.rvtec.manutencao;\n", """package com.rvtec.manutencao;

import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
""", 1)

if "private static final String BIO_PREFS" not in j:
    j = once(j, r"(private Bitmap selectedPartPhoto;)", r"""\1
    private static final String BIO_PREFS = "rvtec_biometric";
    private static final String BIO_ALIAS = "RVTEC_BIOMETRIC_KEY";
    private final Executor biometricExecutor = Executors.newSingleThreadExecutor();""", "campos de biometria")

if "private void authenticateBiometric()" not in j:
    methods = r'''
    private boolean biometricAvailable() {
        int result = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        return result == BiometricManager.BIOMETRIC_SUCCESS && hasBiometricCredentials();
    }
    private boolean hasBiometricCredentials() {
        return getSharedPreferences(BIO_PREFS, MODE_PRIVATE).contains("ciphertext");
    }
    private SecretKey getOrCreateBiometricKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(BIO_ALIAS)) return (SecretKey) keyStore.getKey(BIO_ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(BIO_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
    private void saveBiometricCredentials(String email, String password) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateBiometricKey());
            byte[] encrypted = cipher.doFinal((email + "\n" + password).getBytes(StandardCharsets.UTF_8));
            getSharedPreferences(BIO_PREFS, MODE_PRIVATE).edit()
                    .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply();
            notifyBiometricAvailability();
            notifyBiometricResult(false, null, null, "Acesso por digital habilitado.");
        } catch (Exception e) {
            notifyBiometricResult(false, null, null, "Não foi possível habilitar a digital. Continue usando e-mail e senha.");
        }
    }
    private void notifyBiometricAvailability() {
        if (webView == null) return;
        boolean available = biometricAvailable();
        runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript("window.rvtecBiometricAvailability(" + available + ");", null); });
    }
    private void notifyBiometricResult(boolean ok, String email, String password, String message) {
        if (webView == null) return;
        String js = "window.rvtecBiometricResult(" + ok + "," + JSONObject.quote(email == null ? "" : email) + "," +
                JSONObject.quote(password == null ? "" : password) + "," + JSONObject.quote(message == null ? "" : message) + ");";
        runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(js, null); });
    }
    private void authenticateBiometric() {
        if (!hasBiometricCredentials()) {
            notifyBiometricResult(false, null, null, "Entre primeiro com e-mail e senha para habilitar a digital.");
            return;
        }
        try {
            SharedPreferences prefs = getSharedPreferences(BIO_PREFS, MODE_PRIVATE);
            byte[] iv = Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateBiometricKey(), new GCMParameterSpec(128, iv));
            BiometricPrompt prompt = new BiometricPrompt(this, biometricExecutor, new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    try {
                        byte[] encrypted = Base64.decode(prefs.getString("ciphertext", ""), Base64.NO_WRAP);
                        String[] credentials = new String(result.getCryptoObject().getCipher().doFinal(encrypted), StandardCharsets.UTF_8).split("\n", 2);
                        if (credentials.length != 2) throw new Exception("Credencial inválida");
                        notifyBiometricResult(true, credentials[0], credentials[1], "Digital confirmada.");
                    } catch (Exception e) {
                        notifyBiometricResult(false, null, null, "Não foi possível desbloquear o acesso. Entre com sua senha atual.");
                    }
                }
                @Override public void onAuthenticationError(int errorCode, CharSequence errString) {
                    notifyBiometricResult(false, null, null, errString == null ? "Biometria cancelada." : errString.toString());
                }
                @Override public void onAuthenticationFailed() {
                    notifyBiometricResult(false, null, null, "Digital não reconhecida. Tente novamente.");
                }
            });
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Entrar no RVTEC")
                    .setSubtitle("Confirme sua identidade pela digital")
                    .setNegativeButtonText("Usar e-mail e senha")
                    .build();
            prompt.authenticate(info, new BiometricPrompt.CryptoObject(cipher));
        } catch (Exception e) {
            notifyBiometricResult(false, null, null, "Biometria indisponível. Use e-mail e senha.");
        }
    }
'''
    j = once(j, r"(?=\s*@Override\s+protected void onActivityResult)", methods + "\n", "métodos nativos")

if "public void saveBiometricCredentials(String email, String password)" not in j:
    bridge = r'''
        @JavascriptInterface public void saveBiometricCredentials(String email, String password) {
            if (email != null && password != null && !email.trim().isEmpty() && !password.isEmpty())
                runOnUiThread(() -> MainActivity.this.saveBiometricCredentials(email.trim(), password));
        }
        @JavascriptInterface public void authenticateBiometric() {
            runOnUiThread(() -> MainActivity.this.authenticateBiometric());
        }
        @JavascriptInterface public void checkBiometricAvailability() {
            runOnUiThread(() -> notifyBiometricAvailability());
        }
        @JavascriptInterface public void disableBiometric() {
            runOnUiThread(() -> {
                getSharedPreferences(BIO_PREFS, MODE_PRIVATE).edit().clear().apply();
                try { KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null); ks.deleteEntry(BIO_ALIAS); } catch (Exception ignored) {}
                notifyBiometricAvailability();
            });
        }
'''
    j = once(j, r"(private class GeminiBridge\s*\{)", "\\1\n" + bridge, "ponte JavaScript/Android")

if "androidx.biometric:biometric" not in g:
    g = once(g, r"(dependencies\s*\{)", lambda m: m.group(1) + "\n    implementation 'androidx.biometric:biometric:1.1.0'\n    implementation 'androidx.fragment:fragment:1.8.5'", "dependências AndroidX")
# Versão de entrega nova, sem sobrescrever os artefatos anteriores.
if "versionName '1.6.4'" in g:
    g = g.replace("versionCode 164", "versionCode 165", 1).replace("versionName '1.6.4'", "versionName '1.6.5'", 1)
elif "versionName '1.6.2'" in g:
    g = g.replace("versionCode 162", "versionCode 165", 1).replace("versionName '1.6.2'", "versionName '1.6.5'", 1)
else:
    raise SystemExit("Versão Gradle base não reconhecida; sem alteração.")

# Botão de digital no login (mantém o botão de senha original).
if 'id="biometricButton"' not in h:
    h = once(h, r'(<button[^>]*id="loginButton"[^>]*>.*?</button>)',
             r'\1<button type="button" class="secondary" id="biometricButton" style="display:none" onclick="rvtecBiometricLogin()">Entrar com digital</button>',
             "botão de login", re.S)

# Salva a credencial apenas no armazenamento criptografado do Android após login Firebase válido.
if "RVTECNative.saveBiometricCredentials(email,password)" not in h:
    h = once(h, r"(await\s+rvtecAuth\.signInWithEmailAndPassword\(email,password\);)",
             r"\1if(window.RVTECNative&&RVTECNative.saveBiometricCredentials)RVTECNative.saveBiometricCredentials(email,password);",
             "vincular login por senha")

if "function rvtecBiometricLogin()" not in h:
    js = r'''
function rvtecBiometricLogin(){
 if(!window.RVTECNative){showLoginMessage('A digital está disponível somente no aplicativo Android.');return;}
 showLoginMessage('Aguardando confirmação da digital...');
 RVTECNative.authenticateBiometric();
}
function rvtecBiometricAvailability(available){
 const b=document.getElementById('biometricButton');
 if(b)b.style.display=available?'inline-flex':'none';
}
async function rvtecBiometricResult(ok,email,password,message){
 if(!ok){showLoginMessage(message||'Não foi possível entrar pela digital.');return;}
 $('loginEmail').value=email;$('loginPassword').value=password;
 showLoginMessage('Digital confirmada. Entrando...');
 try{await rvtecAuth.signInWithEmailAndPassword(email,password);}
 catch(e){showLoginMessage(firebaseError(e)+' Entre com sua senha atual.');$('loginPassword').value='';}
}
'''
    h = once(h, r"(?=async function rvtecResetPassword\s*\()", js + "\n", "funções de login biométrico")

# Consulta disponibilidade ao abrir a tela; HTML continua funcionando fora do Android.
if "RVTECNative.checkBiometricAvailability" not in h:
    h = once(h, r"(async function rvtecResetPassword\s*\()",
             "if(window.RVTECNative&&RVTECNative.checkBiometricAvailability)RVTECNative.checkBiometricAvailability();\n\1",
             "consulta de disponibilidade")

JAVA.write_text(j, encoding="utf-8")
HTML.write_text(h, encoding="utf-8")
GRADLE.write_text(g, encoding="utf-8")
print("RVTEC 1.6.5: biometria Android integrada; login Firebase por e-mail/senha preservado.")
