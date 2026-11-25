# RVTEC — Especificação oficial de evolução
**Documento:** ROADMAP_RVTEC  
**Versão de referência:** 1.0  
**Data:** 22/11/2026  
**Projeto:** RVTEC — Manutenção Industrial  
**Repositório:** reginaldovsantos-bit/Rvtech

## 1. Objetivo e regra de preservação
Este documento é a referência oficial para as próximas evoluções do RVTEC. A evolução deve partir do código-fonte original do projeto Android, preservando a estrutura, identidade visual, navegação e funções já operacionais.

**Regras obrigatórias**
- Não desenvolver a partir de APK. APK é somente artefato de instalação/teste.
- Não reconstruir o aplicativo do zero.
- Não remover nem substituir funções existentes sem autorização expressa.
- Preservar autenticação, permissões, dados e integrações existentes.
- Fazer alterações pequenas e isoladas, com revisão e teste após cada módulo.
- Não publicar credenciais, tokens, chaves privadas ou arquivos de configuração sensíveis.
- Gerar APK somente após compilação e testes; não declarar teste como aprovado sem execução real.

## 2. Plataforma e arquitetura
- Aplicativo Android, mantendo o projeto-fonte e sua configuração de build.
- Firebase Authentication e Cloud Firestore.
- Firebase App Check, com configuração separada para debug e release.
- Gemini por Firebase AI Logic, conforme a arquitetura vigente do projeto.
- Cloud Functions ou autenticação secundária para provisionar usuários sem encerrar a sessão administrativa atual.
- Armazenamento de anexos com regras de acesso compatíveis com os perfis e máquinas.

## 3. Prioridade zero — estabilização técnica
### 3.1 Firebase App Check e IA
- Conferir os providers configurados para debug e produção.
- Registrar SHA-256 dos certificados de debug e release no Firebase quando aplicável.
- Usar token de depuração apenas em ambiente de teste; nunca embutir token de debug na versão de produção.
- Verificar configuração do App Check para Firestore e Storage, além dos serviços efetivamente utilizados.
- Testar Gemini/AI Logic e upload de fotos com sessão autenticada.
- Não alterar chaves ou credenciais existentes sem validação do projeto Firebase.

### 3.2 Estoque
- Permitir editar e excluir itens, com confirmação e validação.
- Registrar movimentações de entrada, saída e ajuste; não apagar histórico de movimentações.
- Ao concluir OS, baixar peças vinculadas uma única vez, com operação idempotente e validação de saldo.
- Bloquear saldo negativo, salvo regra de negócio explicitamente aprovada.
- Manter trilha de auditoria de usuário, data, quantidade e OS.

### 3.3 Usuários e perfis
- Criar usuários administrativos e técnicos sem deslogar o administrador atual.
- Preferir Cloud Functions com privilégios no servidor; alternativa: instância secundária de Firebase Auth.
- Nunca colocar credenciais administrativas ou service-account no aplicativo.
- Aplicar regras de autorização também no Firestore/Storage, não apenas ocultar botões na interface.
- Registrar criação, alteração, bloqueio e perfil dos usuários.

## 4. Funcionalidades — Operação e agilidade
1. QR Code exclusivo por máquina, abrindo sua ficha, histórico e OS.
2. Abertura expressa de parada crítica em poucos passos.
3. Fila e agendamento de atendimento por setor.
4. Duplicação de OS para falhas recorrentes, gerando novo identificador e novo ciclo de vida.
5. Cadastro de máquinas similares por clonagem controlada da estrutura, sem copiar histórico ou identificadores.
6. Estado visual da máquina: operando, em manutenção ou bloqueada.
7. Fotos de defeito e peça substituída; manuais e esquemas elétricos/hidráulicos na máquina e na OS.
8. Checklist rápido de manutenção preventiva.
9. Lembrete de teste e retorno pós-reparo.
10. Etapa de segurança obrigatória antes do reparo: LOTO, confirmação de energia zero e EPIs aplicáveis.
11. Modo offline com fila local, sincronização automática e resolução segura de conflitos ao reconectar.

## 5. Funcionalidades — Estoque e suprimentos
1. Alerta configurável de estoque mínimo.
2. Cadastro de peças substitutas/compatíveis, com aprovação técnica.
3. Acompanhamento de solicitação, aprovação, pedido e entrega de compras.
4. Histórico de fornecedores, preços, datas, garantias e peças fornecidas.

## 6. Funcionalidades — Gestão, indicadores e controle
1. Painel de máquinas paradas com atualização em tempo real quando online.
2. Filtros e histórico por máquina, setor, turno e técnico.
3. Motivos padronizados de parada: elétrica, mecânica, hidráulica, pneumática, processo, falta de peça e outros configuráveis.
4. MTTR calculado a partir de timestamps consistentes e regras documentadas.
5. Custo estimado da parada: horas paradas, peças e mão de obra, com parâmetros configuráveis.
6. Registro de refugo/perda de material durante parada e partida.
7. Contador de horas/ciclos para manutenção preventiva.
8. Passagem de turno com pendências, riscos e OS abertas.
9. Assinatura eletrônica do responsável na conclusão da OS, com identificação e data/hora.
10. Alertas de preventiva vencida, parada crítica e falta de peça.
11. Exportação de relatórios em PDF e CSV/Excel.

