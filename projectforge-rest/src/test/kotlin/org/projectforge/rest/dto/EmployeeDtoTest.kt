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


package org.projectforge.rest.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate

class EmployeeDtoTest : AbstractTestBase() {
    /** The employee cache holds no deleted employee: its name and flags are taken from the employee itself. */
    @Test
    fun `a deleted or left employee keeps its name and flags in a reference list`() {
        val deleted = EmployeeDO().also {
            it.id = 4711_0001
            it.deleted = true
            it.user = PFUserDO().also { user -> user.firstname = "Deleted"; user.lastname = "Employee" }
        }
        val left = EmployeeDO().also {
            it.id = 4711_0002
            it.austrittsDatum = LocalDate.now().minusYears(1)
            it.user = PFUserDO().also { user -> user.firstname = "Left"; user.lastname = "Employee" }
        }
        val list = Employee.toEmployeeList(listOf(deleted, left))!!.associateBy { it.id }
        list[deleted.id]!!.let {
            assertEquals("Deleted Employee", it.displayName)
            assertEquals(true, it.deleted)
            assertEquals(false, it.deactivated)
        }
        list[left.id]!!.let {
            assertEquals(true, it.deactivated)
            assertEquals(false, it.deleted)
        }
        // Not in the cache: restoreDisplayNames leaves them as they are.
        Employee.restoreDisplayNames(list.values.toList())
        assertEquals("Deleted Employee", list[deleted.id]!!.displayName)
        assertEquals(true, list[deleted.id]!!.deleted)
    }
}
