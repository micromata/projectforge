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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.configuration.ApplicationContextProvider
import java.math.BigDecimal
import java.time.LocalDate

class Employee(
  id: Long? = null,
  displayName: String? = null,
  var user: User? = null,
  var kost1: Kost1? = null,
  var status: EmployeeStatus? = null,
  var position: String? = null,
  var eintrittsDatum: LocalDate? = null,
  var austrittsDatum: LocalDate? = null,
  var abteilung: String? = null,
  var staffNumber: String? = null,
  var urlaubstage: Int? = null,
  var weeklyWorkingHours: BigDecimal? = null,
  var comment: String? = null,
) : BaseDTODisplayObject<EmployeeDO>(id, displayName = displayName) {
  companion object {
    private val employeeDao = ApplicationContextProvider.getApplicationContext().getBean(EmployeeDao::class.java)

    /**
     * Set display names of any existing user in the given list.
     * @see UserService.getUser
     */
    fun restoreDisplayNames(employees: List<Employee>?) {
      employees?.forEach { it.displayName = employeeDao.getById(it.id, checkAccess = false)?.displayName }
    }
  }
}
