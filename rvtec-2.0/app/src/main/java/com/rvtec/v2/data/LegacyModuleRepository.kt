package com.rvtec.v2.data

import android.content.Context
import com.rvtec.v2.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persistência local dos módulos recuperados da RVTEC legada.
 * A importação é explícita: preview nunca grava; commit valida e persiste.
 */
class LegacyModuleRepository(context: Context) {
    private val prefs = context.getSharedPreferences("rvtec_v2_legacy_modules", Context.MODE_PRIVATE)
    private val preventives = linkedMapOf<String, PreventivePlan>()
    private val diagnostics = linkedMapOf<String, DiagnosticRecord>()
    private val memories = linkedMapOf<String, TechnicalMemoryRecord>()
    private val stock = linkedMapOf<String, StockItem>()
    private val movements = linkedMapOf<String, StockMovement>()
    private val staff = linkedMapOf<String, StaffMember>()
    private val notifications = linkedMapOf<String, RvtecNotification>()
    private var settings = RvtecSettings()

    init { restore() }

    data class ImportPreview(
        val preventives: Int, val diagnostics: Int, val memories: Int,
        val stockItems: Int, val staff: Int, val notifications: Int
    )

    @Synchronized fun previewImport(data: LegacyNativeMapper.Mapped) = ImportPreview(
        data.preventives.size, data.diagnostics.size, data.technicalMemory.size,
        data.stock.size, data.staff.size, data.notifications.size
    )

    @Synchronized fun importLegacy(data: LegacyNativeMapper.Mapped): ImportPreview {
        val preview = previewImport(data)
        data.preventives.forEach { preventives[it.id.ifBlank { UUID.randomUUID().toString() }] = it }
        data.diagnostics.forEach { diagnostics[it.id.ifBlank { UUID.randomUUID().toString() }] = it }
        data.technicalMemory.forEach { memories[it.id.ifBlank { UUID.randomUUID().toString() }] = it }
        data.stock.forEach { stock[it.id.ifBlank { UUID.randomUUID().toString() }] = it }
        data.staff.forEach { staff[it.id.ifBlank { UUID.randomUUID().toString() }] = it }
        data.notifications.forEach { notifications[it.id.ifBlank { UUID.randomUUID().toString() }] = it }
        settings = data.settings
        persist()
        return preview
    }

    @Synchronized fun listPreventives() = preventives.values.toList()
    @Synchronized fun listDiagnostics() = diagnostics.values.sortedByDescending { it.createdAt }
    @Synchronized fun listMemories() = memories.values.sortedByDescending { it.updatedAt }
    @Synchronized fun listStock() = stock.values.toList()
    @Synchronized fun listMovements() = movements.values.sortedByDescending { it.createdAt }
    @Synchronized fun listStaff() = staff.values.toList()
    @Synchronized fun listNotifications() = notifications.values.sortedByDescending { it.createdAt }
    @Synchronized fun getSettings() = settings

    @Synchronized fun savePreventive(item: PreventivePlan) {
        require(item.machineId.isNotBlank() && item.title.isNotBlank())
        require(item.intervalDays > 0)
        preventives[item.id.ifBlank { UUID.randomUUID().toString() }] = item
        persist()
    }

    @Synchronized fun saveDiagnostic(item: DiagnosticRecord) {
        require(item.machineId.isNotBlank() && item.title.isNotBlank())
        diagnostics[item.id.ifBlank { UUID.randomUUID().toString() }] = item
        persist()
    }

    @Synchronized fun saveMemory(item: TechnicalMemoryRecord) {
        require(item.title.isNotBlank() && item.content.isNotBlank())
        memories[item.id.ifBlank { UUID.randomUUID().toString() }] = item
        persist()
    }

    @Synchronized fun saveStaff(item: StaffMember) {
        require(item.name.isNotBlank())
        require(item.role != StaffRole.SUPERVISOR || item.email.isNotBlank()) {
            "Supervisor precisa ter e-mail cadastrado."
        }
        staff[item.id.ifBlank { UUID.randomUUID().toString() }] = item
        persist()
    }

