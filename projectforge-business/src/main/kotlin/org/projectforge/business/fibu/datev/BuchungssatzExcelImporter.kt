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
import de.micromata.merlin.excel.importer.ImportLogger
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
import java.math.BigDecimal
import java.math.RoundingMode

class BuchungssatzExcelImporter(private val storage: ImportStorage<BuchungssatzDO>, private val kontoDao: KontoDao, private val kost1Dao: Kost1Dao,
                                private val kost2Dao: Kost2Dao) {
    private val dateValidator = ExcelColumnDateValidator("dd.MM.yyyy", "dd.MM.yy", "yyyy-MM-dd")

    private enum class Cols(override val head: String, override vararg val aliases: String) : ExcelColumnName {
        SATZNR("SatzNr.", "Satz-Nr."),
        BETRAG("Betrag"),
        SH("SH", "S/H"),
        KOST1("Alt.-Kst.", "Kost1"),
        KOST2("Kostenstelle/-träger", "Kost2", "Kst."),
        KONTO("Konto"),
        GEGENKONTO("Gegenkonto"),
        MENGE("Menge"),
        BELEG("Beleg"),
        DATUM("Datum"),
        TEXT("Text"),
        KOMMENTAR("Kommentar", "Bemerkung")
    }

    fun doImport(workbook: ExcelWorkbook) {
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
            storage.logger.info("Ignoring sheet '$name' for importing Buchungssätze.")
            return null
        }
        storage.logger.info("Reading sheet '$name'.")
        sheet.registerColumn(Cols.DATUM, dateValidator)
        sheet.registerColumn(Cols.SATZNR, ExcelColumnNumberValidator(1.0).setRequired().setUnique())
        sheet.registerColumn(Cols.BETRAG, ExcelColumnNumberValidator().setRequired())
        sheet.registerColumn(Cols.SH, ExcelColumnOptionsValidator("S", "H").setRequired())
        //sheet.registerColumn("SH", "S/H"); // Second column not needed.
        sheet.registerColumn(Cols.KONTO, ExcelColumnNumberValidator().setRequired())
        sheet.registerColumn(Cols.GEGENKONTO, ExcelColumnValidator().setRequired())
        sheet.registerColumn(Cols.KOST1, ExcelColumnValidator().setRequired())
        sheet.registerColumn(Cols.KOST2, ExcelColumnValidator().setRequired())
        sheet.registerColumns(Cols.MENGE, Cols.BELEG, Cols.TEXT, Cols.KOMMENTAR)
        if (sheet.headRow == null) {
            storage.logger.info("Ignoring sheet '$name' for importing AccountingRecords (Buchungssätze), no valid head row found.")
            return null
        }
        sheet.analyze(true)
        val test = sheet.allValidationErrors
        return importBuchungssaetze(sheet, month)
    }

    /**
     * @param month 1-January, ..., 12-December
     */
    private fun importBuchungssaetze(excelSheet: ExcelSheet, month: Int): ImportedSheet<BuchungssatzDO> {
        val ctx = ExcelWriterContext(CoreI18n.setDefault(ThreadLocalUserContext.getLocale()), excelSheet!!.excelWorkbook).setAddErrorColumn(true)
        //excelSheet.markErrors(ctx)
        val importedSheet = ImportedSheet<BuchungssatzDO>(excelSheet, ImportLogger.Level.WARN, "'${excelSheet.excelWorkbook.filename}':", log)
        importedSheet.origName = excelSheet.sheetName
        importedSheet.logger.addValidationErrors(excelSheet)
        val it = excelSheet.dataRowIterator
        var year = 0
        while (it.hasNext()) {
            val row = it.next()
            if (excelSheet.isRowEmpty(row, *Cols.values())) {
                importedSheet.logger.info("Skipping empty row.", row)
                continue
            }
            val element = MyImportedElement(storage.nextVal(), BuchungssatzDO::class.java,
                    *DatevImportDao.BUCHUNGSSATZ_DIFF_PROPERTIES)
            val satz = BuchungssatzDO()
            element.value = satz
            satz.satznr = excelSheet.getCellInt(row, Cols.SATZNR)
            if (satz.satznr == null) {
                importedSheet.logger.error("Satznr. nicht gültig.", row, Cols.SATZNR, true)
                continue
            }
            val day = from(dateValidator.convert(excelSheet.getCell(row, Cols.DATUM)))
            if (day == null) {
                importedSheet.logger.error("Invalid date.", row, Cols.DATUM, true)
                continue
            }
            // Empty row? date not given.
            satz.datum = day.sqlDate
            if (year == 0) {
                year = day.year
            } else if (year != day.year) {
                val msg = "Not supported: Buchungssätze liegen in verschiedenen Jahren."
                importedSheet.logger.error(msg, row, Cols.DATUM)
                element.putErrorProperty("datum", "Buchungssatz liegt außerhalb des Buchungsmonats.")
            }
            if (day.monthValue > month) {
                val msg = "Buchungssätze können nicht in die Zukunft für den aktuellen Monat '${KostFormatter.formatBuchungsmonat(year, day.monthValue)}'' gebucht werden!"
                importedSheet.logger.error(msg, row, Cols.DATUM)
                element.putErrorProperty("datum", msg)
            } else if (day.monthValue < month) {
                val msg = "Buchungssatz liegt vor Monat '${KostFormatter.formatBuchungsmonat(year, month)}' (OK)."
                importedSheet.logger.info(msg, row, Cols.DATUM)
            }
            satz.year = year
            satz.month = month
            satz.betrag = BigDecimal(excelSheet.getCellDouble(row, Cols.BETRAG)!!).setScale(2, RoundingMode.HALF_UP)
            satz.menge = excelSheet.getCellString(row, Cols.MENGE)
            val commentColDef = excelSheet.getColumnDef(Cols.KOMMENTAR)
            if (commentColDef!!.found()) {
                satz.comment = excelSheet.getCellString(row, commentColDef)
            }
            satz.setSH(excelSheet.getCellString(row, Cols.SH)!!)
            satz.beleg = excelSheet.getCellString(row, Cols.BELEG)
            satz.text = excelSheet.getCellString(row, Cols.TEXT)
            var kontoInt = excelSheet.getCellInt(row, Cols.KONTO)
            var konto = kontoDao.getKonto(kontoInt)
            if (konto != null) {
                satz.konto = konto
            } else {
                element.putErrorProperty("konto", kontoInt!!)
            }
            kontoInt = excelSheet.getCellInt(row, Cols.GEGENKONTO)
            konto = kontoDao.getKonto(kontoInt)
            if (konto != null) {
                satz.gegenKonto = konto
            } else {
                element.putErrorProperty("gegenkonto", kontoInt!!)
            }
            var kostString = excelSheet.getCellString(row, Cols.KOST1)
            val kost1 = kost1Dao.getKost1(kostString)
            if (kost1 != null) {
                satz.kost1 = kost1
            } else {
                element.putErrorProperty("kost1", kostString!!)
            }
            kostString = excelSheet.getCellString(row, Cols.KOST2)
            val kost2 = kost2Dao.getKost2(kostString)
            if (kost2 != null) {
                satz.kost2 = kost2
            } else {
                element.putErrorProperty("kost2", kostString!!)
            }
            satz.calculate(true)
            importedSheet.addElement(element)
            //log.debug(satz.toString())
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
