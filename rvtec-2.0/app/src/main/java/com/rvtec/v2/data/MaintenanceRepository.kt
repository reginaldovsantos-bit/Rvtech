package com.rvtec.v2.data

import com.rvtec.v2.domain.*
import java.util.UUID

/**
 * Núcleo transacional em memória da primeira etapa.
 * A persistência local e Firestore serão conectadas por interface sem mudar as regras.
 */
class MaintenanceRepository {
    private val machines = linkedMapOf<String, Machine>()
    private val stops = linkedMapOf<String, StopRecord>()
    private val orders = linkedMapOf<String, WorkOrder>()
    private val entries = linkedMapOf<String, ExecutionEntry>()

    @Synchronized fun addMachine(machine: Machine) {
        require(machine.id.isNotBlank() && machine.name.isNotBlank())
        require(machine.productionKgPerHour >= 0)
        check(!machines.containsKey(machine.id)) { "Máquina já cadastrada." }
        machines[machine.id] = machine
    }

    @Synchronized fun listMachines(): List<Machine> = machines.values.toList()
    @Synchronized fun listStops(): List<StopRecord> = stops.values.toList()
    @Synchronized fun listOrders(): List<WorkOrder> = orders.values.toList()
    @Synchronized fun listEntries(): List<ExecutionEntry> = entries.values.toList()

    @Synchronized fun registerStop(machineId: String, cause: String, now: Long = System.currentTimeMillis()): StopRecord {
        val machine = machines[machineId] ?: error("Máquina não encontrada.")
        check(machine.status == MachineStatus.RUNNING && machine.activeStopId == null) {
            "Máquina já está parada ou em manutenção."
        }
        val stop = StopRecord(UUID.randomUUID().toString(), machineId, now, cause = cause.trim())
        stops[stop.id] = stop
        machines[machineId] = machine.copy(status = MachineStatus.STOPPED, activeStopId = stop.id)
        return stop
    }

    @Synchronized fun openWorkOrder(machineId: String, title: String, description: String = "", stopId: String? = null, now: Long = System.currentTimeMillis()): WorkOrder {
        require(title.isNotBlank()) { "Informe o título da OS." }
        val machine = machines[machineId] ?: error("Máquina não encontrada.")
        val stop = stopId?.let { stops[it] ?: error("Parada não encontrada.") }
        if (stop != null) {
            require(stop.machineId == machineId) { "A parada pertence a outra máquina." }
            require(stop.status != StopStatus.CLOSED) { "A parada já está encerrada." }
            require(stop.workOrderId == null) { "A parada já possui uma OS vinculada." }
        }
        val order = WorkOrder(UUID.randomUUID().toString(), machineId, stopId, title.trim(), description.trim(), createdAt = now)
        orders[order.id] = order
        if (stop != null) stops[stop.id] = stop.copy(status = StopStatus.LINKED_TO_ORDER, workOrderId = order.id)
        if (machine.status == MachineStatus.RUNNING && stop == null) {
            // OS preventiva/corretiva sem parada não altera estado operacional.
        }
        return order
    }

    @Synchronized fun startWork(orderId: String): WorkOrder {
        val order = orders[orderId] ?: error("OS não encontrada.")
        check(order.status == WorkOrderStatus.OPEN || order.status == WorkOrderStatus.IN_PROGRESS)
        return order.copy(status = WorkOrderStatus.IN_PROGRESS).also { orders[orderId] = it }
    }

    @Synchronized fun addExecution(orderId: String, technician: String, description: String, partsUsed: String = "", durationMinutes: Int = 0, now: Long = System.currentTimeMillis()): ExecutionEntry {
        val order = orders[orderId] ?: error("OS não encontrada.")
        check(order.status == WorkOrderStatus.IN_PROGRESS) { "Inicie a OS antes de apontar execução." }
        require(technician.isNotBlank() && description.isNotBlank())
        require(durationMinutes >= 0)
        return ExecutionEntry(UUID.randomUUID().toString(), orderId, technician.trim(), description.trim(), partsUsed.trim(), durationMinutes, now)
            .also { entries[it.id] = it }
    }

    @Synchronized fun completeWork(orderId: String, solution: String, releaseMachine: Boolean, now: Long = System.currentTimeMillis()): WorkOrder {
        val order = orders[orderId] ?: error("OS não encontrada.")
        check(order.status == WorkOrderStatus.IN_PROGRESS) { "Somente OS em execução pode ser concluída." }
        val stop = order.stopId?.let { stops[it] }
        if (stop != null) {
            require(solution.isNotBlank()) { "Informe a solução para encerrar a parada." }
            val machine = machines[order.machineId] ?: error("Máquina não encontrada.")
            stops[stop.id] = stop.copy(endedAt = now, solution = solution.trim(), status = StopStatus.CLOSED)
            machines[order.machineId] = machine.copy(
                status = if (releaseMachine) MachineStatus.RUNNING else MachineStatus.MAINTENANCE,
                activeStopId = if (releaseMachine) null else stop.id
            )
        }
        return order.copy(status = WorkOrderStatus.COMPLETED, completedAt = now, releasedMachine = releaseMachine)
            .also { orders[orderId] = it }
    }
}
