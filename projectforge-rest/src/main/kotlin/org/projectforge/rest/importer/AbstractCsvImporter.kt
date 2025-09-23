/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.common.BeanHelper
import org.projectforge.common.CSVParser
import org.projectforge.framework.utils.ValueParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Abstract base class for extensible CSV importers that allows custom object mapping and parsing.
 * Implements the Template Method Pattern to provide extension points for custom logic while
 * maintaining the standard CSV import workflow.
 *
 * @param O the type of objects being imported, must extend ImportPairEntry.Modified
 */
abstract class AbstractCsvImporter<O : ImportPairEntry.Modified<O>> {

    /**
     * Parse CSV from InputStream with charset detection.
     *
     * @param inputStream the input stream containing CSV data
     * @param importStorage the storage to hold imported data
     * @param defaultCharset charset to use if UTF-8/UTF-16 detection fails
     */
    fun parse(
        inputStream: InputStream,
        importStorage: ImportStorage<O>,
        defaultCharset: Charset? = null,
    ) {
        val bytes = inputStream.readAllBytes()
        parse(ByteArrayInputStream(bytes).reader(charset = detectCharset(bytes, defaultCharset)), importStorage)
    }

    /**
     * Parse CSV from Reader.
     * Main template method that defines the CSV import workflow with extension points.
     *
     * @param reader the reader containing CSV data
     * @param importStorage the storage to hold imported data
     */
    fun parse(reader: Reader, importStorage: ImportStorage<O>) {
        val settings = importStorage.importSettings
        val parser = CSVParser(reader)

        // Parse headers with custom processing hook
        val headCols = parser.parseLine()
        val processedHeaders = processHeaders(headCols ?: emptyList(), importStorage)

        // Map columns with custom field detection
        mapColumns(processedHeaders, importStorage)

        // Initialize for row processing
        val autodetectNumberFormatMap = AutodetectNumberMap<O>()
        val records = mutableListOf<O>()

        // Process each data row
        var rowIndex = 0
        for (i in 0..100000) { // Paranoid loop, read 100000 lines at max.
            val line = parser.parseLine() ?: break  // Finished

            val record = prepareEntity(importStorage)

            // Process each cell in the row
            line.forEachIndexed { columnIndex, value ->
                importStorage.columnMapping[columnIndex]?.let { fieldSettings ->
                    processRowCell(record, fieldSettings, value, autodetectNumberFormatMap, importStorage)
                }
            }

            // Custom post-processing per row
            postProcessEntity(record, rowIndex, importStorage)

            records.add(record)
            rowIndex++
        }

        // Handle number format auto-detection
        processAutodetectedNumberFormats(autodetectNumberFormatMap)

        // Finalize import with custom logic
        finalizeImport(records, importStorage)

        // Commit all records
        records.forEach { record ->
            importStorage.commitEntity(record)
        }
    }

    // =============================================================================
    // Extension Points (Hook Methods)
    // =============================================================================

    /**
     * Hook to process and potentially modify headers after parsing.
     *
     * @param headers the parsed headers from CSV
     * @param importStorage the import storage
     * @return processed headers (can be modified or same as input)
     */
    protected open fun processHeaders(headers: List<String>, importStorage: ImportStorage<O>): List<String> {
        return headers
    }

    /**
     * Hook to create and initialize a target entity.
     * Default implementation uses importStorage.prepareEntity().
     *
     * @param importStorage the import storage
     * @return a new entity instance
     */
    protected open fun prepareEntity(importStorage: ImportStorage<O>): O {
        return importStorage.prepareEntity()
    }

    /**
     * Hook to process individual field values during row parsing.
     * Return true if the field was handled by custom logic, false to use standard processing.
     *
     * @param entity the target entity
     * @param fieldSettings the field configuration
     * @param value the raw CSV value
     * @param importStorage the import storage
     * @return true if field was handled, false for standard processing
     */
    protected open fun processField(
        entity: O,
        fieldSettings: ImportFieldSettings,
        value: String,
        importStorage: ImportStorage<O>
    ): Boolean {
        return false // Default: use standard processing
    }

