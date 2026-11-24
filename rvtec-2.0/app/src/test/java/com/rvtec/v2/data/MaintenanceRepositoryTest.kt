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


    @Test fun independentActiveOrdersCanCoexistOnSameMachine() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val first = repo.openWorkOrder("m1", "Falha no puxador")
        val second = repo.openWorkOrder("m1", "Falha no bobinador")
        assertNotEquals(first.id, second.id)
        assertEquals(2, repo.listOrders().size)
    }

    @Test fun oneStopCannotBeLinkedToTwoOrders() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val stop = repo.registerStop("m1", "Falha no puxador")
        repo.openWorkOrder("m1", "Reparo do puxador", stopId = stop.id)
        try {
            repo.openWorkOrder("m1", "Segunda OS para mesma parada", stopId = stop.id)
            fail("Uma parada não pode ter duas OS")
        } catch (_: IllegalArgumentException) { }
        assertEquals(1, repo.listOrders().size)
    }

    @Test fun executionEntriesAreReturnedOldestFirst() {
        val repo = MaintenanceRepository()
        repo.addMachine(Machine("m1", "Extrusora 108"))
        val order = repo.openWorkOrder("m1", "Inspeção")
        repo.startWork(order.id)
        repo.addExecution(order.id, "Técnico", "Etapa mais recente", now = 300)
        repo.addExecution(order.id, "Técnico", "Etapa mais antiga", now = 100)
        assertEquals(listOf("Etapa mais antiga", "Etapa mais recente"), repo.listEntries().map { it.description })
    }

}
