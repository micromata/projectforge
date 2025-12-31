/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.importer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KProperty

class AbstractCsvImporterTest {

    // Test DTO that implements ImportPairEntry.Modified
    class TestDTO : ImportPairEntry.Modified<TestDTO> {
        var name: String? = null
        var age: Int? = null
        var amount: BigDecimal? = null
        var birthDate: LocalDate? = null
        var active: Boolean? = null
        var customField: String? = null

        override val properties: Array<KProperty<*>>? = TestDTO::class.members
            .filterIsInstance<KProperty<*>>()
            .filter { it.name != "properties" }
            .toTypedArray()
    }

    // Mock ImportStorage for testing
    class TestImportStorage(settings: ImportSettings = ImportSettings()) : ImportStorage<TestDTO>(settings) {
        val entities = mutableListOf<TestDTO>()

        override fun prepareEntity(): TestDTO = TestDTO()
        override fun commitEntity(entity: TestDTO) {
            entities.add(entity)
        }
    }

    // Test importer that uses default behavior
    class DefaultTestImporter : AbstractCsvImporter<TestDTO>()

    // Test importer with custom processing
    class CustomTestImporter : AbstractCsvImporter<TestDTO>() {
        val processedHeaders = mutableListOf<String>()
        val processedFields = mutableMapOf<String, String>()
        val postProcessedEntities = mutableListOf<TestDTO>()
        var finalizeImportCalled = false

        override fun processHeaders(headers: List<String>, importStorage: ImportStorage<TestDTO>): List<String> {
            processedHeaders.addAll(headers)
            return headers.map { header ->
                when (header) {
                    "Name" -> "name"
                    "Age" -> "age"
                    "Amount" -> "amount"
                    "Birth Date" -> "birthDate"
                    "Active" -> "active"
                    else -> header
                }
            }
        }

        override fun processField(
            entity: TestDTO,
            fieldSettings: ImportFieldSettings,
            value: String,
            rowContext: CsvRowContext<TestDTO>
        ): Boolean {
            return when (fieldSettings.property) {
                "customField" -> {
                    // Store first occurrence only for test verification
                    if (!processedFields.containsKey(fieldSettings.property)) {
                        processedFields[fieldSettings.property] = value
                    }
                    entity.customField = "CUSTOM_$value"
                    true
                }
                else -> false
            }
        }

        override fun postProcessEntity(entity: TestDTO, rowIndex: Int, importStorage: ImportStorage<TestDTO>) {
            postProcessedEntities.add(entity)
            // Add row index to name if present
            if (entity.name != null) {
                entity.name = "${entity.name}_ROW_$rowIndex"
            }
        }

        override fun finalizeImport(records: List<TestDTO>, importStorage: ImportStorage<TestDTO>) {
            finalizeImportCalled = true
            // Convert all names to uppercase
            records.forEach { record ->
                record.name = record.name?.uppercase()
            }
        }
    }

    @Test
    fun `test default importer with simple CSV`() {
        val csvData = """
            "name";"age";"amount"
            "John";"25";"100.50"
            "Jane";"30";"200.75"
        """.trimIndent()

        val importSettings = ImportSettings()
        // Setup field mappings
        importSettings.addFieldSettings(ImportFieldSettings("name"))
        importSettings.addFieldSettings(ImportFieldSettings("age"))
        importSettings.addFieldSettings(ImportFieldSettings("amount"))

        val importStorage = TestImportStorage(importSettings)
        val importer = DefaultTestImporter()
        importer.parse(StringReader(csvData), importStorage)

        assertEquals(2, importStorage.entities.size)

        val entity1 = importStorage.entities[0]
        assertEquals("John", entity1.name)
        assertEquals(25, entity1.age)
        assertEquals(BigDecimal("100.50"), entity1.amount)

        val entity2 = importStorage.entities[1]
        assertEquals("Jane", entity2.name)
        assertEquals(30, entity2.age)
        assertEquals(BigDecimal("200.75"), entity2.amount)
    }

    @Test
    fun `test custom importer with header processing`() {
        val csvData = """
            "Name";"Age";"Amount"
            "John";"25";"100.50"
            "Jane";"30";"200.75"
        """.trimIndent()

        val importSettings = ImportSettings()
        // Setup field mappings with original header names (will be transformed by processHeaders)
        importSettings.addFieldSettings(ImportFieldSettings("name").apply { aliasList.add("Name") })
        importSettings.addFieldSettings(ImportFieldSettings("age").apply { aliasList.add("Age") })
        importSettings.addFieldSettings(ImportFieldSettings("amount").apply { aliasList.add("Amount") })

        val importStorage = TestImportStorage(importSettings)
        val importer = CustomTestImporter()
        importer.parse(StringReader(csvData), importStorage)

        // Verify headers were processed
        assertEquals(listOf("Name", "Age", "Amount"), importer.processedHeaders)

        // Verify entities were processed
        assertEquals(2, importStorage.entities.size)

        val entity1 = importStorage.entities[0]
        assertEquals("JOHN_ROW_0", entity1.name) // uppercase from finalizeImport + row index from postProcessEntity
        assertEquals(25, entity1.age)
        assertEquals(BigDecimal("100.50"), entity1.amount)
    }

