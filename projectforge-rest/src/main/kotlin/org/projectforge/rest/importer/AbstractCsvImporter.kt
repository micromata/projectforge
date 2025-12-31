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
 * Context information available during CSV row processing.
 * Provides access to all row values and column mappings.
 */
data class CsvRowContext<O : ImportPairEntry.Modified<O>>(
    val rowValues: List<String>,
    val columnMapping: Map<Int, ImportFieldSettings>,
    val importStorage: ImportStorage<O>,
) {
    /**
     * Get the value from a specific column by field property name.
     */
    fun getValueByProperty(property: String): String? {
        val columnIndex = columnMapping.entries.find { it.value.property == property }?.key
        return if (columnIndex != null && columnIndex < rowValues.size) {
            rowValues[columnIndex]
        } else {
            null
        }
    }

    /**
     * Get the ImportFieldSettings for a specific property name.
     */
    fun getFieldSettingsByProperty(property: String): ImportFieldSettings? {
        return columnMapping.values.find { it.property == property }
    }
}

/**
 * Abstract base class for extensible CSV importers that allows custom object mapping and parsing.
 * Implements the Template Method Pattern to provide extension points for custom logic while
 * maintaining the standard CSV import workflow.
 *
 * @param O the type of objects being imported, must extend ImportPairEntry.Modified
 */
abstract class AbstractCsvImporter<O : ImportPairEntry.Modified<O>> {

    open val logErrorOnPropertyParsing: Boolean = true

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
        val detectedCharset = detectCharset(bytes, defaultCharset)

        try {
            parse(ByteArrayInputStream(bytes).reader(charset = detectedCharset), importStorage)
        } catch (e: Exception) {
            log.warn("Failed to parse with detected charset ${detectedCharset.name()}, trying fallback charset", e)
            // Try with alternative charset if the first attempt fails
            val fallbackCharset = if (detectedCharset == StandardCharsets.UTF_8) {
                StandardCharsets.ISO_8859_1
            } else {
                StandardCharsets.UTF_8
            }
            log.info("Retrying CSV parse with fallback charset: ${fallbackCharset.name()}")
            parse(ByteArrayInputStream(bytes).reader(charset = fallbackCharset), importStorage)
        }
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

            // Create row context for custom processing
            val rowContext = CsvRowContext(line, importStorage.columnMapping, importStorage)

