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

package org.projectforge.framework.json

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class JsonTestUtils {
    val employee1 = EmployeeDO().also { employee ->
        employee.id = 101
        employee.abteilung = "Abteilung 1"
        employee.user = PFUserDO().also {
            it.id = 1
            it.username = "user1"
        }
    }
    val employee2 = EmployeeDO().also { employee ->
        employee.id = 102
        employee.abteilung = "Abteilung 2"
        employee.user = PFUserDO().also {
            it.id = 2
            it.username = "user2"
        }
    }
    val employee3 = EmployeeDO().also { employee ->
        employee.id = 103
        employee.abteilung = "Abteilung 3"
        employee.user = PFUserDO().also {
            it.id = 3
            it.username = "user3"
        }
    }
    val vacation = VacationDO().also {
        it.id = 5
        it.comment = "This is a comment"
        it.employee = employee1
        it.manager = employee2
        it.replacement = employee2
        it.otherReplacements = mutableSetOf(employee2, employee3)
    }
}
