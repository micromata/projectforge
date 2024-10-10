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

package org.projectforge.framework.persistence.candh

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.orga.ContractDO
import org.projectforge.business.orga.ContractStatus
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.test.AbstractTestBase
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class CandHCopyTest : AbstractTestBase() {
    @Test
    fun baseTests() {
        val src = ContractDO()
        val dest = ContractDO()
        assertContracts(src, dest, EntityCopyStatus.NONE)
        src.id = 42
        assertContracts(src, dest, EntityCopyStatus.NONE, "Id is not copied.")
        src.title = "Title"
        assertContracts(src, dest, EntityCopyStatus.MAJOR)
        src.attachmentsSize = 100
        assertContracts(src, dest, EntityCopyStatus.MINOR)
        src.attachmentsCounter = 2
        assertContracts(src, dest, EntityCopyStatus.MINOR)
        src.number = 42
        assertContracts(src, dest, EntityCopyStatus.MAJOR)
        src.validFrom = LocalDate.of(2024, Month.SEPTEMBER, 9)
        assertContracts(src, dest, EntityCopyStatus.MAJOR)
        src.status = ContractStatus.SIGNED
        assertContracts(src, dest, EntityCopyStatus.MAJOR)
    }

    @Test
    fun collectionTests() {
        val src = GroupDO()
        val dest = GroupDO()
        copyValues(src, dest, EntityCopyStatus.NONE).let { context ->
            Assertions.assertEquals(0, context.historyEntries!!.size)
        }
        dest.assignedUsers = mutableSetOf()
        copyValues(src, dest, EntityCopyStatus.NONE).let { context ->
            Assertions.assertEquals(0, context.historyEntries!!.size)
        }

        val user1 = createUser(1, "user1")
        val user2 = createUser(2, "user2")
        val user3 = createUser(3, "user3")
        dest.assignedUsers = mutableSetOf(user1, user2)
        copyValues(src, dest, EntityCopyStatus.MAJOR).let { context ->
            Assertions.assertEquals(1, context.historyEntries!!.size)
            assertHistoryEntry(context, "assignedUsers", "", "1,2")
        }
        Assertions.assertTrue(dest.assignedUsers.isNullOrEmpty())

        src.assignedUsers = mutableSetOf(user1, user2)
        copyValues(src, dest, EntityCopyStatus.MAJOR)
        Assertions.assertEquals(2, dest.assignedUsers!!.size)
        Assertions.assertTrue(dest.assignedUsers!!.any { it.id == 1L })
        Assertions.assertTrue(dest.assignedUsers!!.any { it.id == 2L })

        src.assignedUsers!!.add(user3)
        copyValues(src, dest, EntityCopyStatus.MAJOR)
        Assertions.assertEquals(3, dest.assignedUsers!!.size)
        Assertions.assertTrue(dest.assignedUsers!!.any { it.id == 1L })
        Assertions.assertTrue(dest.assignedUsers!!.any { it.id == 2L })
        Assertions.assertTrue(dest.assignedUsers!!.any { it.id == 3L })

        src.assignedUsers!!.remove(user2)
        copyValues(src, dest, EntityCopyStatus.MAJOR)
        Assertions.assertEquals(2, dest.assignedUsers!!.size)
        Assertions.assertTrue(dest.assignedUsers!!.any { it.id == 1L })
        Assertions.assertTrue(dest.assignedUsers!!.any { it.id == 3L })
    }

    @Test
    fun auftragTest() {
        val src = AuftragDO()
        val dest = AuftragDO()
        copyValues(src, dest, EntityCopyStatus.NONE)
        val pos1 = AuftragsPositionDO()
        pos1.auftrag = src
        pos1.nettoSumme = BigDecimal.valueOf(2590, 2)
        src.addPosition(pos1)
        copyValues(src, dest, expectedStatus = EntityCopyStatus.MAJOR)
        Assertions.assertEquals(1, dest.positionen!!.size)
        Assertions.assertEquals("25.90", dest.positionen!![0].nettoSumme!!.toString())
        val destPos1 = AuftragsPositionDO()
        destPos1.auftrag = dest
        destPos1.nettoSumme = BigDecimal.valueOf(259, 1)
        destPos1.number = 1
        dest.positionen = mutableListOf(destPos1)
        copyValues(src, dest, EntityCopyStatus.NONE)
        Assertions.assertEquals(
            "25.9",
            dest.positionen!![0].nettoSumme!!.toString(),
            "25.90 should be equals to 25.9, scale of BigDecimal isn't checked."
        )

        pos1.nettoSumme = BigDecimal.TEN
        copyValues(src, dest, EntityCopyStatus.MAJOR)
        Assertions.assertEquals("10", dest.positionen!![0].nettoSumme!!.toString())
    }

    private fun createUser(id: Long, username: String): PFUserDO {
        val user = PFUserDO()
        user.id = id
        user.username = username
        return user
    }

    private fun assertContracts(
        src: ContractDO,
        dest: ContractDO,
        expectedStatus: EntityCopyStatus,
        msg: String = "",
    ) {
        copyValues(src, dest, expectedStatus)
        // Assertions.assertEquals(src.id, dest.id, msg) // Id is not copied.
        Assertions.assertEquals(src.title, dest.title, msg)
        Assertions.assertEquals(src.number, dest.number, msg)
        Assertions.assertEquals(src.attachmentsSize, dest.attachmentsSize, msg)
        Assertions.assertEquals(src.attachmentsCounter, dest.attachmentsCounter, msg)
        Assertions.assertEquals(src.validFrom, dest.validFrom, msg)
        Assertions.assertEquals(src.status, dest.status, msg)
    }

    private fun assertHistoryEntry(
        context: CandHContext,
        propertyName: String,
        oldValue: Any?,
        newValue: Any?,
        type: PropertyOpType = PropertyOpType.Update,
    ) {
        context.historyEntries!!.flatMap { it.attributes ?: emptySet() }.find { it.propertyName == propertyName }.apply {
            Assertions.assertEquals(type, type)
            Assertions.assertEquals(propertyName, propertyName)
            Assertions.assertEquals(oldValue, oldValue)
            Assertions.assertEquals(newValue, newValue)
        }
    }

    private fun <IdType : Serializable> copyValues(
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        expectedStatus: EntityCopyStatus,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHContext {
        val resultContext =
            CandHContext(src, entityOpType = EntityOpType.Update)
        CandHMaster.copyValues(src, dest, resultContext)
        Assertions.assertEquals(expectedStatus, resultContext.currentCopyStatus)
        CandHContext(src, entityOpType = entityOpType).let { context ->
            CandHMaster.copyValues(src, dest, context)
            Assertions.assertEquals(EntityCopyStatus.NONE, context.currentCopyStatus)
        }
        return resultContext
    }
}
