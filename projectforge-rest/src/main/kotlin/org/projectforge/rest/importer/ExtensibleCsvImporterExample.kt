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

/**
 * Example demonstrating how to extend the AbstractCsvImporter for custom object mapping
 * and processing during CSV import.
 *
 * Usage Examples:
 *
 * ## 1. Simple Custom Field Processing
 * ```kotlin
 * class CustomCsvImporter : AbstractCsvImporter<MyDTO>() {
 *     override fun processField(
 *         entity: MyDTO,
 *         fieldSettings: ImportFieldSettings,
 *         value: String,
 *         rowContext: CsvRowContext<MyDTO>
 *     ): Boolean {
 *         return when (fieldSettings.property) {
 *             "customField" -> {
 *                 entity.customProperty = parseCustomValue(value)
 *                 true // Field was handled by custom logic
 *             }
 *             else -> false // Use standard processing
 *         }
 *     }
 * }
 * ```
 *
 * ## 2. Post-Processing After Row Parsing
 * ```kotlin
 * class ValidationCsvImporter : AbstractCsvImporter<MyDTO>() {
 *     override fun postProcessEntity(entity: MyDTO, rowIndex: Int, importStorage: ImportStorage<MyDTO>) {
 *         // Custom validation or transformation
 *         if (entity.name.isNullOrBlank()) {
 *             entity.name = "Row_$rowIndex"
 *         }
 *
 *         // Cross-reference lookups
 *         entity.category = lookupCategory(entity.categoryCode)
 *     }
 * }
 * ```
 *
 * ## 3. Header Processing and Field Mapping
 * ```kotlin
 * class HeaderMappingCsvImporter : AbstractCsvImporter<MyDTO>() {
 *     override fun processHeaders(headers: List<String>, importStorage: ImportStorage<MyDTO>): List<String> {
 *         // Transform headers before field mapping
 *         return headers.map { header ->
 *             when (header.lowercase()) {
 *                 "name", "titel" -> "name"
 *                 "alter", "age" -> "age"
 *                 else -> header
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## 4. Final Processing After All Rows
 * ```kotlin
 * class ConsolidatingCsvImporter : AbstractCsvImporter<InvoicePositionDTO>() {
 *     override fun finalizeImport(records: List<InvoicePositionDTO>, importStorage: ImportStorage<InvoicePositionDTO>) {
 *         // Group positions by invoice number
 *         val invoiceGroups = records.groupBy { it.invoiceNumber }
 *
 *         // Validate consistency within each invoice
 *         invoiceGroups.forEach { (invoiceNumber, positions) ->
 *             validateInvoiceConsistency(invoiceNumber, positions)
 *         }
 *     }
 * }
 * ```
 *
 * ## 5. Complete Custom Importer Example
 * ```kotlin
 * class AdvancedInvoiceCsvImporter(
 *     private val kontoCache: KontoCache,
 *     private val kostCache: KostCache
 * ) : AbstractCsvImporter<InvoiceDTO>() {
 *
 *     override fun processField(
 *         entity: InvoiceDTO,
 *         fieldSettings: ImportFieldSettings,
 *         value: String,
 *         rowContext: CsvRowContext<InvoiceDTO>
 *     ): Boolean {
 *         return when (fieldSettings.property) {
 *             "datevAccount" -> {
 *                 entity.konto = kontoCache.findKontoByNumber(value.toInt())
 *                 true
 *             }
 *             "periodString" -> {
 *                 parsePeriod(value, entity)
 *                 true
 *             }
 *             else -> false
 *         }
 *     }
 *
 *     override fun postProcessEntity(entity: InvoiceDTO, rowIndex: Int, importStorage: ImportStorage<InvoiceDTO>) {
 *         // Parse KOST references
 *         parseKostReferences(entity)
 *
 *         // Calculate derived values
 *         calculateVatAmount(entity)
 *     }
 *
 *     override fun finalizeImport(records: List<InvoiceDTO>, importStorage: ImportStorage<InvoiceDTO>) {
 *         // Consolidate by invoice number and validate consistency
 *         consolidateAndValidate(records)
 *     }
 * }
 * ```
 *
 * ## Usage
 * ```kotlin
 * val importer = CustomCsvImporter()
 * importer.parse(inputStream, importStorage)
 * ```
 *
 * ## Backward Compatibility
 * The original CsvImporter object still works unchanged:
 * ```kotlin
 * CsvImporter.parse(inputStream, importStorage) // Works as before
 * ```
 */
class ExtensibleCsvImporterExample

// Note: This is just documentation - the actual implementation examples are shown above.
