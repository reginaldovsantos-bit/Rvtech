# Plano de migração da RVTEC 1.8.5

## Origem confirmada
Na versão 1.8.5, o objeto principal é armazenado no WebView por meio de localStorage. A chave inicial observada no código é `manutencaoIndustrial10`; após login, os dados por usuário usam `manutencaoIndustrial10_<uid>`.

## Campos de primeiro nível observados
- machines
- stops
- orders
- preventives
- diagnostics
- technicalMemory
- failures
- staff
- stock
- stopDeletions
- notifications
- settings

## Fluxo obrigatório
1. Exportar o JSON da conta autenticada, sem apagar nem modificar a origem.
2. Gerar backup íntegro e registrar tamanho, data e hash SHA-256.
3. Validar JSON, contagem de registros por coleção e identificadores duplicados.
4. Importar em ambiente de teste, preservando IDs e timestamps.
5. Comparar contagens e amostras entre origem e destino.
6. Somente após aceite, habilitar migração de produção.

## Regras de segurança
- Nunca sobrescrever automaticamente dados remotos ou locais.
- Se houver conflito, manter ambos os backups e interromper a gravação até decisão explícita.
- Não importar credenciais, tokens ou segredos.
- Não declarar migração concluída sem relatório de reconciliação.
