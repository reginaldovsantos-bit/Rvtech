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
}
