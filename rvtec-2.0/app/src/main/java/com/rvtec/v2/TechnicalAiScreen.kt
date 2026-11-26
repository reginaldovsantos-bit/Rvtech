package com.rvtec.v2

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rvtec.v2.data.TechnicalAiService
import com.rvtec.v2.data.TechnicalChatMessage
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun TechnicalAiScreen() {
    val context = LocalContext.current.applicationContext
    val preferences = remember {
        context.getSharedPreferences("rvtec_ai_conversation", Context.MODE_PRIVATE)
    }
    val messages = remember {
        mutableStateListOf<TechnicalChatMessage>().apply {
            addAll(loadConversation(preferences))
        }
    }
    var question by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val service = remember { TechnicalAiService() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Memória Técnica IA", style = MaterialTheme.typography.headlineSmall)
        Text("Conversa única e contínua para diagnóstico de extrusoras, puxadores, bobinadores, motores, inversores WEG, CLP, IHM, sensores e encoders.")

        if (messages.isEmpty()) {
            Text(
                "Comece descrevendo uma falha ou dúvida. O histórico ficará salvo nesta conversa.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        messages.forEach { message ->
            val isUser = message.role == "user"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (isUser) "Você" else "RVTEC IA",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(message.text)
                }
            }
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text("Nova mensagem") },
            placeholder = { Text("Ex.: CFW700 apresenta F074 quando o puxador acelera...") }
        )
        Button(
            enabled = question.isNotBlank() && !loading,
            onClick = {
                val submitted = question.trim()
                question = ""
                error = ""
                val userMessage = TechnicalChatMessage(role = "user", text = submitted)
                messages.add(userMessage)
                saveConversation(preferences, messages)
                val contextForRequest = messages.toList()
                loading = true
                scope.launch {
                    try {
                        val answer = service.diagnose(submitted, contextForRequest)
                        messages.add(TechnicalChatMessage(role = "assistant", text = answer))
                        saveConversation(preferences, messages)
                    } catch (e: Exception) {
                        val detail = e.localizedMessage
                            ?.takeIf { it.isNotBlank() }
                            ?: e.javaClass.simpleName
                        val normalized = detail.lowercase()
                        error = when {
                            "app check" in normalized || "appcheck" in normalized ->
                                "O Firebase recusou o token App Check. Confirme que o token deste aparelho está cadastrado no app Android RVTEC 2.0 (com.rvtec.manutencao.v2), e não no app antigo. Detalhe: $detail"
                            "unauthenticated" in normalized || "unauthorized" in normalized ->
                                "Falha de autenticação/autorização na chamada ao Gemini. Detalhe: $detail"
                            else ->
                                "Não foi possível consultar o Gemini. Verifique a conexão e o Firebase AI Logic. Detalhe: $detail"
                        }
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Enviar mensagem")
        }
        if (error.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(error, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Text(
            "Segurança: use a análise como apoio. Confirme medições, diagramas e procedimentos do fabricante antes de intervir. Não trabalhe em circuitos energizados sem autorização e procedimento apropriado.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun loadConversation(preferences: android.content.SharedPreferences): List<TechnicalChatMessage> {
    val raw = preferences.getString("messages", null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TechnicalChatMessage(
                role = item.optString("role", "assistant"),
                text = item.optString("text"),
                timestamp = item.optLong("timestamp")
            )
        }.filter { it.text.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveConversation(
    preferences: android.content.SharedPreferences,
    messages: List<TechnicalChatMessage>
) {
    val array = JSONArray()
    messages.forEach { message ->
        array.put(
            JSONObject()
                .put("role", message.role)
                .put("text", message.text)
                .put("timestamp", message.timestamp)
        )
    }
    preferences.edit().putString("messages", array.toString()).apply()
}