    @Synchronized fun saveSettings(value: RvtecSettings) {
        settings = value
        persist()
    }

    @Synchronized fun addStock(item: StockItem) {
        require(item.name.isNotBlank())
        require(item.quantity >= 0 && item.minimumQuantity >= 0)
        val id = item.id.ifBlank { UUID.randomUUID().toString() }
        check(!stock.containsKey(id)) { "Peça já cadastrada." }
        stock[id] = item.copy(id = id)
        persist()
    }

    @Synchronized fun moveStock(
        itemId: String, type: StockMovementType, quantity: Double,
        reason: String, workOrderId: String? = null,
        responsible: String = "", now: Long = System.currentTimeMillis()
    ): StockMovement {
        require(quantity > 0 && quantity.isFinite()) { "Quantidade deve ser maior que zero." }
        val item = stock[itemId] ?: error("Peça não encontrada.")
        require(reason.isNotBlank()) { "Informe o motivo da movimentação." }
        val balance = when(type) {
            StockMovementType.ENTRY -> item.quantity + quantity
            StockMovementType.EXIT -> item.quantity - quantity
            StockMovementType.ADJUSTMENT -> quantity
        }
        require(balance >= 0) { "Saldo insuficiente no estoque." }
        stock[itemId] = item.copy(quantity = balance)
        val movement = StockMovement(UUID.randomUUID().toString(), itemId, type, quantity,
            reason.trim(), workOrderId, now, responsible.trim())
        movements[movement.id] = movement
        persist()
        return movement
    }

    private fun restore() {
        val root = runCatching { JSONObject(prefs.getString("database", "{}") ?: "{}") }.getOrNull() ?: return
        fun array(key: String, block: (JSONObject) -> Unit) {
            val a = root.optJSONArray(key) ?: JSONArray()
            for (i in 0 until a.length()) runCatching { a.getJSONObject(i).let(block) }
        }
        array("stock") { o ->
            val item = StockItem(o.getString("id"), o.optString("code"), o.getString("name"),
                o.optString("description"), o.optString("unit", "un"), o.optDouble("quantity"),
                o.optDouble("minimumQuantity"), o.optString("location"))
            stock[item.id] = item
        }
        array("movements") { o ->
            val m = StockMovement(o.getString("id"), o.getString("itemId"),
                StockMovementType.valueOf(o.getString("type")), o.getDouble("quantity"),
                o.optString("reason"), o.optString("workOrderId").takeIf { it.isNotBlank() },
                o.getLong("createdAt"), o.optString("responsible"))
            movements[m.id] = m
        }
        array("preventives") { o ->
            val p = PreventivePlan(o.getString("id"), o.optString("machineId"), o.optString("title"),
                o.optString("description"), o.optInt("intervalDays",30),
                o.takeIf { it.has("lastPerformedAt") && !it.isNull("lastPerformedAt") }?.optLong("lastPerformedAt"),
                o.takeIf { it.has("nextDueAt") && !it.isNull("nextDueAt") }?.optLong("nextDueAt"),
                o.optBoolean("active",true))
            preventives[p.id] = p
        }
        array("diagnostics") { o ->
            val d = DiagnosticRecord(o.getString("id"),o.optString("machineId"),o.optString("title"),
                o.optString("symptoms"),o.optString("probableCauses"),o.optString("checks"),
                o.optString("solution"),o.optLong("createdAt"),o.optString("technician"))
            diagnostics[d.id] = d
        }
        array("memories") { o ->
            val m = TechnicalMemoryRecord(o.getString("id"),o.optString("machineId").takeIf { it.isNotBlank() },
                o.optString("title"),o.optString("content"),
                (o.optJSONArray("tags") ?: JSONArray()).let { a -> (0 until a.length()).map { a.optString(it) } },
                o.optLong("createdAt"),o.optLong("updatedAt"))
            memories[m.id] = m
        }
        array("staff") { o ->
            val s = StaffMember(o.getString("id"),o.optString("name"),o.optString("email"),
                runCatching { StaffRole.valueOf(o.optString("role","TECHNICIAN")) }.getOrDefault(StaffRole.TECHNICIAN),
                o.optBoolean("active",true))
            staff[s.id] = s
        }
        array("notifications") { o ->
            val n = RvtecNotification(o.getString("id"),o.optString("title"),o.optString("message"),
                o.optLong("createdAt"),o.optBoolean("read"),o.optString("relatedMachineId").ifBlank { null })
            notifications[n.id] = n
        }
        root.optJSONObject("settings")?.let { o ->
            settings = RvtecSettings(o.optString("companyName"),
                o.optString("shift1Start","06:30"),o.optString("shift1End","14:30"),
                o.optString("shift2Start","14:30"),o.optString("shift2End","22:30"),
                o.optString("shift3Start","22:30"),o.optString("shift3End","06:30"),
                o.optBoolean("darkTheme"),
                (o.optJSONArray("enabledScreens") ?: JSONArray()).let { a -> (0 until a.length()).map { a.optString(it) }.toSet() })
        }
    }

