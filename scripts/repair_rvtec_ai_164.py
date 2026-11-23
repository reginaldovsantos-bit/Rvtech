from pathlib import Path
import re

root = Path("RVTEC_Android")
main = root / "app/src/main"
html = main / "assets/index.html"
activity = main / "java/com/rvtec/manutencao/MainActivity.java"

if not html.is_file() or not activity.is_file():
    raise SystemExit("Fonte Android incompleta: HTML ou MainActivity.java ausente.")

# Corrige o identificador antigo em todos os arquivos-fonte UTF-8 do módulo,
# não apenas Java/Kotlin. Arquivos binários são ignorados.
changed = []
model_hits = []
for path in main.rglob("*"):
    if not path.is_file():
        continue
    try:
        source = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        continue
    if "gemini-4-hydrogen" in source:
        source = source.replace("gemini-4-hydrogen", "gemini-2.5-flash")
        path.write_text(source, encoding="utf-8")
        changed.append(str(path))
    model_hits.extend(re.findall(r"gemini-[A-Za-z0-9._-]+", source))

if "gemini-2.5-flash" not in model_hits:
    raise SystemExit("Modelo Gemini 2.5 Flash não encontrado após correção. Modelos: " + ", ".join(sorted(set(model_hits))))
if "gemini-4-hydrogen" in model_hits:
    raise SystemExit("Identificador Gemini legado ainda presente no código-fonte.")

java = activity.read_text(encoding="utf-8")
if "void ask(String requestId, String prompt)" not in java:
    raise SystemExit("Ponte Java RVTECNative.ask não encontrada.")
if "sendResult(" not in java:
    raise SystemExit("Retorno assíncrono sendResult da ponte nativa não encontrado.")

js = html.read_text(encoding="utf-8")
required = [
    "window.rvtecGeminiNativeResult",
    "window.rvtecGeminiReady",
    "window.rvtecGeminiGenerate",
    "window.RVTECNative.ask"
]
missing = [marker for marker in required if marker not in js]
if missing:
    raise SystemExit("Ponte HTML/JavaScript incompleta: " + ", ".join(missing))

print("AUDITORIA IA: ponte HTML/JavaScript/Java presente.")
print("MODELOS GEMINI:", ", ".join(sorted(set(model_hits))))
print("ARQUIVOS CORRIGIDOS:", ", ".join(changed) if changed else "nenhum")
