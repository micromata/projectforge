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

import org.jetbrains.kotlin.builtins.StandardNames.FqNames.list
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.framework.persistence.history.DisplayHistoryEntry.Companion.translateProperty
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class BaseDaoHistoryTest : AbstractTestBase() {
    @Autowired
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun testOldInvoiceHistory() {
        HistoryServiceTest.ensureSetup(persistenceService, historyService)
        persistenceService.runReadOnly { context ->
            val invoice = context.em.find(RechnungDO::class.java, 351958)
            logon(TEST_FINANCE_USER)
            val entries = rechnungDao.getDisplayHistoryEntries(invoice, context)
            entries.filter { it.masterId == HistoryServiceTest.getNewMasterId(2972182L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                list[0].apply { Assertions.assertEquals(EntityOpType.Insert, entryType) }
            }
            entries.filter { it.masterId == HistoryServiceTest.getNewMasterId(3042917L) }.let { list ->
                Assertions.assertEquals(3, list.size)
                assertHistoryEntry(list[0], RechnungDO::class.java, "bezahlDatum", null, "2010-02-22")
                assertHistoryEntry(list[1], RechnungDO::class.java, "status", "GESTELLT", "BEZAHLT")
                assertHistoryEntry(list[2], RechnungDO::class.java, "zahlBetrag", null, "4455.00")
            }
            entries.filter { it.masterId == HistoryServiceTest.getNewMasterId(3062919L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                assertHistoryEntry(list[0], RechnungDO::class.java, "betreff", "DM 2010 #674", "DM 2010")
            }
            entries.filter { it.masterId == HistoryServiceTest.getNewMasterId(6191673L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                assertHistoryEntry(list[0], RechnungDO::class.java, "konto", null, "167040")
            }
            // 4 main entries in t_pf_history
            entries.forEach { entry ->
                println(entry)
            }
            Assertions.assertEquals(46, entries.size)
        }
    }

    private fun assertHistoryEntry(
        entry: DisplayHistoryEntry,
        clazz: Class<*>,
        propertyName: String,
        oldValue: String?,
        newValue: String?,
    ) {
        Assertions.assertEquals(translateProperty(clazz, propertyName), entry.propertyName, "$clazz.$propertyName")
        Assertions.assertEquals(EntityOpType.Update,  entry.entryType, "$clazz.$propertyName")
        Assertions.assertEquals(oldValue, entry.oldValue, "$clazz.$propertyName")
        Assertions.assertEquals(newValue, entry.newValue, "$clazz.$propertyName")
    }
}
