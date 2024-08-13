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

package org.projectforge.business.vacation.service

import mu.KotlinLogging
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.cache.AbstractCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * Cache with vacation conflicts (vacations of employee conflicting to vacations of substitutes / replacements).
 * Only current vacations or vacations in the future are considered.
 */
@Service
class ConflictingVacationsCache() : AbstractCache() {
  @Autowired
  private lateinit var employeeService: EmployeeService

  @Autowired
  private lateinit var vacationDao: VacationDao

  private lateinit var vacationService: VacationService

  /**
   * Key is the employee id.
   */
  private var conflictingVacationsByEmployee = mutableMapOf<Int, MutableList<VacationDO>>()

  /**
   * List of vacation id's with conflicts.
   */
  private var allConflictingVacations = mutableSetOf<Int>()

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @PostConstruct
  private fun postConstruct() {
    this.vacationService = applicationContext.getBean(VacationService::class.java)
  }

  fun updateVacation(vacationDO: VacationDO, conflict: Boolean) {
    checkRefresh()
    synchronized(allConflictingVacations) {
      vacationDO.id?.let { id ->
        if (!conflict) {
          allConflictingVacations.remove(id) // If given
        } else {
          allConflictingVacations.add(id)
        }
      }
    }
    synchronized(conflictingVacationsByEmployee) {
      if (!conflict) {
        conflictingVacationsByEmployee[vacationDO.employeeId]?.let { vacations ->
          vacations.remove(vacationDO) // If exist
        }
      } else {
        ensureEmployeeList(conflictingVacationsByEmployee, vacationDO.employeeId).add(vacationDO)
      }
    }
  }

  fun hasConflict(vacationDO: VacationDO): Boolean {
    return hasConflict(vacationDO.id)
  }

  fun hasConflict(vacationId: Int?): Boolean {
    vacationId ?: return false
    checkRefresh()
    synchronized(allConflictingVacations) {
      return allConflictingVacations.contains(vacationId)
    }
  }

  fun numberOfConflicts(userId: Int): Int {
    employeeService.getEmployeeByUserId(userId)?.id?.let { employeeId ->
      checkRefresh()
      synchronized(allConflictingVacations) {
        return conflictingVacationsByEmployee[employeeId]?.size ?: 0
      }
    }
    return 0
  }

  override fun refresh() {
    log.info("Refreshing cache of conflicting vacations...")
    val vacationByEmployee = mutableMapOf<Int, MutableList<VacationDO>>()
    // First, order all vacations by employee:
    val all = vacationDao.getCurrentAndFutureVacations()
    all.forEach { vacation ->
      vacation.employeeId?.let { employeeId ->
        ensureEmployeeList(vacationByEmployee, employeeId).add(vacation)
      }
    }
    val newConflictingVacations = mutableMapOf<Int, MutableList<VacationDO>>()
    val newAllConflictingVacations = mutableSetOf<Int>()
    // Now find conflicting entries:
    all.forEach { vacation ->
      val vacationsOfReplacements = mutableListOf<VacationDO>()
      vacation.allReplacements.forEach { replacementEmployee ->
        vacationsOfReplacements.addAll(all.filter { it.employeeId == replacementEmployee.id })
      }
      if (vacationService.checkConflict(vacation, vacationsOfReplacements)) {
        ensureEmployeeList(newConflictingVacations, vacation.employeeId).add(vacation)
        vacation.id?.let {
          newAllConflictingVacations.add(it)
        }
      }
    }
    conflictingVacationsByEmployee = newConflictingVacations
    allConflictingVacations = newAllConflictingVacations
    log.info("Refreshing cache of conflicting vacations done. Found ${allConflictingVacations.size} conflicts of ${conflictingVacationsByEmployee.size} employees.")
  }

  private fun ensureEmployeeList(
    vacationsByEmployee: MutableMap<Int, MutableList<VacationDO>>,
    employeeId: Int?,
  ): MutableList<VacationDO> {
    employeeId ?: return mutableListOf() // Shouldn't occur.
    var vacations = vacationsByEmployee[employeeId]
    if (vacations == null) {
      vacations = mutableListOf()
      vacationsByEmployee[employeeId] = vacations
    }
    return vacations
  }
}
