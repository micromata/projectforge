/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.api.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDao
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.task.TaskDao

class HibernateSearchClassInfoTest {
    @Test
    fun addressTest() {
        val info = HibernateSearchClassInfo(AddressDao())
        Assertions.assertEquals("name", info.get("name")?.field)
        Assertions.assertTrue(info.isStringField("name"))
        Assertions.assertNull(info.get("uid"))
        Assertions.assertFalse(info.isStringField("uid"))
        Assertions.assertNull(info.getClassBridge("uid"))
        Assertions.assertEquals(0, info.classBridges.size)
    }

    @Test
    fun taskTest() {
        val info = HibernateSearchClassInfo(TaskDao())
        Assertions.assertEquals("title", info.get("title")?.field)
        Assertions.assertNull(info.get("status"))
        Assertions.assertEquals("progress", info.get("progress")?.field)
        Assertions.assertFalse(info.isStringField("progress"))
        Assertions.assertNotNull(info.getClassBridge("taskpath"))
        Assertions.assertEquals(1, info.classBridges.size)
    }

    @Test
    fun rechnungTest() {
        val info = HibernateSearchClassInfo(RechnungDao())
        Assertions.assertEquals("kunde.name", info.get("kunde.name")?.field)
    }
}
