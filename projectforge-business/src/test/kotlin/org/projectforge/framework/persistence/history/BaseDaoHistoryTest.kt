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
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.framework.persistence.history.DisplayHistoryEntry.Companion.translatePropertyName
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class BaseDaoHistoryTest : AbstractTestBase() {
    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun testOldInvoiceHistory() {
        HistoryServiceTest.ensureSetup(persistenceService, historyService)
        persistenceService.runReadOnly { context ->
            val invoice = context.em.find(RechnungDO::class.java, 351958)
            logon(TEST_FINANCE_USER)
            val entries = rechnungDao.getDisplayHistoryEntries(invoice)
            // 6 entries for RechnungDO: 351958
            entries.filter { it.historyEntryId == HistoryServiceTest.getNewHistoryEntryId(2972182L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                list[0].apply { Assertions.assertEquals(EntityOpType.Insert, entryType) }
            }
            entries.filter { it.historyEntryId == HistoryServiceTest.getNewHistoryEntryId(3042917L) }.let { list ->
                Assertions.assertEquals(3, list.size)
                assertHistoryEntry(list[0], RechnungDO::class.java, "bezahlDatum", "", "2010-02-22")
                assertHistoryEntry(list[1], RechnungDO::class.java, "status", "invoiced", "paid")
                assertHistoryEntry(list[2], RechnungDO::class.java, "zahlBetrag", "", "4455.00")
            }
            entries.filter { it.historyEntryId == HistoryServiceTest.getNewHistoryEntryId(3062919L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                assertHistoryEntry(list[0], RechnungDO::class.java, "betreff", "DM 2010 #674", "DM 2010")
            }
            entries.filter { it.historyEntryId == HistoryServiceTest.getNewHistoryEntryId(6191673L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                assertHistoryEntry(list[0], RechnungDO::class.java, "konto", "", "12202 - ACME Int.")
            }

            // 4 entries for RechnungsPositionDO 351960: 2972178,2985201,3026625,3062923
            entries.filter { it.historyEntryId == HistoryServiceTest.getNewHistoryEntryId(3026625L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                assertHistoryEntry(
                    list[0],
                    RechnungsPositionDO::class.java,
                    "kostZuweisungen",
                    "",
                    "0:null|null:10.10, 1:null|null:20.20, 2:null|null:30.30"
                )
            }

            // 4 entries for RechnungsPositionDO 351960: 2972186,2988849,3026621,3062915
            entries.filter { it.historyEntryId == HistoryServiceTest.getNewHistoryEntryId(2988849L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                assertHistoryEntry(list[0], RechnungsPositionDO::class.java, "vat", "0.19000", "0.19")
            }
            // 2 entries for KostZuweisungDO: 382506
            entries.filter { it.historyEntryId == HistoryServiceTest.getNewHistoryEntryId(3024280L) }.let { list ->
                Assertions.assertEquals(1, list.size)
                // Kost2DO#166807 and Kost2DO#331838 aren't in the database.
                assertHistoryEntry(list[0], KostZuweisungDO::class.java, "kost2", "Kost2DO#166807", "Kost2DO#331838")
            }
            // 2 entries for KostZuweisungDO: 382507
            // 2 entries for KostZuweisungDO: 382508
            // 2 entries for KostZuweisungDO: 382509

            // 22 in total
            Assertions.assertEquals(22, entries.size)
        }
    }

    private fun assertHistoryEntry(
        entry: DisplayHistoryEntry,
        clazz: Class<*>?,
        propertyName: String,
        oldValue: String?,
        newValue: String?,
    ) {
        clazz?.let { cls ->
            Assertions.assertEquals(
                translatePropertyName(cls, propertyName),
                entry.displayPropertyName,
                "$cls.$propertyName"
            )
        }
        Assertions.assertEquals(propertyName, entry.propertyName, "$clazz.$propertyName")
        Assertions.assertEquals(EntityOpType.Update, entry.entryType, "$clazz.$propertyName")
        Assertions.assertEquals(oldValue, entry.oldValue, "$clazz.$propertyName")
        Assertions.assertEquals(newValue, entry.newValue, "$clazz.$propertyName")
    }
}
