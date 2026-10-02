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

package org.projectforge.rest.fibu.importer

import de.micromata.merlin.excel.ExcelColumnDef
import de.micromata.merlin.excel.ExcelColumnName
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * Parses the chart of accounts (Kontenplan) of the tax office's original DATEV xlsx into the
 * [DatevAccountImportStorage].
 *
 * The legacy [org.projectforge.business.fibu.datev.KontenplanExcelImporter] only finds a sheet named exactly
 * `Kontenplan`, but the tax office names it by month (`07_Kontenplan`). Here the sheet is found by its name
 * (`Kontenplan`, optionally prefixed by the month number). Only if there is no such sheet, a sheet with the
 * columns Konto and Beschriftung/Bezeichnung but without balance columns is used: the balance list (`07_SuSa`)
 * has the same columns plus balances and must never be read as chart of accounts.
 *
 * @author Kai Reinhard
 */
class DatevAccountExcelImporter {
    private enum class Cols(override val head: String, override vararg val aliases: String) : ExcelColumnName {
        // Same heads and aliases as the legacy KontenplanExcelImporter.
        KONTO("Konto", "Konto von"),
        BEZEICHNUNG("Bezeichnung", "Beschriftung"),
        // Only registered for excluding balance sheets (SuSa) in the fallback detection.
        SALDO("Saldo"),
        EB_WERT("EB-Wert"),
    }

    fun parse(inputStream: InputStream, storage: DatevAccountImportStorage) {
        ExcelWorkbook(inputStream, storage.filename ?: "unknown", ThreadLocalUserContext.locale).use { workbook ->
            val (sheet, cols) = detectSheet(workbook)
                ?: throw IllegalArgumentException(translate("fibu.datev.import.accounts.error.noSheet"))
            log.info { "Reading chart of accounts of sheet '${sheet.sheetName}'." }
            val numbers = mutableSetOf<Int>()
            val it = sheet.dataRowIterator
            while (it.hasNext()) {
                val row = it.next()
                val nummer = DatevRecordExcelImporter.intValue(sheet.getCell(row, cols.first, ensureCell = false))
                val bezeichnung =
                    DatevRecordExcelImporter.stringValue(sheet.getCell(row, cols.second, ensureCell = false))
                if (nummer == null && bezeichnung == null) {
                    continue // Empty row.
                }
                val dto = DatevAccountImportDTO(nummer = nummer, bezeichnung = bezeichnung)
                if (nummer == null) {
                    dto.addError(translate("fibu.datev.import.accounts.error.nummerMissing"))
                } else if (!numbers.add(nummer)) {
                    dto.addError(translateMsg("fibu.datev.import.accounts.error.nummerDuplicate", "$nummer"))
                }
                if (bezeichnung == null) {
                    dto.addError(translate("fibu.datev.import.accounts.error.bezeichnungMissing"))
                }
                storage.commitEntity(dto)
            }
        }
    }

    private fun detectSheet(workbook: ExcelWorkbook): Pair<ExcelSheet, Pair<ExcelColumnDef, ExcelColumnDef>>? {
        val sheets = (0 until workbook.numberOfSheets).map { workbook.getSheet(it) }
        sheets.filter { SHEET_NAME_REGEX.matches(it.sheetName.trim()) }.forEach { sheet ->
            val cols = registerColumns(sheet)
            if (sheet.headRow != null && cols.first.found() && cols.second.found()) {
                return sheet to cols
            }
        }
        sheets.filterNot { SHEET_NAME_REGEX.matches(it.sheetName.trim()) }.forEach { sheet ->
            val cols = registerColumns(sheet)
            val saldo = sheet.registerColumn(Cols.SALDO)
            val ebWert = sheet.registerColumn(Cols.EB_WERT)
            if (sheet.headRow != null && cols.first.found() && cols.second.found() && !saldo.found() && !ebWert.found()) {
                return sheet to cols
            }
        }
        return null
    }

    private fun registerColumns(sheet: ExcelSheet): Pair<ExcelColumnDef, ExcelColumnDef> {
        sheet.autotrimCellValues = true
        return sheet.registerColumn(Cols.KONTO) to sheet.registerColumn(Cols.BEZEICHNUNG)
    }

    companion object {
        private val SHEET_NAME_REGEX = Regex("""(?i)^(\d{1,2}[_ -]?)?Kontenplan$""")
    }
}
