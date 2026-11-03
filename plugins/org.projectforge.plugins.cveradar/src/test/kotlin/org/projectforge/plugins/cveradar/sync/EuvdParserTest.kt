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


package org.projectforge.plugins.cveradar.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

/** The answers of the EUVD: the dates in the locale format, aliases, the rating (0.0 is not rated), the products. */
class EuvdParserTest {
    @Test
    fun `an entry with its CVE id, the rating of CVSS 4, the EPSS and the products`() {
        val item = EuvdParser.parsePage(page(item())).items.single()
        assertEquals("EUVD-2026-116039", item.id)
        assertEquals("CVE-2026-121604", item.cveId)
        assertEquals("CVE-2026-121604", item.key)
        assertEquals(listOf("GHSA-aaaa-bbbb-cccc"), item.aliases)
        assertEquals(Instant.parse("2026-11-03T16:26:55Z"), item.published, "UTC")
        assertEquals(Instant.parse("2026-11-04T09:01:02Z"), item.updated, "12 hour clock with AM")
        assertEquals(BigDecimal("4.8"), item.score)
        assertTrue(item.cvss4)
        assertEquals("CVSS:4.0/AV:L/AC:L/AT:N/PR:L/UI:N/VC:L/VI:N/VA:N/SC:N/SI:N/SA:N", item.vector)
        assertEquals(BigDecimal("0.00430"), item.epss, "the EUVD gives percent")
        assertEquals(BigDecimal("0.69540"), EuvdParser.parsePage(page(item(epss = "69.54"))).items.single().epss)
        assertNull(EuvdParser.parsePage(page(item(epss = "250"))).items.single().epss, "more than 100 percent is no probability")
        assertEquals(Instant.parse("2026-11-02T10:00:00Z"), item.exploitedSince)
        assertEquals(listOf(EuvdProduct("wlroots", "wlroots", "0 <0.20.2")), item.products, "doubles dropped")
    }

    @Test
    fun `an entry without CVE id has the EUVD id as key, a score of 0 is not rated, CVSS 3 is not 4`() {
        val item = EuvdParser.parsePage(
            page(item(aliases = "GHSA-aaaa-bbbb-cccc\\n", score = "0.0", version = null, vector = null)),
        ).items.single()
        assertNull(item.cveId)
        assertEquals("EUVD-2026-116039", item.key)
        assertEquals(listOf("GHSA-aaaa-bbbb-cccc"), item.aliases)
        assertNull(item.score)
        assertNull(item.vector)
        assertFalse(item.cvss4)

        val v3 = EuvdParser.parsePage(page(item(score = "7.5", version = "3.1", vector = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"))).items.single()
        assertEquals(BigDecimal("7.5"), v3.score)
        assertFalse(v3.cvss4)
    }

    @Test
    fun `untrusted input is dropped or ignored`() {
        val page = EuvdParser.parsePage(
            """{"total":3,"items":[{"id":"nonsense","aliases":"CVE-2026-1"},{"description":"no id"},
               ${item(date = "yesterday", exploitedSince = null)}]}""",
        )
        assertEquals(3, page.total)
        val item = page.items.single()
        assertNull(item.published)
        assertNull(item.exploitedSince)
        assertEquals(1, EuvdParser.parsePage("[${item()}]").items.size, "the other endpoints answer a plain array")
        assertEquals(Instant.parse("2026-11-03T16:28:35Z"), EuvdParser.parseTimestamp("Nov 3, 2026, 4:28:35\u202fPM"), "narrow no-break space")
    }

    companion object {
        internal fun page(vararg items: String) = """{"items":[${items.joinToString(",")}],"total":${items.size}}"""

        internal fun item(
            id: String = "EUVD-2026-116039",
            aliases: String = "CVE-2026-121604\\nGHSA-aaaa-bbbb-cccc\\n",
            score: String = "4.8",
            version: String? = "4.0",
            vector: String? = "CVSS:4.0/AV:L/AC:L/AT:N/PR:L/UI:N/VC:L/VI:N/VA:N/SC:N/SI:N/SA:N",
            date: String = "Nov 3, 2026, 4:26:55 PM",
            exploitedSince: String? = "Nov 2, 2026, 10:00:00 AM",
            epss: String = "0.43",
            product: String = "wlroots",
        ): String = """
            {"id":"$id","enisaUuid":"c76c2489","description":"An out-of-bounds read.","datePublished":"$date",
             "dateUpdated":"Nov 4, 2026, 9:01:02 AM","baseScore":$score,
             ${version?.let { """"baseScoreVersion":"$it",""" } ?: ""}
             ${vector?.let { """"baseScoreVector":"$it",""" } ?: ""}
             "references":"https://example.org/a\n","aliases":"$aliases","assigner":"VulnCheck","epss":$epss,
             ${exploitedSince?.let { """"exploitedSince":"$it",""" } ?: ""}
             "enisaIdProduct":[
               {"id":"1","product":{"name":"$product","vendor":{"name":"wlroots"}},"product_version":"0 <0.20.2"},
               {"id":"2","product":{"name":"${product.uppercase()}","vendor":{"name":"wlroots"}},"product_version":"1"}],
             "enisaIdVendor":[]}"""
    }
}
