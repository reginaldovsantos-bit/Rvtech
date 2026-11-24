#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1])
java = root / "app/src/main/java/com/rvtec/manutencao/MainActivity.java"
src = java.read_text(encoding="utf-8")

if "private FirebaseAppCheck appCheck;" not in src:
    src = src.replace(
        "private GenerativeModelFutures gemini;",
        "private GenerativeModelFutures gemini;\n    private FirebaseAppCheck appCheck;"
    )

src = src.replace(
    "FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance(app);",
    "appCheck = FirebaseAppCheck.getInstance(app);"
)

if "private void sendAppCheckDiagnostic" not in src:
    anchor = "    private void sendResult(String requestId, String answer, String error) {"
    diagnostic = r'''
    private void sendAppCheckDiagnostic(String requestId, boolean tokenOk, boolean geminiOk, String message) {
        if (webView == null) return;
        String js = "window.rvtecAppCheckDiagnosticResult(" + JSONObject.quote(requestId) + "," +
                tokenOk + "," + geminiOk + "," + JSONObject.quote(message == null ? "" : message) + ");";
        runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(js, null); });
    }

'''
    if anchor not in src:
        raise SystemExit("Não encontrou ponto para inserir retorno do diagnóstico")
    src = src.replace(anchor, diagnostic + anchor, 1)

bridge_anchor = '        @JavascriptInterface\n        public void ask(String requestId, String prompt) {'
if "public void diagnoseAppCheck(String requestId)" not in src:
    method = r'''        @JavascriptInterface
        public void diagnoseAppCheck(String requestId) {
            if (appCheck == null) {
                sendAppCheckDiagnostic(requestId, false, false,
                        "Firebase não inicializou. Verifique google-services.json e o projeto.");
                return;
            }
            appCheck.getAppCheckToken(false)
                    .addOnSuccessListener(token -> {
                        android.util.Log.i("RVTEC_APP_CHECK", "Token App Check obtido pelo SDK; conteúdo omitido por segurança.");
                        if (gemini == null) {
                            sendAppCheckDiagnostic(requestId, true, false,
                                    "Token obtido, mas Firebase AI Logic não inicializou.");
                            return;
                        }
                        Content probe = new Content.Builder()
                                .addText("Responda somente: RVTEC_DIAGNOSTICO_OK")
                                .build();
                        ListenableFuture<GenerateContentResponse> future = gemini.generateContent(probe);
                        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
                            @Override public void onSuccess(GenerateContentResponse response) {
                                String answer = response.getText();
                                boolean ok = answer != null && answer.contains("RVTEC_DIAGNOSTICO_OK");
                                sendAppCheckDiagnostic(requestId, true, ok,
                                        ok ? "App Check aceitou o fluxo e Gemini respondeu."
                                           : "Token obtido e chamada concluída, mas resposta de diagnóstico inesperada.");
                            }
                            @Override public void onFailure(Throwable error) {
                                Throwable cause = error.getCause() != null ? error.getCause() : error;
                                String message = cause.getMessage();
                                android.util.Log.e("RVTEC_APP_CHECK", "Falha na chamada de diagnóstico Gemini: " +
                                        cause.getClass().getSimpleName() + ": " + message);
                                sendAppCheckDiagnostic(requestId, true, false,
                                        "Token obtido; Gemini recusou/falhou: " +
                                        (message == null ? cause.getClass().getSimpleName() : message));
                            }
                        }, MoreExecutors.directExecutor());
                    })
                    .addOnFailureListener(error -> {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        String message = cause.getMessage();
                        android.util.Log.e("RVTEC_APP_CHECK", "Falha ao obter token App Check: " +
                                cause.getClass().getSimpleName() + ": " + message);
                        sendAppCheckDiagnostic(requestId, false, false,
                                "Não foi possível obter token App Check: " +
                                (message == null ? cause.getClass().getSimpleName() : message));
                    });
        }

'''
    if bridge_anchor not in src:
        raise SystemExit("Não encontrou ponto para inserir método na ponte JavaScript")
    src = src.replace(bridge_anchor, method + bridge_anchor, 1)

# Injeção de botão de diagnóstico apenas no APK debug; nenhuma tela de produção é alterada.
needle = '            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {'
if "rvtec-appcheck-diagnostic" not in src:
    onfinished = r'''            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (BuildConfig.DEBUG) {
                    String diagnosticUi = "(function(){if(document.getElementById('rvtec-appcheck-diagnostic'))return;" +
                        "var b=document.createElement('button');b.id='rvtec-appcheck-diagnostic';" +
                        "b.textContent='Diagnóstico Firebase';" +
                        "b.style.cssText='position:fixed;z-index:2147483647;right:12px;bottom:14px;padding:11px 14px;border:0;border-radius:22px;background:#173b5e;color:#fff;font-weight:700;box-shadow:0 3px 12px #0006;font-size:12px;';" +
                        "b.onclick=function(){b.disabled=true;b.textContent='Testando Firebase...';" +
                        "var id='rvtec_diag_'+Date.now();window.rvtecAppCheckDiagnosticResult=function(r,t,g,m){" +
                        "if(r!==id)return;b.disabled=false;b.textContent='Diagnóstico: '+(t&&g?'APROVADO':t?'TOKEN OK / IA FALHOU':'TOKEN FALHOU');" +
                        "alert('RVTEC - Diagnóstico Firebase\\\\n\\\\nApp Check: '+(t?'TOKEN OBTIDO':'FALHA')+" +
                        "'\\\\nFirebase AI Logic/Gemini: '+(g?'RESPOSTA OK':'NÃO APROVADO')+'\\\\n\\\\n'+m);};" +
                        "try{window.RVTECNative.diagnoseAppCheck(id);}catch(e){b.disabled=false;b.textContent='Falha no diagnóstico';alert(String(e));}};" +
                        "document.body.appendChild(b);})();";
                    view.evaluateJavascript(diagnosticUi, null);
                }
            }
'''
    if needle not in src:
        raise SystemExit("Não encontrou ponto para adicionar botão de diagnóstico")
    src = src.replace(needle, onfinished + needle, 1)

java.write_text(src, encoding="utf-8")
print("Diagnóstico App Check/Gemini adicionado: obtenção do token + chamada real Firebase AI Logic; sem expor JWT/debug secret.")
