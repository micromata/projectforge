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


package org.projectforge.business.lanesandplanes

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.test.AbstractTestBase

/** With the test context, because the effective status of a Kost2 is read through PfCaches. */
class LanesAndPlanesSettingsTest : AbstractTestBase() {
    @Test
    fun parse() {
        val defaults = LanesAndPlanesSettings.parse(null)
        Assertions.assertTrue(defaults.accountingInvoiceProfileIds.isEmpty())
        Assertions.assertTrue(defaults.generalKost1.isEmpty())
        Assertions.assertTrue(LanesAndPlanesSettings.parse("no json").generalKost2.isEmpty())
        val settings = LanesAndPlanesSettings.parse(
            """{"accountingInvoiceProfileIds":[12,null],"generalKost1":["1.005.01.00"],"generalKost2":["1.010.01.00"," "],""" +
                    """"kost2Patterns":["5.*.02"],"costObjectField":"COST_UNITS","unknown":1,""" +
                    """"additionalUsers":[{"email":"a@example.org","firstName":"A","lastName":"B"},{"email":" "}]}"""
        )
        Assertions.assertEquals(listOf(12L), settings.invoiceProfileIds)
        Assertions.assertEquals(listOf("1.010.01.00"), settings.generalKost2Numbers)
        Assertions.assertEquals(listOf("1.005.01.00"), settings.generalKost1Numbers, "Old JSON with costObjectField still parses.")
        Assertions.assertEquals(1, settings.kost2Regexes.size)
        Assertions.assertEquals(listOf("a@example.org"), settings.additionalUserList.map { it.email }, "Empty rows are ignored.")
    }

    @Test
    fun validate() {
        val active = kost2(1)
        val closed = kost2(2, KostentraegerStatus.ENDED)
        val kost2Map = mapOf("1.010.01.00" to active, "1.010.02.00" to closed, "1010.01.00" to active)
        val find: (String) -> Kost2DO? = { kost2Map[it] }
        val kost1Map = mapOf("1.005.01.00" to kost1(1), "1.005.02.00" to kost1(2, KostentraegerStatus.ENDED))
        val findKost1: (String) -> Kost1DO? = { kost1Map[it] }
        Assertions.assertEquals(
            listOf("accountingInvoiceProfileIds:lanesAndPlanes.config.error.invoiceProfileRequired:null:null"),
            LanesAndPlanesSettings().validate(findKost1, find).map(::format),
        )
        val settings = LanesAndPlanesSettings(
            accountingInvoiceProfileIds = listOf(12L, null, 0L),
            generalKost1 = listOf("1.005.01.00", "1.005.02.00", "1.000.00.00", "1.005.01.00"),
            generalKost2 = listOf("1.010.01.00", "", "9.999.99.99", "1.010.02.00", "1010.01.00"),
            kost2Patterns = listOf("5.*.02", "", "5.[0-9]"),
            additionalUsers = listOf(
                LanesAndPlanesSettings.AdditionalUser("a@example.org", "A", "B"),
                LanesAndPlanesSettings.AdditionalUser(),
                LanesAndPlanesSettings.AdditionalUser("no email", "A", "B"),
                LanesAndPlanesSettings.AdditionalUser("A@example.org", "A", " "),
            ),
        )
        Assertions.assertEquals(
            listOf(
                "accountingInvoiceProfileIds:lanesAndPlanes.config.error.invoiceProfileId:2:0",
                "generalKost1:lanesAndPlanes.config.error.kost1NotActive:1:1.005.02.00",
                "generalKost1:lanesAndPlanes.config.error.kost1NotFound:2:1.000.00.00",
                "generalKost1:lanesAndPlanes.config.error.kost1Duplicate:3:1.005.01.00",
                "generalKost2:lanesAndPlanes.config.error.kost2NotFound:2:9.999.99.99",
                "generalKost2:lanesAndPlanes.config.error.kost2NotActive:3:1.010.02.00",
                "generalKost2:lanesAndPlanes.config.error.kost2Duplicate:4:1010.01.00",
                "kost2Patterns:lanesAndPlanes.config.error.pattern:2:5.[0-9]",
                "additionalUsers:lanesAndPlanes.config.error.additionalUserEmail:2:no email",
                "additionalUsers:lanesAndPlanes.config.error.additionalUserDuplicate:3:A@example.org",
                "additionalUsers:lanesAndPlanes.config.error.additionalUserName:3:null",
            ),
            settings.validate(findKost1, find).map(::format),
        )
    }

    @Test
    fun globToRegex() {
        val regex = LanesAndPlanesSettings.globToRegex("5.*.02")
        Assertions.assertTrue(regex.matches("5.123.45.02"))
        Assertions.assertFalse(regex.matches("5.123.45.03"))
        Assertions.assertFalse(regex.matches("15.123.45.02"))
        Assertions.assertFalse(regex.matches("5.123.45.021"))
        Assertions.assertTrue(LanesAndPlanesSettings.globToRegex("5.123.??.*").matches("5.123.45.11"))
        Assertions.assertFalse(LanesAndPlanesSettings.globToRegex("5.123.??.*").matches("5.123.4.11"))
        Assertions.assertFalse(LanesAndPlanesSettings.globToRegex("5.1").matches("531"), "The dot is no wildcard.")
    }

    private fun format(error: LanesAndPlanesSettings.Error) =
        "${error.field}:${error.i18nKey}:${error.index}:${error.param}"

    private fun kost1(id: Long, status: KostentraegerStatus? = null) = Kost1DO().also {
        it.id = id
        it.kostentraegerStatus = status
    }

    private fun kost2(id: Long, status: KostentraegerStatus? = null) = Kost2DO().also {
        it.id = id
        it.kostentraegerStatus = status
    }
}
