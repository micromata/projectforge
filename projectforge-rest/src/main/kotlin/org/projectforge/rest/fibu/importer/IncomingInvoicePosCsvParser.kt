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
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.rest.importer.ImportFieldSettings
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

class IncomingInvoicePosCsvParser(
    private val storage: EingangsrechnungImportStorage,
    private val eingangsrechnungDao: EingangsrechnungDao,
    private val kostCache: KostCache,
    private val kontoCache: KontoCache,
) {

    /**
     * Post-process the imported data after CSV parsing is complete.
     * This method handles special field processing that requires custom logic.
     */
    fun postProcessImportedData() {
        log.info("Starting post-processing of imported CSV data...")

        storage.pairEntries.forEach { pairEntry ->
            val invoicePos = pairEntry.read ?: return@forEach

            // Parse period from period string (e.g., "01.05.2025-31.05.2025")
            val periodStr = invoicePos.bemerkung // Assuming period is stored in bemerkung temporarily
            if (periodStr != null && periodStr.contains("-")) {
                parsePeriod(periodStr, invoicePos, pairEntry)
            }

            // Parse DATEV account
            if (invoicePos.konto != null && invoicePos.konto!!.nummer != null) {
                parseKonto(invoicePos.konto!!.nummer.toString(), invoicePos, pairEntry)
            }

            // Parse KOST1 and KOST2
            parseKost1(invoicePos, pairEntry)
            parseKost2(invoicePos, pairEntry)

            // Calculate VAT amount from tax rate and gross sum
            calculateVatAmount(invoicePos, pairEntry)
        }

        consolidateInvoicesByRenr()

        log.info("Post-processing completed. Processed ${storage.readInvoices.size} invoice positions.")
        log.info("Found ${storage.consolidatedInvoices.size} consolidated invoices.")
        if (storage.errorList.isNotEmpty()) {
            log.warn("Import has ${storage.errorList.size} errors: ${storage.errorList}")
        }
    }

    private fun parsePeriod(periodStr: String, invoicePos: EingangsrechnungPosImportDTO, pairEntry: org.projectforge.rest.importer.ImportPairEntry<EingangsrechnungPosImportDTO>) {
        val parts = periodStr.split("-")
        if (parts.size == 2) {
            try {
                val dateSettings = ImportFieldSettings("periodFrom")
                invoicePos.periodFrom = dateSettings.parseLocalDate(parts[0].trim())
                invoicePos.periodUntil = dateSettings.parseLocalDate(parts[1].trim())
            } catch (e: Exception) {
                val errorMsg = "Could not parse period '$periodStr'"
                pairEntry.addError(errorMsg)
                log.warn(errorMsg, e)
            }
        }
    }

    private fun parseKonto(datevAccountNumberStr: String, invoicePos: EingangsrechnungPosImportDTO, pairEntry: org.projectforge.rest.importer.ImportPairEntry<EingangsrechnungPosImportDTO>) {
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
                pairEntry.addError(errorMsg)
                log.warn(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Could not parse DATEV account '$datevAccountNumberStr'"
            pairEntry.addError(errorMsg)
            log.warn(errorMsg, e)
        }
    }

    private fun parseKost1(invoicePos: EingangsrechnungPosImportDTO, pairEntry: org.projectforge.rest.importer.ImportPairEntry<EingangsrechnungPosImportDTO>) {
        val kost1Property = BeanHelper.getProperty(invoicePos, "kost1")
        val kost1Val = when (kost1Property) {
            is String -> if (kost1Property.isBlank()) null else kost1Property
            is Number -> kost1Property.toString()
            else -> null
        }

        if (kost1Val != null) {
            val kost1 = kostCache.findKost1(kost1Val)
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
                val errorMsg = "KOST1 '$kost1Val' not found."
                pairEntry.addError(errorMsg)
                log.warn(errorMsg)
            }
        }
    }

    private fun parseKost2(invoicePos: EingangsrechnungPosImportDTO, pairEntry: org.projectforge.rest.importer.ImportPairEntry<EingangsrechnungPosImportDTO>) {
        val kost2Property = BeanHelper.getProperty(invoicePos, "kost2")
        val kost2Val = when (kost2Property) {
            is String -> if (kost2Property.isBlank()) null else kost2Property
            is Number -> kost2Property.toString()
            else -> null
        }

        if (kost2Val != null) {
            val kost2 = kostCache.findKost2(kost2Val)
            if (kost2 != null) {
                invoicePos.kost2 = org.projectforge.rest.dto.Kost2().apply {
                    id = kost2.id
                    description = kost2.description
                }
            } else {
                val errorMsg = "KOST2 '$kost2Val' not found."
                pairEntry.addError(errorMsg)
                log.warn(errorMsg)
            }
        }
    }

    private fun calculateVatAmount(invoicePos: EingangsrechnungPosImportDTO, pairEntry: org.projectforge.rest.importer.ImportPairEntry<EingangsrechnungPosImportDTO>) {
        // Assuming tax rate is stored as a string property that needs parsing
        val taxRateProperty = BeanHelper.getProperty(invoicePos, "customernr") // Temporary storage for tax rate
        if (taxRateProperty is String && taxRateProperty.isNotBlank() && invoicePos.grossSum != null) {
            try {
                val taxRate = BigDecimal(taxRateProperty)
                invoicePos.vatAmountSum = invoicePos.grossSum!! * taxRate / BigDecimal("100")
            } catch (e: NumberFormatException) {
                val errorMsg = "Could not parse tax rate '$taxRateProperty'"
                pairEntry.addError(errorMsg)
                log.warn(errorMsg, e)
            }
        }
    }

    private fun consolidateInvoicesByRenr() {
        log.info("Starting consolidation of invoices by RENR...")

        val groupedByRenr = storage.readInvoices.groupBy { it.referenz ?: "UNKNOWN" }
        log.info("Found ${groupedByRenr.size} different RENR groups with ${storage.readInvoices.size} total positions")

        groupedByRenr.forEach { (renr, positions) ->
            log.debug("Processing RENR '$renr' with ${positions.size} positions")

            // Assign position numbers (1, 2, 3, ...) in reading order
            assignPositionNumbers(renr, positions)

            // Validate header consistency
            validateInvoiceHeaderConsistency(renr, positions)
        }

        storage.consolidatedInvoices = groupedByRenr.filterKeys { it != "UNKNOWN" }
        log.info("Consolidation completed. ${storage.consolidatedInvoices.size} invoices consolidated.")
    }

    private fun assignPositionNumbers(renr: String, positions: List<EingangsrechnungPosImportDTO>) {
        positions.forEachIndexed { index, position ->
            position.positionNummer = index + 1
            log.debug("Assigned position number ${position.positionNummer} to position in RENR '$renr'")
        }
    }

    private fun validateInvoiceHeaderConsistency(renr: String, positions: List<EingangsrechnungPosImportDTO>) {
        if (positions.isEmpty()) return

        val invoiceHeaderFields = listOf(
            "kreditor", "konto", "referenz", "datum", "faelligkeit",
            "bezahlDatum", "currency", "zahlBetrag", "discountMaturity",
            "discountPercent", "iban", "bic", "receiver", "paymentType",
            "customernr", "bemerkung"
        )

        invoiceHeaderFields.forEach { fieldName ->
            validateFieldConsistency(renr, fieldName, positions)
        }
    }

    private fun validateFieldConsistency(
        renr: String,
        fieldName: String,
        positions: List<EingangsrechnungPosImportDTO>
    ) {
        val distinctValues = positions.map { getFieldValue(it, fieldName) }
            .filter { it != null }
            .distinct()

        if (distinctValues.size > 1) {
            val errorMessage =
                "RENR '$renr': Inkonsistente Werte für '$fieldName': ${distinctValues.joinToString(", ")}"
            log.error(errorMessage)
            storage.addError(errorMessage)

            positions.forEach { position ->
                val pairEntry = storage.pairEntries.find { it.read == position }
                if (pairEntry != null) {
                    pairEntry.addError("Inkonsistente Rechnungsheader-Daten für $fieldName")
                }
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

    companion object {
        /**
         * Stores the import storage in session and returns URL to navigate to import page
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