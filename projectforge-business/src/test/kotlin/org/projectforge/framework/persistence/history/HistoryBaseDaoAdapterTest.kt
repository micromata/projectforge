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
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class HistoryBaseDaoAdapterTest {
    @Test
    fun createHistoryUpdateEntryWithSingleAttributeTests() {
        // Simulating adding collection entries
        val user = PFUserDO()
        user.id = 42
        // First, check oldValue and newValue already transformed to csv string:
        HistoryBaseDaoAdapter.createHistoryUpdateEntryWithSingleAttribute(
            entity = user,
            propertyName = "test",
            oldValue = "1,2,3",
            newValue = "2,4,5", // removing 1 and 3, adding 4 and 5
            propertyTypeClass = GroupDO::class.java,
        ).let { entry ->
            Assertions.assertEquals("org.projectforge.framework.persistence.user.entities.PFUserDO", entry.entityName)
            assertAttr(entry.attributes!!.first())
        }
        // Now, check oldValue and newValue not transformed to csv string:
        HistoryBaseDaoAdapter.createHistoryUpdateEntryWithSingleAttribute(
            entity = user,
            propertyName = "test",
            oldValue = listOf(createGroup(1), createGroup(2), createGroup(3)),
            newValue = listOf(createGroup(2), createGroup(4), createGroup(5)), // removing 1 and 3, adding 4 and 5
            propertyTypeClass = GroupDO::class.java,
        ).let { entry ->
            Assertions.assertEquals("org.projectforge.framework.persistence.user.entities.PFUserDO", entry.entityName)
            assertAttr(entry.attributes!!.first())
        }
    }

    private fun assertAttr(attr: HistoryEntryAttrDO) {
        Assertions.assertEquals("1,2,3", attr.oldValue)
        Assertions.assertEquals("2,4,5", attr.value)
        Assertions.assertEquals("test", attr.propertyName)
        Assertions.assertEquals(PropertyOpType.Update, attr.opType)
        Assertions.assertEquals(GroupDO::class.java.name, attr.propertyTypeClass)
    }

    private fun createGroup(id: Long): GroupDO {
        val group = GroupDO()
        group.id = id
        return group
    }
}
