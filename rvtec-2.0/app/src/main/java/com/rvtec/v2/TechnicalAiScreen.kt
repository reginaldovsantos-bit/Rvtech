package com.rvtec.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.rvtec.v2.data.TechnicalAiService
import com.rvtec.v2.data.TechnicalAiTurn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

private data class AiChatMessage(val question: String, val answer: String)

@Composable
fun TechnicalAiScreen(firebaseApp: FirebaseApp?) {
    var question by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var appCheckStatus by remember { mutableStateOf("") }
    var checkingAppCheck by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<AiChatMessage>() }
    val scope = rememberCoroutineScope()
    val service = remember(firebaseApp) { firebaseApp?.let(::TechnicalAiService) }

    LaunchedEffect(service) {
        error = if (service == null) {
            "Firebase não inicializou nesta instalação. Confira o google-services.json do pacote com.rvtec.manutencao.v2."
        } else ""
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Memória Técnica IA", style = MaterialTheme.typography.headlineSmall)
            TextButton(enabled = messages.isNotEmpty() && !loading, onClick = {
                messages.clear()
                error = ""
            }) { Text("Nova conversa") }
        }
        Text("Assistente técnico do RVTEC para extrusão de filme plástico, puxadores, bobinadores, motores, inversores WEG, CLP, IHM, sensores e encoders.")

        if (BuildConfig.DEBUG && firebaseApp != null) {
            OutlinedButton(
                enabled = !checkingAppCheck,
                onClick = {
                    checkingAppCheck = true
                    appCheckStatus = "Verificando obtenção do token App Check..."
                    scope.launch {
                        try {
                            val result = FirebaseAppCheck.getInstance(firebaseApp).getToken(false).await()
                            appCheckStatus = if (result.token.isNotBlank()) {
                                "Token App Check obtido. Isso não garante, sozinho, a autorização da API Gemini."
                            } else "O serviço App Check retornou um token vazio."
                        } catch (e: Exception) {
                            val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
                            appCheckStatus = "Falha no App Check: $detail"
                        } finally {
                            checkingAppCheck = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (checkingAppCheck) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Testar App Check")
            }
            if (appCheckStatus.isNotBlank()) {
                Text(appCheckStatus, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (messages.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "Descreva o equipamento, o modelo, o código de falha e em que condição o problema acontece. A IA fará perguntas quando faltarem informações.",
                    Modifier.padding(14.dp)
                )
            }
        } else {
            messages.forEachIndexed { index, message ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Você", style = MaterialTheme.typography.labelLarge)
                        Text(message.question)
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("RVTEC IA • resposta ${index + 1}", style = MaterialTheme.typography.labelLarge)
                        Text(message.answer)
                    }
                }
            }
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5,
            label = { Text("Digite sua pergunta técnica") },
            placeholder = { Text("Ex.: CFW700 apresenta F074 quando o puxador acelera...") }
        )
        Button(
            enabled = question.isNotBlank() && !loading && service != null,
            onClick = {
                val submitted = question.trim()
                val previous = messages.map { TechnicalAiTurn(it.question, it.answer) }
                question = ""
                loading = true
                error = ""
                scope.launch {
                    try {
                        val response = requireNotNull(service).diagnose(submitted, previous)
                        messages.add(AiChatMessage(submitted, response))
                    } catch (e: Exception) {
                        question = submitted
                        error = "Não foi possível consultar o Gemini: ${e.localizedMessage?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName}"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Enviar pergunta")
        }
        if (loading) Text("Analisando a ocorrência...")
        if (error.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(error, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Text(
            "Segurança: use a análise como apoio. Confirme medições, diagramas e procedimentos do fabricante. Não intervenha em circuitos energizados sem autorização e procedimento apropriado.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
