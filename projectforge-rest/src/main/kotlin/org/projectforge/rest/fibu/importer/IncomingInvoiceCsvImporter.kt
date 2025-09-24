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
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.common.BeanHelper
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Konto
import org.projectforge.rest.importer.AbstractCsvImporter
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.rest.importer.ImportFieldSettings
import org.projectforge.rest.importer.ImportStorage

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
    private val eingangsrechnungDao: EingangsrechnungDao,
    private val kostCache: KostCache,
    private val kontoCache: KontoCache,
) : AbstractCsvImporter<EingangsrechnungPosImportDTO>() {

    private lateinit var storage: EingangsrechnungImportStorage

    override fun processField(
        entity: EingangsrechnungPosImportDTO,
        fieldSettings: ImportFieldSettings,
        value: String,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ): Boolean {
        // Store reference to storage for use in other methods
        if (!::storage.isInitialized && importStorage is EingangsrechnungImportStorage) {
            storage = importStorage
        }

        // Handle special fields that need custom processing during parsing
        return when (fieldSettings.property) {
            "konto" -> {
                // Parse DATEV account number directly from Konto field
                parseKonto(value, entity, importStorage)
                false // Also let standard processing store the raw value
            }

            "kost1" -> {
                // Parse KOST1 directly during CSV parsing
                parseKost1FromString(value, entity, importStorage)
                true // Prevent standard processing since we've handled it
            }

            "kost2" -> {
                // Parse KOST2 directly during CSV parsing
                parseKost2FromString(value, entity, importStorage)
                true // Prevent standard processing since we've handled it
            }

            "periodString" -> {
                // Parse period directly from a field like "01.05.2025-31.05.2025"
                parsePeriod(value, entity, importStorage)
                true
            }

            "leistungsdatum" -> {
                // Handle leistungsdatum field which might contain period info
                if (value.contains("-")) {
                    // Try to parse as period from leistungsdatum field
                    parsePeriod(value, entity, importStorage)
                }
                false // Let standard processing also handle date parsing
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

        // KOST1 and KOST2 are now parsed during CSV processing in processField method
    }

    override fun finalizeImport(
        records: List<EingangsrechnungPosImportDTO>,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ) {
        // Consolidate and validate after all rows are processed
        if (importStorage is EingangsrechnungImportStorage) {
            consolidateInvoicesByRenr(importStorage)

            log.info("Import finalized: ${records.size} positions processed")
            log.info("Found ${importStorage.consolidatedInvoices.size} consolidated invoices")

            if (importStorage.errorList.isNotEmpty()) {
                log.warn("Import has ${importStorage.errorList.size} errors: ${importStorage.errorList}")
            }
        }
    }

    // =============================================================================
    // Custom Processing Methods (from IncomingInvoicePosCsvParser)
    // =============================================================================

    private fun parsePeriod(
        periodStr: String,
        invoicePos: EingangsrechnungPosImportDTO,
        importStorage: ImportStorage<EingangsrechnungPosImportDTO>
    ) {
        val parts = periodStr.split("-")
        if (parts.size == 2) {
            try {
                val dateSettings = ImportFieldSettings("periodFrom")
                invoicePos.periodFrom = dateSettings.parseLocalDate(parts[0].trim())
                invoicePos.periodUntil = dateSettings.parseLocalDate(parts[1].trim())
            } catch (e: Exception) {
                val errorMsg = "Could not parse period '$periodStr'"
                addError(invoicePos, errorMsg, importStorage)
                log.warn(errorMsg, e)
            }
        }
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
                val errorMsg = "Konto '$datevAccountNumberStr' not found."
                addError(invoicePos, errorMsg, importStorage)
                log.warn(errorMsg)
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
                    description = kost1.description
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
                    description = kost2.description
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


    private fun consolidateInvoicesByRenr(importStorage: EingangsrechnungImportStorage) {
        log.info("Starting consolidation of invoices by RENR...")

        val groupedByRenr = importStorage.readInvoices.groupBy { it.referenz ?: "UNKNOWN" }
        log.info("Found ${groupedByRenr.size} different RENR groups with ${importStorage.readInvoices.size} total positions")

        groupedByRenr.forEach { (renr, positions) ->
            log.debug("Processing RENR '$renr' with ${positions.size} positions")

            // Assign position numbers (1, 2, 3, ...) in reading order
            assignPositionNumbers(renr, positions)

            // Validate header consistency
            validateInvoiceHeaderConsistency(renr, positions, importStorage)
        }

        importStorage.consolidatedInvoices = groupedByRenr.filterKeys { it != "UNKNOWN" }
        log.info("Consolidation completed. ${importStorage.consolidatedInvoices.size} invoices consolidated.")
    }

    private fun assignPositionNumbers(renr: String, positions: List<EingangsrechnungPosImportDTO>) {
        log.debug("Assigning position numbers for RENR '$renr' with ${positions.size} positions")
        positions.forEachIndexed { index, position ->
            val posNumber = index + 1
            position.positionNummer = posNumber
            log.debug("Assigned position number $posNumber to position in RENR '$renr' (creditor: ${position.kreditor}, amount: ${position.grossSum})")
        }
        log.debug("Position number assignment completed for RENR '$renr'")
    }

    private fun validateInvoiceHeaderConsistency(
        renr: String,
        positions: List<EingangsrechnungPosImportDTO>,
        importStorage: EingangsrechnungImportStorage
    ) {
        if (positions.isEmpty()) return

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
