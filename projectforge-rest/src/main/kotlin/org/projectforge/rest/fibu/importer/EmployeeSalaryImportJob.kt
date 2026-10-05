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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryDao
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.importer.AbstractImportJob
import org.projectforge.rest.importer.ImportPairEntry

private val log = KotlinLogging.logger {}

/**
 * Persists the selected employee-salary import rows. A row with a matching database salary
 * ([ImportPairEntry.stored]) is updated by its id, an unmatched row is inserted; salaries have no
 * deletion in this import. Vorbild: [EingangsrechnungImportJob].
 *
 * @author Kai Reinhard
 */
class EmployeeSalaryImportJob(
    private val employeeSalaryDao: EmployeeSalaryDao,
    private val selectedEntries: List<ImportPairEntry<EmployeeSalaryImportDTO>>,
    private val importStorage: EmployeeSalaryImportStorage,
) : AbstractImportJob(
    translateMsg("fibu.employee.salaries.import.job.title", selectedEntries.size.toString()),
    area = "EmployeeSalaryImport",
    queueName = "employeeSalaryImport",
    timeoutSeconds = 600,
    importStorage = importStorage,
    selectedEntries = selectedEntries,
) {

    init {
        totalNumber = selectedEntries.size
        processedNumber = 0
    }

    override fun onBeforeStart() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
        // Mark all entries as not reconciled so they show status UNKNOWN until processed.
        importStorage.pairEntries.forEach { it.reconciled = false }
    }

    override fun onAfterTermination() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
    }

    override suspend fun run() {
        log.info { "Starting import of ${selectedEntries.size} employee-salary entries." }
        val toPersist = mutableListOf<EmployeeSalaryDO>()
        for (entry in selectedEntries) {
            if (!isActive) {
                return
            }
            val read = entry.read ?: continue // No deletions for salaries.
            val employeeId = read.employeeId
            if (employeeId == null || read.year == null || read.month == null) {
                // Faulty rows are filtered out before commit, this is a last-resort guard.
                log.warn { "Skipping incomplete salary row: staffNumber=${read.staffNumber}, ${read.year}/${read.month}" }
                continue
            }
            val salary = EmployeeSalaryDO()
            employeeSalaryDao.setEmployee(salary, employeeId)
            salary.year = read.year
            salary.month = read.month
            salary.bruttoMitAgAnteil = read.bruttoMitAgAnteil
            val storedId = entry.stored?.id
            if (storedId != null) {
                salary.id = storedId
                result.updated += 1
            } else {
                result.inserted += 1
            }
            toPersist.add(salary)
            processedNumber += 1
        }
        if (toPersist.isNotEmpty()) {
            employeeSalaryDao.insertOrUpdate(toPersist, SALARY_INSERT_BLOCK_SIZE, checkAccess = false)
        }
        log.info { "Import completed: inserted=${result.inserted}, updated=${result.updated}." }
    }

    override fun readAccess(user: PFUserDO?): Boolean {
        user ?: return false
        return isOwner || employeeSalaryDao.hasUserSelectAccess(user, false)
    }

    override fun writeAccess(user: PFUserDO?): Boolean {
        user ?: return false
        return isOwner || employeeSalaryDao.hasLoggedInUserInsertAccess()
    }

    companion object {
        private const val SALARY_INSERT_BLOCK_SIZE = 50
    }
}
