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

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The vulnerable CPE entries of an NVD document (MAT-01): ranges, all versions, escapes, only what is vulnerable. */
class NvdCpeParserTest {
    private fun parse(configurations: String) =
        NvdCpeParser.parse(ObjectMapper().readTree("""{"configurations":[$configurations]}"""))

    @Test
    fun `a range keeps its bounds and a star or a dash means all versions`() {
        val entries = parse(
            """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
                {"vulnerable":true,"criteria":"cpe:2.3:a:Apache:Tomcat:*:*:*:*:*:*:*:*","versionStartIncluding":"9.0.0","versionEndExcluding":"9.0.90"},
                {"vulnerable":true,"criteria":"cpe:2.3:a:apache:tomcat:10.1.0:-:*:*:*:*:*:*"},
                {"vulnerable":true,"criteria":"cpe:2.3:a:vendor:thing:-:*:*:*:*:*:*:*"}]}]}""",
        )
        assertEquals(3, entries.size)
        assertEquals("apache", entries[0].vendor, "lower case")
        assertEquals("tomcat", entries[0].product)
        assertNull(entries[0].version)
        assertEquals("9.0.0", entries[0].startIncluding)
        assertEquals("9.0.90", entries[0].endExcluding)
        assertTrue(entries[0].hasRange)
        assertEquals("10.1.0", entries[1].version)
        assertNull(entries[2].version)
        assertTrue(!entries[2].hasRange)
    }

    @Test
    fun `entries that aren't vulnerable or aren't a CPE 2·3 are skipped, an AND node counts entry by entry, escapes are removed`() {
        val entries = parse(
            """{"operator":"AND","nodes":[
                {"operator":"OR","cpeMatch":[{"vulnerable":true,"criteria":"cpe:2.3:a:acme:app\\:pro:1.0:*:*:*:*:*:*:*"}]},
                {"operator":"OR","cpeMatch":[{"vulnerable":false,"criteria":"cpe:2.3:o:microsoft:windows:-:*:*:*:*:*:*:*"},
                                             {"vulnerable":true,"criteria":"cpe:2.2:a:old:format"},
                                             {"vulnerable":true,"criteria":"cpe:2.3:a:*:*:1.0:*:*:*:*:*:*:*"}]}]}""",
        )
        assertEquals(listOf("app:pro"), entries.map { it.product })
    }
}
