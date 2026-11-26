package com.rvtec.v2.data

import com.rvtec.v2.domain.*
import org.junit.Assert.*
import org.junit.Test

class MaintenanceIndicatorsTest {
    @Test fun calculatesAvailabilityMttrAndMtbfForClosedStop() {
        val hour = 3_600_000L
        val stop = StopRecord(
            id = "s1", machineId = "m1", startedAt = hour * 5,
            endedAt = hour * 7, cause = "Falha no puxador", solution = "Sensor ajustado",
            status = StopStatus.CLOSED
        )
        val result = MaintenanceIndicatorsCalculator.calculate(listOf(stop), 0, hour * 24)
        assertEquals(24.0, result.observationHours, 0.001)
        assertEquals(2.0, result.downtimeHours, 0.001)
        assertEquals(22.0, result.operatingHours, 0.001)
        assertEquals(22.0 / 24.0 * 100.0, result.availabilityPercent, 0.001)
        assertEquals(1, result.failureCount)
        assertEquals(2.0, result.mttrHours!!, 0.001)
        assertEquals(22.0, result.mtbfHours!!, 0.001)
    }

    @Test fun openStopCountsAsDowntimeButNotCompletedRepairForMttr() {
        val hour = 3_600_000L
        val stop = StopRecord(
            id = "s1", machineId = "m1", startedAt = hour * 5,
            cause = "Falha no bobinador", status = StopStatus.OPEN
        )
        val result = MaintenanceIndicatorsCalculator.calculate(listOf(stop), 0, hour * 8)
        assertEquals(3.0, result.downtimeHours, 0.001)
        assertNull(result.mttrHours)
        assertEquals(1, result.failureCount)
    }

    @Test fun stopStartedBeforeWindowReducesAvailabilityButIsNotNewFailure() {
        val hour = 3_600_000L
        val stop = StopRecord(
            id = "s1", machineId = "m1", startedAt = hour,
            endedAt = hour * 5, cause = "Falha", status = StopStatus.CLOSED
        )
        val result = MaintenanceIndicatorsCalculator.calculate(listOf(stop), hour * 2, hour * 8)
        assertEquals(3.0, result.downtimeHours, 0.001)
        assertEquals(0, result.failureCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidObservationWindow() {
        MaintenanceIndicatorsCalculator.calculate(emptyList(), 100, 100)
    }
}
