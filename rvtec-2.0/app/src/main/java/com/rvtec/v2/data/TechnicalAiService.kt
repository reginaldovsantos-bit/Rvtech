package com.rvtec.v2.data

import com.google.firebase.Firebase
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TechnicalChatMessage(
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Assistente técnico RVTEC baseado em Firebase AI Logic.
 * Mantém o contexto da conversa única fornecido pela tela.
 */
class TechnicalAiService {
    suspend fun diagnose(
        question: String,
        history: List<TechnicalChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        require(question.isNotBlank()) { "Descreva a falha ou dúvida técnica." }
        // A tela já adiciona a pergunta atual ao histórico antes da chamada.
        // Removê-la daqui evita enviar a mesma mensagem duas vezes no prompt.
        val previousMessages = history
            .dropLast(1)
            .takeLast(30)
        val conversationContext = previousMessages.joinToString("\n") { message ->
            val speaker = if (message.role == "user") "Técnico" else "Assistente RVTEC"
            "$speaker: ${message.text}"
        }
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-4-hydrogen")
        val prompt = """
            Você é o assistente técnico do RVTEC, especializado em manutenção industrial
            e linhas de extrusão de filme plástico. Responda em português brasileiro,
            de forma objetiva e técnica.
            Priorize extrusora, puxador, bobinador, motores, inversores WEG CFW,
            CLP Siemens/WEG, IHM, sensores PNP, PT100, encoders, sincronismo,
            comunicação industrial e diagnóstico de falhas.
            Estruture as respostas em: hipótese provável, verificações em ordem,
            medições necessárias, solução possível e cuidados de segurança.
            Nunca recomende trabalhar energizado sem procedimento autorizado.
            Se faltarem dados, indique exatamente quais dados são necessários.
            Não invente parâmetros de fabricante.

            HISTÓRICO DA CONVERSA ÚNICA:
            ${conversationContext.ifBlank { "Início da conversa." }}

            NOVA MENSAGEM:
            $question
        """.trimIndent()
        val response = model.generateContent(prompt)
        response.text?.takeIf { it.isNotBlank() }
            ?: error("A IA não retornou uma resposta. Tente novamente.")
    }
}
