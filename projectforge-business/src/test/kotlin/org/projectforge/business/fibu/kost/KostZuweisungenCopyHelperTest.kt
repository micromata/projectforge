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

package org.projectforge.business.fibu.kost

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import java.math.BigDecimal

class KostZuweisungenCopyHelperTest : AbstractTestBase() {
    @Test
    fun copy() {
        val srcPos = RechnungsPositionDO()
        val destPos = RechnungsPositionDO()

        KostZuweisungenCopyHelper.copy(srcPos.kostZuweisungen, destPos)
        Assertions.assertEquals(0, destPos.kostZuweisungen!!.size)

        var kostZuweisung = KostZuweisungDO()
        kostZuweisung.netto = BigDecimal.ONE
        kostZuweisung.comment = "1"
        kostZuweisung.id = 4711L // simulate non deletable
        srcPos.addKostZuweisung(kostZuweisung)
        KostZuweisungenCopyHelper.copy(srcPos.kostZuweisungen, destPos)
        Assertions.assertEquals(1, destPos.kostZuweisungen!!.size)
        Assertions.assertEquals(srcPos.kostZuweisungen!![0], destPos.kostZuweisungen!![0])

        kostZuweisung = KostZuweisungDO()
        kostZuweisung.netto = BigDecimal.ONE
        kostZuweisung.comment = "1"

        destPos.addKostZuweisung(kostZuweisung)
        Assertions.assertEquals(2, destPos.kostZuweisungen!!.size)

        // srcPos "overwrites" dstPos
        KostZuweisungenCopyHelper.copy(srcPos.kostZuweisungen, destPos)
        Assertions.assertEquals(1, destPos.kostZuweisungen!!.size)
        Assertions.assertEquals(srcPos.kostZuweisungen!![0], destPos.kostZuweisungen!![0])

        srcPos.getKostZuweisung(0)!!.netto = BigDecimal.TEN
        srcPos.getKostZuweisung(0)!!.comment = "10"
        KostZuweisungenCopyHelper.copy(srcPos.kostZuweisungen, destPos)
        Assertions.assertEquals(1, destPos.kostZuweisungen!!.size)
        Assertions.assertEquals(BigDecimal.TEN, destPos.getKostZuweisung(0)!!.netto)
        Assertions.assertEquals("10", destPos.getKostZuweisung(0)!!.comment)

        kostZuweisung = KostZuweisungDO()
        kostZuweisung.netto = BigDecimal.ONE
        kostZuweisung.comment = "2"

        srcPos.addKostZuweisung(kostZuweisung)

        kostZuweisung = KostZuweisungDO()
        kostZuweisung.netto = BigDecimal.ONE
        kostZuweisung.comment = "3"

        srcPos.addKostZuweisung(kostZuweisung)

        kostZuweisung = KostZuweisungDO()
        kostZuweisung.netto = BigDecimal.ONE
        kostZuweisung.comment = "4"

        srcPos.addKostZuweisung(kostZuweisung)

        kostZuweisung = KostZuweisungDO()
        kostZuweisung.netto = BigDecimal.ONE
        kostZuweisung.comment = "5"

        srcPos.addKostZuweisung(kostZuweisung)
        KostZuweisungenCopyHelper.copy(srcPos.kostZuweisungen, destPos)
        Assertions.assertEquals(5, destPos.kostZuweisungen!!.size)
        Assertions.assertEquals(srcPos.kostZuweisungen!![0], destPos.kostZuweisungen!![0])
        Assertions.assertEquals(srcPos.kostZuweisungen!![1], destPos.kostZuweisungen!![1])
        Assertions.assertEquals(srcPos.kostZuweisungen!![2], destPos.kostZuweisungen!![2])
        Assertions.assertEquals(srcPos.kostZuweisungen!![3], destPos.kostZuweisungen!![3])
        Assertions.assertEquals(srcPos.kostZuweisungen!![4], destPos.kostZuweisungen!![4])

        srcPos.deleteKostZuweisung(3)
        srcPos.deleteKostZuweisung(2)
        srcPos.deleteKostZuweisung(1)
        suppressErrorLogs {
            srcPos.deleteKostZuweisung(0) // is not deletable, see above
        }
        KostZuweisungenCopyHelper.copy(srcPos.kostZuweisungen, destPos)
        Assertions.assertEquals(2, destPos.kostZuweisungen!!.size)
        Assertions.assertEquals(srcPos.kostZuweisungen!![0], destPos.kostZuweisungen!![0])
        Assertions.assertEquals(srcPos.kostZuweisungen!![1], destPos.kostZuweisungen!![1])
        Assertions.assertEquals(srcPos.kostZuweisungen!![0].netto, destPos.kostZuweisungen!![0].netto)
        Assertions.assertEquals(srcPos.kostZuweisungen!![1].netto, destPos.kostZuweisungen!![1].netto)
        Assertions.assertEquals(4, srcPos.kostZuweisungen!![1].index.toInt())
    }
}
