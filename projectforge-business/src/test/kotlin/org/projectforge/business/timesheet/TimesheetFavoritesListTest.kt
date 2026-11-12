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

package org.projectforge.business.timesheet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

/**
 * The app applies a template from the list alone, so the list must carry the content, not only id and name.
 */
class TimesheetFavoritesListTest : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetFavoritesService: TimesheetFavoritesService

    @Test
    fun listContainsTemplateContent() {
        logon(TEST_USER)
        timesheetFavoritesService.createFavorite(
            TimesheetFavorite(
                name = "listContent", taskId = 42L, cost2Id = 7L, description = "desc", location = "loc",
                reference = "ref", tag = "tag",
            )
        )
        val favorite = timesheetFavoritesService.getList().single { it.name == "listContent" }
        assertEquals(42L, favorite.taskId)
        assertEquals(7L, favorite.cost2Id)
        assertEquals("desc", favorite.description)
        assertEquals("loc", favorite.location)
        assertEquals("ref", favorite.reference)
        assertEquals("tag", favorite.tag)
    }
}
