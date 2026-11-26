package com.rvtec.v2.domain

enum class MachineStatus { RUNNING, STOPPED, MAINTENANCE }

data class Machine(
    val id: String,
    val name: String,
    val code: String = "",
    val sector: String = "",
    val productionKgPerHour: Double = 0.0,
    val status: MachineStatus = MachineStatus.RUNNING,
    val activeStopId: String? = null
)

enum class StopStatus { OPEN, LINKED_TO_ORDER, CLOSED }

data class StopRecord(
    val id: String,
    val machineId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val cause: String = "",
    val solution: String = "",
    val status: StopStatus = StopStatus.OPEN,
    val workOrderId: String? = null
)

enum class WorkOrderStatus { OPEN, IN_PROGRESS, COMPLETED, CANCELLED }

data class WorkOrder(
    val id: String,
    val machineId: String,
    val stopId: String? = null,
    val title: String,
    val description: String = "",
    val status: WorkOrderStatus = WorkOrderStatus.OPEN,
    val createdAt: Long,
    val completedAt: Long? = null,
    val releasedMachine: Boolean = false
)

data class ExecutionEntry(
    val id: String,
    val workOrderId: String,
    val technician: String,
    val description: String,
    val partsUsed: String = "",
    val durationMinutes: Int = 0,
    val createdAt: Long
)
