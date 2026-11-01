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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** The zips of an upload (INV-26): which files are read, how they are named, the limits against zip bombs. */
class LibraryArchiveTest {
    @Test
    fun `the lists of a zip are read with their path, files that may hold tokens and nested zips are skipped unread`() {
        val zip = zip(
            "frontend/pnpm-lock.yaml" to "lockfileVersion: '9.0'\n",
            "/os/packages.txt" to "\uFEFFopenssl 3.0.11-1~deb12u2\n",
            "bom.JSON" to "{}",
            "frontend/.npmrc" to "//registry.example.org/:_authToken=secret\n",
            ".config/deps.txt" to "x",
            "backend/gradle.properties" to "token=secret",
            "settings.xml" to "<settings/>",
            "__MACOSX/frontend/._pnpm-lock.yaml" to "x",
            "inner.zip" to "PK",
            "frontend/" to null,
        )
        val files = LibraryArchive.expand(listOf(Triple("plain.txt", "a 1", null), Triple("libs.zip", null, zip)))
        assertEquals(
            listOf(
                "plain.txt" to "a 1",
                "libs.zip/frontend/pnpm-lock.yaml" to "lockfileVersion: '9.0'\n",
                "libs.zip/os/packages.txt" to "openssl 3.0.11-1~deb12u2\n",
                "libs.zip/bom.JSON" to "{}",
            ),
            files,
        )
        assertEquals(LibraryFormat.PNPM, LibraryListParser.parse(files[1].first, "lockfileVersion: '9.0'\nimporters:\n  .:\n    dependencies:\n      a:\n        specifier: 1\n        version: 1.0.0\nsnapshots:\n  a@1.0.0: {}\n").format)
    }

    @Test
    fun `too many files, a file or all of them too large, and a broken zip are refused`() {
        val many = zip(*(1..LibraryArchive.MAX_FILES + 1).map { "f$it.txt" to "x" }.toTypedArray())
        assertThrows(UserException::class.java) { LibraryArchive.expand(listOf(Triple("many.zip", null, many))) }
        // Zeros compress very well: a small zip that unpacks too large.
        val bomb = zip("big.txt" to "\u0000".repeat(LibraryListParser.MAX_FILE_BYTES + 1))
        assertThrows(UserException::class.java) { LibraryArchive.expand(listOf(Triple("bomb.zip", null, bomb))) }
        val half = zip(*(1..11).map { "f$it.txt" to "\u0000".repeat(LibraryListParser.MAX_FILE_BYTES - 1) }.toTypedArray())
        assertThrows(UserException::class.java) { LibraryArchive.expand(listOf(Triple("all.zip", null, half))) }
        // The limits apply to all zips of an upload together.
        val two = zip(*(1..60).map { "f$it.txt" to "x" }.toTypedArray())
        assertThrows(UserException::class.java) { LibraryArchive.expand(listOf(Triple("a.zip", null, two), Triple("b.zip", null, two))) }
        val large = "A".repeat(LibraryArchive.MAX_ARCHIVE_BYTES / 3 * 4 + 8)
        assertThrows(UserException::class.java) { LibraryArchive.expand(listOf(Triple("large.zip", null, large))) }
        assertThrows(UserException::class.java) { LibraryArchive.expand(listOf(Triple("text.zip", null, "not base64!"))) }
        val noZip = Base64.getUrlEncoder().encodeToString("hello world".toByteArray())
        assertThrows(UserException::class.java) { LibraryArchive.expand(listOf(Triple("text.zip", null, noZip))) }
    }

    companion object {
        /** A zip as base64url, like the browser sends it; a null content is a directory. */
        fun zip(vararg files: Pair<String, String?>): String {
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                files.forEach { (name, text) ->
                    zip.putNextEntry(ZipEntry(name))
                    text?.let { zip.write(it.toByteArray()) }
                    zip.closeEntry()
                }
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
        }
    }
}
