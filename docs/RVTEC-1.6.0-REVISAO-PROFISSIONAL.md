# RVTEC 1.6.0 — Revisão Profissional

## Objetivo
Preservar as funcionalidades existentes e melhorar segurança, integridade dos dados, desempenho e manutenção do código.

## Base criada
- Arquivo de interface: RVTEC_1.6.0.html
- Branch isolada: rvtec-1.6.0-professional-review
- Pipeline de APK de teste: .github/workflows/build-rvtec-160.yml
- Atualização do ciclo de manutenção: painel a cada 10 s; verificações de escalonamento a cada 30 s; suspende verificações enquanto o app está em segundo plano; retoma ao voltar ao primeiro plano.

## Correções obrigatórias antes de liberar
1. Sincronização segura: estabelecer versão/updatedAt por registro, controle de conflitos e exclusões com tombstones. Não fazer merge local/remoto sem política explícita.
2. Banco: planejar migração do documento único rvtec_data/main para coleções por domínio (máquinas, ordens, estoque, paradas, preventivas, diagnósticos e memória técnica). Fazer backup e migração idempotente.
3. Estoque: mover baixa/estorno para transação Firestore no servidor ou operação nativa confiável; validar saldo no momento da gravação.
4. Segurança: verificar regras ativas do Firestore; impor autorização no backend/servidor e restringir a ponte JavaScript nativa. Não confiar em role enviada pelo JavaScript.
5. Usuários: testar criação de conta e rollback em falha de gravação do perfil.
6. Notificações: deixar claro que o registro de canais não equivale a envio externo; integrar serviço autenticado caso haja requisito de envio real.
7. Interface: consolidar CSS duplicado e separar componentes sem alterar telas aprovadas.
8. Desempenho: medir tempo de abertura, memória, uso de CPU, quantidade de renderizações e tráfego Firestore em Android real.

## Testes de aceitação
- Login, logout, recuperação de senha e troca de usuário no mesmo aparelho.
- Dois usuários alterando ordens simultaneamente.
- Criar, editar, excluir e sincronizar máquinas, paradas, ordens, preventivas e diagnósticos.
- Baixa simultânea da última unidade de uma peça sem saldo negativo.
- Exclusão de registros sem reaparecimento após sincronização.
- Uso offline, reconexão e resolução de conflito.
- Perfis administrador, supervisor, eletricista e produção.
- IA e App Check em APK debug com token registrado e em release com Play Integrity.
- Câmera, galeria, microfone, navegação externa e botão voltar.
- Teste em Android 12 e Android 15 ou superior.

## Regra de segurança
A 1.6.0 é uma versão de desenvolvimento. Não substituir a versão em uso nem publicar em produção até concluir backup, migração, testes de concorrência e testes em aparelho real.
