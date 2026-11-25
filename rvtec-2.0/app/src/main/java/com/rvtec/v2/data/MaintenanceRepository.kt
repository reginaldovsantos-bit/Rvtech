package com.rvtec.v2.data

import android.content.Context
import com.rvtec.v2.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Repositório local persistente em SharedPreferences. */
class MaintenanceRepository(context: Context? = null) {
    private val prefs = context?.getSharedPreferences("rvtec_v2_data", Context.MODE_PRIVATE)
    private val machines = linkedMapOf<String, Machine>()
    private val stops = linkedMapOf<String, StopRecord>()
    private val orders = linkedMapOf<String, WorkOrder>()
    private val entries = linkedMapOf<String, ExecutionEntry>()
    private var restoreFailed = false

    init { restore() }

    private fun restore() {
        val raw = prefs?.getString("database", null) ?: return
        try {
            val root = JSONObject(raw)
            val ma = root.optJSONArray("machines") ?: JSONArray()
            for (i in 0 until ma.length()) {
                val o = ma.getJSONObject(i)
                val m = Machine(o.getString("id"), o.getString("name"), o.optString("code"), o.optString("sector"),
                    o.optDouble("productionKgPerHour"), MachineStatus.valueOf(o.optString("status", "RUNNING")),
                    o.optString("activeStopId").takeIf { it.isNotBlank() })
                machines[m.id] = m
            }
            val sa = root.optJSONArray("stops") ?: JSONArray()
            for (i in 0 until sa.length()) {
                val o = sa.getJSONObject(i)
                val s = StopRecord(o.getString("id"), o.getString("machineId"), o.getLong("startedAt"),
                    if (o.has("endedAt") && !o.isNull("endedAt")) o.getLong("endedAt") else null,
                    o.optString("cause"), o.optString("solution"), StopStatus.valueOf(o.optString("status", "OPEN")),
                    o.optString("workOrderId").takeIf { it.isNotBlank() })
                stops[s.id] = s
            }
            val oa = root.optJSONArray("orders") ?: JSONArray()
            for (i in 0 until oa.length()) {
                val o = oa.getJSONObject(i)
                val w = WorkOrder(o.getString("id"), o.getString("machineId"),
                    o.optString("stopId").takeIf { it.isNotBlank() }, o.getString("title"), o.optString("description"),
                    WorkOrderStatus.valueOf(o.optString("status", "OPEN")), o.getLong("createdAt"),
                    if (o.has("completedAt") && !o.isNull("completedAt")) o.getLong("completedAt") else null,
                    o.optBoolean("releasedMachine"))
                orders[w.id] = w
            }
            val ea = root.optJSONArray("entries") ?: JSONArray()
            for (i in 0 until ea.length()) {
                val o = ea.getJSONObject(i)
                val e = ExecutionEntry(o.getString("id"), o.getString("workOrderId"), o.getString("technician"),
                    o.getString("description"), o.optString("partsUsed"), o.optInt("durationMinutes"), o.getLong("createdAt"))
                entries[e.id] = e
            }
        } catch (_: Exception) {
            // Keep the original persisted payload untouched and block writes until recovery.
            machines.clear()
            stops.clear()
            orders.clear()
            entries.clear()
            restoreFailed = true
        }
    }

    /** Aplica a alteração em memória e persiste. Se a gravação falhar, restaura todos os mapas. */
    private inline fun <T> transaction(block: () -> T): T {
        check(!restoreFailed) {
            "Os dados locais estão inconsistentes e foram protegidos contra sobrescrita. É necessária uma recuperação antes de continuar."
        }
        val machineSnapshot = machines.toMap()
        val stopSnapshot = stops.toMap()
        val orderSnapshot = orders.toMap()
        val entrySnapshot = entries.toMap()
        return try {
            val result = block()
            persist()
            result
        } catch (failure: Exception) {
            machines.clear(); machines.putAll(machineSnapshot)
            stops.clear(); stops.putAll(stopSnapshot)
            orders.clear(); orders.putAll(orderSnapshot)
            entries.clear(); entries.putAll(entrySnapshot)
            throw failure
        }
    }

