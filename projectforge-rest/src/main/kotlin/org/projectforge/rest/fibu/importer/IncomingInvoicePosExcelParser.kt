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

import de.micromata.merlin.excel.*
import de.micromata.merlin.excel.importer.ImportHelper
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.CellType
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Konto
import org.projectforge.rest.importer.AbstractImportPageRest
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

class IncomingInvoicePosExcelParser(
    private val storage: EingangsrechnungImportStorage,
    private val eingangsrechnungDao: EingangsrechnungDao,
    private val kostCache: KostCache,
    private val kontoCache: KontoCache,
) {
    private val dateValidator = ExcelColumnDateValidator(
        ExcelColumnDateValidator.GERMAN_FORMATS,
        minimum = LocalDate.of(1990, 1, 1),
        maximum = LocalDate.of(2100, 12, 31)
    )

    private enum class Cols(override val head: String, override vararg val aliases: String) : ExcelColumnName {
        PERIOD("Periode"), // e.g. "01.05.2025-31.05.2025"
        AMMOUNT("Betrag"),
        CURRENCY("Währung", "WKZ"), // e.g. "EUR", "USD", ...
        DATE("Datum", "Rechnungsdatum"), // e.g. "01.05." or "01.05.2025"
        INVOICE_NUMBER("RENR", "Rechnungs-Nr."),
        CREDITOR("LieferantName", "Geschäftspartner-Name"),
        DATEV_ACCOUNT("LieferantKonto", "Geschäftspartner-Konto"),
        TEXT("Ware/Leistung"),
        DUE_DATE("Fällig_am"),
        PAID_DATE("gezahlt_am"),
        TAX_RATE("Steuer%"),
        COST1("KOST1", "KOST 1"),
        COST2("KOST2", "KOST 2"),
        ;
    }

    /**
     * @param storage The storage to store the imported data.
     * @param workbook The Excel workbook to import.
     * @param month The month of the salary data (begin of month).
     */
    fun parse(workbook: ExcelWorkbook) {
        val sheet = workbook.getSheet(0)
        val name = sheet.sheetName
        sheet.autotrimCellValues = true
        log.info("Reading sheet '${sheet.sheetName}'.")
        sheet.registerColumn(Cols.INVOICE_NUMBER, ExcelColumnValidator()).setTargetProperty("referenz")
        sheet.registerColumn(Cols.AMMOUNT, ExcelColumnValidator().setRequired()).setTargetProperty("grossSum")
        sheet.registerColumn(Cols.CREDITOR, ExcelColumnValidator()).setTargetProperty("kreditor")
        sheet.registerColumn(Cols.DATEV_ACCOUNT, ExcelColumnNumberValidator()).setTargetProperty("konto")
        sheet.registerColumn(Cols.COST1, ExcelColumnValidator()).setTargetProperty("kost1")
        sheet.registerColumn(Cols.COST2, ExcelColumnValidator()).setTargetProperty("kost2")
        sheet.registerColumn(Cols.CURRENCY, ExcelColumnValidator()).setTargetProperty("currency")
        sheet.registerColumn(Cols.DATE, dateValidator).setTargetProperty("datum")
        sheet.registerColumn(Cols.DUE_DATE, dateValidator).setTargetProperty("faelligkeit")
        sheet.registerColumn(Cols.PAID_DATE, dateValidator).setTargetProperty("bezahlDatum")
        sheet.registerColumn(Cols.TAX_RATE, ExcelColumnValidator())
        sheet.registerColumn(Cols.TEXT, ExcelColumnValidator()).setTargetProperty("betreff")
        sheet.registerColumn(Cols.PERIOD, ExcelColumnValidator())
        if (sheet.headRow == null) {
            val errorMsg = "Ignoring sheet '$name' for importing IncomingInvoicePositions, no valid head row found."
            log.info(errorMsg)
            storage.addError(errorMsg)
            return
        }
        sheet.setColumnsForRowEmptyCheck(
            // Cols.PERIOD, // Optional for invoice import (without all positions)
            Cols.DATE,
            Cols.INVOICE_NUMBER,
        )
        sheet.analyze(true)
        sheet.allValidationErrors
        storage.info
        // Use the REST import pattern instead of Merlin ImportedSheet
        // Parse Excel data and add to storage
        val it = sheet.dataRowIterator
        while (it.hasNext()) {
            val row = it.next()
            val pairEntry = storage.prepareImportPairEntry()
            val invoicePos = pairEntry.read!!

            // Use ImportHelper.fillBean to automatically fill basic fields
            ImportHelper.fillBean(invoicePos, sheet, row.rowNum)

            // Parse period from period string (e.g., "01.05.2025-31.05.2025")
            val periodStr = sheet.getCellString(row, Cols.PERIOD)
            if (periodStr != null && periodStr.contains("-")) {
                val parts = periodStr.split("-")
                if (parts.size == 2) {
                    try {
                        invoicePos.periodFrom =
                            dateValidator.getDate(sheet.getCell(row, Cols.PERIOD)?.takeIf { parts[0].isNotBlank() })
                        invoicePos.periodUntil =
                            dateValidator.getDate(sheet.getCell(row, Cols.PERIOD)?.takeIf { parts[1].isNotBlank() })
                    } catch (e: Exception) {
                        val errorMsg = "Could not parse period '$periodStr' in row ${row.rowNum}"
                        pairEntry.addError(errorMsg)
                        log.warn(errorMsg)
                    }
                }
            }

            // Parse tax rate and calculate VAT amount
            val taxRateStr = sheet.getCellString(row, Cols.TAX_RATE)
            var taxRate: BigDecimal? = null
            if (!taxRateStr.isNullOrBlank()) {
                try {
                    taxRate = BigDecimal(taxRateStr)
                } catch (e: NumberFormatException) {
                    val errorMsg = "Could not parse taxRate '$taxRateStr' in row ${row.rowNum}"
                    pairEntry.addError(errorMsg)
                    log.warn(errorMsg)
                }
            }
            if (taxRate != null && invoicePos.grossSum != null) {
                invoicePos.vatAmountSum = invoicePos.grossSum!! * taxRate / BigDecimal("100")
            }

            // Parse DATEV account
            sheet.getCellString(row, Cols.DATEV_ACCOUNT)?.let { datevAccountNumberStr ->
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
            }

            // Parse KOST1 and KOST2
            val kost1Val = if (sheet.getCell(row, Cols.COST1)?.cellType == CellType.NUMERIC) {
                sheet.getCellInt(row, Cols.COST1)
            } else {
                sheet.getCellString(row, Cols.COST1)
            }
            if (kost1Val != null) {
                val kost1 = kostCache.findKost1(kost1Val)
                if (kost1 != null) {
                    invoicePos.kost1 = org.projectforge.rest.dto.Kost1()
                    invoicePos.kost1!!.id = kost1.id
                    // Kost1 DTO doesn't have nummer property, it has individual components
                    invoicePos.kost1!!.nummernkreis = kost1.nummernkreis
                    invoicePos.kost1!!.bereich = kost1.bereich
                    invoicePos.kost1!!.teilbereich = kost1.teilbereich
                    invoicePos.kost1!!.endziffer = kost1.endziffer
                    invoicePos.kost1!!.description = kost1.description
                } else {
                    val errorMsg = "KOST1 '$kost1Val' not found."
                    pairEntry.addError(errorMsg)
                    log.warn(errorMsg)
                }
            }

            val kost2Val = if (sheet.getCell(row, Cols.COST2)?.cellType == CellType.NUMERIC)
                sheet.getCellInt(row, Cols.COST2) else sheet.getCellString(row, Cols.COST2)
            if (kost2Val != null) {
                val kost2 = kostCache.findKost2(kost2Val)
                if (kost2 != null) {
                    invoicePos.kost2 = org.projectforge.rest.dto.Kost2()
                    invoicePos.kost2!!.id = kost2.id
                    invoicePos.kost2!!.description = kost2.description
                } else {
                    val errorMsg = "KOST2 '$kost2Val' not found."
                    pairEntry.addError(errorMsg)
                    log.warn(errorMsg)
                }
            }
            // Tbd: Pos: Datum, fällig, gezahlt am.
            // Tbd: Rechnung: Konto, Datum, fällig, gezahlt am.
            log.debug(invoicePos.toString())

            storage.commitEntity(pairEntry)
        }

        consolidateInvoicesByRenr()

        log.info("Parsing completed. Parsed ${storage.readInvoices.size} invoice positions.")
        log.info("Found ${storage.consolidatedInvoices.size} consolidated invoices.")
        if (storage.errorList.isNotEmpty()) {
            log.warn("Import has ${storage.errorList.size} errors: ${storage.errorList}")
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
        val DIFF_PROPERTIES: Array<String> = arrayOf(
            "AMOUNT", "TEXT", "KOST1", "KOST2",
            "CURRENCY", "DATE", "INVOICE_NUMBER", "CREDITOR",
            "DATEV_ACCOUNT", "TEXT", "DUE_DATE", "PAID_DATE", "TAX_RATE"
        )

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
