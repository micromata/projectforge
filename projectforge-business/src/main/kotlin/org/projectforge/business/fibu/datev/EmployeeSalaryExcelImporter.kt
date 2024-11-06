/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.utils.MyImportedElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Month

private val log = KotlinLogging.logger {}

@Service
class EmployeeSalaryExcelImporter {
    private enum class Cols(override val head: String, override vararg val aliases: String) : ExcelColumnName {
        PERSONALNUMMER("Personalnummer"),
        GESAMT_BRUTTO("bruttoMitAgAnteil", "Gesamt-")
    }
    // Personalnummer |	Name, Vorname | Gesamtbrutto | bAV AG-Anteil | Kostenrelevante NBA | SV AG-Anteil
    // Umlage | Pauschale Steuern | Gesamt-

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    /**
     * Imports the employee salaries from the given Excel workbook.
     * @param storage The storage to store the imported data.
     * @param workbook The Excel workbook to import.
     * @param month The month of the salary data (begin of month).
     */
    fun doImport(storage: ImportStorage<EmployeeSalaryDO>, workbook: ExcelWorkbook, year: Int, month: Month) {
        val sheet = workbook.getSheet(NAME_OF_EXCEL_SHEET)
        if (sheet == null) {
            val msg = "Gehälter können nicht importiert werden: Blatt '$NAME_OF_EXCEL_SHEET' nicht gefunden."
            storage.logger.error(msg)
            throw UserException(msg)
        }

        sheet.autotrimCellValues = true
        storage.logger.info("Reading sheet '$NAME_OF_EXCEL_SHEET'.")
        sheet.registerColumn(Cols.PERSONALNUMMER, ExcelColumnNumberValidator(1.0).setRequired().setUnique())
        sheet.registerColumn(Cols.GESAMT_BRUTTO, ExcelColumnValidator().setRequired())
            .setTargetProperty("bruttoMitAgAnteil")
        sheet.analyze(true)
        if (sheet.headRow == null) {
            storage.logger.info("Ignoring sheet '$NAME_OF_EXCEL_SHEET' for importing salaries, no valid head row found.")
            return
        }
        val importedSheet =
            ImportedSheet(storage, sheet, ImportLogger.Level.WARN, "'${sheet.excelWorkbook.filename}':", log)
        storage.addSheet(importedSheet)
        importedSheet.name = NAME_OF_EXCEL_SHEET
        importedSheet.logger.addValidationErrors(sheet)
        val it = sheet.dataRowIterator
        while (it.hasNext()) {
            val row = it.next()
            val element = MyImportedElement(
                importedSheet, row.rowNum, EmployeeSalaryDO::class.java,
                *SALARY_DIFF_PROPERTIES
            )
            val salary = EmployeeSalaryDO()
            element.value = salary
            ImportHelper.fillBean(salary, sheet, row.rowNum)
            val staffNumber = sheet.getCellString(row, "Personalnummer")
            val employee = employeeCache.findByStaffNumber(staffNumber)
                ?: employeeDao.findEmployeeByStaffnumber(staffNumber)
            salary.employee = employee
            salary.year = year
            salary.month = month.value
            importedSheet.addElement(element)
            log.debug(salary.toString())
        }
    }

    companion object {
        const val NAME_OF_EXCEL_SHEET = "employeeSalaries"

        val SALARY_DIFF_PROPERTIES: Array<String> = arrayOf("Personalnummer", "bruttoMitAgAnteil")

    }
}
