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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDao.Companion.getNettoSumme
import org.projectforge.framework.time.PFDay.Companion.now
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class RechnungCacheTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun baseTest() {
        val today = now()
        lateinit var auftrag: AuftragDO
        persistenceService.runInTransaction { context ->
            logon(getUser(TEST_FINANCE_USER))
            auftrag = AuftragDO()
            var auftragsPosition = AuftragsPositionDO()
            auftragsPosition.titel = "Pos 1"
            auftrag.addPosition(auftragsPosition)
            auftragsPosition = AuftragsPositionDO()
            auftragsPosition.titel = "Pos 2"
            auftrag.addPosition(auftragsPosition)
            auftrag.nummer = auftragDao.getNextNumber(auftrag)
            auftragDao.insert(auftrag)
        }
        persistenceService.runInTransaction { context ->
            val rechnung1 = RechnungDO()
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
            rechnungDao.insert(rechnung1)
        }
        lateinit var rechnung2: RechnungDO
        persistenceService.runInTransaction { context ->
            rechnung2 = RechnungDO()
            val position = RechnungsPositionDO()
            position.auftragsPosition = auftrag.getPosition(1.toShort())
            position.einzelNetto = BigDecimal("400")
            position.text = "2.1"
            rechnung2.addPosition(position)
            rechnung2.nummer = rechnungDao.getNextNumber(rechnung2)
            rechnung2.datum = today.localDate
            rechnung2.faelligkeit = LocalDate.now()
            rechnung2.projekt = initTestDB.addProjekt(null, 1, "foo")
            rechnungDao.insert(rechnung2)
        }
        var set = rechnungDao.rechnungCache.getRechnungsPositionVOSetByAuftragId(auftrag.id)
        Assertions.assertEquals(3, set!!.size, "3 invoice positions expected.")
        val it = set.iterator()
        var posVO = it.next() // Positions are ordered.
        Assertions.assertEquals("1.1", posVO.text)
        posVO = it.next()
        Assertions.assertEquals("1.2", posVO.text)
        posVO = it.next()
        Assertions.assertEquals("2.1", posVO.text)
        Assertions.assertEquals(0, BigDecimal("700").compareTo(getNettoSumme(set)))

        set = rechnungDao.rechnungCache
            .getRechnungsPositionVOSetByAuftragsPositionId(auftrag.getPosition(1.toShort())!!.id)
        Assertions.assertEquals(2, set!!.size, "2 invoice positions expected.")
        Assertions.assertEquals(0, BigDecimal("500").compareTo(getNettoSumme(set)))

        set = rechnungDao.rechnungCache
            .getRechnungsPositionVOSetByAuftragsPositionId(auftrag.getPosition(2.toShort())!!.id)
        Assertions.assertEquals(1, set!!.size, "1 invoice positions expected.")
        Assertions.assertEquals(0, BigDecimal("200").compareTo(getNettoSumme(set)))

        persistenceService.runInTransaction { context ->
            val rechnung = rechnungDao.find(rechnung2.id)
            rechnung!!.positionen!![0].auftragsPosition = null
            rechnungDao.update(rechnung)
            set = rechnungDao.rechnungCache.getRechnungsPositionVOSetByAuftragId(auftrag.id)
            Assertions.assertEquals(2, set!!.size, "2 invoice positions expected.")
            Assertions.assertEquals(0, BigDecimal("300").compareTo(getNettoSumme(set)))
        }
    }
}