    private fun persist() {
        val p = prefs ?: return
        val root = JSONObject()
        fun arr(values: Collection<JSONObject>) = JSONArray().also { a -> values.forEach { a.put(it) } }
        root.put("machines", arr(machines.values.map { JSONObject().put("id",it.id).put("name",it.name).put("code",it.code).put("sector",it.sector).put("productionKgPerHour",it.productionKgPerHour).put("status",it.status.name).put("activeStopId",it.activeStopId ?: "") }))
        root.put("stops", arr(stops.values.map { JSONObject().put("id",it.id).put("machineId",it.machineId).put("startedAt",it.startedAt).put("endedAt",it.endedAt ?: JSONObject.NULL).put("cause",it.cause).put("solution",it.solution).put("status",it.status.name).put("workOrderId",it.workOrderId ?: "") }))
        root.put("orders", arr(orders.values.map { JSONObject().put("id",it.id).put("machineId",it.machineId).put("stopId",it.stopId ?: "").put("title",it.title).put("description",it.description).put("status",it.status.name).put("createdAt",it.createdAt).put("completedAt",it.completedAt ?: JSONObject.NULL).put("releasedMachine",it.releasedMachine) }))
        root.put("entries", arr(entries.values.map { JSONObject().put("id",it.id).put("workOrderId",it.workOrderId).put("technician",it.technician).put("description",it.description).put("partsUsed",it.partsUsed).put("durationMinutes",it.durationMinutes).put("createdAt",it.createdAt) }))
        check(p.edit().putString("database", root.toString()).commit()) {
            "Não foi possível salvar os dados de manutenção no aparelho. Verifique o espaço disponível e tente novamente."
        }
    }

    @Synchronized fun addMachine(machine: Machine) {
        require(machine.id.isNotBlank() && machine.name.isNotBlank())
        require(machine.productionKgPerHour >= 0)
        check(!machines.containsKey(machine.id)) { "Máquina já cadastrada." }
        transaction { machines[machine.id] = machine }
    }
    @Synchronized fun listMachines(): List<Machine> = machines.values.toList()
    @Synchronized fun listStops(): List<StopRecord> = stops.values.toList()
    @Synchronized fun listOrders(): List<WorkOrder> = orders.values.toList()
    @Synchronized fun listEntries(): List<ExecutionEntry> = entries.values.toList()

    @Synchronized fun registerStop(machineId: String, cause: String, now: Long = System.currentTimeMillis()): StopRecord {
        require(cause.isNotBlank()) { "Informe a causa da parada." }
        val machine = machines[machineId] ?: error("Máquina não encontrada.")
        check(machine.status == MachineStatus.RUNNING && machine.activeStopId == null) { "Máquina já está parada ou em manutenção." }
        val stop = StopRecord(UUID.randomUUID().toString(), machineId, now, cause = cause.trim())
        return transaction {
            stops[stop.id] = stop
            machines[machineId] = machine.copy(status = MachineStatus.STOPPED, activeStopId = stop.id)
            stop
        }
    }

    @Synchronized fun openWorkOrder(machineId: String, title: String, description: String = "", stopId: String? = null, now: Long = System.currentTimeMillis()): WorkOrder {
        require(title.isNotBlank()) { "Informe o título da OS." }
        val machine = machines[machineId] ?: error("Máquina não encontrada.")
        // OS independentes podem coexistir na mesma máquina.
        // A prevenção de duplicidade é feita pelo vínculo único da parada (stopId),
        // não por um bloqueio global de todas as OS da máquina.
        val stop = stopId?.let { stops[it] ?: error("Parada não encontrada.") }
        if (stop != null) {
            require(stop.machineId == machineId) { "A parada pertence a outra máquina." }
            require(stop.status != StopStatus.CLOSED) { "A parada já está encerrada." }
            require(stop.workOrderId == null) { "A parada já possui uma OS vinculada." }
            require(machine.status == MachineStatus.STOPPED && machine.activeStopId == stop.id) {
                "A máquina não está no estado de parada correspondente. Vínculo com OS bloqueado."
            }
        }
        val order = WorkOrder(UUID.randomUUID().toString(), machineId, stopId, title.trim(), description.trim(), createdAt = now)
        return transaction {
            orders[order.id] = order
            if (stop != null) stops[stop.id] = stop.copy(status = StopStatus.LINKED_TO_ORDER, workOrderId = order.id)
            order
        }
    }