    /**
     * Hook for custom post-processing after a row has been parsed.
     *
     * @param entity the parsed entity
     * @param rowIndex the current row index (0-based)
     * @param importStorage the import storage
     */
    protected open fun postProcessEntity(entity: O, rowIndex: Int, importStorage: ImportStorage<O>) {
        // Default: no additional processing
    }

    /**
     * Hook for final processing after all rows have been parsed.
     *
     * @param records all parsed records
     * @param importStorage the import storage
     */
    protected open fun finalizeImport(records: List<O>, importStorage: ImportStorage<O>) {
        // Default: no additional processing
    }

    // =============================================================================
    // Standard Implementation Methods
    // =============================================================================

    private fun mapColumns(headers: List<String>, importStorage: ImportStorage<O>) {
        val settings = importStorage.importSettings
        headers.forEachIndexed { index, head ->
            val fieldSettings = settings.getFieldSettings(head)
            if (fieldSettings != null) {
                log.debug { "Field '$head' found: -> ${fieldSettings.property}." }
                importStorage.columnMapping[index] = fieldSettings
                importStorage.detectedColumns[head] = fieldSettings
            } else {
                log.debug { "Field '$head' not found." }
                importStorage.unknownColumns.add(head)
            }
        }
    }

    private fun processRowCell(
        record: O,
        fieldSettings: ImportFieldSettings,
        value: String,
        autodetectNumberFormatMap: AutodetectNumberMap<O>,
        importStorage: ImportStorage<O>
    ) {
        // Try custom processing first
        if (processField(record, fieldSettings, value, importStorage)) {
            return // Custom processing handled the field
        }

        // Check if ImportStorage wants to handle it
        if (!importStorage.setProperty(record, fieldSettings, value)) {
            // Standard type-based processing
            val targetValue = parseValueByType(record, fieldSettings, value, autodetectNumberFormatMap)

            if (targetValue != null) {
                setPropertyValue(record, fieldSettings, targetValue)
            }
        }
    }

    private fun parseValueByType(
        record: O,
        fieldSettings: ImportFieldSettings,
        value: String,
        autodetectNumberFormatMap: AutodetectNumberMap<O>
    ): Any? {
        return when (BeanHelper.determinePropertyType(record::class.java, fieldSettings.property)) {
            LocalDate::class.java -> {
                fieldSettings.parseLocalDate(value)
            }

            Date::class.java -> {
                fieldSettings.parseDate(value)
            }

            BigDecimal::class.java -> {
                if (fieldSettings.parseFormatList.isEmpty()) {
                    // Don't parse value, store value for later format auto-detection:
                    autodetectNumberFormatMap.add(record, fieldSettings, value)
                    null
                } else {
                    // Use given format list:
                    fieldSettings.parseBigDecimal(value)
                }
            }

            Int::class.java -> {
                fieldSettings.parseInt(value)
            }

            Long::class.java -> {
                fieldSettings.parseLong(value)
            }

            Boolean::class.java -> {
                fieldSettings.parseBoolean(value)
            }

            else -> {
                value
            }
        }
    }

    private fun setPropertyValue(record: O, fieldSettings: ImportFieldSettings, targetValue: Any) {
        try {
            if (targetValue is String && targetValue.isNotBlank()) {
                val existingValue = BeanHelper.getProperty(record, fieldSettings.property)
                if (existingValue != null && existingValue is String && existingValue.isNotBlank()) {
                    if (existingValue.trim() != targetValue.trim()) {
                        // Only concat if new value differs:
                        BeanHelper.setProperty(record, fieldSettings.property, "$existingValue$targetValue")
                    }
                } else {
                    // Set value because no existing one as String given:
                    BeanHelper.setProperty(record, fieldSettings.property, targetValue)
                }
            } else if (targetValue !is String) {
                BeanHelper.setProperty(record, fieldSettings.property, targetValue)
            }
        } catch (ex: Exception) {
            log.error("Can't parse property: '${fieldSettings.property}': ${ex.message}")
        }
    }

