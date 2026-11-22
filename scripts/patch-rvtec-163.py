from pathlib import Path
p=Path("RVTEC_Android/app/src/main/assets/index.html")
s=p.read_text(encoding="utf-8")
repls = [
(""" .rvtec-os-detail{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:10px}""",
""" .rvtec-os-detail{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:10px}
 .rvtec-os-toggle{border:0!important;background:transparent!important;color:var(--muted)!important;font-size:22px!important;line-height:1;padding:10px 13px!important;min-width:44px;min-height:44px;border-radius:50%;cursor:pointer;touch-action:manipulation}
 .rvtec-os-toggle:focus-visible{outline:2px solid var(--accent);outline-offset:2px}
 .rvtec-os-collapsed .rvtec-os-detail{display:none}
 .rvtec-os-head{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}
 .rvtec-os-parts summary{cursor:pointer;color:var(--muted);padding:7px 0;min-height:24px}"""),
(""" o.diagnosis=diagnosis.trim();o.parts=parts.trim();o.workDone=work.trim();""",
""" o.diagnosis=diagnosis.trim();o.parts=parts.trim();o.workDone=work.trim();o.solution=work.trim();"""),
(""" const linkedStop=db.stops.find(s=>String(s.id)===String(o.stopId));if(linkedStop&&!linkedStop.end){linkedStop.end=o.finished;linkedStop.status='Finalizada';linkedStop.solution=linkedStop.solution||o.workDone||o.diagnosis||'Atendimento concluído pela OS vinculada';}""",
""" const linkedStop=db.stops.find(s=>String(s.id)===String(o.stopId))||db.stops.find(s=>String(s.machine)===String(o.machine)&&!s.end);if(linkedStop&&!linkedStop.end){linkedStop.end=o.finished;linkedStop.status='Finalizada';linkedStop.solution=linkedStop.solution||o.workDone||o.diagnosis||'Atendimento concluído pela OS vinculada';}"""),
("""function rvtecRenderOrders(){
 const el=$('orderList');if(!el)return;""",
"""function rvtecToggleOrder(id,button){
 const card=$('rvtec-os-'+id);if(!card)return;
 const expanded=card.classList.toggle('rvtec-os-collapsed')===false;
 button.textContent=expanded?'⌃':'⌄';
 button.setAttribute('aria-expanded',expanded?'true':'false');
 button.setAttribute('aria-label',expanded?'Recolher detalhes da OS':'Expandir detalhes da OS');
}
function rvtecRenderOrders(){
 const el=$('orderList');if(!el)return;"""),
("""  return `<div class="item ${cls}"><div style="flex:1"><h3>${open?'<span class="rvtec-alert-dot"></span>':''}OS #${String(o.id).slice(-5)} — ${esc(mname(o.machine))}</h3><div class="muted">${esc(o.desc)}<br>Prioridade: ${esc(o.priority)} • Área: ${esc(o.area||'Não definida')} • ${esc(o.shift||rvtecShiftAt(o.created||new Date().toISOString()))}<br>Aberta: ${new Date(o.created).toLocaleString('pt-BR')} • ${open?'Parada/aberta há: '+elapsed:'Duração: '+elapsed}<br>Responsável: ${esc(o.assignee||'Aguardando aceite')}</div>
  <div class="rvtec-os-detail"><div><label>Diagnóstico</label><textarea id="rvdiag${o.id}" rows="2">${esc(o.diagnosis||'')}</textarea></div><div><label>Peças utilizadas</label><textarea id="rvparts${o.id}" rows="2">${esc(o.parts||'')}</textarea></div><div style="grid-column:1/-1"><label>Serviço executado</label><textarea id="rvwork${o.id}" rows="2">${esc(o.workDone||'')}</textarea></div></div>""",
"""  return `<div class="item ${cls} rvtec-os-collapsed" id="rvtec-os-${o.id}"><div style="flex:1;min-width:0"><div class="rvtec-os-head"><div><h3>${open?'<span class="rvtec-alert-dot"></span>':''}OS #${String(o.id).slice(-5)} — ${esc(mname(o.machine))}</h3><div class="muted">${esc(o.desc)}<br>Prioridade: ${esc(o.priority)} • Área: ${esc(o.area||'Não definida')} • ${esc(o.shift||rvtecShiftAt(o.created||new Date().toISOString()))}<br>${open?'Parada/aberta há: '+elapsed:'Duração: '+elapsed} • ${esc(o.assignee||'Aguardando aceite')}</div></div><button type="button" class="rvtec-os-toggle" aria-label="Expandir detalhes da OS" aria-expanded="false" onclick="rvtecToggleOrder(${o.id},this)">⌄</button></div>
  <div class="rvtec-os-detail"><div><label>Causa da falha</label><textarea id="rvdiag${o.id}" rows="2">${esc(o.diagnosis||'')}</textarea></div><div style="grid-column:1/-1"><label>Solução aplicada</label><textarea id="rvwork${o.id}" rows="2">${esc(o.solution||o.workDone||'')}</textarea></div><details class="rvtec-os-parts" style="grid-column:1/-1"><summary>Peças utilizadas e estoque</summary><textarea id="rvparts${o.id}" rows="2" aria-label="Observações sobre peças utilizadas">${esc(o.partsNotes||'')}</textarea></details></div>"""),
(""" o.diagnosis=diagnosis.trim();o.partsNotes=parts.trim();o.workDone=work.trim();""",
""" o.diagnosis=diagnosis.trim();o.partsNotes=parts.trim();o.workDone=work.trim();o.solution=work.trim();"""),
(""" const work=$('rvwork'+id)?.value?.trim()||o.workDone||'';""",
""" const work=$('rvwork'+id)?.value?.trim()||o.solution||o.workDone||'';"""),
(""" o.diagnosis=diagnosis;o.workDone=work;""",
""" o.diagnosis=diagnosis;o.workDone=work;o.solution=work;"""),
(""" if(linkedStop&&!linkedStop.end){linkedStop.end=o.finished;linkedStop.status='Finalizada';linkedStop.solution=linkedStop.solution||o.workDone||o.diagnosis;}""",
""" if(linkedStop&&!linkedStop.end){linkedStop.end=o.finished;linkedStop.status='Finalizada';linkedStop.solution=linkedStop.solution||o.solution||o.workDone||o.diagnosis;linkedStop.cause=linkedStop.cause||o.diagnosis;}""")
]
import re
for old,new in repls:
    old="\n".join(line.lstrip() for line in old.splitlines())
    new="\n".join(line.lstrip() for line in new.splitlines())
    pattern=re.compile(r"\n[ \t]*".join(re.escape(line) for line in old.splitlines()))
    count=len(pattern.findall(s))
    if count != 1:
        raise SystemExit(f"Patch abortado: trecho esperado apareceu {count} vezes: {old[:100]!r}")
    s=pattern.sub(lambda _: new, s, count=1)
p.write_text(s,encoding="utf-8")
g=Path("RVTEC_Android/app/build.gradle")
t=g.read_text(encoding="utf-8")
if "versionCode 161" not in t or "versionName '1.6.2'" not in t:
    raise SystemExit("Versão base inesperada; não alterei o Gradle.")
t=t.replace("versionCode 161","versionCode 163",1).replace("versionName '1.6.2'","versionName '1.6.3'",1)
g.write_text(t,encoding="utf-8")
print("Patch RVTEC 1.6.3 aplicado com verificações.")
