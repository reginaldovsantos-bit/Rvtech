package com.rvtec.v2.data

import com.google.firebase.Firebase
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Assistente técnico RVTEC baseado em Firebase AI Logic.
 * Requer google-services.json registrado para o applicationId da versão instalada
 * e Firebase AI Logic habilitado no projeto.
 */
class TechnicalAiService {
    suspend fun diagnose(question: String): String = withContext(Dispatchers.IO) {
        require(question.isNotBlank()) { "Descreva a falha ou dúvida técnica." }
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")
        val prompt = """
            Você é o assistente técnico do RVTEC, especializado em manutenção industrial
            e linhas de extrusão de filme plástico. Responda em português brasileiro,
            de forma objetiva e técnica.
            Priorize extrusora, puxador, bobinador, motores, inversores WEG CFW,
            CLP Siemens/WEG, IHM, sensores PNP, PT100, encoders, sincronismo,
            comunicação industrial e diagnóstico de falhas.
            Estruture a resposta em: hipótese provável, verificações em ordem,
            medições necessárias, solução possível e cuidados de segurança.
            Nunca recomende trabalhar energizado sem procedimento autorizado.
            Se faltarem dados, indique exatamente quais dados são necessários.
            Não invente parâmetros de fabricante.
            Dúvida/ocorrência: $question
        """.trimIndent()
        val response = model.generateContent(prompt)
        response.text?.takeIf { it.isNotBlank() }
            ?: error("A IA não retornou uma resposta. Tente novamente.")
    }
}