    private fun persist() {
        fun <T> json(values: Collection<T>, encode: (T) -> JSONObject) =
            JSONArray().also { a -> values.forEach { a.put(encode(it)) } }
        val root = JSONObject()
        root.put("stock", json(stock.values) { JSONObject().put("id",it.id).put("code",it.code).put("name",it.name)
            .put("description",it.description).put("unit",it.unit).put("quantity",it.quantity)
            .put("minimumQuantity",it.minimumQuantity).put("location",it.location) })
        root.put("movements", json(movements.values) { JSONObject().put("id",it.id).put("itemId",it.itemId)
            .put("type",it.type.name).put("quantity",it.quantity).put("reason",it.reason)
            .put("workOrderId",it.workOrderId ?: JSONObject.NULL).put("createdAt",it.createdAt).put("responsible",it.responsible) })
        root.put("preventives", json(preventives.values) { JSONObject().put("id",it.id).put("machineId",it.machineId)
            .put("title",it.title).put("description",it.description).put("intervalDays",it.intervalDays)
            .put("lastPerformedAt",it.lastPerformedAt ?: JSONObject.NULL).put("nextDueAt",it.nextDueAt ?: JSONObject.NULL).put("active",it.active) })
        root.put("diagnostics", json(diagnostics.values) { JSONObject().put("id",it.id).put("machineId",it.machineId)
            .put("title",it.title).put("symptoms",it.symptoms).put("probableCauses",it.probableCauses)
            .put("checks",it.checks).put("solution",it.solution).put("createdAt",it.createdAt).put("technician",it.technician) })
        root.put("memories", json(memories.values) { m ->
            JSONObject().put("id",m.id).put("machineId",m.machineId ?: JSONObject.NULL).put("title",m.title)
                .put("content",m.content).put("tags",JSONArray(m.tags)).put("createdAt",m.createdAt).put("updatedAt",m.updatedAt)
        })
        root.put("staff", json(staff.values) { JSONObject().put("id",it.id).put("name",it.name).put("email",it.email)
            .put("role",it.role.name).put("active",it.active) })
        root.put("notifications", json(notifications.values) { JSONObject().put("id",it.id).put("title",it.title)
            .put("message",it.message).put("createdAt",it.createdAt).put("read",it.read).put("relatedMachineId",it.relatedMachineId ?: JSONObject.NULL) })
        root.put("settings", JSONObject().put("companyName",settings.companyName)
            .put("shift1Start",settings.shift1Start).put("shift1End",settings.shift1End)
            .put("shift2Start",settings.shift2Start).put("shift2End",settings.shift2End)
            .put("shift3Start",settings.shift3Start).put("shift3End",settings.shift3End)
            .put("darkTheme",settings.darkTheme).put("enabledScreens",JSONArray(settings.enabledScreens.toList())))
        check(prefs.edit().putString("database",root.toString()).commit()) { "Falha ao salvar módulos legados." }
    }
}
