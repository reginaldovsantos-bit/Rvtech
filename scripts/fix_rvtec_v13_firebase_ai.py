from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()
html = root / "app/src/main/assets/index.html"
gradle = root / "app/build.gradle"
s = html.read_text(encoding="utf-8")

# Keep the modular Firebase app used by Firebase AI Logic explicitly initialized.
old_import = """import { getApp } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-app.js';
import { getAI, getGenerativeModel, GoogleAIBackend } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-ai.js';"""
new_import = """import { initializeApp, getApps } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-app.js';
import { getAI, getGenerativeModel, GoogleAIBackend } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-ai.js';"""
old_init = """window.rvtecGeminiReady = (async () => {
  const app = getApp();"""
new_init = """window.rvtecGeminiReady = (async () => {
  const modularConfig = {
    apiKey: 'AIzaSyCNqRK8qcEP1dA4T0dFoFzbE2Lotfpq1K0',
    authDomain: 'rvtech-cfd2e.firebaseapp.com',
    projectId: 'rvtech-cfd2e',
    storageBucket: 'rvtech-cfd2e.firebasestorage.app',
    messagingSenderId: '171095099635',
    appId: '1:171095099635:web:58017bc059de606f7ecf86',
    measurementId: 'G-5KR4W30QZM'
  };
  const app = getApps().length ? getApps()[0] : initializeApp(modularConfig);"""
if old_import in s:
    s = s.replace(old_import, new_import, 1)
elif new_import not in s:
    raise SystemExit("Firebase AI import anchor not found")
if old_init in s:
    s = s.replace(old_init, new_init, 1)
elif new_init not in s:
    raise SystemExit("Firebase initialization anchor not found")

# TEST BUILD ONLY. The debug token is generated per installation; never store it in Git.
bootstrap = r"""<script>
self.FIREBASE_APPCHECK_DEBUG_TOKEN = true;
(function () {
  function showRvtecDebugToken(token) {
    window.__rvtecAppCheckDebugToken = token;
    const render = function () {
      const panel = document.getElementById('appCheckDebugPanel');
      const value = document.getElementById('appCheckDebugToken');
      if (!panel || !value) return;
      value.textContent = token;
      panel.style.display = 'block';
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', render, { once: true });
    else render();
  }
  ['log', 'info', 'warn', 'error'].forEach(function (level) {
    const original = console[level] && console[level].bind(console);
    if (!original) return;
    console[level] = function () {
      try {
        const message = Array.prototype.map.call(arguments, function (part) {
          return typeof part === 'string' ? part : (part && typeof part.message === 'string' ? part.message : String(part));
        }).join(' ');
                const match = message.match(/App\\s*Check.*debug token:\\s*[\"']?([0-9a-fA-F-]{36})/i);
        if (match) showRvtecDebugToken(match[1]);
      } catch (_) {}
      return original.apply(console, arguments);
    };
  });
  window.rvtecCopyAppCheckDebugToken = function () {
    const token = window.__rvtecAppCheckDebugToken || document.getElementById('appCheckDebugToken')?.textContent || '';
    if (!token) return;
    const field = document.createElement('textarea');
    field.value = token;
    field.style.position = 'fixed'; field.style.opacity = '0';
    document.body.appendChild(field); field.select();
    try { document.execCommand('copy'); alert('Token App Check copiado.'); }
    catch (_) { alert('Selecione e copie o token exibido.'); }
    field.remove();
  };
})();
</script>"""
if "self.FIREBASE_APPCHECK_DEBUG_TOKEN = true;" not in s:
    s = s.replace("</head>", bootstrap + "\n</head>", 1)

panel = """<div id="appCheckDebugPanel" class="panel" style="display:none;border:1px solid #d7a83a;background:#fffaf0">
<h3>Token App Check — teste</h3>
<p class="muted">Copie este token e cadastre no Firebase em Segurança → App Check → Apps → Gerenciar tokens de depuração. Mantenha-o privado.</p>
<div id="appCheckDebugToken" style="font-size:12px;overflow-wrap:anywhere;font-family:monospace;background:#f5f5f5;padding:10px;border-radius:8px"></div>
<div class="actions"><button class="secondary" type="button" onclick="rvtecCopyAppCheckDebugToken()">Copiar token</button></div>
</div>"""
status_anchor = '<p id="aiStatus" class="muted" role="status">Aguardando configuração segura do serviço de IA.</p>'
if 'id="appCheckDebugPanel"' not in s:
    if status_anchor not in s:
        raise SystemExit("AI status anchor not found")
    s = s.replace(status_anchor, status_anchor + "\n" + panel, 1)

ai_import = "import { getAI, getGenerativeModel, GoogleAIBackend } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-ai.js';"
appcheck_import = "import { initializeAppCheck, ReCaptchaV3Provider, getToken } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-app-check.js';"
if "firebase-app-check.js" not in s:
    if ai_import not in s:
        raise SystemExit("Firebase AI Logic import not found")
    s = s.replace(ai_import, ai_import + "\n" + appcheck_import, 1)

old_app_line = """  const app = getApps().length ? getApps()[0] : initializeApp(modularConfig);
  const ai = getAI(app, { backend: new GoogleAIBackend() });"""
new_app_lines = """  const app = getApps().length ? getApps()[0] : initializeApp(modularConfig);
  const appCheck = initializeAppCheck(app, {
    provider: new ReCaptchaV3Provider('rvtec-debug-test-provider'),
    isTokenAutoRefreshEnabled: true
  });
  window.rvtecAppCheck = appCheck;
  await getToken(appCheck, false);
  const ai = getAI(app, { backend: new GoogleAIBackend() });"""
if old_app_line in s:
    s = s.replace(old_app_line, new_app_lines, 1)
elif new_app_lines not in s:
    raise SystemExit("Firebase AI initialization anchor not found")

html.write_text(s, encoding="utf-8")

g = gradle.read_text(encoding="utf-8")
g = g.replace("versionCode 3", "versionCode 5").replace("versionCode 4", "versionCode 5")
g = g.replace("versionName '1.2'", "versionName '1.3.2'").replace("versionName '1.3'", "versionName '1.3.2'").replace("versionName '1.3.1'", "versionName '1.3.2'")
gradle.write_text(g, encoding="utf-8")
print("RVTEC Firebase initialization + App Check debug provider patch applied (test build only).")
