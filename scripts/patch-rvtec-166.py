from pathlib import Path
import re

html_path = Path("RVTEC_Android/app/src/main/assets/index.html")
gradle_path = Path("RVTEC_Android/app/build.gradle")
if not html_path.is_file() or not gradle_path.is_file():
    raise SystemExit("Fonte Android não encontrada; patch cancelado.")

html = html_path.read_text(encoding="utf-8")
gradle = gradle_path.read_text(encoding="utf-8")

# Primeiro tenta o símbolo hambúrguer dentro de um botão; em seguida, o controle
# identificado explicitamente como menu/hambúrguer. Não altera telas ou rotas.
patterns = [
    re.compile(r"""<button\b[^>]*>[\s\S]*?(?:☰|&#9776;|&#x2630;)[\s\S]*?</button>""", re.I),
    re.compile(r"""<button\b(?=[^>]*(?:id|class|aria-label|title)="[^"]*(?:menu|hamb|nav)[^"]*")[^>]*>[\s\S]*?</button>""", re.I),
]
found = []
for pattern in patterns:
    found.extend((m.start(), m.end()) for m in pattern.finditer(html))
spans = sorted(set(found))
if len(spans) != 1:
    raise SystemExit(f"Esperava exatamente 1 botão de menu sem função; encontrados: {len(spans)}. Nenhuma alteração aplicada.")

start, end = spans[0]
removed = html[start:end]
# Proteção: só aceitar o controle visual, nunca um container de navegação ou tela.
if not re.search(r"(?:☰|&#9776;|&#x2630;|menu|hamb|nav)", removed, re.I):
    raise SystemExit("Elemento encontrado não foi reconhecido como botão de menu.")

html = html[:start] + html[end:]
if re.search(r"""<button\b[^>]*>[\s\S]*?(?:☰|&#9776;|&#x2630;)[\s\S]*?</button>""", html, re.I):
    raise SystemExit("O botão hambúrguer ainda aparece após o patch.")

if "versionCode 165" not in gradle or "versionName '1.6.5'" not in gradle:
    raise SystemExit("Versão-base inesperada; esperado RVTEC 1.6.5. Gradle não foi alterado.")
gradle = gradle.replace("versionCode 165", "versionCode 166", 1)
gradle = gradle.replace("versionName '1.6.5'", "versionName '1.6.6'", 1)

html_path.write_text(html, encoding="utf-8")
gradle_path.write_text(gradle, encoding="utf-8")
print("RVTEC 1.6.6: botão de menu removido; restante da aplicação preservado.")
