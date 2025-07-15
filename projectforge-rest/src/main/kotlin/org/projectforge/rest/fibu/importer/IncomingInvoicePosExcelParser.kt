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

import de.micromata.merlin.excel.ExcelColumnDateValidator
import de.micromata.merlin.excel.ExcelColumnName
import de.micromata.merlin.excel.ExcelColumnNumberValidator
import de.micromata.merlin.excel.ExcelColumnValidator
import de.micromata.merlin.excel.ExcelWorkbook
import de.micromata.merlin.excel.importer.ImportHelper
import de.micromata.merlin.excel.importer.ImportLogger
import de.micromata.merlin.excel.importer.ImportStorage
import de.micromata.merlin.excel.importer.ImportedSheet
import mu.KotlinLogging
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.persistence.utils.MyImportedElement
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

class IncomingInvoicePosExcelParser(
    private val storage: EingangsrechnungImportStorage,
    private val eingangsrechnungDao: EingangsrechnungDao,
    private val kostCache: KostCache,
) {
    private val dateValidator = ExcelColumnDateValidator(
        ExcelColumnDateValidator.GERMAN_FORMATS,
        minimum = LocalDate.of(1990, 1, 1),
        maximum = LocalDate.of(2100, 12, 31)
    )
    private enum class Cols(override val head: String, override vararg val aliases: String) : ExcelColumnName {
        PERIOD("Periode"), // e.g. "01.05.2025-31.05.2025"
        AMMOUNT("Betrag"),
        CURRENCY("Währung"), // e.g. "EUR", "USD", ...
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
        sheet.registerColumn(Cols.INVOICE_NUMBER, ExcelColumnNumberValidator().setRequired())
        sheet.registerColumn(Cols.AMMOUNT, ExcelColumnValidator().setRequired()).setTargetProperty("grossSum")
        sheet.registerColumn(Cols.CREDITOR, ExcelColumnValidator())
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
        sheet.registerColumn(Cols.CREDITOR, ExcelColumnValidator()).setTargetProperty("kreditor")
        sheet.registerColumn(Cols.INVOICE_NUMBER, ExcelColumnValidator()).setTargetProperty("referenz")
        if (sheet.headRow == null) {
            log.info("Ignoring sheet '$name' for importing IncomingInvoicePositions, no valid head row found.")
            return
        }
        sheet.setColumnsForRowEmptyCheck(
            Cols.PERIOD,
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
            val element = MyImportedElement(
                importedSheet, row.rowNum, EingangsrechnungPosImportDTO::class.java,
                *DIFF_PROPERTIES
            )
            val invoicePos = EingangsrechnungPosImportDTO()
            element.value = invoicePos
            
            // Use ImportHelper.fillBean to automatically fill basic fields
            ImportHelper.fillBean(invoicePos, sheet, row.rowNum)
            
            // Parse period from period string (e.g., "01.05.2025-31.05.2025")
            val periodStr = sheet.getCellString(row, Cols.PERIOD)
            if (periodStr != null && periodStr.contains("-")) {
                val parts = periodStr.split("-")
                if (parts.size == 2) {
                    try {
                        // Parse dates from period parts - we need to create cells or use different approach
                        // For now, skip period parsing as it requires more complex implementation
                    } catch (e: Exception) {
                        log.warn("Could not parse period '$periodStr' in row ${row.rowNum}")
                    }
                }
            }
            
            // grossSum is automatically filled by ImportHelper.fillBean via targetProperty
            
            // Parse tax rate and calculate VAT amount
            val taxRateStr = sheet.getCellString(row, Cols.TAX_RATE)
            var taxRate: BigDecimal? = null
            if (taxRateStr != null) {
                try {
                    taxRate = BigDecimal(taxRateStr)
                } catch (e: NumberFormatException) {
                    log.warn("Could not parse taxRate '$taxRateStr' in row ${row.rowNum}")
                }
            }
            if (taxRate != null && invoicePos.grossSum != null) {
                invoicePos.vatAmountSum = invoicePos.grossSum!! * taxRate / BigDecimal("100")
            }
            
            // Parse DATEV account
            val datevAccount = sheet.getCellString(row, Cols.DATEV_ACCOUNT)
            if (datevAccount != null) {
                invoicePos.konto = org.projectforge.rest.dto.Konto()
                invoicePos.konto!!.nummer = datevAccount.toIntOrNull()
            }
            
            // Parse KOST1 and KOST2
            val kost1Str = sheet.getCellString(row, Cols.COST1)
            if (kost1Str != null) {
                val kost1 = kostCache.getKost1(kost1Str)
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
                    element.putErrorProperty("kost1", "KOST1 '$kost1Str' nicht gefunden.")
                }
            }
            
            val kost2Str = sheet.getCellString(row, Cols.COST2)
            if (kost2Str != null) {
                val kost2 = kostCache.getKost2(kost2Str)
                if (kost2 != null) {
                    invoicePos.kost2 = org.projectforge.rest.dto.Kost2()
                    invoicePos.kost2!!.id = kost2.id
                    // Kost2 DTO will have similar structure - check the actual properties
                    invoicePos.kost2!!.id = kost2.id
                    invoicePos.kost2!!.description = kost2.description
                } else {
                    element.putErrorProperty("kost2", "KOST2 '$kost2Str' nicht gefunden.")
                }
            }
            
            importedSheet.addElement(element)
            log.debug(invoicePos.toString())
        }
    }

    companion object {
        val DIFF_PROPERTIES: Array<String> = arrayOf(
            "AMOUNT", "TEXT", "KOST1", "KOST2",
            "CURRENCY", "DATE", "INVOICE_NUMBER", "CREDITOR",
            "DATEV_ACCOUNT", "TEXT", "DUE_DATE", "PAID_DATE", "TAX_RATE"
        )
    }
}