    @Test
    fun `test custom field processing`() {
        val csvData = """
            "name";"customField"
            "John";"special_value"
            "Jane";"another_value"
        """.trimIndent()

        val importSettings = ImportSettings()
        importSettings.addFieldSettings(ImportFieldSettings("name"))
        importSettings.addFieldSettings(ImportFieldSettings("customField"))

        val importStorage = TestImportStorage(importSettings)
        val importer = CustomTestImporter()
        importer.parse(StringReader(csvData), importStorage)

        assertEquals(2, importStorage.entities.size)

        val entity1 = importStorage.entities[0]
        assertEquals("JOHN_ROW_0", entity1.name)
        assertEquals("CUSTOM_special_value", entity1.customField)

        val entity2 = importStorage.entities[1]
        assertEquals("JANE_ROW_1", entity2.name)
        assertEquals("CUSTOM_another_value", entity2.customField)

        // Verify processField was called for customField
        assertEquals("special_value", importer.processedFields["customField"])
    }

    @Test
    fun `test post processing and finalization hooks`() {
        val csvData = """
            "name"
            "John"
            "Jane"
        """.trimIndent()

        val importSettings = ImportSettings()
        importSettings.addFieldSettings(ImportFieldSettings("name"))

        val importStorage = TestImportStorage(importSettings)
        val importer = CustomTestImporter()
        importer.parse(StringReader(csvData), importStorage)

        // Verify postProcessEntity was called for each entity
        assertEquals(2, importer.postProcessedEntities.size)

        // Verify finalizeImport was called
        assertTrue(importer.finalizeImportCalled)

        // Verify the combined effect of postProcess (adds row index) and finalize (uppercase)
        assertEquals("JOHN_ROW_0", importStorage.entities[0].name)
        assertEquals("JANE_ROW_1", importStorage.entities[1].name)
    }

    @Test
    fun `test backward compatibility with original CsvImporter`() {
        val csvData = """
            "name";"age";"amount"
            "John";"25";"100.50"
        """.trimIndent()

        val importSettings = ImportSettings()
        importSettings.addFieldSettings(ImportFieldSettings("name"))
        importSettings.addFieldSettings(ImportFieldSettings("age"))
        importSettings.addFieldSettings(ImportFieldSettings("amount"))

        val importStorage = TestImportStorage(importSettings)
        // Test that the facade still works
        CsvImporter.parse(StringReader(csvData), importStorage)

        assertEquals(1, importStorage.entities.size)
        val entity = importStorage.entities[0]
        assertEquals("John", entity.name)
        assertEquals(25, entity.age)
        assertEquals(BigDecimal("100.50"), entity.amount)
    }

    @Test
    fun `test empty CSV handling`() {
        val csvData = """
            "name"
        """.trimIndent()

        val importSettings = ImportSettings()
        importSettings.addFieldSettings(ImportFieldSettings("name"))

        val importStorage = TestImportStorage(importSettings)
        val importer = DefaultTestImporter()
        importer.parse(StringReader(csvData), importStorage)

        assertEquals(0, importStorage.entities.size)
    }

    @Test
    fun `test CSV with BOM handling`() {
        val csvData = "\uFEFF" + """
            "name";"age"
            "John";"25"
        """.trimIndent()

        val importSettings = ImportSettings()
        importSettings.addFieldSettings(ImportFieldSettings("name"))
        importSettings.addFieldSettings(ImportFieldSettings("age"))

        val importStorage = TestImportStorage(importSettings)
        val importer = DefaultTestImporter()
        importer.parse(StringReader(csvData), importStorage)

        assertEquals(1, importStorage.entities.size)
        assertEquals("John", importStorage.entities[0].name)
        assertEquals(25, importStorage.entities[0].age)
    }

    @Test
    fun `test multiline CSV content`() {
        val csvData = """
            "name";"description"
            "Product A";"High quality
            product with
            multiple features"
            "Product B";"Simple product"
        """.trimIndent()

        val importSettings = ImportSettings()
        importSettings.addFieldSettings(ImportFieldSettings("name"))
        importSettings.addFieldSettings(ImportFieldSettings("customField").apply { aliasList.add("description") })

        val importStorage = TestImportStorage(importSettings)
        val importer = CustomTestImporter()
        importer.parse(StringReader(csvData), importStorage)

        assertEquals(2, importStorage.entities.size)

        val entity1 = importStorage.entities[0]
        assertEquals("PRODUCT A_ROW_0", entity1.name)
        assertTrue(entity1.customField!!.contains("High quality\nproduct with\nmultiple features"))
    }
}
