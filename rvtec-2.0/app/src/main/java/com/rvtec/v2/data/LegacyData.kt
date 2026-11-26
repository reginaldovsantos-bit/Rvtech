package com.rvtec.v2.data

/**
 * Contrato de importação da RVTEC 1.8.5.
 * Manter os nomes das coleções/campos legados até concluir validação de migração.
 */
data class LegacyData(
    val machines: List<Map<String, Any?>> = emptyList(),
    val stops: List<Map<String, Any?>> = emptyList(),
    val orders: List<Map<String, Any?>> = emptyList(),
    val preventives: List<Map<String, Any?>> = emptyList(),
    val diagnostics: List<Map<String, Any?>> = emptyList(),
    val technicalMemory: List<Map<String, Any?>> = emptyList(),
    val failures: List<Map<String, Any?>> = emptyList(),
    val staff: List<Map<String, Any?>> = emptyList(),
    val stock: List<Map<String, Any?>> = emptyList(),
    val stopDeletions: List<Map<String, Any?>> = emptyList(),
    val notifications: List<Map<String, Any?>> = emptyList(),
    val settings: Map<String, Any?> = emptyMap()
)
