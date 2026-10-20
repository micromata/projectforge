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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.LocalDateTime

class PropertiesDiffTextTest {
    @TempDir
    lateinit var dir: File

    private val metadata = PropertiesMetadata.instance

    private val content = """
        # My header

        # logging
        #logging.level.org.projectforge.web.rest=DEBUG
        server.port=8080
        # Own comment of the db password
        spring.datasource.password=secret
        my.own.name = own value
        #spring.servlet.multipart.max-file-size=5120MB
        spring.servlet.multipart.max-file-size=10GB

        # Domain comment
        projectforge.domain=https://pf.acme.com
        projectforge.invoicing-quota.info.de=Die Fakturaquote gibt dir einen Überblick.
        # Trailing comment
    """.trimIndent() + "\n"

    @Test
    fun `the order of the parameters and the editor's group titles are ignored`() {
        val sorted = ProjectForgePropertiesWriter(metadata).write(content, emptyList())
        assertNotEquals(content, sorted)
        assertTrue(sorted.contains("### "))
        val text = PropertiesDiffText.build(content, metadata, ignoreOrder = true)
        assertEquals(text, PropertiesDiffText.build(sorted, metadata, ignoreOrder = true))
        val lines = text.lines()
        assertEquals("# My header", lines[0])
        assertEquals("# Trailing comment", lines.last { it.isNotEmpty() })
        assertFalse(lines.any { it.startsWith("### ") })
        // Each comment stays above its parameter.
        assertEquals("# Domain comment", lines[lines.indexOf("projectforge.domain=https://pf.acme.com") - 1])
        assertEquals("# logging", lines[lines.indexOf("#logging.level.org.projectforge.web.rest=DEBUG") - 1])
        // Uniform format: no blanks around '=', non-ASCII escaped as \uXXXX.
        assertTrue(lines.contains("my.own.name=own value"))
        assertTrue(lines.contains("projectforge.invoicing-quota.info.de=Die Fakturaquote gibt dir einen \\u00dcberblick."))
        // The commented-out line of a key before its active one.
        assertEquals(
            lines.indexOf("#spring.servlet.multipart.max-file-size=5120MB") + 1,
            lines.indexOf("spring.servlet.multipart.max-file-size=10GB"),
        )
    }

    @Test
    fun `changed values and comments remain`() {
        val changed = ProjectForgePropertiesWriter(metadata).write(content, listOf(PropertyChange("server.port", "8090")))
            .replace("# Domain comment", "# Domain comment, changed")
        val left = PropertiesDiffText.build(content, metadata, ignoreOrder = true).lines()
        val right = PropertiesDiffText.build(changed, metadata, ignoreOrder = true).lines()
        assertEquals(setOf("server.port=8080", "# Domain comment"), (left - right.toSet()).toSet())
        assertEquals(setOf("server.port=8090", "# Domain comment, changed"), (right - left.toSet()).toSet())
    }

    @Test
    fun `values of secrets are masked, but a change of them is visible`() {
        listOf(true, false).forEach { ignoreOrder ->
            val text = PropertiesDiffText.build(content, metadata, ignoreOrder)
            assertFalse(text.contains("=secret"), "ignoreOrder=$ignoreOrder")
            val masked = text.lines().single { it.startsWith("spring.datasource.password=") }
            assertTrue(masked.startsWith("spring.datasource.password=******"))
            val changed = PropertiesDiffText.build(content.replace("=secret", "=other"), metadata, ignoreOrder)
            assertNotEquals(masked, changed.lines().single { it.startsWith("spring.datasource.password=") })
            assertEquals(text, PropertiesDiffText.build(content, metadata, ignoreOrder))
        }
    }

    @Test
    fun `without ignoring the order the lines stay as they are`() {
        val text = PropertiesDiffText.build(content, metadata, ignoreOrder = false)
        assertEquals(content.replace("=secret", "=" + PropertiesDiffText.maskSecret("secret")), text)
    }

    @Test
    fun `backups are listed newest first`() {
        val file = File(dir, ProjectForgePropertiesFile.FILENAME)
        file.writeText("server.port=8080\n")
        var now = LocalDateTime.of(2026, 10, 20, 9, 30, 15)
        val propertiesFile = ProjectForgePropertiesFile(file, metadata) { now }
        propertiesFile.save(listOf(PropertyChange("server.port", "8081")), propertiesFile.read().version)
        now = now.plusHours(1)
        propertiesFile.save(listOf(PropertyChange("server.port", "8082")), propertiesFile.read().version)
        File(dir, "other.properties.bak").writeText("x=y\n")
        val backups = propertiesFile.backups()
        assertEquals(
            listOf("projectforge.properties.2026-10-20_10-30-15.bak", "projectforge.properties.2026-10-20_09-30-15.bak"),
            backups.map { it.file.name },
        )
        assertEquals(now, backups[0].timestamp)
        assertTrue(backups[0].file.readText().contains("server.port=8081\n"))
    }

    @Test
    fun `backups of the same second are listed newest first, none is overwritten`() {
        val file = File(dir, ProjectForgePropertiesFile.FILENAME)
        file.writeText("server.port=8080\n")
        val now = LocalDateTime.of(2026, 10, 20, 9, 30, 15)
        val propertiesFile = ProjectForgePropertiesFile(file, metadata) { now }
        (8081..8092).forEach { port ->
            propertiesFile.save(listOf(PropertyChange("server.port", "$port")), propertiesFile.read().version)
        }
        val backups = propertiesFile.backups()
        assertEquals(12, backups.size)
        assertEquals("projectforge.properties.2026-10-20_09-30-15-12.bak", backups[0].file.name)
        assertEquals("projectforge.properties.2026-10-20_09-30-15-10.bak", backups[2].file.name)
        assertEquals("projectforge.properties.2026-10-20_09-30-15-9.bak", backups[3].file.name)
        assertEquals("projectforge.properties.2026-10-20_09-30-15.bak", backups.last().file.name)
        // Newest backup: the content before the last save.
        assertTrue(backups[0].file.readText().contains("server.port=8091\n"))
        assertTrue(backups.last().file.readText().contains("server.port=8080\n"))
    }
}
