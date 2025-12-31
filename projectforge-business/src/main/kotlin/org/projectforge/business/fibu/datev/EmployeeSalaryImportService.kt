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

package org.projectforge.business.fibu.datev

import de.micromata.merlin.excel.ExcelWorkbook
import de.micromata.merlin.excel.importer.ImportLogger
import de.micromata.merlin.excel.importer.ImportStatus
import de.micromata.merlin.excel.importer.ImportStorage
import de.micromata.merlin.excel.importer.ImportedSheet
import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryDao
import org.projectforge.business.fibu.EmployeeSalaryService
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.datev.DatevImportService.Type
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.locale
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.InputStream
import java.time.Month

private val log = KotlinLogging.logger {}

@Service
class EmployeeSalaryImportService {
    @Autowired
    private lateinit var employeeSalaryExcelImporter: EmployeeSalaryExcelImporter

    @Autowired
    private lateinit var employeeSalaryService: EmployeeSalaryService

    @Autowired
    private lateinit var employeeSalaryDao: EmployeeSalaryDao


    @Throws(IOException::class)
    fun importData(
        inputStream: InputStream,
        filename: String,
        year: Int,
        month: Month
    ): ImportStorage<EmployeeSalaryDO> {
        checkLoggedinUserRight()
        log.info("importData (EmployeeSalary) called")
        ExcelWorkbook(inputStream, filename, locale).use { workbook ->
            val storage = ImportStorage<EmployeeSalaryDO>(
                Type.KONTENPLAN, workbook, ImportLogger.Level.INFO,
                "'$filename':", log
            )
            employeeSalaryExcelImporter.doImport(storage, workbook, year, month)
            storage.id
            return storage
        }
    }

    fun reconcile(storage: ImportStorage<EmployeeSalaryDO>, sheetName: String, year: Int, month: Month) {
        log.info("Reconcile EmployeeSalary called")
        checkLoggedinUserRight()
        requireNotNull(storage.getSheets())
        val sheet = storage.getNamedSheet(sheetName)
        requireNotNull(sheet)
        val salaries = employeeSalaryService.selectByMonth(year, month)
        sheet.getElements()?.forEach { element ->
            val salary = element.value
            val employeeId = salary?.employee?.id
            salaries.find { it.employee?.id == employeeId }?.let {
                element.oldValue = it
                salary?.id = it.id
                salary?.created = it.created // Needed by baseDao to decide if the object is new or not.
            }
        }
        sheet.setStatus(ImportStatus.RECONCILED)
        sheet.calculateStatistics()
        sheet.numberOfCommittedElements = -1
    }

    fun commit(storage: ImportStorage<*>, sheetName: String) {
        checkLoggedinUserRight()
        requireNotNull(storage.getSheets())
        val sheet = storage.getNamedSheet(sheetName)
        requireNotNull(sheet)
        if (sheet.getStatus() != ImportStatus.RECONCILED) {
            throw UserException("common.import.action.commit.error.notReconciled")
        }
        @Suppress("UNCHECKED_CAST")
        val num = commit(sheet as ImportedSheet<EmployeeSalaryDO?>)
        sheet.numberOfCommittedElements = num
        sheet.setStatus(ImportStatus.IMPORTED)
    }

    private fun commit(sheet: ImportedSheet<EmployeeSalaryDO?>): Int {
        log.info("Commit EmployeeSalaries called")
        val col = mutableListOf<EmployeeSalaryDO>()
        sheet.getElements()?.filter { it.selected }?.forEach { el ->
            el.value?.let { salary ->
                if (el.oldValue != null) {
                    salary.id = el.oldValue!!.id
                }
                col.add(salary)
            }
        }
        employeeSalaryDao.insertOrUpdate(col, SALARY_INSERT_BLOCK_SIZE, checkAccess = false)
        return col.size
    }

    /**
     * @see EmployessSalaryDao.checkLoggedInUserInsertAccess
     */
    fun checkLoggedinUserRight() {
        employeeSalaryDao.checkLoggedInUserInsertAccess(EmployeeSalaryDO())
    }

    companion object {
        private const val SALARY_INSERT_BLOCK_SIZE = 50
    }
}
