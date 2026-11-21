from pathlib import Path
import re

p = Path("buildsrc/RVTEC_Android/app/src/main/assets/index.html")
s = p.read_text(encoding="utf-8")

system = """const system = 'Você é a RVTEC IA, especialista em manutenção elétrica, eletromecânica e automação de INDÚSTRIA PLÁSTICA, com FOCO PRINCIPAL em EXTRUSÃO DE FILME PLÁSTICO. Use os casos em technicalMemory como histórico da fábrica. Ao citar caso anterior, informe máquina, data, causa e solução; não generalize um caso isolado. Separe sempre: fatos confirmados, hipóteses, testes para confirmar e recomendações. Se recurrence=yes, outcome=partial ou outcome=not_resolved, avise que houve reincidência ou que a solução não foi definitiva. Nunca registre hipótese como aprendizado: somente técnicos validam casos na Memória Técnica. PRIORIDADE TÉCNICA: (1) extrusoras de filme: roscas, cilindro, zonas de aquecimento, resistências, termopares/PT100, controle de temperatura, motores, redutores, pressão e processo; (2) puxadores: velocidade, tração, rolos, acionamentos e sincronismo; (3) bobinadores: controle de tensão, diâmetro, torque, dançarino, célula de carga, servo/inversor e acompanhamento do puxador; (4) automação da linha: inversores WEG CFW300/500/700/11, CLP Siemens/WEG, IHM, sensores PNP/NPN, encoders/PPR, comunicação industrial e segurança; (5) áreas complementares: impressão flexográfica, corte e solda, tratamento Corona, injeção, sopro, refrigeração, compressores e utilidades. Responda em português brasileiro, de forma prática e técnica. Antes de concluir, identifique máquina/etapa, sintoma, quando ocorre, alarmes, alterações recentes e condições de processo; se faltarem dados essenciais, faça perguntas objetivas. Estruture o diagnóstico em: entendimento do problema; causas prováveis classificadas por prioridade; verificações em sequência; pontos de medição e valores esperados somente quando houver dados confiáveis do equipamento; correção sugerida; teste de validação e critérios de retorno à produção. Nunca invente parâmetros, códigos ou valores. Priorize NR-10, NR-12, bloqueio e etiquetagem, descarga de energia armazenada e ausência de tensão; não oriente bypass de proteções nem trabalho energizado inseguro.';"""

s, n = re.subn(r"const system = .*?;\n  const prompt = system \+", system + "\n  const prompt = system +", s, count=1, flags=re.S)
if n != 1:
    raise SystemExit(f"Não foi possível substituir prompt principal: {n}")

# O formato do prompt de imagem varia entre revisões; preservamos o fluxo de fotos
# existente e aplicamos a especialização pelo prompt principal do Gemini.
print("Prompt principal especializado; fluxo de fotos preservado.")

old = "function updateMemoryOutcome(id){const m=db.technicalMemory.find(x=>x.id===id);if(!m)return;const outcome=$('memoryOutcome_'+id).value,recurrence=$('memoryRecurrence_'+id).value;m.outcome=outcome;m.recurrence=recurrence;if(recurrence==='yes'&&m.recurrence!=='yes')m.occurrences=Math.max(2,Number(m.occurrences||1)+1);m.lastReviewedAt=new Date().toISOString();m.lastReviewedBy=cloudUser?.email||'Técnico';save();}"
new = "function updateMemoryOutcome(id){const m=db.technicalMemory.find(x=>x.id===id);if(!m)return;const outcome=$('memoryOutcome_'+id).value,recurrence=$('memoryRecurrence_'+id).value,previousRecurrence=m.recurrence||'unknown';m.outcome=outcome;m.recurrence=recurrence;if(recurrence==='yes'&&previousRecurrence!=='yes')m.occurrences=Math.max(2,Number(m.occurrences||1)+1);m.lastReviewedAt=new Date().toISOString();m.lastReviewedBy=cloudUser?.email||'Técnico';save();}"
if old in s:
    s = s.replace(old, new, 1)
elif new not in s:
    raise SystemExit("Função updateMemoryOutcome não encontrada no formato esperado")

required = ["FOCO PRINCIPAL em EXTRUSÃO DE FILME PLÁSTICO", "sincronismo", "function updateMemoryOutcome", "previousRecurrence"]
for marker in required:
    if marker not in s:
        raise SystemExit(f"Validação falhou: {marker}")
p.write_text(s, encoding="utf-8")
print("Especialização industrial aplicada e validada.")
# Diagnóstico seguro de App Check: imprime apenas nomes de classes e configurações, nunca tokens/chaves.
from pathlib import Path
java_root = Path("buildsrc/RVTEC_Android/app/src/main")
for f in list(java_root.rglob("*.java")) + list(java_root.rglob("*.kt")):
    lines = f.read_text(encoding="utf-8", errors="ignore").splitlines()
    for i, line in enumerate(lines):
        if any(term in line.lower() for term in ("appcheck", "playintegrity", "debugappcheck", "appcheckprovider")):
            print(f"APPCHECK_SOURCE {f}:{i+1}: " + " ".join(x.strip() for x in lines[max(0,i-1):min(len(lines),i+2)])[:400])
for f in [Path("buildsrc/RVTEC_Android/app/src/main/AndroidManifest.xml"), Path("buildsrc/RVTEC_Android/app/build.gradle")]:
    if f.exists():
        for i, line in enumerate(f.read_text(encoding="utf-8", errors="ignore").splitlines()):
            if any(term in line.lower() for term in ("appcheck", "applicationid", "signingconfig")):
                print(f"APPCHECK_CONFIG {f}:{i+1}: {line.strip()[:250]}")
