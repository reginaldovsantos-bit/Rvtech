package com.rvtec.v2.data

import org.json.JSONArray
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
        } catch (_: Exception) {
            return Report(false, emptyMap(), listOf("Arquivo não contém JSON válido."))
        }

        val knownFieldsPresent = arrayFields.any { root.has(it) } || root.has("settings")
        if (root.length() == 0 || !knownFieldsPresent) {
            errors += "Backup vazio ou sem campos reconhecidos da RVTEC 1.8.5."
        }

        arrayFields.forEach { field ->
            val value = root.opt(field)
            when {
                value == null || value == JSONObject.NULL -> counts[field] = 0
                value is JSONArray -> {
                    counts[field] = value.length()
                    validateUniqueIds(field, value, errors)
                }
                else -> errors += "Campo '$field' deveria ser uma lista."
            }
        }

        val settings = root.opt("settings")
        if (settings != null && settings != JSONObject.NULL && settings !is JSONObject) {
            errors += "Campo 'settings' deveria ser um objeto."
        }

        return Report(errors.isEmpty(), counts, errors)
    }

    private fun validateUniqueIds(field: String, records: JSONArray, errors: MutableList<String>) {
        val seen = mutableSetOf<String>()
        for (index in 0 until records.length()) {
            val record = records.optJSONObject(index) ?: continue
            val id = record.opt("id") as? String ?: continue
            if (id.isBlank()) continue
            if (!seen.add(id)) {
                errors += "Identificador duplicado '$id' na coleção '$field'."
            }
        }
    }
}
