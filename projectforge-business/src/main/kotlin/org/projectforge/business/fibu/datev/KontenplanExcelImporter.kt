/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.*
import de.micromata.merlin.excel.importer.ImportHelper
import de.micromata.merlin.excel.importer.ImportLogger
import de.micromata.merlin.excel.importer.ImportStorage
import de.micromata.merlin.excel.importer.ImportedSheet
import org.projectforge.business.fibu.KontoDO
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.utils.MyImportedElement
import org.slf4j.LoggerFactory

class KontenplanExcelImporter {
    private enum class Cols(override val head: String, override vararg val aliases: String) : ExcelColumnName {
        KONTO("Konto", "Konto von"),
        BEZEICHNUNG("Bezeichnung", "Beschriftung")
    }

    fun doImport(storage: ImportStorage<KontoDO>, workbook: ExcelWorkbook) {
        val sheet = workbook.getSheet(NAME_OF_EXCEL_SHEET)
        if (sheet == null) {
            val msg = "Konten können nicht importiert werden: Blatt '$NAME_OF_EXCEL_SHEET' nicht gefunden."
            storage.logger.error(msg)
            throw UserException(msg)
        }
        importKontenplan(storage, sheet)
    }

    private fun importKontenplan(storage: ImportStorage<KontoDO>, sheet: ExcelSheet) {
        sheet.autotrimCellValues = true
        storage.logger.info("Reading sheet '$NAME_OF_EXCEL_SHEET'.")
        sheet.registerColumn(Cols.KONTO, ExcelColumnNumberValidator(1.0).setRequired().setUnique())
        sheet.registerColumn(Cols.BEZEICHNUNG,ExcelColumnValidator().setRequired()).setTargetProperty("bezeichnung")
        sheet.analyze(true)
        if (sheet.headRow == null) {
            storage.logger.info("Ignoring sheet '$NAME_OF_EXCEL_SHEET' for importing Buchungssätze, no valid head row found.")
            return
        }
        val importedSheet = ImportedSheet(storage, sheet, ImportLogger.Level.WARN, "'${sheet.excelWorkbook.filename}':", log)
        storage.addSheet(importedSheet)
        importedSheet.name = NAME_OF_EXCEL_SHEET
        importedSheet.logger.addValidationErrors(sheet)
        val it = sheet.dataRowIterator
        while (it.hasNext()) {
            val row = it.next()
            val element = MyImportedElement(importedSheet, row.rowNum, KontoDO::class.java,
                    *DatevImportDao.KONTO_DIFF_PROPERTIES)
            val konto = KontoDO()
            element.value = konto
            ImportHelper.fillBean(konto, sheet, row.rowNum)
            konto.nummer = sheet.getCellInt(row, "Konto")
            importedSheet.addElement(element)
            log.debug(konto.toString())
        }
    }

    companion object {
        const val NAME_OF_EXCEL_SHEET = "Kontenplan"
        private val log = LoggerFactory.getLogger(KontenplanExcelImporter::class.java)
    }
}
