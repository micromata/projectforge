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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDao.Companion.getNettoSumme
import org.projectforge.framework.time.PFDay.Companion.now
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class RechnungCacheTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun baseTest() {
        val today = now()
        lateinit var auftrag: AuftragDO
        persistenceService.runInTransaction {
            logon(getUser(TEST_FINANCE_USER))
            auftrag = createOrder()
            var auftragsPosition = createOrderPos()
            auftragsPosition.titel = "Pos 1"
            auftrag.addPosition(auftragsPosition)
            auftragsPosition = createOrderPos()
            auftragsPosition.titel = "Pos 2"
            auftrag.addPosition(auftragsPosition)
            auftrag.nummer = auftragDao.getNextNumber(auftrag)
            auftragDao.insert(auftrag)
        }
        lateinit var rechnung1: RechnungDO
        persistenceService.runInTransaction {
            rechnung1 = RechnungDO()
            var position = RechnungsPositionDO()
            position.auftragsPosition = auftrag.getPosition(1.toShort())
            position.einzelNetto = BigDecimal("100")
            position.text = "1.1"
            rechnung1.addPosition(position)
            position = RechnungsPositionDO()
            position.auftragsPosition = auftrag.getPosition(2.toShort())
            position.einzelNetto = BigDecimal("200")
            position.text = "1.2"
            rechnung1.addPosition(position)
            rechnung1.nummer = rechnungDao.getNextNumber(rechnung1)
            rechnung1.datum = today.localDate
            rechnung1.faelligkeit = LocalDate.now()
            rechnung1.projekt = initTestDB.addProjekt(null, 1, "foo")
        }
        rechnungDao.insert(rechnung1)
        lateinit var rechnung2: RechnungDO
        rechnung2 = RechnungDO().also {
            val position = RechnungsPositionDO()
            position.auftragsPosition = auftrag.getPosition(1.toShort())
            position.einzelNetto = BigDecimal("400")
            position.text = "2.1"
            it.addPosition(position)
            it.nummer = rechnungDao.getNextNumber(it)
            it.datum = today.localDate
            it.faelligkeit = LocalDate.now()
            it.projekt = initTestDB.addProjekt(null, 1, "foo")
        }
        rechnungDao.insert(rechnung2)
        var posInfos: Collection<RechnungPosInfo>? = rechnungCache.getRechnungsPosInfosByAuftragId(auftrag.id)
        Assertions.assertEquals(3, posInfos!!.size, "3 invoice positions expected.")
        // The positions are sorted by invoice number and position number.
        posInfos.elementAt(0).let {
            Assertions.assertEquals(0, BigDecimal("100").compareTo(it.netSum))
        }
        posInfos.elementAt(1).let {
            Assertions.assertEquals(0, BigDecimal("200").compareTo(it.netSum))
        }
        posInfos.elementAt(2).let {
            Assertions.assertEquals(0, BigDecimal("400").compareTo(it.netSum))
        }
        Assertions.assertEquals(0, BigDecimal("700").compareTo(getNettoSumme(posInfos)))

        posInfos = rechnungCache.getRechnungsPosInfosByAuftragsPositionId(auftrag.getPosition(1.toShort())!!.id)
        Assertions.assertEquals(2, posInfos!!.size, "2 invoice positions expected.")
        Assertions.assertEquals(0, BigDecimal("500").compareTo(getNettoSumme(posInfos)))

        posInfos = rechnungCache.getRechnungsPosInfosByAuftragsPositionId(auftrag.getPosition(2.toShort())!!.id)
        Assertions.assertEquals(1, posInfos!!.size, "1 invoice positions expected.")
        Assertions.assertEquals(0, BigDecimal("200").compareTo(getNettoSumme(posInfos)))

        persistenceService.runInTransaction { context ->
            val rechnung = rechnungDao.find(rechnung2.id)
            rechnung!!.positionen!![0].auftragsPosition = null
            rechnungDao.update(rechnung)
        }
        posInfos = rechnungCache.getRechnungsPosInfosByAuftragId(auftrag.id)
        Assertions.assertEquals(2, posInfos!!.size, "2 invoice positions expected.")
        Assertions.assertEquals(0, BigDecimal("300").compareTo(getNettoSumme(posInfos)))
    }

    private fun createOrder(): AuftragDO {
        return AuftragDO().also {
            it.status = AuftragsStatus.GELEGT
        }
    }

    private fun createOrderPos(): AuftragsPositionDO {
        return AuftragsPositionDO().also {
            it.status = AuftragsStatus.GELEGT
        }
    }
}
