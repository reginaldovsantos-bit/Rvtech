package com.rvtec.v2.domain

import org.junit.Assert.*
import org.junit.Test

class UserProfileTest {
    private fun profile(role: UserRole, active: Boolean = true) = UserProfile(
        uid = "uid-1",
        companyId = "empresa-1",
        role = role,
        displayName = "Usuário Teste",
        email = "teste@empresa.com",
        active = active
    )

    @Test fun wireRolesAreStableAndParseable() {
        assertEquals(UserRole.SUPERVISOR, UserRole.fromWireValue("SUPERVISOR"))
        assertEquals(UserRole.TECHNICIAN, UserRole.fromWireValue("TECHNICIAN"))
        assertEquals(UserRole.OPERATOR, UserRole.fromWireValue("OPERATOR"))
        assertNull(UserRole.fromWireValue("ADMIN"))
        assertNull(UserRole.fromWireValue(null))
    }

    @Test fun supervisorCanManageUsersAndMaintenance() {
        val user = profile(UserRole.SUPERVISOR)
        assertTrue(user.canManageUsers())
        assertTrue(user.canPerformMaintenance())
        assertTrue(user.canRegisterOperations())
    }

    @Test fun technicianCanPerformMaintenanceButCannotManageUsers() {
        val user = profile(UserRole.TECHNICIAN)
        assertFalse(user.canManageUsers())
        assertTrue(user.canPerformMaintenance())
        assertFalse(user.canRegisterOperations())
    }

    @Test fun operatorCanRegisterOperationsButCannotPerformMaintenance() {
        val user = profile(UserRole.OPERATOR)
        assertFalse(user.canManageUsers())
        assertFalse(user.canPerformMaintenance())
        assertTrue(user.canRegisterOperations())
    }

    @Test fun inactiveUserHasNoCapabilities() {
        for (role in UserRole.entries) {
            val user = profile(role, active = false)
            assertFalse(user.canManageUsers())
            assertFalse(user.canPerformMaintenance())
            assertFalse(user.canRegisterOperations())
        }
    }

    @Test fun requiredIdentityFieldsCannotBeBlank() {
        try {
            UserProfile("", "empresa-1", UserRole.TECHNICIAN, "Nome", "a@b.com", true)
            fail("UID vazio deveria ser rejeitado")
        } catch (_: IllegalArgumentException) { }
        try {
            UserProfile("uid-1", "", UserRole.TECHNICIAN, "Nome", "a@b.com", true)
            fail("Empresa vazia deveria ser rejeitada")
        } catch (_: IllegalArgumentException) { }
    }
}
