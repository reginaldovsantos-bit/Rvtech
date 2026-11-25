package com.rvtec.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rvtec.v2.data.TechnicalAiService
import kotlinx.coroutines.launch

@Composable
fun TechnicalAiScreen() {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val service = remember { TechnicalAiService() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Memória Técnica IA", style = MaterialTheme.typography.headlineSmall)
        Text("Assistente de diagnóstico para extrusoras, puxadores, bobinadores, motores, inversores WEG, CLP, IHM, sensores e encoders.")
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            label = { Text("Descreva a falha ou dúvida técnica") },
            placeholder = { Text("Ex.: CFW700 apresenta F074 quando o puxador acelera...") }
        )
        Button(
            enabled = question.isNotBlank() && !loading,
            onClick = {
                loading = true
                error = ""
                answer = ""
                scope.launch {
                    try {
                        answer = service.diagnose(question.trim())
                    } catch (e: Exception) {
                        val detail = e.localizedMessage
                            ?.takeIf { it.isNotBlank() }
                            ?: e.javaClass.simpleName
                        val normalized = detail.lowercase()
                        error = when {
                            "app check" in normalized || "appcheck" in normalized ->
                                "O Firebase recusou o token App Check. Confirme em Firebase Console > App Check > Apps que o token de depuração deste aparelho está cadastrado no aplicativo Android RVTEC 2.0 (com.rvtec.manutencao.v2), e não no app antigo. Depois feche e abra o RVTEC 2.0 e tente novamente. Detalhe: $detail"
                            "unauthenticated" in normalized || "unauthorized" in normalized ->
                                "Falha de autenticação/autorização na chamada ao Gemini. Verifique o estado do Firebase AI Logic e as credenciais do projeto. Detalhe: $detail"
                            else ->
                                "Não foi possível consultar o Gemini. Verifique a conexão e o serviço Firebase AI Logic. Detalhe técnico: $detail"
                        }
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
