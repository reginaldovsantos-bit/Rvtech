package com.rvtec.v2.data

import com.rvtec.v2.domain.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Conversor tolerante do backup JSON legado para os modelos Kotlin da 2.0.
 * Não grava dados: a camada de revisão/confirmação deve decidir quando importar.
 */
object LegacyNativeMapper {
    data class Mapped(
        val preventives: List<PreventivePlan>,
        val diagnostics: List<DiagnosticRecord>,
        val technicalMemory: List<TechnicalMemoryRecord>,
        val stock: List<StockItem>,
        val staff: List<StaffMember>,
        val notifications: List<RvtecNotification>,
        val settings: RvtecSettings
    )

    fun map(root: JSONObject): Mapped = Mapped(
        preventives = root.array("preventives").map { o ->
            PreventivePlan(
                id = o.id(),
                machineId = o.string("machineId", "machine_id"),
                title = o.string("title", "name", "description").ifBlank { "Preventiva" },
                description = o.string("description", "details"),
                intervalDays = o.int(30, "intervalDays", "interval", "frequencyDays").coerceAtLeast(1),
                lastPerformedAt = o.longOrNull("lastPerformedAt", "lastDoneAt"),
                nextDueAt = o.longOrNull("nextDueAt", "dueAt"),
                active = o.boolean(true, "active", "enabled")
            )
        },
        diagnostics = root.array("diagnostics", "failures").map { o ->
            DiagnosticRecord(
                id = o.id(),
                machineId = o.string("machineId", "machine_id"),
                title = o.string("title", "name", "failure").ifBlank { "Diagnóstico" },
                symptoms = o.string("symptoms", "problem", "description"),
                probableCauses = o.string("probableCauses", "causes"),
                checks = o.string("checks", "tests", "verification"),
                solution = o.string("solution", "fix", "resolution"),
                createdAt = o.long(0L, "createdAt", "timestamp", "date"),
                technician = o.string("technician", "author", "user")
            )
        },
        technicalMemory = root.array("technicalMemory").map { o ->
            TechnicalMemoryRecord(
                id = o.id(),
                machineId = o.string("machineId", "machine_id").ifBlank { null },
                title = o.string("title", "name").ifBlank { "Memória técnica" },
                content = o.string("content", "text", "description", "notes"),
                tags = o.string("tags").split(',', ';').map { it.trim() }.filter { it.isNotBlank() },
                createdAt = o.long(0L, "createdAt", "timestamp"),
                updatedAt = o.long(o.long(0L, "createdAt", "timestamp"), "updatedAt")
            )
        },
        stock = root.array("stock").map { o ->
            StockItem(
                id = o.id(),
                code = o.string("code", "sku", "partNumber"),
                name = o.string("name", "title", "description").ifBlank { "Item sem nome" },
                description = o.string("details", "description"),
                unit = o.string("unit", "measure").ifBlank { "un" },
                quantity = o.double(0.0, "quantity", "qty", "balance").coerceAtLeast(0.0),
                minimumQuantity = o.double(0.0, "minimumQuantity", "minStock", "minimum").coerceAtLeast(0.0),
                location = o.string("location", "shelf", "warehouse")
            )
        },
        staff = root.array("staff").map { o ->
            val roleText = o.string("role", "profile", "type").lowercase()
            val role = when {
                "super" in roleText || "admin" in roleText -> StaffRole.SUPERVISOR
                "produc" in roleText -> StaffRole.PRODUCTION
                else -> StaffRole.TECHNICIAN
            }
            StaffMember(
                id = o.id(),
                name = o.string("name", "displayName", "user").ifBlank { "Usuário" },
                email = o.string("email"),
                role = role,
                active = o.boolean(true, "active", "enabled")
            )
        },
        notifications = root.array("notifications").map { o ->
            RvtecNotification(
                id = o.id(),
                title = o.string("title", "type").ifBlank { "Notificação" },
                message = o.string("message", "body", "description"),
                createdAt = o.long(0L, "createdAt", "timestamp", "date"),
                read = o.boolean(false, "read", "seen"),
                relatedMachineId = o.string("machineId", "machine_id").ifBlank { null }
            )
        },
        settings = root.optJSONObject("settings")?.let { o ->
            RvtecSettings(
                companyName = o.string("companyName", "company", "name"),
                shift1Start = o.string("shift1Start", "turno1Inicio").ifBlank { "06:30" },
                shift1End = o.string("shift1End", "turno1Fim").ifBlank { "14:30" },
                shift2Start = o.string("shift2Start", "turno2Inicio").ifBlank { "14:30" },
                shift2End = o.string("shift2End", "turno2Fim").ifBlank { "22:30" },
                shift3Start = o.string("shift3Start", "turno3Inicio").ifBlank { "22:30" },
                shift3End = o.string("shift3End", "turno3Fim").ifBlank { "06:30" },
                darkTheme = o.boolean(false, "darkTheme", "darkMode"),
                enabledScreens = o.optJSONArray("enabledScreens").toStringSet()
            )
        } ?: RvtecSettings()
    )

    private fun JSONObject.array(vararg keys: String): List<JSONObject> {
        val value = keys.firstNotNullOfOrNull { key -> optJSONArray(key) } ?: JSONArray()
        return (0 until value.length()).mapNotNull { value.optJSONObject(it) }
    }
    private fun JSONObject.id(): String = string("id", "uid", "key").ifBlank { "" }
    private fun JSONObject.string(vararg keys: String): String =
        keys.firstNotNullOfOrNull { key ->
            if (!has(key) || isNull(key)) null else opt(key)?.toString()
        } ?: ""
    private fun JSONObject.int(default: Int, vararg keys: String): Int =
        keys.firstNotNullOfOrNull { key -> if (has(key)) optInt(key, default) else null } ?: default
    private fun JSONObject.long(default: Long, vararg keys: String): Long =
        keys.firstNotNullOfOrNull { key -> if (has(key)) optLong(key, default) else null } ?: default
    private fun JSONObject.longOrNull(vararg keys: String): Long? =
        keys.firstNotNullOfOrNull { key -> if (has(key) && !isNull(key)) optLong(key) else null }
    private fun JSONObject.double(default: Double, vararg keys: String): Double =
        keys.firstNotNullOfOrNull { key -> if (has(key)) optDouble(key, default) else null } ?: default
    private fun JSONObject.boolean(default: Boolean, vararg keys: String): Boolean =
        keys.firstNotNullOfOrNull { key -> if (has(key)) optBoolean(key, default) else null } ?: default
    private fun JSONArray?.toStringSet(): Set<String> =
        this?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) }.toSet() } ?: emptySet()
}
