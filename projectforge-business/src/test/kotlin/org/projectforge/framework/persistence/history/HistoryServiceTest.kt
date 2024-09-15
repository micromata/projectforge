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
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class HistoryServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var historyService: HistoryService

    @Test
    fun testNonHistorizableProperties() {
        ensureSetup()
    }

    fun ensureSetup() {
        if (pk > 1) {
            return // Already done.
        }
        val user = getUser(TEST_USER)
        // One group assigned:
        addOldFormat(user, value = "1052256", propertyName = "assignedGroups", operationType = EntityOpType.Insert)
        // Assigned: 34,101478,33 unassigned: 17,16,11,31
        addOldFormat(
            user,
            value = "34,101478,33",
            oldValue = "17,16,11,31",
            propertyName = "assignedGroups",
            operationType = EntityOpType.Insert
        )
        // U
        addOldFormat(
            user,
            value = "Project manager",
            oldValue = "Project assistant",
            propertyName = "description",
            operationType = EntityOpType.Update,
        )
    }

    /**
     * Create entries in new format:
     */
    private fun add(
        entity: BaseDO<Long>,
        value: String?,
        oldValue: String? = null,
        propertyName: String,
        operationType: EntityOpType
    ) {
        val master = HistoryServiceUtils.createMaster(entity, operationType)

        val attr1 = HistoryServiceUtils.createAttr(
            GroupDO::class,
            propertyName = propertyName,
            value = value,
            oldValue = oldValue,
        )
        val attrs = mutableListOf(attr1)

        pk = historyService.save(master, attrs)!!

        Assertions.assertEquals("org.projectforge.framework.persistence.user.entities.PFUserDO", master.entityName)
        Assertions.assertEquals(entity.id, master.entityId)
        Assertions.assertEquals("anon", master.modifiedBy)
        val createdAt = master.modifiedAt!!.time
        Assertions.assertTrue(
            Math.abs(System.currentTimeMillis() - createdAt) < 10000,
            "createdAt should be near to now (10s)",
        )

        Assertions.assertEquals(master.id, attr1.master!!.id)
    }

    /**
     * Create entries in old mgc format:
     */
    private fun addOldFormat(
        entity: BaseDO<Long>,
        value: String?,
        oldValue: String? = null,
        propertyName: String,
        operationType: EntityOpType
    ) {
        val master = HistoryServiceUtils.createMaster(entity, operationType)

        val attr1 = HistoryServiceUtils.createAttr(GroupDO::class, propertyName = "$propertyName:nv", value = value)
        val attr2 = HistoryServiceUtils.createAttr(oldPropertyClass, "$propertyName:op", value = operationType.name)
        val attr3 = HistoryServiceUtils.createAttr(GroupDO::class, "$propertyName:ov", value = oldValue)
        val attrs = mutableListOf(attr1, attr2, attr3)

        pk = historyService.save(master, attrs)!!

        Assertions.assertEquals("org.projectforge.framework.persistence.user.entities.PFUserDO", master.entityName)
        Assertions.assertEquals(entity.id, master.entityId)
        Assertions.assertEquals("anon", master.modifiedBy)
        val createdAt = master.modifiedAt!!.time
        Assertions.assertTrue(
            Math.abs(System.currentTimeMillis() - createdAt) < 10000,
            "createdAt should be near to now (10s)",
        )

        Assertions.assertEquals(master.id, attr1.master!!.id)
    }

    companion object {
        private var pk = 1L
        private val oldPropertyClass = "de.micromata.genome.db.jpa.history.entities.PropertyOpType"
    }
}
