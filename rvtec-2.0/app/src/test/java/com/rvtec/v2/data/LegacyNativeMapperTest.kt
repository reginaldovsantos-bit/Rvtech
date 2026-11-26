package com.rvtec.v2.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyNativeMapperTest {
    @Test fun mapsLegacyCollectionsIntoNativeModels() {
        val json = JSONObject(
            """
            {
              "preventives":[{"id":"p1","machineId":"m1","title":"Lubrificação","intervalDays":15}],
              "diagnostics":[{"id":"d1","machineId":"m1","title":"Falha inversor","symptoms":"F074"}],
              "technicalMemory":[{"id":"t1","title":"CFW700","content":"Verificar ventilação","tags":"WEG, inversor"}],
              "stock":[{"id":"s1","name":"Rolamento","quantity":8,"minimumQuantity":2}],
              "staff":[{"id":"u1","name":"Operador","role":"production"}],
              "notifications":[{"id":"n1","title":"Parada","message":"Extrusora 2","read":false}],
              "settings":{"darkTheme":true,"shift1Start":"07:00"}
            }
            """.trimIndent()
        )

        val mapped = LegacyNativeMapper.map(json)

        assertEquals("p1", mapped.preventives.single().id)
        assertEquals(15, mapped.preventives.single().intervalDays)
        assertEquals("F074", mapped.diagnostics.single().symptoms)
        assertEquals(setOf("WEG", "inversor"), mapped.technicalMemory.single().tags.toSet())
        assertEquals(8.0, mapped.stock.single().quantity, 0.0)
        assertEquals("PRODUCTION", mapped.staff.single().role.name)
        assertFalse(mapped.notifications.single().read)
        assertTrue(mapped.settings.darkTheme)
        assertEquals("07:00", mapped.settings.shift1Start)
    }

    @Test fun missingCollectionsProduceEmptyNativeLists() {
        val mapped = LegacyNativeMapper.map(JSONObject("{}"))
        assertTrue(mapped.preventives.isEmpty())
        assertTrue(mapped.diagnostics.isEmpty())
        assertTrue(mapped.technicalMemory.isEmpty())
        assertTrue(mapped.stock.isEmpty())
        assertTrue(mapped.staff.isEmpty())
        assertTrue(mapped.notifications.isEmpty())
    }

    @Test fun mapsLegacyAliasesAndNormalizesInvalidQuantities() {
        val mapped = LegacyNativeMapper.map(JSONObject(
            """
            {
              "failures":[{"uid":"d-alias","machine_id":"extrusora-108","failure":"Falha no encoder","causes":"Cabo rompido","resolution":"Conector refeito","timestamp":1234}],
              "stock":[{"uid":"rol-1","sku":"ROL-6204","title":"Rolamento","qty":-3,"minStock":2,"measure":"un","warehouse":"A1"}],
              "staff":[{"uid":"sup-1","displayName":"Supervisor","profile":"supervisor","email":"sup@example.com"}],
              "settings":{"turno1Inicio":"06:00","turno1Fim":"14:00","darkMode":true}
            }
            """.trimIndent()
        ))
        assertEquals("d-alias", mapped.diagnostics.single().id)
        assertEquals("extrusora-108", mapped.diagnostics.single().machineId)
        assertEquals("Falha no encoder", mapped.diagnostics.single().title)
        assertEquals("Conector refeito", mapped.diagnostics.single().solution)
        assertEquals(0.0, mapped.stock.single().quantity, 0.0)
        assertEquals("ROL-6204", mapped.stock.single().code)
        assertEquals("SUPERVISOR", mapped.staff.single().role.name)
        assertEquals("06:00", mapped.settings.shift1Start)
        assertTrue(mapped.settings.darkTheme)
    }

    @Test fun malformedOptionalFieldsDoNotBreakOtherModules() {
        val mapped = LegacyNativeMapper.map(JSONObject(
            """{"preventives":[{"id":"p","intervalDays":0}],
               "technicalMemory":[{"id":"m","tags":"WEG;CLP"}],
               "notifications":[{"id":"n","seen":true}]}"""
        ))
        assertEquals(1, mapped.preventives.single().intervalDays)
        assertEquals(listOf("WEG", "CLP"), mapped.technicalMemory.single().tags)
        assertTrue(mapped.notifications.single().read)
    }
}
