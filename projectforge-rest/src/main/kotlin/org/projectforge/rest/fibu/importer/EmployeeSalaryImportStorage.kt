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
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryService
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage
import java.time.Month

private val log = KotlinLogging.logger {}

/**
 * The session-held storage of the employee-salary xlsx import, the hand-built sibling of the legacy
 * Merlin/Wicket import. It keeps the rows the [EmployeeSalaryExcelImporter] parsed and reconciles them
 * against the database.
 *
 * The reconcile is far simpler than the Kreditor's fuzzy scoring: a salary is unique per
 * (employee, year, month), so each read row is matched to the one database salary of the same employee in
 * the same accounting month (via [EmployeeSalaryService.selectByMonth]). A match becomes the `stored`
 * side of the pair (→ MODIFIED/UNMODIFIED by the gross amount), no match is a NEW salary, and a row whose
 * staff number resolved to no employee stays FAULTY through its carried error.
 *
 * @author Kai Reinhard
 */
class EmployeeSalaryImportStorage :
    ImportStorage<EmployeeSalaryImportDTO>(ImportSettings()) {

    override val targetEntityTitle: String
        get() = translate("fibu.employee.salaries.import")

    /** The rows the parser read from the file, kept so a re-reconcile can rebuild the pairs. */
    val readSalaries = mutableListOf<EmployeeSalaryImportDTO>()

    override fun prepareEntity(): EmployeeSalaryImportDTO {
        return EmployeeSalaryImportDTO()
    }

    override fun commitEntity(obj: EmployeeSalaryImportDTO) {
        readSalaries.add(obj)
        val pairEntry = ImportPairEntry(read = obj)
        obj.getErrors().forEach { pairEntry.addError(it) }
        addEntry(pairEntry)
    }

    override fun doReconcileImportStorage(rereadDatabaseEntries: Boolean) {
        val context = ApplicationContextProvider.getApplicationContext()
        val service = context.getBean(EmployeeSalaryService::class.java)
        // Resolve the stored salary's employee display from the in-memory cache, never through the DO's
        // lazy `employee` association: navigating `salary.employee.staffNumber`/`.displayName` per row would
        // fire one employee and one user select for every salary of the month (the N+1 seen in the log).
        val employeeCache = context.getBean(EmployeeCache::class.java)
        // One DB lookup per distinct accounting month, cached in this reconcile run.
        val dbByMonth = mutableMapOf<Pair<Int, Int>, Map<Long, EmployeeSalaryDO>>()

        clearEntries()
        readSalaries.forEach { read ->
            val year = read.year
            val month = read.month
            val employeeId = read.employeeId
            val stored = if (year != null && month != null && employeeId != null) {
                val byEmployee = dbByMonth.getOrPut(year to month) {
                    // `salary.employee?.id` reads the foreign key off the lazy proxy without initializing it.
                    service.selectByMonth(year, Month.of(month))
                        .mapNotNull { salary -> salary.employee?.id?.let { it to salary } }
                        .toMap()
                }
                byEmployee[employeeId]?.let { toStoredDTO(it, employeeId, employeeCache) }
            } else {
                null
            }
            val pairEntry = ImportPairEntry(read = read, stored = stored)
            read.getErrors().forEach { pairEntry.addError(it) }
            addEntry(pairEntry)
        }
        log.debug { "Reconciled ${pairEntries.size} employee-salary import rows." }
    }

    override fun commitEntity(pairEntry: ImportPairEntry<EmployeeSalaryImportDTO>) {
        pairEntry.read?.let { dto ->
            readSalaries.add(dto)
            dto.getErrors().forEach { pairEntry.addError(it) }
        }
        addEntry(pairEntry)
    }

    private fun toStoredDTO(
        salary: EmployeeSalaryDO,
        employeeId: Long,
        employeeCache: EmployeeCache,
    ): EmployeeSalaryImportDTO {
        // All display data comes from the cache (in-memory), so no lazy association is touched.
        val employee = employeeCache.getEmployee(employeeId)
        return EmployeeSalaryImportDTO(
            staffNumber = employee?.staffNumber?.toIntOrNull(),
            employee = employeeCache.getUser(employee)?.getFullname(),
            employeeId = employeeId,
            year = salary.year,
            month = salary.month,
            bruttoMitAgAnteil = salary.bruttoMitAgAnteil,
            id = salary.id,
        )
    }
}