    private fun processAutodetectedNumberFormats(autodetectNumberFormatMap: AutodetectNumberMap<O>) {
        autodetectNumberFormatMap.storedFieldSettings.forEach { fieldSettings ->
            // Check format:
            var englishStyle = true
            var germanStyle = true
            autodetectNumberFormatMap.entries.forEach { _, autodetectNumberEntry ->
                val str = autodetectNumberEntry.valueStrings[fieldSettings]
                if (str != null) {
                    if (germanStyle && !ValueParser.isGermanStyle(str)) {
                        germanStyle = false
                    }
                    if (englishStyle && !ValueParser.isEnglishStyle(str)) {
                        englishStyle = false
                    }
                }
            }
            if (germanStyle) {
                fieldSettings.parseFormatList.add(0, "#0,0#")
                fieldSettings.parseFormatList.add(0, "#.##0,0#")
            } else {
                if (!englishStyle) {
                    log.warn { "Property ${fieldSettings.property} is neither in German nor in English number format." }
                }
                fieldSettings.parseFormatList.add(0, "#0.0#")
                fieldSettings.parseFormatList.add(0, "#,##0.0#")
            }
            autodetectNumberFormatMap.entries.forEach { record, autodetectNumberEntry ->
                val str = autodetectNumberEntry.valueStrings[fieldSettings]
                if (str != null) {
                    val targetValue = fieldSettings.parseBigDecimal(str)
                    BeanHelper.setProperty(record, fieldSettings.property, targetValue)
                }
            }
        }
    }

    /**
     * Charset detection from the original CsvImporter.
     * If the users specified e.g. ISO-8859-1 char set, but special bytes of UTF-8 or UTF-16 are found, the
     * returned charset will be UTF-8 or UTF-16.
     */
    protected fun detectCharset(bytes: ByteArray, defaultCharset: Charset?): Charset {
        var utf8EscapeChars = 0
        var utf16NullBytes = 0
        val size = bytes.size
        for (i in 0..100000) {
            if (i >= size) {
                break
            }
            if (bytes[i] == UTF8_ESCAPE_BYTE) {
                utf8EscapeChars += 1
            } else if (bytes[i] == UTF16_NULL_BYTE) {
                utf16NullBytes += 1
            }
        }
        val utf8EscapeCharsRate = utf8EscapeChars * 1000 / size
        val utf16NullBytesRate = utf16NullBytes * 10 / size
        if (utf16NullBytesRate > 1) {
            // More than 1/10 of all bytes are null bytes, seems to be UTF-16.
            return StandardCharsets.UTF_16
        }
        if (utf8EscapeCharsRate > 1) { // More than 1 promille of chars is Ã
            return StandardCharsets.UTF_8
        }
        return defaultCharset ?: StandardCharsets.UTF_8
    }

    private class AutodetectNumberMap<O> {
        val entries = mutableMapOf<O, AutodetectNumberEntry>()
        val storedFieldSettings = mutableListOf<ImportFieldSettings>()

        fun add(bean: O, fieldSettings: ImportFieldSettings, str: String?) {
            str ?: return
            if (storedFieldSettings.none { it.property == fieldSettings.property }) {
                storedFieldSettings.add(fieldSettings)
            }
            var value = entries[bean]
            if (value == null) {
                value = AutodetectNumberEntry()
                entries[bean] = value
            }
            value.add(fieldSettings, str)
        }
    }

    private class AutodetectNumberEntry {
        // Key is the property, value is the string to parse.
        val valueStrings = mutableMapOf<ImportFieldSettings, String>()

        fun add(fieldSettings: ImportFieldSettings, str: String) {
            valueStrings[fieldSettings] = str
        }
    }

    companion object {
        private const val UTF8_ESCAPE_BYTE = 195.toByte() // C3
        private const val UTF16_NULL_BYTE = 0.toByte() // 00
    }
}