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

package org.projectforge.framework.persistence.jpa.candh

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.history.*
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass

class CandHHistoryTest : AbstractTestBase() {
    @Autowired
    private lateinit var userDao: UserDao

    @Test
    fun baseTests() {
        logon(ADMIN_USER)
        val user = PFUserDO()
        user.username = "${PREFIX}test"
        var lastStats = countHistoryEntries()
        userDao.saveInTrans(user)
        assertNumberOfNewHistoryEntries(lastStats, 1, 0)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(1, entries.size)
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER, entries[0])
        }
        user.email = "horst@acme.com"
        user.username = "${PREFIX}test_changed"
        user.firstname = "Horst"
        user.lastname = "Schlemmer"
        lastStats = countHistoryEntries()
        userDao.updateInTrans(user)
        assertNumberOfNewHistoryEntries(lastStats, 1, 4)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(2, entries.size)
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Update, ADMIN_USER, entries[0], 4)
            (entries[0] as PfHistoryMasterDO).let { entry ->
                assertAttrEntry(
                    "java.lang.String",
                    "${PREFIX}test_changed",
                    "${PREFIX}test",
                    "username",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.lang.String",
                    "horst@acme.com",
                    null,
                    "email",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.lang.String",
                    "Horst",
                    null,
                    "firstname",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.lang.String",
                    "Schlemmer",
                    null,
                    "lastname",
                    PropertyOpType.Update,
                    entry.attributes,
                )
            }
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER, entries[1])
        }
    }

    private fun assertMasterEntry(
        entityClass: KClass<*>,
        id: Long?,
        opType: EntityOpType,
        modUser: PFUserDO,
        entry: HistoryEntry,
        numberOfAttributes: Int = 0,
    ) {
        Assertions.assertEquals(entityClass.java.name, entry.entityName)
        Assertions.assertEquals(id, entry.entityId)
        Assertions.assertEquals(opType, entry.entityOpType)
        Assertions.assertEquals(modUser.id?.toString(), entry.modifiedBy)
        Assertions.assertTrue(
            System.currentTimeMillis() - entry.modifiedAt!!.time < 10000,
            "Time difference is too big",
        )
        entry as PfHistoryMasterDO
        Assertions.assertEquals(numberOfAttributes, entry.attributes?.size ?: 0)
    }

    private fun assertAttrEntry(
        propertyClass: String?,
        value: String?,
        oldValue: String?,
        propertyName: String?,
        optype: PropertyOpType,
        attributes: Set<PfHistoryAttrDO>?,
    ) {
        Assertions.assertFalse(attributes.isNullOrEmpty())
        val attr = attributes?.first { it.propertyName == propertyName }
        Assertions.assertNotNull(attr)
        Assertions.assertEquals(propertyClass, attr!!.propertyTypeClass)
        Assertions.assertEquals(value, attr.value)
        Assertions.assertEquals(oldValue, attr.oldValue)
        Assertions.assertEquals(propertyName, attr.propertyName)
        Assertions.assertEquals(optype, attr.optype)

    }

    private fun assertNumberOfNewHistoryEntries(
        lastStats: Pair<Long, Long>,
        expectedNumberOfNewMasterEntries: Long,
        expectedNumberOfNewAttrEntries: Long,
    ) {
        val count = countHistoryEntries()
        Assertions.assertEquals(
            expectedNumberOfNewMasterEntries,
            count.first - lastStats.first,
            "Number of master entries"
        )
        Assertions.assertEquals(
            expectedNumberOfNewAttrEntries,
            count.second - lastStats.second,
            "Number of attr entries"
        )
    }

    private fun countHistoryEntries(): Pair<Long, Long> {
        val countMasterEntries = persistenceService.selectSingleResult(
            "select count(*) from PfHistoryMasterDO",
            Long::class.java,
        )
        val countAttrEntries = persistenceService.selectSingleResult(
            "select count(*) from PfHistoryAttrDO",
            Long::class.java,
        )
        return Pair(countMasterEntries!!, countAttrEntries!!)
    }


    companion object {
        private const val PREFIX = "CandHHistoryTest_"
    }
}
