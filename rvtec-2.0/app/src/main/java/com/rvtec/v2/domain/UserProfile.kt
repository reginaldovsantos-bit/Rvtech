package com.rvtec.v2.domain

/**
 * Identidade e vínculo de acesso do usuário RVTEC.
 *
 * Estes dados são provisionados por um administrador confiável no Firebase;
 * o aplicativo não deve permitir que o próprio usuário altere companyId ou role.
 */
enum class UserRole(val wireValue: String, val displayName: String) {
    SUPERVISOR("SUPERVISOR", "Supervisor"),
    TECHNICIAN("TECHNICIAN", "Técnico"),
    OPERATOR("OPERATOR", "Operador");

    companion object {
        fun fromWireValue(value: String?): UserRole? =
            entries.firstOrNull { it.wireValue == value }
    }
}

data class UserProfile(
    val uid: String,
    val companyId: String,
    val role: UserRole,
    val displayName: String,
    val email: String,
    val active: Boolean
) {
    init {
        require(uid.isNotBlank()) { "UID do usuário não pode ficar vazio." }
        require(companyId.isNotBlank()) { "Empresa do usuário não pode ficar vazia." }
        require(displayName.isNotBlank()) { "Nome do usuário não pode ficar vazio." }
        require(email.isNotBlank()) { "E-mail do usuário não pode ficar vazio." }
    }

    fun canManageUsers(): Boolean = active && role == UserRole.SUPERVISOR
    fun canPerformMaintenance(): Boolean =
        active && role in setOf(UserRole.SUPERVISOR, UserRole.TECHNICIAN)
    fun canRegisterOperations(): Boolean =
        active && role in setOf(UserRole.SUPERVISOR, UserRole.OPERATOR)
}
