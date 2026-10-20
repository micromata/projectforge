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

package org.projectforge.framework.configuration.properties

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.util.Properties

class PropertiesFileParserTest {
    @Test
    fun `parses entries like java util Properties`() {
        val content = """
            # A comment
            ! Another comment
            a.b=value
            c.d = spaced value
            e.f:colon
            g.h space separated
            empty=
            noValue
            continued=first \
                second \
                third
            escaped\=key=€ and \\ backslash\n
            #commented.out=example
            # Not a key: some text
        """.trimIndent()
        val entries = PropertiesFileParser.parse(content).filterIsInstance<PropertiesFileLine.Entry>()
        val active = entries.filter { !it.commentedOut }.associate { it.key to it.value }
        val properties = Properties().apply { load(StringReader(content)) }
        assertEquals(properties.stringPropertyNames(), active.keys)
        properties.stringPropertyNames().forEach { assertEquals(properties.getProperty(it), active[it], it) }
        assertEquals(3, entries.single { it.key == "continued" }.rawLines.size)
        val commented = entries.single { it.commentedOut }
        assertEquals("commented.out", commented.key)
        assertEquals("example", commented.value)
    }

    @Test
    fun `comments and group titles`() {
        val lines = PropertiesFileParser.parse("### General\n#key = with blank before equals\n# text\n\n")
        assertEquals("General", (lines[0] as PropertiesFileLine.Comment).groupTitle)
        assertTrue((lines[1] as PropertiesFileLine.Entry).commentedOut)
        assertEquals(null, (lines[2] as PropertiesFileLine.Comment).groupTitle)
        assertEquals("text", (lines[2] as PropertiesFileLine.Comment).text)
        assertTrue(lines[3] is PropertiesFileLine.Blank)
        assertEquals(4, lines.size)
    }

    @Test
    fun `escaping round trip`() {
        listOf("", " leading space", "#hash", "äöü € \t tab\nnewline", "a=b:c", "\\backslash", "\${placeholder}")
            .forEach { value ->
                val line = PropertiesFileParser.formatEntry("my.key", value)
                assertFalse(line.any { it.code > 0x7e }, line)
                val properties = Properties().apply { load(StringReader(line)) }
                assertEquals(value, properties.getProperty("my.key"), line)
            }
        val properties = Properties().apply { load(StringReader(PropertiesFileParser.formatEntry("a b=c", "x"))) }
        assertEquals("x", properties.getProperty("a b=c"))
    }
}
