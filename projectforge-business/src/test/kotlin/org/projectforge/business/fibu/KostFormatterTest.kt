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
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class KostFormatterTest : AbstractTestBase() {
    @Autowired
    private lateinit var kostFormatter: KostFormatter

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Test
    fun testFormatting() {
        val kunde = KundeDO()
        kunde.nummer = 204
        kunde.name = "ACME very long name of the company with a lot of text to test the formatter"
        kundeDao.insert(kunde, checkAccess = false)
        Assertions.assertEquals("5.204", kostFormatter.formatKunde(kunde))
        Assertions.assertEquals("5.204", kostFormatter.formatKunde(kunde, KostFormatter.FormatType.NUMBER))
        Assertions.assertEquals("5.204", kostFormatter.formatKunde(kunde, KostFormatter.FormatType.FORMATTED_NUMBER))
        Assertions.assertEquals(
            "5.204: ACME very long name ...",
            kostFormatter.formatKunde(kunde, KostFormatter.FormatType.TEXT, 30)
        )
        Assertions.assertEquals(
            "5.204: ACME very long name of the company with a lot of text to test the formatter",
            kostFormatter.formatKunde(kunde, KostFormatter.FormatType.LONG)
        )

        Assertions.assertEquals("?.???.??", kostFormatter.formatProjekt(null))
        Assertions.assertEquals("??????", kostFormatter.formatProjekt(null, KostFormatter.FormatType.NUMBER))

        val projekt = ProjektDO()
        projekt.nummer = 42
        projekt.internKost2_4 = 17
        projekt.name = "Project name very long name of the project with a lot of text to test the formatter"
        Assertions.assertEquals("4.017.42", kostFormatter.formatProjekt(projekt))
        Assertions.assertEquals("401742", kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.NUMBER))
        Assertions.assertEquals(
            "4.017.42",
            kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.FORMATTED_NUMBER)
        )
        Assertions.assertEquals(
            "4.017.42: Project name very...",
            kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.TEXT, 30)
        )
        Assertions.assertEquals(
            "4.017.42: Project name very long name of the project with a lot of text to test the formatter",
            kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.LONG)
        )
        projekt.kunde = kunde
        Assertions.assertEquals("5.204.42", kostFormatter.formatProjekt(projekt))
        Assertions.assertEquals("520442", kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.NUMBER))
        Assertions.assertEquals(
            "5.204.42",
            kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.FORMATTED_NUMBER)
        )
        Assertions.assertEquals(
            "5.204.42: Project name very...",
            kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.TEXT, 30)
        )
        Assertions.assertEquals(
            "5.204.42: Project name very long name of the project with a lot of text to test the formatter",
            kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.LONG)
        )

        val kost1 = Kost1DO()
        kostFormatter.formatKost1(kost1)
        Assertions.assertEquals("0.000.00.00", kostFormatter.formatKost1(kost1))
        Assertions.assertEquals("00000000", kostFormatter.formatKost1(kost1, KostFormatter.FormatType.NUMBER))
        kost1.description = "Kost1 description very long name of the kost1 with a lot of text to test the formatter"
        Assertions.assertEquals(
            "0.000.00.00: Kost1 descript...",
            kostFormatter.formatKost1(kost1, KostFormatter.FormatType.TEXT, 30)
        )
        Assertions.assertEquals(
            "0.000.00.00: Kost1 description very long name of the kost1 with a lot of text to test the formatter",
            kostFormatter.formatKost1(kost1, KostFormatter.FormatType.LONG)
        )
        kost1.nummernkreis = 3
        Assertions.assertEquals(
            "3.000.00.00: Kost1 descript...",
            kostFormatter.formatKost1(kost1, KostFormatter.FormatType.TEXT, 30)
        )
        kost1.bereich = 17
        Assertions.assertEquals(
            "3.017.00.00: Kost1 descript...",
            kostFormatter.formatKost1(kost1, KostFormatter.FormatType.TEXT, 30)
        )
        kost1.teilbereich = 42
        Assertions.assertEquals(
            "3.017.42.00: Kost1 descript...",
            kostFormatter.formatKost1(kost1, KostFormatter.FormatType.TEXT, 30)
        )
        kost1.endziffer = 2
        Assertions.assertEquals(
            "3.017.42.02: Kost1 descript...",
            kostFormatter.formatKost1(kost1, KostFormatter.FormatType.TEXT, 30)
        )

        val kost2 = Kost2DO()
        Assertions.assertEquals("0.000.00.--", kostFormatter.formatKost2(kost2))
        Assertions.assertEquals("000000--", kostFormatter.formatKost2(kost2, KostFormatter.FormatType.NUMBER))
        kost2.description = "Kost2 description very long name of the kost2 with a lot of text to test the formatter"
        Assertions.assertEquals(
            "0.000.00.--: Kost2 descript...",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.TEXT, 30)
        )
        Assertions.assertEquals(
            "0.000.00.--: Kost2 description very long name of the kost2 with a lot of text to test the formatter",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.LONG)
        )
        kost2.nummernkreis = 3
        Assertions.assertEquals(
            "3.000.00.--: Kost2 descript...",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.TEXT, 30)
        )
        kost2.bereich = 17
        Assertions.assertEquals(
            "3.017.00.--: Kost2 descript...",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.TEXT, 30)
        )
        kost2.teilbereich = 42
        Assertions.assertEquals(
            "3.017.42.--: Kost2 descript...",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.TEXT, 30)
        )
        Kost2ArtDO().let { kost2Art ->
            kost2Art.id = 2
            kost2.kost2Art = kost2Art
        }
        Assertions.assertEquals(
            "3.017.42.02: Kost2 descript...",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.TEXT, 30)
        )

        kost2.nummernkreis = 5
        kost2.projekt = projekt
        kost2.kost2Art = null
        Assertions.assertEquals("5.017.42.--", kostFormatter.formatKost2(kost2))
        Assertions.assertEquals("501742--", kostFormatter.formatKost2(kost2, KostFormatter.FormatType.NUMBER))
        kost2.description = "Kost2 description very long name of the kost2 with a lot of text to test the formatter"
        Assertions.assertEquals(
            "5.017.42.--: Project name v...",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.TEXT, 30)
        )
        Assertions.assertEquals(
            "5.017.42.--: Project name very long name of the project with a lot of text to test the formatter",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.LONG)
        )
        Kost2ArtDO().let { kost2Art ->
            kost2Art.id = 2
            kost2.kost2Art = kost2Art
        }
        Assertions.assertEquals(
            "5.017.42.02: Project name v...",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.TEXT, 30)
        )
        kost2.kost2Art?.name = "travel"
        Assertions.assertEquals(
            "5.017.42.02: travel - Proje...",
            kostFormatter.formatKost2(kost2, KostFormatter.FormatType.TEXT, 30)
        )
    }
}
