from pathlib import Path
import sys

app = Path(sys.argv[1])
p = app / "app/src/main/java/com/rvtec/manutencao/MainActivity.java"
s = p.read_text()

anchor = "        @JavascriptInterface\n        public void ask(String requestId, String prompt) {"
method = """        @JavascriptInterface
        public void getAppCheckToken(String requestId) {
            FirebaseAppCheck.getInstance().getAppCheckToken(false)
                .addOnSuccessListener(token -> sendAppCheckTokenResult(
                    requestId, token.getToken(), token.getExpireTimeMillis(), null))
                .addOnFailureListener(error -> sendAppCheckTokenResult(
                    requestId, null, 0L,
                    error.getMessage() == null ? "Falha ao obter token App Check." : error.getMessage()));
        }

"""
if "public void getAppCheckToken(String requestId)" not in s:
    if anchor not in s:
        raise SystemExit("Ponto da ponte App Check não encontrado.")
    s = s.replace(anchor, method + anchor)

result_anchor = "    private void sendResult(String requestId, String answer, String error) {"
result_method = """    private void sendAppCheckTokenResult(String requestId, String token, long expiresAt, String error) {
        if (webView == null) return;
        String js = "window.rvtecAppCheckNativeResult(" + JSONObject.quote(requestId) + "," +
                (token == null ? "null" : JSONObject.quote(token)) + "," + expiresAt + "," +
                (error == null ? "null" : JSONObject.quote(error)) + ");";
        runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(js, null); });
    }

"""
if "private void sendAppCheckTokenResult(" not in s:
    if result_anchor not in s:
        raise SystemExit("Ponto de retorno do App Check não encontrado.")
    s = s.replace(result_anchor, result_method + result_anchor)

p.write_text(s)
print("Ponte nativa App Check aplicada.")
