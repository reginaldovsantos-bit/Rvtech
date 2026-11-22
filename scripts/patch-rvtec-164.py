from pathlib import Path
import re

p = Path("RVTEC_Android/app/src/main/assets/index.html")
s = p.read_text(encoding="utf-8")

def replace_once(pattern, replacement, label, flags=re.S):
    global s
    s2, n = re.subn(pattern, lambda match: replacement, s, count=1, flags=flags)
    if n != 1:
        raise SystemExit(f"Patch 1.6.4 abortado: {label}; ocorrências={n}")
    s = s2

# Iniciar apontamento, identificar técnico e registrar parada quando aplicável.
replace_once(
    r"function rvtecStartOrder\(id\)\{.*?\n\}",
    """function rvtecStartOrder(id){
 const o=db.orders.find(x=>String(x.id)===String(id));if(!o||o.status==='Concluída')return;
 if(!o.acknowledgedAt)rvtecAcknowledgeOrder(id);
 if(o.startedAt)return alert('O apontamento desta OS já foi iniciado.');
 const now=new Date().toISOString();
 const machineStopped=!!o.stopId||confirm('A máquina parou para este atendimento?\\n\\nOK = Sim\\nCancelar = Não');
 o.startedAt=now;o.serviceStartAt=now;o.technician=o.assignee||cloudUser?.email||userProfile?.name||'Técnico';
 o.machineStopped=machineStopped;
 if(machineStopped){
  let stop=db.stops.find(x=>String(x.id)===String(o.stopId)&&!x.end)||db.stops.find(x=>String(x.machine)===String(o.machine)&&!x.end);
  if(!stop){
   const cause=o.diagnosis||o.desc||'Parada registrada pelo apontamento da OS';
   stop={id:Date.now()+Math.random(),machine:o.machine,cause,owner:o.technician,notes:'Parada vinculada automaticamente à OS #'+String(o.id).slice(-5),solution:'',start:now,status:'Aberta',end:null,shift:o.shift||rvtecShiftAt(now),supervisorAlertedAt:null,orderId:o.id,source:'OS'};
   db.stops.unshift(stop);
  }
  o.stopId=stop.id;o.downtimeStartAt=stop.start||now;o.machineStopped=true;
  rvtecOrderLog(o,'Parada de máquina iniciada',new Date(o.downtimeStartAt).toLocaleString('pt-BR'));
 }else{ o.downtimeStartAt=null;rvtecOrderLog(o,'Atendimento iniciado sem parada de máquina','Máquina permaneceu em operação.'); }
 rvtecOrderLog(o,'Apontamento técnico iniciado',`Técnico: ${o.technician} • Início: ${new Date(now).toLocaleString('pt-BR')}`);save();rvtecRenderOrders();
}""",
    "início do apontamento"
)

