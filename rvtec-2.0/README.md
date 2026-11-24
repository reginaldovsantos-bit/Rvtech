# RVTEC 2.0 — fundação limpa

Esta pasta é uma aplicação Android nova e isolada da base legada. O identificador de pacote de teste é `com.rvtec.manutencao.v2`, para permitir instalação paralela sem substituir a 1.8.5.

## Princípios
- Não alterar nem sobrescrever a RVTEC 1.8.5.
- Migrar dados somente por exportação, validação, backup e importação explícita.
- Preservar inicialmente o contrato legado: machines, stops, orders, preventives, diagnostics, technicalMemory, failures, staff, stock, stopDeletions, notifications e settings.
- Apontamentos serão a origem única dos indicadores, gráficos e relatórios; evitar registros duplicados.
- Uma parada, sua OS e seus apontamentos terão vínculos por identificadores explícitos.
- Encerramento de OS e liberação de máquina serão operações consistentes e auditáveis.
- Firebase Auth, Firestore, App Check e Firebase AI Logic serão integrados após registrar o pacote de teste no Firebase; não reutilizar configuração de outro applicationId.

## Estado
Fundação de interface criada. Ainda não é uma versão funcional de produção: módulos, persistência, Firebase, IA, importação e testes precisam ser implementados e aprovados.


## Integração Firebase — etapa alpha02

- Firebase Android BoM 35.2.0, Auth, Cloud Firestore, App Check Play Integrity e Firebase AI Logic adicionados.
- `FirebaseServices` prepara bootstrap, App Check e transferência explícita de snapshot para o caminho isolado `users/{uid}/rvtec/snapshot`.
- O repositório local continua funcionando sem Firebase e não envia dados automaticamente.
- Regras Firestore em `firestore.rules` limitam leitura e escrita ao UID autenticado.
- A aplicação precisa ser registrada no Firebase com o applicationId `com.rvtec.manutencao.v2`; depois, adicionar o `google-services.json` correspondente ao módulo `app`. Não reutilizar o arquivo da versão 1.8.5.
- A sincronização ainda não está ligada à interface e requer fluxo de autenticação antes de habilitar envio/recebimento. Não habilitar regras abertas.

## Correção de integridade de OS

Uma OS sem parada vinculada não pode marcar a máquina como liberada. A conclusão sem parada mantém o estado operacional existente; a liberação explícita permanece vinculada ao encerramento da parada. Testes unitários cobrem os dois caminhos.

## Qualidade operacional — etapa seguinte
- O registro de parada exige uma causa informada na interface; não é mais preenchido com uma descrição genérica.
- O cálculo de indicadores recebe uma janela de observação explícita. Paradas abertas contam como indisponibilidade até o fim da janela; MTTR considera somente paradas encerradas no período; falhas são contadas pela data de início dentro da janela.
- Os indicadores ainda não são exibidos no painel. Antes de publicar, será necessário definir calendário/horas programadas por máquina e validar os resultados com dados reais, evitando tratar 24 horas por dia como tempo produtivo.
- Testes unitários cobrem disponibilidade, MTTR, MTBF, paradas em andamento e limites do período.
