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

package org.projectforge.business.admin

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class DatabaseStatisticsBuilderTest : AbstractTestBase() {
    @Autowired
    private lateinit var databaseStatisticsBuilder: DatabaseStatisticsBuilder

    /**
     * HSQLDB has no row estimate (pg_class), so the history tables are counted exactly as all others.
     */
    @Test
    fun `table counts without row estimate`() {
        val stats = SystemStatisticsData()
        databaseStatisticsBuilder.addStatisticsEntries(stats)
        val values = stats.entries.associate { it.id to it.valueAsString() }
        listOf(
            "totalNumberOfTimesheets",
            "totalNumberOfUsers",
            "totalNumberOfTasks",
            "totalNumberOfHistoryEntries",
            "totalNumberOfHistoryAttrs",
        ).forEach { id ->
            val value = values[id]
            Assertions.assertNotNull(value, id)
            Assertions.assertTrue(value!!.matches(Regex("""\d[\d.,]*""")), "$id: '$value'")
        }
        Assertions.assertNotEquals("0", values["totalNumberOfUsers"])
    }
}