    @Synchronized fun startWork(orderId: String): WorkOrder {
        val order = orders[orderId] ?: error("OS não encontrada.")
        check(order.status == WorkOrderStatus.OPEN || order.status == WorkOrderStatus.IN_PROGRESS)
        return transaction {
            order.copy(status = WorkOrderStatus.IN_PROGRESS).also { orders[orderId] = it }
        }
    }

    @Synchronized fun addExecution(orderId: String, technician: String, description: String, partsUsed: String = "", durationMinutes: Int = 0, now: Long = System.currentTimeMillis()): ExecutionEntry {
        val order = orders[orderId] ?: error("OS não encontrada.")
        check(order.status == WorkOrderStatus.IN_PROGRESS) { "Inicie a OS antes de apontar execução." }
        require(technician.isNotBlank() && description.isNotBlank())
        require(durationMinutes >= 0)
        return transaction {
            ExecutionEntry(UUID.randomUUID().toString(), orderId, technician.trim(), description.trim(), partsUsed.trim(), durationMinutes, now)
                .also { entries[it.id] = it }
        }
    }

    @Synchronized fun releaseMachine(machineId: String): Machine {
        val machine = machines[machineId] ?: error("Máquina não encontrada.")
        check(machine.status == MachineStatus.MAINTENANCE) { "Máquina não está aguardando liberação." }
        val activeStopId = machine.activeStopId
            ?: error("Não existe parada vinculada para validar a liberação.")
        val activeStop = stops[activeStopId]
            ?: error("A parada vinculada não foi encontrada. Liberação bloqueada.")
        check(activeStop.machineId == machineId) { "A parada vinculada pertence a outra máquina." }
        check(activeStop.status == StopStatus.CLOSED) { "A parada ainda não foi encerrada. Liberação bloqueada." }
        return transaction {
            machine.copy(status = MachineStatus.RUNNING, activeStopId = null).also { machines[machineId] = it }
        }
    }

    @Synchronized fun completeWork(orderId: String, solution: String, releaseMachine: Boolean, now: Long = System.currentTimeMillis()): WorkOrder {
        val order = orders[orderId] ?: error("OS não encontrada.")
        check(order.status == WorkOrderStatus.IN_PROGRESS) { "Somente OS em execução pode ser concluída." }
        require(now >= order.createdAt) { "O horário de conclusão não pode ser anterior à abertura da OS." }
        val stop = order.stopId?.let { stops[it] }
        require(!releaseMachine || stop != null) {
            "A máquina só pode ser liberada ao concluir uma OS vinculada a uma parada."
        }
        if (stop != null) {
            require(solution.isNotBlank()) { "Informe a solução para encerrar a parada." }
            require(stop.machineId == order.machineId) { "A parada pertence a outra máquina. Conclusão bloqueada." }
            require(stop.workOrderId == order.id && stop.status == StopStatus.LINKED_TO_ORDER) {
                "O vínculo entre a parada e esta OS não está íntegro. Conclusão bloqueada."
            }
            require(now >= stop.startedAt) { "O horário de conclusão não pode ser anterior ao início da parada." }
            val machine = machines[order.machineId] ?: error("Máquina não encontrada.")
            require(machine.activeStopId == stop.id && machine.status == MachineStatus.STOPPED) {
                "O estado atual da máquina não corresponde à parada desta OS. Conclusão bloqueada."
            }
        }
        return transaction {
            if (stop != null) {
                val machine = machines[order.machineId] ?: error("Máquina não encontrada.")
                stops[stop.id] = stop.copy(endedAt = now, solution = solution.trim(), status = StopStatus.CLOSED)
                machines[order.machineId] = machine.copy(status = if (releaseMachine) MachineStatus.RUNNING else MachineStatus.MAINTENANCE,
                    activeStopId = if (releaseMachine) null else stop.id)
            }
            order.copy(status = WorkOrderStatus.COMPLETED, completedAt = now, releasedMachine = releaseMachine)
                .also { orders[orderId] = it }
        }
    }
}
