/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.*
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.HistoryTester
import org.projectforge.framework.persistence.history.EntityOpType
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class RechnungHistoryTest : AbstractTestBase() {
    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun `test history entries of invoices`() {
        val project = ProjektDO().also { project ->
            project.name = "Test Project for RechnungHistoryTest"
            project.internKost2_4 = 100
            project.nummer = 77
            projektDao.insert(project, checkAccess = false)
        }
        val kost1a = createKost1(1)
        val kost1b = createKost1(2)
        val kost2a = createKost2(project, createKost2Art(77))
        val kost2b = createKost2(project, createKost2Art(78))
        var invoice = RechnungDO().also { invoice ->
            invoice.nummer = rechnungDao.nextNumber
            invoice.kundeText = "Customer"
            invoice.periodOfPerformanceBegin = LocalDate.of(2024, Month.DECEMBER, 1)
            invoice.periodOfPerformanceEnd = LocalDate.of(2024, Month.DECEMBER, 31)
            invoice.datum = LocalDate.of(2024, Month.DECEMBER, 25)
            invoice.faelligkeit = LocalDate.of(2025, Month.JANUARY, 31)
            invoice.addPosition(RechnungsPositionDO().also { pos ->
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal.TEN
                pos.addKostZuweisung(KostZuweisungDO().also { assignment ->
                    assignment.kost1 = kost1a
                    assignment.kost2 = kost2a
                    assignment.netto = BigDecimal.ONE
                })
            })
        }
        val hist = createHistoryTester()
        rechnungDao.insert(invoice, checkAccess = false)
        hist.loadRecentHistoryEntries(1, 0, msg = "Only Insert entry expected.")
        invoice = rechnungDao.find(invoice.id, attached = true, checkAccess = false)!!
        invoice.addPosition(RechnungsPositionDO().also { pos ->
            pos.menge = BigDecimal("2")
            pos.einzelNetto = BigDecimal.TEN
        })
        hist.reset()
        rechnungDao.update(invoice, checkAccess = false)
        hist.loadRecentHistoryEntries(1, 0, msg = "Only Insert entry of RechnungsPosition expected.")
        HistoryTester.assertHistoryEntry(
            hist.recentEntries!![0],
            RechnungsPositionDO::class,
            opType = EntityOpType.Insert
        )

        invoice = rechnungDao.find(invoice.id, attached = true, checkAccess = false)!!
        invoice.positionen!![0].addKostZuweisung(KostZuweisungDO().also { assignment ->
            assignment.kost1 = kost1b
            assignment.kost2 = kost2b
            assignment.netto = BigDecimal.ONE
        })
        hist.reset()
        rechnungDao.update(invoice, checkAccess = false)
        hist.loadRecentHistoryEntries(1, 0, msg = "Only Insert entry of KostZuweisung expected.")
        HistoryTester.assertHistoryEntry(
            hist.recentEntries!![0],
            KostZuweisungDO::class,
            opType = EntityOpType.Insert
        )
    }

    private fun createKost1(endziffer: Int): Kost1DO {
        return Kost1DO().also { kost ->
            kost.nummernkreis = 4
            kost.bereich = 100
            kost.teilbereich = 1
            kost.endziffer = endziffer
            kost1Dao.insert(kost, checkAccess = false)
        }
    }

    private fun createKost2(project: ProjektDO, kost2Art: Kost2ArtDO): Kost2DO {
        return Kost2DO().also { kost ->
            kost.projekt = project
            kost.nummernkreis = project.nummernkreis
            kost.bereich = project.bereich!!
            kost.teilbereich = project.nummer
            kost.kost2Art = kost2Art
            kost2Dao.insert(kost, checkAccess = false)
        }
    }

    private fun createKost2Art(nummer: Long): Kost2ArtDO {
        return Kost2ArtDO().also { art ->
            art.id = nummer
            art.name = "Test Kost2Art $nummer"
            kost2ArtDao.insert(art, checkAccess = false)
        }
    }
}
