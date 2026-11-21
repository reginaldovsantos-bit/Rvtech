# Regras do Firestore — solicitações RVTEC

O app 1.6.0 já possui uma tela separada para o perfil Produção e usa a coleção `rvtec_solicitacoes`.

**Importante:** o repositório não contém o arquivo de regras atualmente publicado no Firebase. Portanto, não substitua o conjunto ativo por este trecho: isso poderia bloquear o login e a sincronização das demais áreas.

Mescle o bloco `match /rvtec_solicitacoes/{requestId}` do arquivo `rvtec-solicitacoes.rules` ao conjunto atual, preservando as regras existentes. A regra permite:
- Produção criar solicitação apenas em nome do próprio usuário.
- Produção ler somente as próprias solicitações.
- Administração, supervisão e eletricistas lerem solicitações da equipe e converterem uma solicitação pendente em OS.
- Ninguém excluir solicitações; o histórico permanece.

Antes de ativar em produção, testar no Firebase Emulator ou em projeto de teste: operador A não lê solicitação de B; operador não atualiza nem exclui; eletricista lê e converte; usuário inativo não acessa.

O app também precisa que as regras existentes continuem permitindo o acesso atual de login a `rvtec_users` e às áreas legadas. A publicação das regras deve ocorrer somente após revisar o arquivo completo que está ativo no Console Firebase.
