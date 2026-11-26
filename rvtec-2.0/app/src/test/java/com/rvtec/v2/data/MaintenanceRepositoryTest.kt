package com.rvtec.v2.data

import com.rvtec.v2.domain.*
import org.junit.Assert.*
import org.junit.Test

class MaintenanceRepositoryTest {
    @Test fun stopLinksOrderAndCompletionReleasesMachine() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val stop = repo.registerStop("m1", "Falha no puxador", 100)
        val order = repo.openWorkOrder("m1", "Verificar puxador", stopId = stop.id, now = 110)
        assertEquals(order.id, repo.listStops().single().workOrderId)
        repo.startWork(order.id)
        repo.addExecution(order.id, "Eletricista", "Inspecionado inversor", durationMinutes = 20)
        repo.completeWork(order.id, "Conector substituído", releaseMachine = true, now = 200)
        assertEquals(MachineStatus.RUNNING, repo.listMachines().single().status)
        assertEquals(StopStatus.CLOSED, repo.listStops().single().status)
        assertEquals(1, repo.listEntries().size)
    }

    @Test fun machineCannotReceiveTwoActiveStops() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        repo.registerStop("m1", "Falha A")
        try {
            repo.registerStop("m1", "Falha B")
            fail("Deveria bloquear segunda parada ativa")
        } catch (_: IllegalStateException) { }
    }

    @Test fun maintenanceRequiresExplicitRelease() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val stop = repo.registerStop("m1", "Falha")
        val order = repo.openWorkOrder("m1", "Reparo", stopId = stop.id)
        repo.startWork(order.id)
        repo.completeWork(order.id, "Reparo concluído", releaseMachine = false)
        assertEquals(MachineStatus.MAINTENANCE, repo.listMachines().single().status)
        repo.releaseMachine("m1")
        assertEquals(MachineStatus.RUNNING, repo.listMachines().single().status)
    }

    @Test fun workOrderWithoutStopCannotReleaseMachine() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val order = repo.openWorkOrder("m1", "Inspeção preventiva")
        repo.startWork(order.id)
        try {
            repo.completeWork(order.id, "Inspeção concluída", releaseMachine = true)
            fail("Não deve liberar máquina sem parada vinculada")
        } catch (_: IllegalArgumentException) { }
        assertEquals(WorkOrderStatus.IN_PROGRESS, repo.listOrders().single().status)
        assertEquals(MachineStatus.RUNNING, repo.listMachines().single().status)
    }

    @Test fun workOrderWithoutStopCanCompleteWithoutChangingMachineState() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val order = repo.openWorkOrder("m1", "Inspeção preventiva")
        repo.startWork(order.id)
        val completed = repo.completeWork(order.id, "Inspeção concluída", releaseMachine = false)
        assertEquals(WorkOrderStatus.COMPLETED, completed.status)
        assertEquals(MachineStatus.RUNNING, repo.listMachines().single().status)
    }

    @Test fun stopCannotLinkToAnotherMachinesOrder() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        repo.addMachine(Machine("m2", "Extrusora 109"))
        val stop = repo.registerStop("m1", "Falha")
        try {
            repo.openWorkOrder("m2", "OS errada", stopId = stop.id)
            fail("Deveria bloquear vínculo cruzado")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun linkingOpenStopKeepsMachineAndStopConsistent() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val stop = repo.registerStop("m1", "Falha no puxador", now = 100)
        val order = repo.openWorkOrder("m1", "Reparar puxador", stopId = stop.id, now = 110)

        assertEquals(stop.id, order.stopId)
        assertEquals(MachineStatus.STOPPED, repo.listMachines().single().status)
        assertEquals(stop.id, repo.listMachines().single().activeStopId)
        assertEquals(StopStatus.LINKED_TO_ORDER, repo.listStops().single().status)
        assertEquals(order.id, repo.listStops().single().workOrderId)
    }

    @Test fun stopRequiresCause() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        try {
            repo.registerStop("m1", "   ")
            fail("Não deve aceitar parada sem causa")
        } catch (_: IllegalArgumentException) { }
        assertEquals(MachineStatus.RUNNING, repo.listMachines().single().status)
        assertTrue(repo.listStops().isEmpty())
    }
    @Test fun independentWorkOrdersCanCoexistOnSameMachine() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))

        val first = repo.openWorkOrder("m1", "Inspecionar puxador")
        val second = repo.openWorkOrder("m1", "Verificar bobinador")

        assertNotEquals(first.id, second.id)
        assertEquals(2, repo.listOrders().size)
        assertEquals(setOf(first.id, second.id), repo.listOrders().map { it.id }.toSet())
    }

    @Test fun completionBeforeStopStartIsRejectedWithoutChangingMachineOrStop() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val stop = repo.registerStop("m1", "Falha no puxador", now = 100)
        val order = repo.openWorkOrder("m1", "Reparar puxador", stopId = stop.id, now = 90)
        repo.startWork(order.id)

        try {
            repo.completeWork(order.id, "Sensor substituído", releaseMachine = true, now = 99)
            fail("Não deve encerrar uma parada antes do seu início")
        } catch (_: IllegalArgumentException) { }

        assertEquals(MachineStatus.STOPPED, repo.listMachines().single().status)
        assertEquals(StopStatus.LINKED_TO_ORDER, repo.listStops().single().status)
        assertNull(repo.listStops().single().endedAt)
        assertEquals(WorkOrderStatus.IN_PROGRESS, repo.listOrders().single().status)
    }

    @Test fun linkedStopCanOnlyBeClosedByItsOwnWorkOrder() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val stop = repo.registerStop("m1", "Falha no bobinador", now = 100)
        val order = repo.openWorkOrder("m1", "Verificar bobinador", stopId = stop.id, now = 110)
        repo.startWork(order.id)

        val completed = repo.completeWork(order.id, "Encoder ajustado", releaseMachine = false, now = 200)

        assertEquals(WorkOrderStatus.COMPLETED, completed.status)
        assertEquals(StopStatus.CLOSED, repo.listStops().single().status)
        assertEquals("Encoder ajustado", repo.listStops().single().solution)
        assertEquals(MachineStatus.MAINTENANCE, repo.listMachines().single().status)
        assertEquals(stop.id, repo.listMachines().single().activeStopId)
    }

    @Test fun machineCanBeEditedWithoutChangingOperationalState() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108", code = "108", sector = "Filme", productionKgPerHour = 35.0))
        repo.registerStop("m1", "Falha no puxador", now = 100)

        val updated = repo.updateMachine("m1", "Extrusora 108A", "108A", "Linha 1", 42.5)

        assertEquals("Extrusora 108A", updated.name)
        assertEquals("108A", updated.code)
        assertEquals("Linha 1", updated.sector)
        assertEquals(42.5, updated.productionKgPerHour, 0.001)
        assertEquals(MachineStatus.STOPPED, repo.listMachines().single().status)
        assertNotNull(repo.listMachines().single().activeStopId)
    }

    @Test fun machineCanBeDeletedOnlyWhenItHasNoHistoryOrActiveMaintenance() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        repo.deleteMachine("m1")
        assertTrue(repo.listMachines().isEmpty())
    }

    @Test fun machineWithStopHistoryCannotBeDeleted() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        repo.registerStop("m1", "Falha no encoder")
        try {
            repo.deleteMachine("m1")
            fail("Não deve excluir máquina com histórico de parada")
        } catch (_: IllegalStateException) { }
        assertEquals(1, repo.listMachines().size)
        assertEquals(1, repo.listStops().size)
    }

    @Test fun activeMachineCannotBeDeleted() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        repo.registerStop("m1", "Falha no encoder")
        try {
            repo.deleteMachine("m1")
            fail("Não deve excluir máquina parada")
        } catch (_: IllegalStateException) { }
        assertEquals(MachineStatus.STOPPED, repo.listMachines().single().status)
    }

}
