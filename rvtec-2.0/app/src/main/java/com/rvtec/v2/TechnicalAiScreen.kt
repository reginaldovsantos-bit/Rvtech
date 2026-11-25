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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun TechnicalAiScreen(firebaseApp: FirebaseApp?) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var appCheckStatus by remember { mutableStateOf("") }
    var checkingAppCheck by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val service = remember(firebaseApp) { firebaseApp?.let(::TechnicalAiService) }

    LaunchedEffect(service) {
        if (service == null) {
            error = "Firebase não inicializou nesta instalação. Confira se o APK foi compilado com o google-services.json que contém o pacote com.rvtec.manutencao.v2."
        } else {
            error = ""
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Memória Técnica IA", style = MaterialTheme.typography.headlineSmall)
        Text("Assistente de diagnóstico para extrusoras, puxadores, bobinadores, motores, inversores WEG, CLP, IHM, sensores e encoders.")

        if (com.rvtec.v2.BuildConfig.DEBUG && firebaseApp != null) {
            OutlinedButton(
                enabled = !checkingAppCheck,
                onClick = {
                    checkingAppCheck = true
                    appCheckStatus = "Verificando obtenção do token App Check..."
                    scope.launch {
                        try {
                            val result = FirebaseAppCheck.getInstance(firebaseApp).getToken(false).await()
                            appCheckStatus = if (result.token.isNotBlank()) {
                                "App Check conseguiu obter um token. Isso confirma a comunicação com o serviço de App Check, mas não garante que o token esteja aceito pela API Gemini."
                            } else {
                                "O serviço App Check retornou um token vazio."
                            }
                        } catch (e: Exception) {
                            val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
                            appCheckStatus = "Falha ao obter token App Check: $detail"
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
                Card(Modifier.fillMaxWidth()) {
                    Text(appCheckStatus, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            label = { Text("Descreva a falha ou dúvida técnica") },
            placeholder = { Text("Ex.: CFW700 apresenta F074 quando o puxador acelera...") }
        )
        Button(
            enabled = question.isNotBlank() && !loading && service != null,
            onClick = {
                loading = true
                error = ""
                answer = ""
                scope.launch {
                    try {
                        answer = requireNotNull(service).diagnose(question.trim())
                    } catch (e: Exception) {
                        val detail = e.localizedMessage
                            ?.takeIf { it.isNotBlank() }
                            ?: e.javaClass.simpleName
                        error = "Não foi possível consultar o Gemini. Detalhe técnico: $detail"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Consultar Gemini")
        }
        if (error.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(error, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        if (answer.isNotBlank()) {
            Text("Análise técnica", style = MaterialTheme.typography.titleLarge)
            Card(Modifier.fillMaxWidth()) { Text(answer, Modifier.padding(14.dp)) }
        }
        Text(
            "Segurança: use a análise como apoio. Confirme medições, diagramas e procedimentos do fabricante antes de intervir. Não trabalhe em circuitos energizados sem autorização e procedimento apropriado.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
