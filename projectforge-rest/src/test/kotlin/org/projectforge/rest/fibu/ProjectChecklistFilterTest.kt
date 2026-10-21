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


package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.customergroup.CustomerGroupConfig
import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.business.fibu.customergroup.ProjectGroup
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.timesheet.TimesheetDO

/** The project checklist matches a row by the id of its project, or by what the project ids map to. */
class ProjectChecklistFilterTest {
    private val filter = ProjectChecklistFilter("order/projectFilterValues")

    @Test
    fun `ids match the order's project`() {
        val predicate = filter.buildPredicate(arrayOf("17", "42"))!!
        assertTrue(predicate.match(order(projektId = 17)))
        assertTrue(predicate.match(order(projektId = 42)))
        assertFalse(predicate.match(order(projektId = 99)))
        assertFalse(predicate.match(order(projektId = null)))
    }

    @Test
    fun `no usable key filters nothing`() {
        assertNull(filter.buildPredicate(null))
        assertNull(filter.buildPredicate(arrayOf()))
        assertNull(filter.buildPredicate(arrayOf("abc")))
    }

    @Test
    fun `mapped ids match a timesheet by its cost 2`() {
        val kost2sOfProject = mapOf(17L to listOf(100L, 101L))
        val sheets = ProjectChecklistFilter("timesheet/projectFilterValues", path = "kost2.id") { ids ->
            ids.flatMap { kost2sOfProject[it].orEmpty() }
        }
        val predicate = sheets.buildPredicate(arrayOf("17"))!!
        assertTrue(predicate.match(sheet(kost2Id = 101)))
        assertFalse(predicate.match(sheet(kost2Id = 200)))
        // A project without cost 2 maps to nothing, and so filters nothing.
        assertNull(sheets.buildPredicate(arrayOf("42")))
    }

    @Test
    fun `a project group stands for its projects, also next to single ids`() {
        val grouped = ProjectChecklistFilter("order/projectFilterValues", groupIndex = { index })
        val predicate = grouped.buildPredicate(arrayOf("pg:prjgr1"))!!
        assertTrue(predicate.match(order(projektId = 17)))
        assertTrue(predicate.match(order(projektId = 18)))
        assertFalse(predicate.match(order(projektId = 42)))
        val mixed = grouped.buildPredicate(arrayOf("pg:prjgr1", "42", "17"))!!
        assertTrue(mixed.match(order(projektId = 42)))
        assertTrue(mixed.match(order(projektId = 18)))
        assertFalse(mixed.match(order(projektId = 99)))
        // Each once, though picked singly and through the group:
        assertEquals(listOf(17L, 18L, 42L), ProjectChecklistFilter.projectIdsOf(arrayOf("pg:prjgr1", "42", "17"), { index }).sorted())
    }

    @Test
    fun `an unknown project group stands for no project`() {
        val grouped = ProjectChecklistFilter("order/projectFilterValues", groupIndex = { index })
        assertNull(grouped.buildPredicate(arrayOf("pg:gone01")))
        val predicate = grouped.buildPredicate(arrayOf("pg:gone01", "42"))!!
        assertTrue(predicate.match(order(projektId = 42)))
        assertFalse(predicate.match(order(projektId = 17)))
    }

    @Test
    fun `a project group of a timesheet filter maps its projects to their cost 2`() {
        val kost2sOfProject = mapOf(17L to listOf(100L), 18L to listOf(101L))
        val sheets = ProjectChecklistFilter("timesheet/projectFilterValues", path = "kost2.id", groupIndex = { index }) { ids ->
            ids.flatMap { kost2sOfProject[it].orEmpty() }
        }
        val predicate = sheets.buildPredicate(arrayOf("pg:prjgr1"))!!
        assertTrue(predicate.match(sheet(kost2Id = 100)))
        assertTrue(predicate.match(sheet(kost2Id = 101)))
        assertFalse(predicate.match(sheet(kost2Id = 200)))
    }

    private val index = CustomerGroupIndex(
        CustomerGroupConfig(
            projectGroups = mutableListOf(
                ProjectGroup().also {
                    it.key = "prjgr1"
                    it.name = "Parent"
                    it.projects = mutableListOf(17, 18)
                },
            ),
        ),
    )

    private fun sheet(kost2Id: Long) = TimesheetDO().also { it.kost2 = Kost2DO().also { k -> k.id = kost2Id } }

    private fun order(projektId: Long?) = AuftragDO().also { order ->
        order.projekt = projektId?.let { id -> ProjektDO().also { it.id = id } }
    }
}
