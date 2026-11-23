from pathlib import Path
import re

root = Path("RVTEC_Android")
main = root / "app/src/main"
html = root / "app/src/main/assets/index.html"
activity = root / "app/src/main/java/com/rvtec/manutencao/MainActivity.java"

if not html.is_file():
    raise SystemExit("HTML principal do RVTEC não encontrado.")
if not activity.is_file():
    raise SystemExit("MainActivity.java não encontrado.")

# A 1.6.x usa a ponte nativa RVTECNative.ask -> Firebase AI Logic.
# Corrige apenas o identificador legado conhecido; não altera prompt nem telas.
changed = []
model_hits = []
for path in list(main.rglob("*.java")) + list(main.rglob("*.kt")):
    source = path.read_text(encoding="utf-8", errors="ignore")
    if "gemini-4-hydrogen" in source:
        source = source.replace("gemini-4-hydrogen", "gemini-2.5-flash")
        path.write_text(source, encoding="utf-8")
        changed.append(str(path))
    model_hits.extend(re.findall(r"gemini-[A-Za-z0-9._-]+", source))

if not model_hits:
    raise SystemExit("Nenhum identificador de modelo Gemini encontrado no código Android; patch interrompido.")
if "gemini-2.5-flash" not in model_hits:
    raise SystemExit("O modelo nativo não é gemini-2.5-flash; revisão manual necessária. Encontrado: " + ", ".join(sorted(set(model_hits))))

java = activity.read_text(encoding="utf-8", errors="ignore")
if "void ask(String requestId, String prompt)" not in java:
    raise SystemExit("Ponte Java RVTECNative.ask não encontrada.")
if "sendResult(" not in java:
    raise SystemExit("Retorno assíncrono sendResult da ponte nativa não encontrado.")

js = html.read_text(encoding="utf-8", errors="ignore")
required = [
    "window.rvtecGeminiNativeResult",
    "window.rvtecGeminiReady",
    "window.rvtecGeminiGenerate",
    "window.RVTECNative.ask"
]
missing = [marker for marker in required if marker not in js]
if missing:
    raise SystemExit("Ponte HTML/JavaScript incompleta: " + ", ".join(missing))

if "gemini-4-hydrogen" in java:
    raise SystemExit("O identificador antigo permaneceu em MainActivity.")

print("AUDITORIA IA: ponte HTML/JavaScript/Java presente.")
print("MODELOS GEMINI:", ", ".join(sorted(set(model_hits))))
print("ARQUIVOS AJUSTADOS:", ", ".join(changed) if changed else "nenhum; o modelo 2.5 Flash já estava aplicado.")
