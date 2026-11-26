package com.rvtec.v2.data

import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Um par de pergunta/resposta usado para dar continuidade à conversa técnica. */
data class TechnicalAiTurn(val question: String, val answer: String)

/**
 * Assistente técnico RVTEC baseado em Firebase AI Logic.
 * A instância Firebase é a mesma inicializada no bootstrap do aplicativo.
 */
class TechnicalAiService(private val firebaseApp: FirebaseApp) {
    suspend fun diagnose(
        question: String,
        previousTurns: List<TechnicalAiTurn> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        require(question.isNotBlank()) { "Descreva a falha ou dúvida técnica." }

        val model = Firebase.ai(
            app = firebaseApp,
            backend = GenerativeBackend.googleAI()
        ).generativeModel("gemini-3.8-flash")

        val conversation = previousTurns.takeLast(12).joinToString("\n\n") { turn ->
            "Técnico: ${turn.question}\nRVTEC IA: ${turn.answer}"
        }.ifBlank { "Ainda não há mensagens anteriores nesta conversa." }

        val prompt = """
            Você é o assistente técnico do RVTEC 2.0, especializado em manutenção industrial
            e linhas de extrusão de filme plástico. Continue a conversa considerando o histórico
            abaixo. Não repita toda a resposta anterior; responda à nova pergunta e mantenha
            coerência com as informações já fornecidas pelo técnico.

            ESPECIALIDADES:
            Extrusora, puxador, bobinador, motores, inversores WEG CFW300/500/700/11,
            CLP Siemens/WEG, IHM, sensores PNP, PT100, encoders, sincronismo e comunicação industrial.

            PADRÃO DE RESPOSTA:
            - Responda em português brasileiro, de forma clara, objetiva e técnica.
            - Separe hipótese provável, verificações em ordem, medições necessárias e solução possível.
            - Quando faltarem dados, faça perguntas específicas antes de concluir.
            - Não invente parâmetros, códigos ou procedimentos de fabricante.
            - Diferencie fatos informados de hipóteses.
            - Nunca oriente intervenção energizada sem autorização, análise de risco e procedimento aplicável.
            - Considere NR-10 e NR-12 quando houver risco elétrico ou mecânico.

            HISTÓRICO DA CONVERSA:
            $conversation

            NOVA PERGUNTA:
            $question
        """.trimIndent()

        val response = model.generateContent(prompt)
        response.text?.takeIf { it.isNotBlank() }
            ?: error("A IA não retornou uma resposta. Tente novamente.")
    }
}
