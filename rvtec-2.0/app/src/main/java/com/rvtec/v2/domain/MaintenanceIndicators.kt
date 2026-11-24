package com.rvtec.v2.domain

/**
 * Indicadores calculados sobre uma janela explícita.
 * Paradas abertas contam até windowEnd; MTTR considera apenas paradas encerradas
 * dentro da janela, evitando tratar uma duração ainda em andamento como reparo concluído.
 */
data class MaintenanceIndicators(
    val observationHours: Double,
    val downtimeHours: Double,
    val operatingHours: Double,
    val availabilityPercent: Double,
    val failureCount: Int,
    val mttrHours: Double?,
    val mtbfHours: Double?
)

object MaintenanceIndicatorsCalculator {
    fun calculate(stops: List<StopRecord>, windowStart: Long, windowEnd: Long): MaintenanceIndicators {
        require(windowEnd > windowStart) { "Período de cálculo inválido." }
        val observationMs = windowEnd - windowStart
        val relevant = stops.mapNotNull { stop ->
            val end = stop.endedAt ?: windowEnd
            val overlapStart = maxOf(stop.startedAt, windowStart)
            val overlapEnd = minOf(end, windowEnd)
            if (overlapEnd > overlapStart) stop to (overlapEnd - overlapStart) else null
        }
        val downtimeMs = relevant.sumOf { it.second }
        val completedDurations = relevant.mapNotNull { (stop, _) ->
            val end = stop.endedAt ?: return@mapNotNull null
            val duration = end - stop.startedAt
            if (duration > 0 && end in (windowStart + 1)..windowEnd) duration else null
        }
        val operatingMs = (observationMs - downtimeMs).coerceAtLeast(0L)
        val observationHours = observationMs / 3_600_000.0
        val downtimeHours = downtimeMs / 3_600_000.0
        val operatingHours = operatingMs / 3_600_000.0
        val failureCount = relevant.size
        return MaintenanceIndicators(
            observationHours = observationHours,
            downtimeHours = downtimeHours,
            operatingHours = operatingHours,
            availabilityPercent = if (observationMs > 0) operatingMs * 100.0 / observationMs else 0.0,
            failureCount = failureCount,
            mttrHours = completedDurations.takeIf { it.isNotEmpty() }?.average()?.div(3_600_000.0),
            mtbfHours = if (failureCount > 0) operatingHours / failureCount else null
        )
    }
}
