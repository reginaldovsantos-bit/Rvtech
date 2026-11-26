# RVTEC 2.0 — Arquitetura do fluxo de apontamento

## Objetivo

Eliminar lançamentos repetidos sem eliminar telas úteis. O usuário registra um fato operacional uma vez; as demais áreas apresentam esse mesmo fato com visões e ações adequadas ao perfil.

## Decisão arquitetural

O apontamento será a entrada operacional única (registro do fato), mas não será um formulário gigante nem uma tabela que mistura tudo. O domínio será separado em entidades relacionadas:

- **Registro operacional (Apontamento):** evento original, com identificador único, máquina, autor, perfil, data/hora, turno, tipo e origem.
- **Parada:** criada/atualizada quando o apontamento informar indisponibilidade; mantém início, fim, causa e situação.
- **Solicitação/OS:** criada pelo operador ou técnico quando houver necessidade de intervenção; vinculada ao apontamento e, quando aplicável, à parada.
- **Execução técnica:** lançamentos de trabalho feitos pelo técnico na OS; inclui diagnóstico, ação, peças, duração e evidências.
- **Encerramento/liberação:** ação exclusiva do técnico autorizado; registra solução, horário, condição final e liberação da máquina.
- **Histórico e indicadores:** consultas derivadas das entidades acima. Não criam cópias editáveis dos mesmos fatos.

## Jornada por perfil

### Operador
1. Seleciona máquina e informa o que ocorreu.
2. Registra apontamento uma única vez.
3. Se exigir manutenção, abre uma OS a partir desse apontamento. O sistema já carrega máquina, horário, turno, descrição e parada relacionada.
4. Pode acompanhar o status, complementar informação permitida e consultar histórico.
5. Não pode concluir OS, encerrar parada técnica nem liberar máquina.

### Técnico
1. Abre a OS existente a partir da fila; não recria o evento informado pelo operador.
2. Assume/inicia a execução.
3. Registra um ou mais apontamentos de execução na mesma OS, com diagnóstico, serviço, peças e tempo.
4. Finaliza a OS, informa solução e condição da máquina.
5. Se autorizado e após validações, encerra a parada e libera a máquina.

## Organização das telas

Manter uma tela principal **Apontamentos** como ponto de entrada e consulta operacional, com ações contextuais conforme perfil e estado.

As abas **Paradas**, **Ordens de Serviço**, **Máquinas** e **Relatórios** permanecem como visões especializadas:
- Paradas: lista/filtra eventos de indisponibilidade; não pede para redigitar causa, máquina ou horário já registrados.
- Ordens: fila e ciclo de vida da OS; abertura a partir de apontamento preenche os dados automaticamente.
- Máquinas: estado atual e histórico consolidado.
- Relatórios: leitura agregada dos mesmos registros.

Não manter dois botões/telas que criem o mesmo tipo de ocorrência. Se duas telas precisarem iniciar a mesma ação, ambas devem chamar o mesmo caso de uso e gerar o mesmo identificador, nunca dois registros.

## Regras contra duplicidade

1. Todo envio recebe um **idempotencyKey** único, gerado antes de salvar. Repetir o envio por toque duplo, timeout ou retry retorna o registro original.
2. Uma parada pode ter no máximo uma OS ativa vinculada. A restrição é pelo stopId, não por toda a máquina.
3. Uma OS criada a partir de apontamento mantém o apontamento de origem; a OS não copia nem recria o evento.
4. Uma máquina pode ter mais de uma OS ativa somente quando forem ocorrências independentes, justificadas e identificadas por apontamentos distintos. Não bloquear indiscriminadamente toda OS adicional da máquina.
5. Não deduplicar por texto livre isolado: descrições parecidas podem representar falhas diferentes.
6. A API/repositório valida as regras de forma transacional; desabilitar botão na interface é apenas proteção adicional.
7. Toda alteração de status registra autor, data/hora e motivo no histórico de auditoria.

## Estados recomendados

- Apontamento: RASCUNHO → REGISTRADO → VINCULADO (quando gerar OS/parada) → ARQUIVADO.
- OS: ABERTA → EM_EXECUÇÃO → AGUARDANDO_PEÇA/AGUARDANDO_PRODUÇÃO (quando aplicável) → CONCLUÍDA ou CANCELADA.
- Parada: ABERTA → VINCULADA À OS → ENCERRADA.
- Máquina: OPERANDO → PARADA → EM_MANUTENÇÃO → LIBERADA.

Transições devem ocorrer por casos de uso, não por edição direta de campos em telas diferentes.

## Dados e indicadores

- MTTR/MTBF/disponibilidade devem ser calculados a partir dos intervalos de parada validados, não de cópias em apontamentos e OS.
- Tempo de execução técnica vem dos apontamentos de execução; não confundir com duração total da parada.
- Relatórios são somente leitura e devem indicar período, filtros e critérios de cálculo.
- Preservar IDs e timestamps da versão 1.8.5 durante a migração; mapear registros legados sem fundir itens apenas por semelhança textual.

## Migração 1.8.5

1. Exportar backup integral da conta, sem modificar a origem.
2. Registrar tamanho, data e SHA-256.
3. Inventariar cada coleção e identificar sobreposição funcional entre stops, orders, diagnostics, technicalMemory e failures.
4. Criar tabela de mapeamento legado → novo domínio, preservando IDs e vínculos.
5. Importar em ambiente de teste e produzir relatório de reconciliação por coleção.
6. Investigar duplicatas candidatas, mas nunca apagar automaticamente; classificar como duplicata confirmada, ocorrência distinta ou inconclusiva.
7. Só migrar produção após validação e aceite explícito.

## Critérios de aceite

- Um toque duplo/reenvio não cria dois apontamentos nem duas OS.
- Uma parada não recebe duas OS ativas.
- O operador consegue abrir OS, mas não concluir nem liberar máquina.
- O técnico consegue registrar várias etapas de execução na mesma OS e é o único perfil que pode concluir/fechar.
- Paradas, OS, máquinas, histórico e relatórios mostram os mesmos dados de origem, sem redigitação.
- Ocorrências independentes na mesma máquina continuam permitidas.
- Nenhum dado legado é descartado sem relatório e aprovação.

## Observação sobre a implementação atual

A proteção temporária que bloqueia qualquer segunda OS ativa na mesma máquina é mais restritiva do que esta arquitetura recomenda. Ela deve ser substituída por deduplicação por idempotencyKey e vínculo da ocorrência/parada, permitindo OS simultâneas quando forem problemas independentes.
