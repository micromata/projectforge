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

package org.projectforge.rest.fibu.importer

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Konto
import org.projectforge.rest.importer.*

private val log = KotlinLogging.logger {}

/**
 * Extensible CSV importer for incoming invoices that handles all processing in a single pass.
 * This consolidates the functionality previously split between CsvImporter and IncomingInvoicePosCsvParser.
 *
 * Features:
 * - Custom field processing during CSV parsing (periods, DATEV accounts, etc.)
 * - Post-processing for each row (KOST lookups, VAT calculations)
 * - Final consolidation and validation by invoice number
 * - Session storage for web navigation
 */
class IncomingInvoiceCsvImporter(
    private val kostCache: KostCache,
    private val kontoCache: KontoCache,
) : AbstractCsvImporter<EingangsrechnungPosImportDTO>() {
    override val logErrorOnPropertyParsing: Boolean = false

    private lateinit var storage: EingangsrechnungImportStorage

    override fun processHeaders(
        headers: List<String>,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ): List<String> {
        // Store typed reference for later use
        storage = importStorage as EingangsrechnungImportStorage
        val normalizedHeaders = headers.map { header -> normalizeHeader(header) }

        // Detect import mode based on presence of "Periode" column
        val hasPeriodenColumn = normalizedHeaders.any { header ->
            header.trim() == "Periode"
        }

        storage.isPositionBasedImport = hasPeriodenColumn

        log.info("Import mode detected: ${if (hasPeriodenColumn) "Position-based" else "Header-only"} (Periode column ${if (hasPeriodenColumn) "found" else "not found"})")

        return normalizedHeaders
    }

    override fun processField(
        entity: EingangsrechnungPosImportDTO,
        fieldSettings: ImportFieldSettings,
        value: String,
        rowContext: CsvRowContext<EingangsrechnungPosImportDTO>
    ): Boolean {
        // Store reference to storage for use in other methods
        if (!::storage.isInitialized && rowContext.importStorage is EingangsrechnungImportStorage) {
            storage = rowContext.importStorage
        }

        // Handle special fields that need custom processing during parsing
        return when (fieldSettings.property) {
            "konto" -> {
                // Parse DATEV account number directly from Konto field
                parseKonto(value, entity, rowContext.importStorage)
                false // Also let standard processing store the raw value
            }

            "kost1" -> {
                // Only parse KOST1 for position-based imports
                if (storage.isPositionBasedImport) {
                    parseKost1FromString(value, entity, rowContext.importStorage)
                    true // Prevent standard processing since we've handled it
                } else {
                    log.debug { "Ignoring KOST1 field '$value' in header-only import mode" }
                    true // Prevent standard processing but don't parse
                }
            }

            "kost2" -> {
                // Only parse KOST2 for position-based imports
                if (storage.isPositionBasedImport) {
                    parseKost2FromString(value, entity, rowContext.importStorage)
                    true // Prevent standard processing since we've handled it
                } else {
                    log.debug { "Ignoring KOST2 field '$value' in header-only import mode" }
                    true // Prevent standard processing but don't parse
                }
            }

            "datum" -> {
                // Handle invoice date with period-based year fallback for DD.MM format
                val parsedDate = parseDateWithPeriodYearFallback(value, entity, fieldSettings, rowContext)
                if (parsedDate != null) {
                    entity.datum = parsedDate
                    true // Prevent standard processing since we handled it successfully
                } else {
                    false // Let standard processing try if our enhanced parsing failed
                }
            }

            else -> {
                false // Let standard processing handle the field normally
            }
        }
    }

    override fun postProcessEntity(
        entity: EingangsrechnungPosImportDTO,
        rowIndex: Int,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ) {
        // Process fields that might have been stored as strings during standard parsing

        // Parse DATEV account if stored as konto number
        if (entity.konto != null && entity.konto!!.nummer != null) {
            parseKonto(entity.konto!!.nummer.toString(), entity, importStorage)
        }

        // Normalize taxRate: CSV values > 1 are in percentage format (e.g., 19.00 for 19%)
        // and need to be divided by 100 to match DB format (e.g., 0.19)
        entity.taxRate?.let { taxRate ->
            if (taxRate > java.math.BigDecimal.ONE) {
                entity.taxRate = taxRate
                    .divide(java.math.BigDecimal(100), 5, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                log.debug { "Normalized taxRate from $taxRate% to ${entity.taxRate} for invoice ${entity.referenz}" }
            }
        }

        // KOST1 and KOST2 are now parsed during CSV processing in processField method
    }

    override fun finalizeImport(
        records: List<EingangsrechnungPosImportDTO>,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ) {
        // Debug logging for import storage type
        log.info("FINALIZE DEBUG: importStorage type is ${importStorage::class.simpleName}, isPositionBasedImport=${(importStorage as? EingangsrechnungImportStorage)?.isPositionBasedImport}")

        // Consolidate and validate after all rows are processed
        if (importStorage is EingangsrechnungImportStorage) {
            if (storage.isPositionBasedImport) {
                log.info("Position-based import: consolidating ${records.size} positions")
                consolidateInvoicesByRenr(records, importStorage)
            } else {
                log.info("Header-only import: processing ${records.size} invoice headers")
                consolidateHeaderOnlyInvoices(records, importStorage)
            }

            log.info("Import finalized: ${records.size} records processed")
            log.info("Found ${importStorage.consolidatedInvoices.size} consolidated invoices")

            if (importStorage.errorList.isNotEmpty()) {
                log.warn("Import has ${importStorage.errorList.size} errors: ${importStorage.errorList}")
            }
        } else {
            log.warn("FINALIZE DEBUG: importStorage is NOT EingangsrechnungImportStorage - no consolidation will occur!")
        }
    }

    // =============================================================================
    // Custom Processing Methods (from IncomingInvoicePosCsvParser)
    // =============================================================================

    private fun parseDateWithPeriodYearFallback(
        dateStr: String,
        invoicePos: EingangsrechnungPosImportDTO,
        fieldSettings: ImportFieldSettings,
        rowContext: CsvRowContext<EingangsrechnungPosImportDTO>
    ): java.time.LocalDate? {
        // First try normal date parsing
        val normalParse = fieldSettings.parseLocalDate(dateStr)
        if (normalParse != null) {
            return normalParse
        }

        // If normal parsing failed, try DD.MM format with year from period
        if (dateStr.matches(Regex("\\d{1,2}\\.\\d{1,2}\\.?"))) {
            val yearFromPeriod = extractYearFromPeriodString(rowContext)

            if (yearFromPeriod != null) {
                try {
                    // Clean the date string and add the year
                    val cleanDateStr = dateStr.removeSuffix(".")
                    val dateWithYear = "$cleanDateStr.$yearFromPeriod"
                    log.debug { "Trying to parse invoice date '$dateStr' as '$dateWithYear' using year from period" }

                    return fieldSettings.parseLocalDate(dateWithYear)
                } catch (e: Exception) {
                    log.debug(e) { "Could not parse invoice date '$dateStr' with period year $yearFromPeriod" }
                }
            }
        }

        return null
    }

    private fun extractYearFromPeriodString(rowContext: CsvRowContext<EingangsrechnungPosImportDTO>): Int? {
        // Try to get period string from various possible column names
        val periodString = rowContext.getValueByProperty("periode")

        if (!periodString.isNullOrBlank()) {
            val parts = periodString.split("-")
            if (parts.size == 2) {
                try {
                    // Use existing field settings for period parsing, or fallback to a basic one
                    val periodFieldSettings = rowContext.getFieldSettingsByProperty("datum")!!

                    val firstDate = periodFieldSettings.parseLocalDate(parts[0].trim())
                    return firstDate?.year
                } catch (e: Exception) {
                    log.debug(e) { "Could not extract year from period string '$periodString'" }
                }
            }
        }

        return null
    }


    private fun parseKonto(
        datevAccountNumberStr: String,
        invoicePos: EingangsrechnungPosImportDTO,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ) {
        try {
            val datevAccountNumber = NumberHelper.parseLocalizedInt(datevAccountNumberStr, strict = true)
            val konto = kontoCache.findKontoByNumber(datevAccountNumber)

            if (konto != null) {
                invoicePos.konto = Konto().apply {
                    id = konto.id
                    nummer = konto.nummer
                }
            } else {
                if (datevAccountNumberStr.isNotBlank()) {
                    val errorMsg = "Konto '$datevAccountNumberStr' not found."
                    addError(invoicePos, errorMsg, importStorage)
                    log.warn(errorMsg)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Could not parse DATEV account '$datevAccountNumberStr'"
            addError(invoicePos, errorMsg, importStorage)
            log.warn(errorMsg, e)
        }
    }


    private fun parseKost1FromString(
        kost1String: String,
        invoicePos: EingangsrechnungPosImportDTO,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ) {
        if (kost1String.isBlank()) return

        try {
            val kost1 = kostCache.findKost1(kost1String)
            if (kost1 != null) {
                invoicePos.kost1 = org.projectforge.rest.dto.Kost1().apply {
                    id = kost1.id
                    nummernkreis = kost1.nummernkreis
                    bereich = kost1.bereich
                    teilbereich = kost1.teilbereich
                    endziffer = kost1.endziffer
                    description = kost1.formattedNumber
                }
            } else {
                val errorMsg = "KOST1 '$kost1String' not found."
                addError(invoicePos, errorMsg, importStorage)
                log.warn(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Could not parse KOST1 '$kost1String'"
            addError(invoicePos, errorMsg, importStorage)
            log.warn(errorMsg, e)
        }
    }

    private fun parseKost2FromString(
        kost2String: String,
        invoicePos: EingangsrechnungPosImportDTO,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ) {
        if (kost2String.isBlank()) return

        try {
            val kost2 = kostCache.findKost2(kost2String)
            if (kost2 != null) {
                invoicePos.kost2 = org.projectforge.rest.dto.Kost2().apply {
                    id = kost2.id
                    description = kost2.formattedNumber
                }
            } else {
                val errorMsg = "KOST2 '$kost2String' not found."
                addError(invoicePos, errorMsg, importStorage)
                log.warn(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Could not parse KOST2 '$kost2String'"
            addError(invoicePos, errorMsg, importStorage)
            log.warn(errorMsg, e)
        }
    }


    private fun consolidateInvoicesByRenr(
        records: List<EingangsrechnungPosImportDTO>,
        importStorage: EingangsrechnungImportStorage
    ) {
        log.info("Starting consolidation of invoices by RENR, Kreditor, and Datum...")

        // Group by combination of RENR, Kreditor, and Datum to identify unique invoices
        val groupedByInvoiceKey = records.groupBy {
            val renr = it.referenz ?: "UNKNOWN"
            val kreditor = it.kreditor ?: "UNKNOWN_KREDITOR"
            val datum = it.datum?.toString() ?: "UNKNOWN_DATUM"
            "$renr|$kreditor|$datum"
        }

        log.info("Found ${groupedByInvoiceKey.size} unique invoices (by RENR+Kreditor+Datum) with ${records.size} total positions")

        var consolidatedInvoices = mutableMapOf<String, List<EingangsrechnungPosImportDTO>>()

        groupedByInvoiceKey.forEach { (invoiceKey, positions) ->
            val parts = invoiceKey.split("|")
            val renr = parts[0]
            val kreditor = parts[1]
            val datum = parts[2]

            log.debug { "Processing invoice RENR='$renr', Kreditor='$kreditor', Datum='$datum' with ${positions.size} positions" }

            // Assign position numbers (1, 2, 3, ...) in reading order
            assignPositionNumbers(renr, positions)

            // Validate header consistency (only for positions of the same actual invoice)
            validateInvoiceHeaderConsistency(renr, positions, importStorage)

            // Store by RENR for backward compatibility (multiple invoices may have same RENR but different kreditor/datum)
            if (renr != "UNKNOWN") {
                consolidatedInvoices[renr] = (consolidatedInvoices[renr] ?: emptyList()) + positions
            }
        }

        importStorage.consolidatedInvoices = consolidatedInvoices
        log.info("Consolidation completed. ${importStorage.consolidatedInvoices.size} different RENRs with ${groupedByInvoiceKey.size} unique invoices.")

        // Check for duplicate invoices (same RENR+datum, different kreditor)
        checkForDuplicateInvoices(records, importStorage)
    }

    private fun consolidateHeaderOnlyInvoices(
        records: List<EingangsrechnungPosImportDTO>,
        importStorage: EingangsrechnungImportStorage
    ) {
        log.info("Starting consolidation of header-only invoices...")

        // For header-only imports, each record represents a complete invoice
        // Group by RENR+Kreditor+Datum to handle duplicates but don't create multiple positions
        val groupedByInvoiceKey = records.groupBy {
            val renr = it.referenz ?: "UNKNOWN"
            val kreditor = it.kreditor ?: "UNKNOWN_KREDITOR"
            val datum = it.datum?.toString() ?: "UNKNOWN_DATUM"
            "$renr|$kreditor|$datum"
        }

        log.info("Found ${groupedByInvoiceKey.size} unique invoices (by RENR+Kreditor+Datum) from ${records.size} header records")

        var consolidatedInvoices = mutableMapOf<String, List<EingangsrechnungPosImportDTO>>()

        groupedByInvoiceKey.forEach { (invoiceKey, headerRecords) ->
            val parts = invoiceKey.split("|")
            val renr = parts[0]
            val kreditor = parts[1]
            val datum = parts[2]

            log.debug { "Processing header-only invoice RENR='$renr', Kreditor='$kreditor', Datum='$datum'" }

            if (headerRecords.size > 1) {
                val warningMsg =
                    "Multiple header records found for same invoice: RENR '$renr', Kreditor '$kreditor', Datum '$datum' (${headerRecords.size} records). Using first record."
                importStorage.addWarning(warningMsg)
                log.warn(warningMsg)
            }

            // For header-only import, use only the first record as single position
            val primaryRecord = headerRecords.first()
            primaryRecord.positionNummer = 1

            // Validate header consistency within duplicate records
            if (headerRecords.size > 1) {
                validateInvoiceHeaderConsistency(renr, headerRecords, importStorage)
            }

            // Store by RENR for backward compatibility
            if (renr != "UNKNOWN") {
                consolidatedInvoices[renr] = listOf(primaryRecord)
            }
        }

        importStorage.consolidatedInvoices = consolidatedInvoices
        log.info("Header-only consolidation completed. ${importStorage.consolidatedInvoices.size} invoices consolidated.")

        // Check for duplicate invoices (same RENR+datum, different kreditor)
        checkForDuplicateInvoices(records, importStorage)
    }

    private fun checkForDuplicateInvoices(
        records: List<EingangsrechnungPosImportDTO>,
        importStorage: EingangsrechnungImportStorage
    ) {
        log.info("Checking for duplicate invoices (same RENR+datum, different kreditor)...")

        // Group by RENR+datum combination
        val groupedByRenrAndDatum = records.groupBy {
            val renr = it.referenz ?: "UNKNOWN"
            val datum = it.datum?.toString() ?: "UNKNOWN_DATUM"
            "$renr|$datum"
        }

        var duplicatesFound = 0

        groupedByRenrAndDatum.forEach { (renrDatumKey, positions) ->
            // Check if there are multiple different kreditors for the same RENR+datum
            val kreditors = positions.map { it.kreditor }.filterNotNull().distinct()

            if (kreditors.size > 1) {
                val parts = renrDatumKey.split("|")
                val renr = parts[0]
                val datum = parts[1]

                val warningMsg =
                    "Duplicate invoice detected: RENR '$renr', Datum '$datum' has ${kreditors.size} different creditors: ${
                        kreditors.joinToString(", ")
                    }"
                importStorage.addWarning(warningMsg)
                log.warn(warningMsg)
                duplicatesFound++

                // Log details for each creditor
                kreditors.forEach { kreditor ->
                    val positionsForKreditor = positions.filter { it.kreditor == kreditor }
                    log.debug { "  - Kreditor '$kreditor': ${positionsForKreditor.size} positions" }
                }
            }
        }

        if (duplicatesFound > 0) {
            log.info("Found $duplicatesFound duplicate invoice(s) with same RENR+datum but different kreditors")
        } else {
            log.info("No duplicate invoices found")
        }
    }

    private fun assignPositionNumbers(renr: String, positions: List<EingangsrechnungPosImportDTO>) {
        log.debug { "Assigning position numbers for RENR '$renr' with ${positions.size} positions" }
        positions.forEachIndexed { index, position ->
            val posNumber = index + 1
            position.positionNummer = posNumber
            log.debug { "Assigned position number $posNumber to position in RENR '$renr' (creditor: ${position.kreditor}, amount: ${position.grossSum})" }
        }
        log.debug { "Position number assignment completed for RENR '$renr'" }
    }

    private fun validateInvoiceHeaderConsistency(
        renr: String,
        positions: List<EingangsrechnungPosImportDTO>,
        importStorage: EingangsrechnungImportStorage
    ) {
        if (positions.isEmpty()) return

        // For header-only imports, validation is less strict as there's only one record per invoice
        if (!storage.isPositionBasedImport) {
            log.debug { "Header-only import: skipping position consistency validation for RENR '$renr'" }
            return
        }

        val invoiceHeaderFields = listOf(
            "kreditor", "konto", "referenz", "datum", "faelligkeit",
            "bezahlDatum", "currency", "zahlBetrag", "discountMaturity",
            "discountPercent", "iban", "bic", "receiver", "paymentType",
            "customernr", "bemerkung"
        )

        invoiceHeaderFields.forEach { fieldName ->
            validateFieldConsistency(renr, fieldName, positions, importStorage)
        }
    }

    private fun validateFieldConsistency(
        renr: String,
        fieldName: String,
        positions: List<EingangsrechnungPosImportDTO>,
        importStorage: EingangsrechnungImportStorage
    ) {
        val distinctValues = positions.map { getFieldValue(it, fieldName) }
            .filter { it != null }
            .distinct()

        if (distinctValues.size > 1) {
            val errorMessage =
                "RENR '$renr': Inkonsistente Werte für '$fieldName': ${distinctValues.joinToString(", ")}"
            log.error(errorMessage)
            importStorage.addError(errorMessage)

            positions.forEach { position ->
                addError(position, "Inkonsistente Rechnungsheader-Daten für $fieldName", importStorage)
            }
        }
    }

    private fun getFieldValue(dto: EingangsrechnungPosImportDTO, fieldName: String): Any? {
        return when (fieldName) {
            "kreditor" -> dto.kreditor
            "konto" -> dto.konto?.let { "${it.id}:${it.nummer}" }
            "referenz" -> dto.referenz
            "datum" -> dto.datum
            "faelligkeit" -> dto.faelligkeit
            "bezahlDatum" -> dto.bezahlDatum
            "currency" -> dto.currency
            "zahlBetrag" -> dto.zahlBetrag
            "discountMaturity" -> dto.discountMaturity
            "discountPercent" -> dto.discountPercent
            "iban" -> dto.iban
            "bic" -> dto.bic
            "receiver" -> dto.receiver
            "paymentType" -> dto.paymentType
            "customernr" -> dto.customernr
            "bemerkung" -> dto.bemerkung
            else -> null
        }
    }

    private fun addError(
        entity: EingangsrechnungPosImportDTO,
        errorMsg: String,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ) {
        if (importStorage is EingangsrechnungImportStorage) {
            val pairEntry = importStorage.pairEntries.find { it.read == entity }
            pairEntry?.addError(errorMsg)
        }
    }

    companion object {
        /**
         * Stores the import storage in session and returns URL to navigate to import page.
         * Moved from IncomingInvoicePosCsvParser for consolidation.
         */
        fun storeInSessionAndGetNavigationUrl(
            request: HttpServletRequest,
            storage: EingangsrechnungImportStorage
        ): String {
            val sessionAttributeName =
                AbstractImportPageRest.getSessionAttributeName(IncomingInvoicePosImportPageRest::class.java)
            log.info("Storing import storage in session with key: $sessionAttributeName")
            log.info("Storage contains ${storage.readInvoices.size} invoices, ${storage.pairEntries.size} pair entries")

            ExpiringSessionAttributes.setAttribute(
                request,
                sessionAttributeName,
                storage,
                20 // TTL in minutes
            )

            val navigationUrl =
                PagesResolver.getDynamicPageUrl(IncomingInvoicePosImportPageRest::class.java, absolute = true)
            log.info("Generated navigation URL: $navigationUrl")
            return navigationUrl
        }
    }
}
