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

import de.micromata.merlin.excel.ExcelColumnName
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode

private val log = KotlinLogging.logger {}

/**
 * Parses an employee-salary xlsx with Merlin into the layout-free [EmployeeSalaryImportStorage].
 *
 * Unlike the legacy [org.projectforge.business.fibu.datev.EmployeeSalaryExcelImporter] it does not rely on
 * a fixed sheet name (`employeeSalaries`) or a manually chosen accounting month:
 *  - The sheet is detected by scanning every sheet for a head row that carries the required columns, so the
 *    real tax-office export (whose sheet is named `Aktueller Monat NB im Abrechnun`, truncated to 31 chars)
 *    is read as well.
 *  - The accounting year/month is read per row from the `Abrechnungsmonat` column (text `yyyy/MM`).
 *  - Only one value is imported into `bruttoMitAgAnteil`: the `Gesamtkosten` column (the full employer
 *    cost), which is what ProjectForge's "Brutto mit AG-Anteil" means; the remaining value columns
 *    (including the narrower `*Gesamtbrutto m. bAV AG-Anteil`) are ignored.
 *
 * Column heads carry aliases so the older format (`Personalnummer`, `bruttoMitAgAnteil`) still parses. A row
 * whose staff number resolves to no employee is kept with an error (→ FAULTY, not importable); the synthetic
 * aggregate row (e.g. staff number `99998`) therefore drops out on its own.
 *
 * @author Kai Reinhard
 */
class EmployeeSalaryExcelImporter(
    private val employeeCache: EmployeeCache,
    private val employeeService: EmployeeService,
) {
    private enum class Cols(override val head: String, override vararg val aliases: String) : ExcelColumnName {
        ABRECHNUNGSMONAT("Abrechnungsmonat", "Monat"),
        PERSONALNUMMER("Pers.Nr.", "Personalnummer", "Personalnr."),

        /**
         * The value imported into [EmployeeSalaryDO.bruttoMitAgAnteil]. In the tax-office format that is the
         * "Gesamtkosten" column — the full employer cost (gross plus every employer contribution: bAV AG
         * share, SV AG share, Umlage, flat-rate taxes), which is what ProjectForge's "Brutto mit AG-Anteil"
         * means — not the narrower "*Gesamtbrutto m. bAV AG-Anteil" column. The `bruttoMitAgAnteil` alias
         * keeps the older, programmatically-headed format importable.
         */
        GESAMTKOSTEN("Gesamtkosten", "bruttoMitAgAnteil"),
    }

    fun parse(inputStream: InputStream, storage: EmployeeSalaryImportStorage) {
        ExcelWorkbook(inputStream, storage.filename ?: "unknown", ThreadLocalUserContext.locale).use { workbook ->
            val sheet = detectSheet(workbook)
                ?: throw IllegalArgumentException(translate("fibu.employee.salaries.import.error.noSheet"))
            sheet.autotrimCellValues = true
            val rowIterator = sheet.dataRowIterator
            while (rowIterator.hasNext()) {
                val row = rowIterator.next()
                val staffNumber = sheet.getCellInt(row, Cols.PERSONALNUMMER)
                val monthText = sheet.getCellString(row, Cols.ABRECHNUNGSMONAT, nullAsEmpty = true)
                val brutto = sheet.getCellDouble(row, Cols.GESAMTKOSTEN)
                if (staffNumber == null && brutto == null && monthText.isNullOrBlank()) {
                    continue // Empty row.
                }
                val dto = EmployeeSalaryImportDTO(
                    staffNumber = staffNumber,
                    bruttoMitAgAnteil = brutto?.let { BigDecimal.valueOf(it).setScale(2, RoundingMode.HALF_UP) },
                )
                parseAccountingMonth(monthText).let { (year, month) ->
                    dto.year = year
                    dto.month = month
                    if (year == null || month == null) {
                        dto.addError(translateMsg("fibu.employee.salaries.import.error.month", monthText ?: ""))
                    }
                }
                val employee = employeeCache.findByStaffNumber(staffNumber)
                    ?: employeeService.findByStaffnumber(staffNumber)
                if (employee == null) {
                    dto.addError(translateMsg("fibu.employee.salaries.import.error.employeeNotFound", "$staffNumber"))
                } else {
                    dto.employee = employee.displayName
                    dto.employeeId = employee.id
                }
                if (brutto == null) {
                    dto.addError(translate("fibu.employee.salaries.import.error.noBrutto"))
                }
                storage.commitEntity(dto)
                log.debug { "Read salary row: staffNumber=$staffNumber, ${dto.year}/${dto.month}, brutto=${dto.bruttoMitAgAnteil}" }
            }
        }
    }

    /**
     * Scans every sheet for a head row that carries both required columns, so the sheet is found regardless of
     * its (in the real export truncated and untranslated) name.
     */
    private fun detectSheet(workbook: ExcelWorkbook): ExcelSheet? {
        for (idx in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheet(idx)
            sheet.registerColumn(Cols.ABRECHNUNGSMONAT)
            val staffNumberCol = sheet.registerColumn(Cols.PERSONALNUMMER)
            val bruttoCol = sheet.registerColumn(Cols.GESAMTKOSTEN)
            sheet.analyze(true)
            if (sheet.headRow != null && staffNumberCol.found() && bruttoCol.found()) {
                log.info { "Detected employee-salary sheet '${sheet.sheetName}'." }
                return sheet
            }
        }
        return null
    }

    /**
     * Parses the accounting month cell (`2026/08`, also `2026/8`) into (year, month); returns (null, null)
     * when the cell is blank or unparseable.
     */
    private fun parseAccountingMonth(text: String?): Pair<Int?, Int?> {
        if (text.isNullOrBlank()) {
            return null to null
        }
        val match = MONTH_REGEX.find(text) ?: return null to null
        val year = match.groupValues[1].toIntOrNull()
        val month = match.groupValues[2].toIntOrNull()
        if (month == null || month !in 1..12) {
            return year to null
        }
        return year to month
    }

    companion object {
        private val MONTH_REGEX = Regex("""(\d{4})\D+(\d{1,2})""")
    }
}
