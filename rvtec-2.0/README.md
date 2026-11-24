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
