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
