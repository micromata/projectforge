/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.datev

import de.micromata.merlin.CoreI18n
import de.micromata.merlin.excel.*
import de.micromata.merlin.excel.importer.ImportStorage
import de.micromata.merlin.excel.importer.ImportedSheet
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.utils.MyImportedElement
import org.projectforge.framework.time.PFDay.Companion.from
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode

class BuchungssatzExcelImporter(private val storage: ImportStorage<BuchungssatzDO>, private val kontoDao: KontoDao, private val kost1Dao: Kost1Dao,
                                private val kost2Dao: Kost2Dao) {
    private val dateValidator = ExcelColumnDateValidator("dd.MM.yyyy", "dd.MM.yy", "yyyy-MM-dd")
    fun doImport(`is`: InputStream?) {
        val workbook = ExcelWorkbook(`is`!!, storage.filename!!)
        storage.workbook = workbook
        for (idx in 0 until workbook.numberOfSheets) {
            val importedSheet = importBuchungssaetze(workbook, idx)
            if (importedSheet != null) {
                storage.addSheet(importedSheet)
            }
        }
    }

    private fun importBuchungssaetze(workbook: ExcelWorkbook, idx: Int): ImportedSheet<BuchungssatzDO>? {
        val sheet = workbook.getSheet(idx)!!
        sheet.autotrimCellValues = true
        val name = sheet.sheetName
        var month: Int? = null
        try {
            month = name.toInt() // month beginnt bei 01 - Januar.
        } catch (ex: NumberFormatException) { // ignore
        }
        if (month == null) {
            log.info("Ignoring sheet '$name' for importing Buchungssätze.")
            return null
        }
        storage.logger.info("Reading sheet '$name'.")
        sheet.registerColumn("SatzNr.", "Satz-Nr.").addColumnListener(ExcelColumnNumberValidator(1.0).setRequired().setUnique())
        sheet.registerColumn("Betrag").addColumnListener(ExcelColumnNumberValidator().setRequired())
        sheet.registerColumn("SH", "S/H").addColumnListener(ExcelColumnOptionsValidator("S", "H").setRequired())
        sheet.registerColumn("Konto").addColumnListener(ExcelColumnNumberValidator().setRequired())
        sheet.registerColumn("Kostenstelle/-träger", "Kost2", "Kst.").addColumnListener(ExcelColumnValidator().setRequired())
        sheet.registerColumn("Menge")
        //sheet.registerColumn("SH", "S/H"); // Second column not needed.
        sheet.registerColumn("Beleg")
        sheet.registerColumn("Datum").addColumnListener(dateValidator)
        sheet.registerColumn("Gegenkonto").addColumnListener(ExcelColumnValidator().setRequired())
        sheet.registerColumn("Text")
        sheet.registerColumn("Alt.-Kst.", "Kost1").addColumnListener(ExcelColumnValidator().setRequired())
        //sheet.registerColumn("Beleg 2");
        //sheet.registerColumn("KR-BSNr.");
        //sheet.registerColumn("ZI");
        sheet.registerColumn("Kommentar", "Bemerkung")
        sheet.analyze(true)
        if (sheet.headRow == null) {
            storage.logger.info("Ignoring sheet '$name' for importing AccountingRecords (Buchungssätze), no valid head row found.")
            return null
        }
        return importBuchungssaetze(sheet, month)
    }

    /**
     * @param month 1-January, ..., 12-December
     */
    private fun importBuchungssaetze(excelSheet: ExcelSheet?, month: Int): ImportedSheet<BuchungssatzDO> {
        val ctx = ExcelWriterContext(CoreI18n.setDefault(ThreadLocalUserContext.getLocale()), excelSheet!!.excelWorkbook).setAddErrorColumn(true)
        excelSheet.markErrors(ctx)
        val importedSheet = ImportedSheet<BuchungssatzDO>(excelSheet)
        importedSheet.origName = excelSheet.sheetName
        val it = excelSheet.dataRowIterator
        var year = 0
        while (it.hasNext()) {
            val row = it.next()
            val element = MyImportedElement(storage.nextVal(), BuchungssatzDO::class.java,
                    *DatevImportDao.BUCHUNGSSATZ_DIFF_PROPERTIES)
            val satz = BuchungssatzDO()
            element.value = satz
            satz.satznr = excelSheet.getCellInt(row, "SatzNr.")
            val day = from(dateValidator.convert(excelSheet.getCell(row, "Datum"))) ?: continue
            // Empty row? date not given.
            satz.datum = day.sqlDate
            if (year == 0) {
                year = day.year
            } else if (year != day.year) {
                val msg = "Not supported: Buchungssätze innerhalb eines Excel-Sheets liegen in verschiedenen Jahren: Im Blatt '" + excelSheet.sheetName + "', in Zeile " + (row.rowNum + 1)
                importedSheet.logger.error(msg)
                element.putErrorProperty("datum", "Buchungssatz liegt außerhalb des Buchungsmonats.")
            }
            if (day.monthValue > month) {
                val msg = ("Buchungssätze können nicht in die Zukunft für den aktuellen Monat '"
                        + KostFormatter.formatBuchungsmonat(year, day.monthValue)
                        + " gebucht werden! "
                        + satz)
                importedSheet.logger.error(msg)
                element.putErrorProperty("datum", "Buchungssätze können nicht in die Zukunft für den aktuellen Monat erfasst werden.")
            } else if (day.monthValue < month) {
                val msg = "Buchungssatz liegt vor Monat '" + KostFormatter.formatBuchungsmonat(year, month) + "' (OK): " + satz
                importedSheet.logger.info(msg)
            }
            satz.year = year
            satz.month = month
            satz.betrag = BigDecimal(excelSheet.getCellDouble(row, "Betrag")!!).setScale(2, RoundingMode.HALF_UP)
            satz.menge = excelSheet.getCellString(row, "Menge")
            val commentColDef = excelSheet.getColumnDef("Kommentar")
            if (commentColDef!!.found()) {
                satz.comment = excelSheet.getCellString(row, commentColDef.columnHeadname)
            }
            satz.setSH(excelSheet.getCellString(row, "SH")!!)
            satz.beleg = excelSheet.getCellString(row, "Beleg")
            satz.text = excelSheet.getCellString(row, "Text")
            var kontoInt = excelSheet.getCellInt(row, "Konto")
            var konto = kontoDao.getKonto(kontoInt)
            if (konto != null) {
                satz.konto = konto
            } else {
                element.putErrorProperty("konto", kontoInt!!)
            }
            kontoInt = excelSheet.getCellInt(row, "Gegenkonto")
            konto = kontoDao.getKonto(kontoInt)
            if (konto != null) {
                satz.gegenKonto = konto
            } else {
                element.putErrorProperty("gegenkonto", kontoInt!!)
            }
            var kostString = excelSheet.getCellString(row, "Kost1")
            val kost1 = kost1Dao.getKost1(kostString)
            if (kost1 != null) {
                satz.kost1 = kost1
            } else {
                element.putErrorProperty("kost1", kostString!!)
            }
            kostString = excelSheet.getCellString(row, "Kost2")
            val kost2 = kost2Dao.getKost2(kostString)
            if (kost2 != null) {
                satz.kost2 = kost2
            } else {
                element.putErrorProperty("kost2", kostString!!)
            }
            satz.calculate(true)
            importedSheet.addElement(element)
            log.debug(satz.toString())
        }
        importedSheet.name = KostFormatter.formatBuchungsmonat(year, month)
        importedSheet.setProperty("year", year)
        importedSheet.setProperty("month", month)
        return importedSheet
    }

    companion object {
        private val log = LoggerFactory.getLogger(BuchungssatzExcelImporter::class.java)
    }

}