            // Process each cell in the row
            line.forEachIndexed { columnIndex, value ->
                importStorage.columnMapping[columnIndex]?.let { fieldSettings ->
                    processRowCell(record, fieldSettings, value, autodetectNumberFormatMap, rowContext)
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
     * @param rowContext the row context containing all row values and import storage
     * @return true if field was handled, false for standard processing
     */
    protected open fun processField(
        entity: O,
        fieldSettings: ImportFieldSettings,
        value: String,
        rowContext: CsvRowContext<O>
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
        rowContext: CsvRowContext<O>
    ) {
        // Try custom processing first
        if (processField(record, fieldSettings, value, rowContext)) {
            return // Custom processing handled the field
        }

        // Check if ImportStorage wants to handle it
        if (!rowContext.importStorage.setProperty(record, fieldSettings, value)) {
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
        val propertyType = BeanHelper.determinePropertyType(record::class.java, fieldSettings.property)
        return when (propertyType) {
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

            Int::class.java, Integer::class.java -> {
                fieldSettings.parseInt(value)
            }

            Long::class.java, java.lang.Long::class.java -> {
                fieldSettings.parseLong(value)
            }

            Boolean::class.java, java.lang.Boolean::class.java -> {
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
            if (logErrorOnPropertyParsing) {
                log.error("Can't parse property: '${fieldSettings.property}': ${ex.message}")
            } else {
                log.debug("Can't parse property: '${fieldSettings.property}': ${ex.message}")
            }
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
     * Enhanced charset detection for better handling of UTF-8 vs ISO-8859-1.
     * Uses multiple heuristics including BOM detection, UTF-8 validation, and content analysis.
     */
    protected fun detectCharset(bytes: ByteArray, defaultCharset: Charset?): Charset {
        if (bytes.isEmpty()) {
            return defaultCharset ?: StandardCharsets.UTF_8
        }

        // 1. Check for BOM (Byte Order Mark)
        val detectedByBOM = detectCharsetByBOM(bytes)
        if (detectedByBOM != null) {
            log.debug("Charset detected by BOM: ${detectedByBOM.name()}")
            return detectedByBOM
        }

        // 2. Check for UTF-16 (null bytes)
        val utf16Detection = detectUTF16(bytes)
        if (utf16Detection != null) {
            log.debug("Charset detected as UTF-16")
            return utf16Detection
        }

        // 3. Try to validate as UTF-8
        val isValidUTF8 = isValidUTF8(bytes)
        if (isValidUTF8) {
            log.debug("Charset detected as UTF-8 (valid UTF-8 sequences)")
            return StandardCharsets.UTF_8
        }

        // 4. Check for typical German characters in ISO-8859-1 range
        val hasISO88591GermanChars = hasGermanCharsInISO88591Range(bytes)
        if (hasISO88591GermanChars) {
            log.debug("Charset detected as ISO-8859-1 (German characters in ISO range)")
            return StandardCharsets.ISO_8859_1
        }

        // 5. Fallback to default or UTF-8
        val fallback = defaultCharset ?: StandardCharsets.UTF_8
        log.debug("Charset detection fallback: ${fallback.name()}")
        return fallback
    }

    private fun detectCharsetByBOM(bytes: ByteArray): Charset? {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }
        return null
    }

    private fun detectUTF16(bytes: ByteArray): Charset? {
        var nullBytes = 0
        val size = bytes.size
        val sampleSize = minOf(size, 10000) // Sample first 10KB

        for (i in 0 until sampleSize) {
            if (bytes[i] == 0.toByte()) {
                nullBytes++
            }
        }

        val nullByteRate = nullBytes * 10 / sampleSize
        return if (nullByteRate > 1) { // More than 10% null bytes
            StandardCharsets.UTF_16
        } else {
            null
        }
    }

    private fun isValidUTF8(bytes: ByteArray): Boolean {
        return try {
            val decoded = String(bytes, StandardCharsets.UTF_8)
            // Check if re-encoding produces the same bytes (valid UTF-8)
            val reencoded = decoded.toByteArray(StandardCharsets.UTF_8)
            bytes.contentEquals(reencoded)
        } catch (e: Exception) {
            false
        }
    }

    private fun hasGermanCharsInISO88591Range(bytes: ByteArray): Boolean {
        // Check for German umlauts in ISO-8859-1 byte range (0x80-0xFF)
        val germanCharsISO88591 = setOf(
            0xE4.toByte(), // ä
            0xF6.toByte(), // ö
            0xFC.toByte(), // ü
            0xC4.toByte(), // Ä
            0xD6.toByte(), // Ö
            0xDC.toByte(), // Ü
            0xDF.toByte()  // ß
        )

        return bytes.any { byte -> byte in germanCharsISO88591 }
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

    // =============================================================================
    // Header Encoding Utilities
    // =============================================================================

    protected fun normalizeHeader(header: String): String {
        val trimmedHeader = header.trim()

        // First try to fix common encoding issues
        val fixedHeader = fixEncodingIssues(trimmedHeader)

        // If we still have problematic characters, try to recover from wrong charset interpretation
        val finalHeader = if (containsEncodingProblems(fixedHeader)) {
            tryRecoverFromWrongCharset(fixedHeader)
        } else {
            fixedHeader
        }

        if (trimmedHeader != finalHeader) {
            log.debug("Header normalization: '$trimmedHeader' -> '$finalHeader'")
        }
        return finalHeader
    }

    private fun containsEncodingProblems(text: String): Boolean {
        // Check for common signs of encoding problems
        return text.contains("�") ||
               text.contains("Ã") ||
               text.matches(Regex(".*[À-ÿ]{2,}.*")) // Multiple accented chars in sequence (likely encoding issue)
    }

    private fun tryRecoverFromWrongCharset(text: String): String {
        // Common recovery scenarios
        val recoveryAttempts = listOf(
            // Try ISO-8859-1 -> UTF-8 conversion
            { tryCharsetConversion(text, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8) },
            // Try UTF-8 -> ISO-8859-1 conversion
            { tryCharsetConversion(text, StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1) },
            // Try Windows-1252 -> UTF-8 conversion
            { tryCharsetConversion(text, Charset.forName("Windows-1252"), StandardCharsets.UTF_8) }
        )

        for (attempt in recoveryAttempts) {
            val recovered = attempt()
            if (recovered != null && !containsEncodingProblems(recovered) && recovered != text) {
                log.debug("Successfully recovered header encoding: '$text' -> '$recovered'")
                return recovered
            }
        }

        return text // Return original if no recovery worked
    }

    private fun tryCharsetConversion(text: String, sourceCharset: Charset, targetCharset: Charset): String? {
        return try {
            val bytes = text.toByteArray(sourceCharset)
            val converted = String(bytes, targetCharset)
            // Only return if the conversion seems meaningful (contains expected German characters)
            if (converted.any { it in "äöüÄÖÜß" } || !converted.contains("�")) {
                converted
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    protected fun fixEncodingIssues(text: String): String {
        var result = text

        // Common UTF-8 to ISO-8859-1 encoding issues for individual characters
        val encodingFixes = mapOf(
            // UTF-8 characters incorrectly displayed as ISO-8859-1
            "Ã¤" to "ä",  // UTF-8 ä as ISO-8859-1
            "Ã¶" to "ö",  // UTF-8 ö as ISO-8859-1
            "Ã¼" to "ü",  // UTF-8 ü as ISO-8859-1
            "Ã„" to "Ä",  // UTF-8 Ä as ISO-8859-1
            "Ã–" to "Ö",  // UTF-8 Ö as ISO-8859-1
            "Ãœ" to "Ü",  // UTF-8 Ü as ISO-8859-1
            "ÃŸ" to "ß",  // UTF-8 ß as ISO-8859-1

            // Replacement character fixes
            "ä" to "ä",
            "ö" to "ö",
            "ü" to "ü",
            "Ä" to "Ä",
            "Ö" to "Ö",
            "Ü" to "Ü",
            "ß" to "ß"
        )

        encodingFixes.forEach { (broken, fixed) ->
            result = result.replace(broken, fixed)
        }

        // Additional fallback: try to convert bytes if it still looks like mojibake
        if (result.contains("�") || result.contains("Ã")) {
            result = tryFixMojibake(result)
        }

        return result
    }

    protected fun tryFixMojibake(text: String): String {
        return try {
            // Try to fix double-encoding issues: assume text was UTF-8 encoded as ISO-8859-1
            val bytes = text.toByteArray(StandardCharsets.ISO_8859_1)
            val recovered = String(bytes, StandardCharsets.UTF_8)

            // Only use the recovered version if it looks better (contains proper German characters)
            if (recovered.any { it in "äöüÄÖÜß" } && !recovered.contains("�")) {
                recovered
            } else {
                text
            }
        } catch (e: Exception) {
            log.debug("Failed to fix mojibake for: $text", e)
            text // Return original if fixing fails
        }
    }

    companion object {
        private const val UTF8_ESCAPE_BYTE = 195.toByte() // C3
        private const val UTF16_NULL_BYTE = 0.toByte() // 00
    }
}
