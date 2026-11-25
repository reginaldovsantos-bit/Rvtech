package com.rvtec.v2.data

import org.junit.Assert.*
import org.junit.Test

class LegacyImportValidatorTest {
    @Test
    fun acceptsCompleteBackupWithKnownCollections() {
        val json = """
            {
              "machines": [{"id":"m1"}],
              "stops": [{"id":"s1"}],
              "orders": [],
              "preventives": [],
              "diagnostics": [],
              "technicalMemory": [],
              "failures": [],
              "staff": [],
              "stock": [],
              "stopDeletions": [],
              "notifications": [],
              "settings": {"theme":"dark"}
            }
        """.trimIndent()

        val report = LegacyImportValidator.validate(json)

        assertTrue(report.valid)
        assertTrue(report.errors.isEmpty())
        assertEquals(1, report.counts["machines"])
        assertEquals(1, report.counts["stops"])
        assertEquals(0, report.counts["orders"])
    }

    @Test
    fun rejectsMalformedJson() {
        val report = LegacyImportValidator.validate("{not-json")

        assertFalse(report.valid)
        assertTrue(report.errors.any { it.contains("JSON válido") })
    }

    @Test
    fun rejectsCollectionWithWrongType() {
        val report = LegacyImportValidator.validate("""{"machines": "não é lista"}""")

        assertFalse(report.valid)
        assertTrue(report.errors.any { it.contains("machines") })
    }

    @Test
    fun rejectsSettingsWithWrongType() {
        val report = LegacyImportValidator.validate("""{"machines": [], "settings": []}""")

        assertFalse(report.valid)
        assertTrue(report.errors.any { it.contains("settings") })
    }

    @Test
    fun rejectsEmptyBackup() {
        val report = LegacyImportValidator.validate("{}")

        assertFalse(report.valid)
        assertTrue(report.errors.any { it.contains("Backup vazio") })
    }

    @Test
    fun acceptsMissingOptionalCollectionsAsEmpty() {
        val report = LegacyImportValidator.validate("""{"machines": [{"id":"m1"}]}""")

        assertTrue(report.valid)
        assertEquals(1, report.counts["machines"])
        assertEquals(0, report.counts["stops"])
        assertEquals(0, report.counts["stock"])
    }
}
