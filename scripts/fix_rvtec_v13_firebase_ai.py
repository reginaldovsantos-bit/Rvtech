from pathlib import Path
import sys

root = Path(sys.argv[1])
html = root / "app/src/main/assets/index.html"
gradle = root / "app/build.gradle"
s = html.read_text(encoding="utf-8")

old_import = """import { getApp } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-app.js';
import { getAI, getGenerativeModel, GoogleAIBackend } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-ai.js';"""
new_import = """import { initializeApp, getApps } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-app.js';
import { getAI, getGenerativeModel, GoogleAIBackend } from 'https://www.gstatic.com/firebasejs/12.1.0/firebase-ai.js';"""
old_init = """window.rvtecGeminiReady = (async () => {
  const app = getApp();"""
new_init = """window.rvtecGeminiReady = (async () => {
  // Initialize the modular Firebase app used by Firebase AI Logic.
  // The compat SDK used by Auth/Firestore may have a separate app registry in WebView.
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
    raise SystemExit("Firebase AI initialization anchor not found")
html.write_text(s, encoding="utf-8")

g = gradle.read_text(encoding="utf-8")
g = g.replace("versionCode 3", "versionCode 4").replace("versionName '1.2'", "versionName '1.3.1'")
gradle.write_text(g, encoding="utf-8")
print("RVTEC Firebase AI initialization patch applied")