# Fechamento calcula separadamente atendimento e parada.
replace_once(
    r"function rvtecFinishOrder\(id\)\{.*?\n\}",
    """function rvtecFinishOrder(id){
 const o=db.orders.find(x=>String(x.id)===String(id));if(!o)return;
 const diagnosis=$('rvdiag'+id)?.value?.trim()||o.diagnosis||'';
 const solution=$('rvwork'+id)?.value?.trim()||o.solution||o.workDone||'';
 if(!solution)return alert('Informe a solução aplicada antes de concluir a OS.');
 if(!confirm('Concluir o apontamento técnico e liberar a máquina?'))return;
 const now=new Date().toISOString();
 o.diagnosis=diagnosis;o.solution=solution;o.workDone=solution;
 o.serviceEndAt=now;o.finished=now;o.returnedAt=now;o.status='Concluída';
 o.serviceMinutes=Math.max(0,Math.round((new Date(now)-new Date(o.serviceStartAt||o.startedAt||o.created))/60000));
 let linkedStop=db.stops.find(s=>String(s.id)===String(o.stopId));
 if(o.machineStopped&&linkedStop&&!linkedStop.end){
  linkedStop.end=now;linkedStop.status='Finalizada';linkedStop.solution=solution;linkedStop.cause=linkedStop.cause||diagnosis;
  o.downtimeEndAt=now;o.downtimeMinutes=Math.max(0,Math.round((new Date(now)-new Date(o.downtimeStartAt||linkedStop.start))/60000));
 }else if(o.machineStopped){
  o.downtimeEndAt=linkedStop?.end||now;
  o.downtimeMinutes=linkedStop?Math.max(0,Math.round((new Date(o.downtimeEndAt)-new Date(o.downtimeStartAt||linkedStop.start))/60000)):0;
 }else{o.downtimeMinutes=0;o.downtimeEndAt=null;}
 rvtecOrderLog(o,'Apontamento técnico concluído',`Técnico: ${o.technician||o.assignee||'Não identificado'} • Atendimento: ${o.serviceMinutes} min • Máquina parada: ${o.downtimeMinutes} min • Causa: ${diagnosis||'Não informada'} • Solução: ${solution}`);
 rvtecOrderLog(o,'OS encerrada / máquina liberada',`Tempo de atendimento: ${o.serviceMinutes} min${o.machineStopped?' • Tempo de parada: '+o.downtimeMinutes+' min':' • Sem parada de máquina'}`);
 save();rvtecRenderOrders();
}""",
    "conclusão e cálculo dos tempos"
)

# Resumo do apontamento na OS; controles de expansão já existem na 1.6.3.
replace_once(
    r"function rvtecRenderOrders\(\)\{",
    """function rvtecToggleOrder(id,button){
 const card=$('rvtec-os-'+id);if(!card)return;
 const expanded=card.classList.toggle('rvtec-os-collapsed')===false;
 button.textContent=expanded?'⌃':'⌄';
 button.setAttribute('aria-expanded',expanded?'true':'false');
 button.setAttribute('aria-label',expanded?'Recolher detalhes da OS':'Expandir detalhes da OS');
}
function rvtecRenderOrders(){""",
    "controle de expansão da OS"
)

# Adiciona o resumo do apontamento logo antes dos botões da OS.
needle = '  <div class="rvtec-os-actions">'
summary = '''  <div class="rvtec-pointing-summary" style="grid-column:1/-1;margin:8px 0;padding:9px 11px;border:1px solid var(--line);border-radius:10px;font-size:13px"><strong>Apontamento técnico</strong><br>${o.startedAt?'Técnico: '+esc(o.technician||o.assignee||'Não identificado')+' • Início: '+new Date(o.serviceStartAt||o.startedAt).toLocaleString('pt-BR'):'Aguardando início do apontamento'}${o.finished?'<br>Atendimento: '+(o.serviceMinutes??0)+' min • Máquina parada: '+(o.downtimeMinutes??0)+' min':o.startedAt?'<br>Máquina parada: '+(o.machineStopped?'Sim — '+(o.downtimeStartAt?new Date(o.downtimeStartAt).toLocaleString('pt-BR'):'início registrado'):'Não')+' • Atendimento em andamento':''}</div>
'''
if s.count(needle) != 1:
    raise SystemExit("Patch 1.6.4 abortado: local do resumo da OS não encontrado unicamente.")
s = s.replace(needle, summary + needle, 1)

# Nome explícito do botão de início.
s = s.replace('>Iniciar atendimento</button>', '>Iniciar apontamento</button>', 1)

p.write_text(s, encoding="utf-8")

g = Path("RVTEC_Android/app/build.gradle")
t = g.read_text(encoding="utf-8")
if "versionCode 163" not in t or "versionName '1.6.3'" not in t:
    raise SystemExit("Versão base 1.6.3 inesperada; Gradle não alterado.")
t = t.replace("versionCode 163", "versionCode 164", 1).replace("versionName '1.6.3'", "versionName '1.6.4'", 1)
g.write_text(t, encoding="utf-8")
print("RVTEC 1.6.4: apontamento técnico aplicado.")
