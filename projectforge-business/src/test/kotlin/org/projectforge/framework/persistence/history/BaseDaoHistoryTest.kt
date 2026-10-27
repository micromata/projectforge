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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.history.HistoryFormatUtils.Companion.translatePropertyName
import org.springframework.beans.factory.annotation.Autowired

class BaseDaoHistoryTest : AbstractTestBase() {
    @Autowired
    private lateinit var flatHistoryFormatService: FlatHistoryFormatService

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun testOldInvoiceHistory() {
        HistoryServiceOldFormatTest.ensureSetup(this, persistenceService, historyService)
        persistenceService.runReadOnly { context ->
            val invoice = context.em.find(RechnungDO::class.java, 703300)
            logon(TEST_FINANCE_USER)
            val entries = flatHistoryFormatService.selectHistoryEntriesAndConvert(rechnungDao, invoice)
            // 6 entries for RechnungDO: 703300
            entries.filter { it.historyEntryId == HistoryServiceOldFormatTest.getNewHistoryEntryId(7024000L) }
                .let { list ->
                    Assertions.assertEquals(1, list.size)
                    list[0].apply { Assertions.assertEquals(EntityOpType.Insert, opType) }
                }
            entries.filter { it.historyEntryId == HistoryServiceOldFormatTest.getNewHistoryEntryId(7066000L) }
                .let { list ->
                    Assertions.assertEquals(3, list.size)
                    assertHistoryEntry(list[0], RechnungDO::class.java, "bezahlDatum", "", "2010-02-22")
                    assertHistoryEntry(list[1], RechnungDO::class.java, "status", "invoiced", "paid")
                    assertHistoryEntry(list[2], RechnungDO::class.java, "zahlBetrag", "", "1500.00")
                }
            entries.filter { it.historyEntryId == HistoryServiceOldFormatTest.getNewHistoryEntryId(7072000L) }
                .let { list ->
                    Assertions.assertEquals(1, list.size)
                    assertHistoryEntry(list[0], RechnungDO::class.java, "betreff", "XY 2010 #123", "XY 2010")
                }
            entries.filter { it.historyEntryId == HistoryServiceOldFormatTest.getNewHistoryEntryId(7078000L) }
                .let { list ->
                    Assertions.assertEquals(1, list.size)
                    assertHistoryEntry(list[0], RechnungDO::class.java, "konto", "", "19001 - ACME Int.")
                }

            // 4 entries for RechnungsPositionDO 703900: 7021000,7030000,7063000,7075000
            entries.filter { it.historyEntryId == HistoryServiceOldFormatTest.getNewHistoryEntryId(7063000L) }
                .let { list ->
                    Assertions.assertEquals(1, list.size)
                    assertHistoryEntry(
                        list[0],
                        RechnungsPositionDO::class.java,
                        "kostZuweisungen",
                        "",
                        "0:null|null:10.10, 1:null|null:20.20, 2:null|null:30.30"
                    )
                }

            // 4 entries for RechnungsPositionDO 703900: 7027000,7033000,7060000,7069000
            entries.filter { it.historyEntryId == HistoryServiceOldFormatTest.getNewHistoryEntryId(7033000L) }
                .let { list ->
                    Assertions.assertEquals(1, list.size)
                    assertHistoryEntry(list[0], RechnungsPositionDO::class.java, "vat", "0.19000", "0.19")
                }
            // 2 entries for KostZuweisungDO: 704200
            entries.filter { it.historyEntryId == HistoryServiceOldFormatTest.getNewHistoryEntryId(7039000L) }
                .let { list ->
                    Assertions.assertEquals(1, list.size)
                    // Kost2DO#701200 and Kost2DO#703000 aren't in the database.
                    assertHistoryEntry(
                        list[0],
                        KostZuweisungDO::class.java,
                        "kost2",
                        "Kost2DO#701200",
                        "Kost2DO#703000"
                    )
                }
            // 2 entries for KostZuweisungDO: 704500
            // 2 entries for KostZuweisungDO: 704800
            // 2 entries for KostZuweisungDO: 705100

            // 22 in total
            Assertions.assertEquals(22, entries.size)
        }
    }

    private fun assertHistoryEntry(
        entry: FlatDisplayHistoryEntry,
        clazz: Class<*>?,
        propertyName: String,
        oldValue: String?,
        newValue: String?,
    ) {
        clazz?.let { cls ->
            Assertions.assertEquals(
                translatePropertyName(cls, propertyName),
                entry.displayPropertyName?.substringAfterLast(':'), // "1:kostZuweisungen" -> "kostZuweisungen"
                "$cls.$propertyName"
            )
        }
        Assertions.assertEquals(propertyName, entry.propertyName, "$clazz.$propertyName")
        Assertions.assertEquals(EntityOpType.Update, entry.opType, "$clazz.$propertyName")
        Assertions.assertEquals(oldValue, entry.oldValue, "$clazz.$propertyName")
        Assertions.assertEquals(newValue, entry.newValue, "$clazz.$propertyName")
    }
}
