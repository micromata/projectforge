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

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.LocalDateTime

class ProjectForgePropertiesFileTest {
    @TempDir
    lateinit var dir: File

    private val metadata = PropertiesMetadata.instance

    private val writer = ProjectForgePropertiesWriter(metadata)

    @Test
    fun `sorts by groups and keeps own comments above their keys`() {
        val content = """
            # My header
            # second line

            # Own comment of the db password
            spring.datasource.password=secret
            my.own.key=own value
            # Domain comment
            projectforge.domain=https://pf.acme.com
            projectforge.einvoice.seller.bankAccounts[3].iban=DE02
            # Trailing comment
        """.trimIndent() + "\n"
        val result = writer.write(content, emptyList())
        val lines = result.lines()
        assertEquals("# My header", lines[0])
        assertEquals("# second line", lines[1])
        val domain = lines.indexOf("projectforge.domain=https://pf.acme.com")
        assertEquals("# Domain comment", lines[domain - 1])
        val password = lines.indexOf("spring.datasource.password=secret")
        assertEquals("# Own comment of the db password", lines[password - 1])
        assertTrue(lines.indexOf("### General") < domain)
        assertTrue(domain < lines.indexOf("### Database"))
        assertTrue(lines.indexOf("### Database") < password)
        val iban = lines.indexOf("projectforge.einvoice.seller.bankAccounts[3].iban=DE02")
        assertTrue(lines.indexOf("### E-invoice") < iban)
        assertTrue(lines.indexOf("### Other") < lines.indexOf("my.own.key=own value"))
        assertEquals("# Trailing comment", lines.last { it.isNotEmpty() })
        // Writing again is stable.
        assertEquals(result, writer.write(result, emptyList()))
        assertEquals(ProjectForgePropertiesFile.load(content.toByteArray()), ProjectForgePropertiesFile.load(result.toByteArray()))
    }

    @Test
    fun `detached comment paragraphs with unknown keys are sorted by their keys`() {
        val content = """
            projectforge.domain=https://pf
            # Old mail settings
            #mail.smtp.user=no-reply
            #mail.smtp.ssl.enable=true

            # Week settings
            projectforge.defaultFirstDayOfWeek=MONDAY
            # Debugging
            #logging.level.org.projectforge.idp=DEBUG

            spring.datasource.username=pf
        """.trimIndent()
        val lines = writer.write(content, listOf(PropertyChange("mail.smtp.user", "set"))).lines()
        assertEquals("# Week settings", lines[lines.indexOf("projectforge.defaultFirstDayOfWeek=MONDAY") - 1])
        val mail = lines.indexOf("#mail.smtp.user=no-reply")
        assertTrue(lines.indexOf("### Other") < mail, "Unrelated paragraph goes to Other.")
        assertTrue(lines.contains("mail.smtp.user=set"), "A comment paragraph is never activated.")
        val logging = lines.indexOf("#logging.level.org.projectforge.idp=DEBUG")
        assertTrue(lines.indexOf("### Logging & error monitoring") < logging)
        assertTrue(logging < lines.indexOf("### Other"))
    }

    @Test
    fun `set, activate commented-out, add and remove`() {
        val content = """
            projectforge.domain=https://old
            # Plugins for the test system
            #projectforge.plugins.active=all
            spring.datasource.password=secret
            projectforge.support.mail=support@acme.com
        """.trimIndent()
        val result = writer.write(
            content,
            listOf(
                PropertyChange("projectforge.domain", "https://new äöü €"),
                PropertyChange("projectforge.plugins.active", "datatransfer"),
                PropertyChange("new.key", "value"),
                PropertyChange("spring.datasource.password", remove = true),
                PropertyChange("projectforge.support.mail", remove = true),
            )
        )
        val values = ProjectForgePropertiesFile.load(result.toByteArray(Charsets.ISO_8859_1))
        assertEquals("https://new äöü €", values["projectforge.domain"])
        assertEquals("datatransfer", values["projectforge.plugins.active"])
        assertEquals("value", values["new.key"])
        assertFalse(values.containsKey("spring.datasource.password"))
        assertFalse(values.containsKey("projectforge.support.mail"))
        assertFalse(result.contains("secret"), "A removed secret must not stay as comment.")
        assertTrue(result.contains("#projectforge.support.mail=support@acme.com"))
        val lines = result.lines()
        assertEquals("# Plugins for the test system", lines[lines.indexOf("projectforge.plugins.active=datatransfer") - 1])
        assertFalse(result.any { it.code > 0x7e })
    }

    @Test
    fun `the initial template survives a write`() {
        val initial = String(javaClass.getResourceAsStream("/initialBaseDirFiles/initialProjectForge.properties")!!.readBytes(), Charsets.ISO_8859_1)
        val result = writer.write(initial, emptyList())
        assertEquals(ProjectForgePropertiesFile.load(initial.toByteArray(Charsets.ISO_8859_1)), ProjectForgePropertiesFile.load(result.toByteArray(Charsets.ISO_8859_1)))
        // Every comment line is kept.
        val comments = { s: String -> s.lines().filter { it.startsWith("#") && !it.startsWith("### ") }.sorted() }
        assertEquals(comments(initial), comments(result))
    }

    @Test
    fun `save writes a backup first and detects conflicts`() {
        val file = File(dir, ProjectForgePropertiesFile.FILENAME)
        val original = "# Comment\nprojectforge.domain=https://old\n"
        file.writeText(original, Charsets.ISO_8859_1)
        val pfFile = ProjectForgePropertiesFile(file, metadata) { LocalDateTime.of(2026, 10, 20, 9, 30, 15) }
        val version = pfFile.read().version
        val backup = pfFile.save(listOf(PropertyChange("projectforge.domain", "https://new")), version)
        assertNotNull(backup)
        assertEquals("projectforge.properties.2026-10-20_09-30-15.bak", backup!!.name)
        assertArrayEquals(original.toByteArray(), backup.readBytes())
        assertEquals("https://new", pfFile.read().values["projectforge.domain"])

        assertThrows<ProjectForgePropertiesFile.ConflictException> {
            pfFile.save(listOf(PropertyChange("projectforge.domain", "https://other")), version)
        }
        assertEquals("https://new", pfFile.read().values["projectforge.domain"])

        val second = pfFile.save(listOf(PropertyChange("projectforge.domain", "https://third")), pfFile.read().version)
        assertEquals("projectforge.properties.2026-10-20_09-30-15-2.bak", second!!.name)
        assertEquals(2, dir.listFiles()!!.count { it.name.endsWith(".bak") })
    }
}
