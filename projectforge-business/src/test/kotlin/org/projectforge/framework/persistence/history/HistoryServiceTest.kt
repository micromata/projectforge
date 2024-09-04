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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.task.TaskDO
import org.projectforge.test.AbstractTestBase

class HistoryServiceTest : AbstractTestBase() {
    @Test
    fun testNonHistorizableProperties() {
        var set = HistoryService.get().getNoHistoryProperties(TaskDO::class.java)
        Assertions.assertEquals(2, set.size)
        Assertions.assertTrue(set.contains("lastUpdate"))
        Assertions.assertTrue(set.contains("created"))
        Assertions.assertTrue(set.contains("lastUpdate"))
        Assertions.assertTrue(set.contains("created"))
        Assertions.assertFalse(set.contains("title"))

        set = HistoryService.get().getNoHistoryProperties(AuftragDO::class.java)
        Assertions.assertEquals(7, set.size)
        //    not, because transient Assertions.assertTrue(set.contains("uiStatus"));
        Assertions.assertTrue(set.contains("uiStatusAsXml"))
        Assertions.assertFalse(set.contains("subject"))
    }
}
