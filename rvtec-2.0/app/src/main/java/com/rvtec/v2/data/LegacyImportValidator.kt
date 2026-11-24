package com.rvtec.v2.data

import org.json.JSONObject

/**
 * Valida um backup JSON exportado da RVTEC 1.8.5 sem gravar ou alterar dados.
 * A importação efetiva só deve ocorrer depois da tela de conferência e backup.
 */
object LegacyImportValidator {
    private val arrayFields = listOf(
        "machines", "stops", "orders", "preventives", "diagnostics",
        "technicalMemory", "failures", "staff", "stock",
        "stopDeletions", "notifications"
    )

    data class Report(
        val valid: Boolean,
        val counts: Map<String, Int>,
        val errors: List<String>
    )

    fun validate(json: String): Report {
        val errors = mutableListOf<String>()
        val counts = linkedMapOf<String, Int>()
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            return Report(false, emptyMap(), listOf("Arquivo não contém JSON válido."))
        }

        arrayFields.forEach { field ->
            val value = root.opt(field)
            when {
                value == null || value == JSONObject.NULL ->
                    counts[field] = 0
                value is org.json.JSONArray ->
                    counts[field] = value.length()
                else -> errors += "Campo '$field' deveria ser uma lista."
            }
        }

        val settings = root.opt("settings")
        if (settings != null && settings != JSONObject.NULL && settings !is JSONObject) {
            errors += "Campo 'settings' deveria ser um objeto."
        }

        if (root.length() == 0) errors += "Backup vazio."
        return Report(errors.isEmpty(), counts, errors)
    }
}
