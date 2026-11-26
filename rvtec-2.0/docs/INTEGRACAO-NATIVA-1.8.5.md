# RVTEC 2.0 — integração nativa dos módulos legados

## Diretriz
A interface e as regras de negócio da RVTEC 2.0 são Android nativo (Kotlin/Jetpack Compose). O HTML da 1.8.5 é apenas referência funcional e fonte de exportação JSON; não será embutido em WebView.

## Entregue nesta etapa
- Modelos Kotlin para preventivas, diagnósticos, memória técnica, estoque, movimentações de estoque, equipe, notificações e configurações.
- Conversor sem gravação `LegacyNativeMapper` para transformar o JSON legado nesses modelos.
- Testes unitários do mapeamento e do comportamento com coleções ausentes.
- Branch isolada: `feature/rvtec-2.0-native-legacy-integration`.

## Módulos ainda não integrados à interface/repositório
- Persistência dos novos modelos no repositório local.
- Telas Compose de preventivas, diagnóstico, memória técnica, estoque, equipe e notificações.
- Regras de movimentação/baixa de estoque e associação com OS.
- Exportação/backup, prévia de importação e reconciliação integral de todos os registros.
- Sincronização Firebase por usuário para todas as coleções.
- Testes de interface, migração com backup real da 1.8.5 e regressão completa.

## Segurança da migração
O conversor não grava, não apaga e não substitui dados. A migração de produção permanece bloqueada até backup íntegro, validação de IDs, revisão das contagens e aprovação explícita.

## Próxima sequência
1. Persistir os novos modelos e movimentos com transações e validação.
2. Construir telas Compose e navegação.
3. Conectar permissões e Firebase.
4. Importar backup de teste, comparar contagens/IDs e executar testes automatizados.
5. Gerar APK e validar instalação/atualização em dispositivo.
