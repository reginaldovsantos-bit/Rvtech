# RVTEC 2.0 — Android alpha

Aplicativo Android independente da RVTEC 1.8.5. O pacote de teste é `com.rvtec.manutencao.v2`, permitindo instalação paralela.

## Regras de preservação
- Não alterar nem sobrescrever a RVTEC 1.8.5.
- Não mesclar este PR à `main` antes de aprovação explícita e validação integral.
- Migração dos dados antigos somente com exportação, validação, backup e importação explícita.
- Uma parada, sua OS e os apontamentos devem manter vínculos por identificadores explícitos.
- Apontamentos são a fonte única dos indicadores, gráficos e relatórios; evitar duplicidade de fatos operacionais.

## Implementado nesta alpha
- Cadastro local de máquinas com código, setor e produção kg/h.
- Registro de paradas com causa obrigatória.
- Ordens de serviço com vínculo à máquina/parada, início e conclusão.
- Apontamentos de execução associados à OS.
- Conclusão de OS sem parada sem liberar indevidamente a máquina.
- Liberação explícita da máquina quando a manutenção estiver concluída.
- Persistência local para manter os dados ao reabrir o app.
- Painel com máquinas, paradas e OS abertas, além de MTBF, MTTR e disponibilidade dos últimos 30 dias corridos.
- Tela Memória Técnica IA com integração Firebase AI Logic preparada para Gemini.

## Firebase — configuração verificada no repositório
- Projeto: `rvtech-cfd2e`.
- Aplicativo Android 2.0: `com.rvtec.manutencao.v2`.
- O arquivo `app/google-services.json` contém os registros do app antigo e do app 2.0; o identificador 2.0 foi conferido.
- Firebase BoM 35.2.0, Authentication, Firestore, App Check e Firebase AI Logic estão nas dependências.
- App Check usa Debug Provider em builds debug e Play Integrity em release.
- O token de depuração não pode ser gravado no Git nem distribuído. Deve ser registrado no Firebase Console para o cadastro Android 2.0 correto.
- A sincronização Firestore ainda não está ligada a um fluxo completo de login na interface. Não considerar sincronização em nuvem pronta.
- A chamada Gemini ainda depende de validação ponta a ponta no aparelho. O último bloqueio observado foi rejeição do token App Check; build aprovado, isoladamente, não comprova chamada IA funcional.

## Indicadores
O painel atual calcula com horas corridas. Não desconta turnos, folgas ou paradas programadas. Os números são provisórios até existir calendário/horas programadas por máquina e validação com dados reais.

## Build e validação
O workflow `.github/workflows/rvtec-2-build.yml` executa `gradle testDebugUnitTest assembleDebug` e publica o APK debug como artefato. Uma execução verde comprova compilação e testes unitários daquele commit; não substitui teste funcional em aparelho físico.

## Próximas prioridades
1. Corrigir e validar App Check no app Android 2.0.
2. Confirmar uma consulta Gemini real e testar erros de rede/autorização.
3. Implementar login Firebase na interface e sincronização com política de conflito/recuperação.
4. Consolidar apontamentos, calendário de turnos e indicadores industriais.
5. Implementar preventivas, estoque, memória técnica persistente, relatórios e migração validada da 1.8.5.
