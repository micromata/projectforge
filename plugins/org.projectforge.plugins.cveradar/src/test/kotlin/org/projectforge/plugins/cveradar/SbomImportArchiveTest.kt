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


package org.projectforge.plugins.cveradar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.projectforge.common.i18n.UserException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** The zip of a bulk import: SBOMs read, everything else counted and skipped; a zip bomb is refused (SEC-05). */
class SbomImportArchiveTest {
    @TempDir
    lateinit var dir: Path

    private fun zip(vararg entries: Pair<String, ByteArray>): Path {
        val file = dir.resolve("import.zip")
        ZipOutputStream(Files.newOutputStream(file)).use { out ->
            entries.forEach { (name, bytes) ->
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }
        return file
    }

    private val sbom = """{"bomFormat":"CycloneDX","components":[{"purl":"pkg:npm/lodash@4.17.21"}]}""".toByteArray()

    @Test
    fun `SBOMs are read, logs, raw reports, broken JSON and dot files are skipped`() {
        val result = SbomImportArchive.read(
            zip(
                "all/repositories/a/bom.cdx.json" to sbom,
                "all/repositories/a/scan/scan.log" to "log".toByteArray(),
                "all/repositories/a/scan/syft.json" to """{"artifacts":[]}""".toByteArray(),
                "all/images/x/syft.cdx.json" to ByteArray(0),
                "all/.secrets/bom.json" to sbom,
                "__MACOSX/all/bom.json" to sbom,
            ),
        )
        assertEquals(listOf("all/repositories/a/bom.cdx.json"), result.documents.map { it.path })
        assertEquals(3, result.otherFiles)
        assertEquals(1, result.notSbom)
        assertEquals(1, result.invalid)
    }

    @Test
    fun `a file unpacking to far more than its packed size is refused`() {
        // Spaces pack to almost nothing: far more than all files of the zip may unpack to together.
        val bomb = """{"bomFormat":"CycloneDX","x":"${" ".repeat(200 * 1024)}"}""".toByteArray()
        val ex = assertThrows<UserException> { SbomImportArchive.read(zip("bomb.json" to bomb)) }
        assertEquals(SbomImportArchive.ERROR_RATIO, ex.i18nKey)
    }

    @Test
    fun `not a zip`() {
        val file = dir.resolve("no.zip").also { Files.writeString(it, "plain text") }
        val ex = assertThrows<UserException> { SbomImportArchive.read(file) }
        assertEquals(SbomImportArchive.ERROR_INVALID, ex.i18nKey)
    }
}
