# RVTEC 2.0 — Template de Comunicação

## Objetivo
Adicionar comunicação interna à equipe sem misturar o chat com a Memória Técnica IA e sem duplicar o cadastro de técnicos ou os registros de manutenção.

## Estado deste template
- Interface Android nativa em Jetpack Compose.
- Abas: Conversas, Grupos e Equipe.
- Lista de contatos/canais de demonstração.
- Abertura de conversa e envio local de mensagens de demonstração.
- Ainda não conectado ao Firebase; não usar como mensageiro de produção.

## Arquivo inicial
`app/src/main/java/com/rvtec/v2/CommunicationTemplate.kt`

O componente `CommunicationTemplateScreen()` é isolado e pode ser ligado à navegação principal depois da revisão visual. Não cria uma segunda tela de IA nem altera os módulos existentes.

## Próximas etapas
1. Revisar nomes, abas, permissões e aparência com o usuário.
2. Criar repositório de mensagens com Firebase Authentication + Cloud Firestore.
3. Implementar conversas individuais e grupos com regras de acesso por participante.
4. Implementar notificações push (FCM), confirmação de leitura e envio de imagens/arquivos.
5. Executar testes de regras, sincronização, modo offline e compilação APK.

## Modelo de dados sugerido
- `conversations/{conversationId}`: type, title, participantIds, createdAt, updatedAt, lastMessage.
- `conversations/{conversationId}/messages/{messageId}`: senderId, text, createdAt, attachment, readBy.
- `users/{uid}`: reaproveitar o usuário autenticado e o perfil já existente no RVTEC.

## Regras de produto
- Reutilizar a autenticação e os perfis existentes; não cadastrar técnico novamente.
- Separar mensagens comuns de eventos operacionais. Uma mensagem pode referenciar uma OS ou máquina por ID, sem copiar os dados da OS.
- Não duplicar histórico técnico: diagnóstico, apontamento e conclusão continuam nos módulos próprios.
- A aba de comunicação deve poder ser ocultada nas Configurações sem apagar mensagens.
