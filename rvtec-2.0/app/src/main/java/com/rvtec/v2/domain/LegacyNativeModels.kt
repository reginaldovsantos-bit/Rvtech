package com.rvtec.v2.domain

/**
 * Modelos nativos para os módulos existentes na RVTEC 1.8.5.
 * Os campos legados são preservados para permitir migração sem perda.
 */

data class PreventivePlan(
    val id: String,
    val machineId: String,
    val title: String,
    val description: String = "",
    val intervalDays: Int = 30,
    val lastPerformedAt: Long? = null,
    val nextDueAt: Long? = null,
    val active: Boolean = true
)

data class DiagnosticRecord(
    val id: String,
    val machineId: String,
    val title: String,
    val symptoms: String = "",
    val probableCauses: String = "",
    val checks: String = "",
    val solution: String = "",
    val createdAt: Long,
    val technician: String = ""
)

data class TechnicalMemoryRecord(
    val id: String,
    val machineId: String? = null,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long = createdAt
)

enum class StockMovementType { ENTRY, EXIT, ADJUSTMENT }

data class StockItem(
    val id: String,
    val code: String = "",
    val name: String,
    val description: String = "",
    val unit: String = "un",
    val quantity: Double = 0.0,
    val minimumQuantity: Double = 0.0,
    val location: String = ""
)

data class StockMovement(
    val id: String,
    val itemId: String,
    val type: StockMovementType,
    val quantity: Double,
    val reason: String = "",
    val workOrderId: String? = null,
    val createdAt: Long,
    val responsible: String = ""
)

enum class StaffRole { TECHNICIAN, PRODUCTION, SUPERVISOR }

data class StaffMember(
    val id: String,
    val name: String,
    val email: String = "",
    val role: StaffRole = StaffRole.TECHNICIAN,
    val active: Boolean = true
)

data class RvtecNotification(
    val id: String,
    val title: String,
    val message: String,
    val createdAt: Long,
    val read: Boolean = false,
    val relatedMachineId: String? = null
)

data class RvtecSettings(
    val companyName: String = "",
    val shift1Start: String = "06:30",
    val shift1End: String = "14:30",
    val shift2Start: String = "14:30",
    val shift2End: String = "22:30",
    val shift3Start: String = "22:30",
    val shift3End: String = "06:30",
    val darkTheme: Boolean = false,
    val enabledScreens: Set<String> = emptySet()
)