## 7. Plano de implementação por etapas
| Etapa | Escopo | Critério de aceite |
|---|---|---|
| 0 | Inventário do código, build e testes de regressão | Build reproduzível; funções atuais catalogadas |
| 1 | App Check, Firebase e IA | Login, leitura/escrita autorizada, IA e anexos testados |
| 2 | Estoque e baixa por OS | Editar/excluir com segurança; baixa única e auditável |
| 3 | Usuários e permissões | Criar usuário sem deslogar administrador; regras validadas |
| 4 | QR Code, máquinas e OS expressa | QR abre máquina correta; OS preserva histórico |
| 5 | Anexos, preventivas e segurança | Upload/download autorizado; checklist e LOTO obrigatórios |
| 6 | Offline e sincronização | Operação sem rede e sincronização sem duplicidade |
| 7 | Painéis, MTTR, custos e turnos | Indicadores conferidos com dados de teste |
| 8 | Alertas e exportações | Alertas acionados por regra; arquivos exportados legíveis |

## 8. Testes obrigatórios por módulo
- Compilação limpa e análise de erros.
- Testes de inclusão, leitura, edição e exclusão conforme permissões.
- Testes com usuário administrador e técnico.
- Testes de perda e retorno de conexão.
- Testes contra duplicidade de OS, movimentação de estoque e notificações.
- Testes de anexos e regras de acesso.
- Regressão das funções já existentes.
- Verificação de logs e tratamento de falhas, sem expor dados sensíveis.

## 9. Critério para liberar uma versão
Uma versão só pode ser considerada pronta quando: o código-fonte estiver versionado; o build terminar com sucesso; os testes previstos forem executados; as funções antigas passarem pela regressão; e o APK de teste estiver identificado com número de versão e origem do commit.

## 10. Primeira ação de execução
Iniciar pela Etapa 0: identificar no código-fonte atual a versão-base mais recente, mapear módulos, dependências Firebase, regras Firestore/Storage, fluxo de autenticação e processo de build. Não iniciar alterações funcionais antes desse inventário e da confirmação de uma compilação reproduzível.


## 11. Auditoria confirmada do pacote RVTEC 1.8.5
**Fonte examinada:** `RVTEC_1.8.5_SEM_BOTAO_MENU.zip`, branch `main`.  
**Execução de auditoria:** GitHub Actions run `45544769247`, concluída com sucesso em 25/11/2026.

### 11.1 Estrutura real do pacote
- `index.html`: 222.381 bytes; título interno identificado como **RVTEC 1.8.4 — Grade compacta**.
- `manifest.webmanifest`, `sw.js`, `LEIA-ME.txt` e ícones PNG de 192 e 512 px.
- O ZIP não contém `build.gradle`, `settings.gradle`, `gradlew` ou `AndroidManifest.xml`: é um pacote PWA/web, não um projeto Android nativo.
- O service worker declara cache `rvtec-shell-1.8.4`. Há, portanto, divergência entre o nome do ZIP (1.8.5) e marcadores internos (1.8.4), que precisa ser esclarecida antes de rotular a versão migrada.

### 11.2 Recursos comprovados no HTML
- Estrutura com 190 IDs de elementos e 56 botões.
- Painel com alerta visual piscante para máquinas paradas, grade de máquinas e estados.
- Ficha/histórico de máquina com ordens, paradas abertas, MTTR e paradas mensais.
- Ordens de serviço e apontamentos técnicos, incluindo salvamento parcial e encerramento com causa, solução, técnico e tempos.
- Estoque com cadastro, edição, exclusão, movimentações e baixa de peças vinculada à OS.
- Configurações de tema claro/escuro, cor de destaque, gráficos, visibilidade de telas e ícones.
- Autenticação Firebase, Firestore e App Check aparecem no código web.
- O código contém integração Gemini via Firebase AI Logic, com modelo declarado `gemini-2.5-flash` e ponte para resultado nativo Android.
- Há funções de gestão de usuários/perfis e notificações no código; cada fluxo ainda precisa de teste funcional e revisão das regras de segurança.

### 11.3 Limitações e cuidados identificados
- O LEIA-ME declara que a prévia local existente não mantém Firebase e IA nativos ativos e não deve ser usada com dados reais de produção.
- A presença de nomes, funções ou referências no HTML não prova que todos os fluxos estão operacionais. A migração deve validar cada fluxo e suas regras.
- Não copiar o HTML inteiro para uma WebView como substituto da arquitetura Android; usar o HTML como referência de comportamento, layout e requisitos, reconstruindo os módulos no projeto Kotlin/Compose.
- Não descartar o painel, grade, indicadores, identidade visual, menu configurável, fluxos de OS/apontamentos nem estoque ao fazer a migração.

### 11.4 Ordem de recuperação recomendada
1. Reproduzir no Android o painel, grade/semáforo de máquinas e histórico/KPIs.
2. Completar ciclo de parada → OS → apontamento parcial/final → liberação da máquina.
3. Completar estoque e movimentações ligadas à OS, com proteção contra baixa duplicada.
4. Recuperar configurações de tema, menu, ícones e gráficos.
5. Migrar usuários/perfis, notificações e anexos com regras Firebase verificadas.
6. Integrar Gemini por Firebase AI Logic e validar a ponte nativa em aparelho real.
7. Executar regressão dos dados e testar em múltiplos aparelhos antes de gerar prévia para aprovação.

**Status da auditoria da fonte:** inventário estático concluído.  
**Status da migração funcional:** ainda não concluída; requer implementação e testes por módulo.
