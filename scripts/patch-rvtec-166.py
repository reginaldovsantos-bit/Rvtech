from pathlib import Path
import re

html_path = Path("RVTEC_Android/app/src/main/assets/index.html")
gradle_path = Path("RVTEC_Android/app/build.gradle")
if not html_path.is_file() or not gradle_path.is_file():
    raise SystemExit("Fonte Android não encontrada; patch cancelado.")

html = html_path.read_text(encoding="utf-8")
gradle = gradle_path.read_text(encoding="utf-8")

# Remove somente o controle visual do menu hambúrguer; não remove telas ou navegação.
patterns = [
    re.compile(r'<button\\b(?=[^>]*(?:id|class|aria-label|title)=["\\'][^"\\']*(?:menu|hamb|nav)[^"\\']*["\\'])[^>]*>.*?(?:☰|&#9776;|&#x2630;|\\\\u2630|menu-icon|hamburger).*?</button>', re.I | re.S),
    re.compile(r'<button\\b[^>]*>\\s*(?:☰|&#9776;|&#x2630;|\\\\u2630)\\s*</button>', re.I | re.S),
]
matches = []
for pattern in patterns:
    matches.extend(list(pattern.finditer(html)))
# Deduplicate overlapping spans.
spans = sorted(set((m.start(), m.end()) for m in matches))
if len(spans) != 1:
    raise SystemExit(f"Esperava encontrar exatamente 1 botão hambúrguer; encontrados: {len(spans)}. Nenhuma alteração aplicada.")

start, end = spans[0]
html = html[:start] + html[end:]

# Confirma que o controle não permaneceu no HTML.
if re.search(r'<button\\b[^>]*>\\s*(?:☰|&#9776;|&#x2630;|\\\\u2630)\\s*</button>', html, re.I | re.S):
    raise SystemExit("O botão hambúrguer ainda aparece após o patch.")

if "versionCode 165" not in gradle or "versionName '1.6.5'" not in gradle:
    raise SystemExit("Versão-base inesperada; esperado RVTEC 1.6.5. Gradle não foi alterado.")
gradle = gradle.replace("versionCode 165", "versionCode 166", 1)
gradle = gradle.replace("versionName '1.6.5'", "versionName '1.6.6'", 1)

html_path.write_text(html, encoding="utf-8")
gradle_path.write_text(gradle, encoding="utf-8")
print("RVTEC 1.6.6: botão hambúrguer removido; telas, Firebase, Gemini e login preservados.")
