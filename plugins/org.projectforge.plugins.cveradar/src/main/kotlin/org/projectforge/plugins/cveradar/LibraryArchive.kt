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

import org.projectforge.common.i18n.UserException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * The zip files of an upload (INV-26): a script of the project collects the lists of a component (lockfiles, SBOMs,
 * package lists, output of the build tools) into one zip. Only regular files with the [EXTENSIONS] of such lists are
 * read; everything else is skipped unread, also every file or directory starting with a dot (`.npmrc`, `.env`) and
 * `__MACOSX`, so files that may hold tokens (`.npmrc`, `gradle.properties`, `settings.xml`) are never read. A zip in the
 * zip isn't unpacked. The archive is untrusted (SEC-05): its size, the size of every file (counted while reading, not
 * taken from the header) and of all files together and their number are limited.
 */
object LibraryArchive {
    const val MAX_ARCHIVE_BYTES = 10 * 1024 * 1024
    const val MAX_UNPACKED_BYTES = 50 * 1024 * 1024
    const val MAX_FILES = 100
    private const val MAX_ZIP_ENTRIES = 2000

    const val ERROR_TOO_LARGE = "plugins.cveradar.libraries.error.archiveTooLarge"
    const val ERROR_TOO_MANY_FILES = "plugins.cveradar.libraries.error.archiveTooManyFiles"
    const val ERROR_INVALID = "plugins.cveradar.libraries.error.archiveInvalid"

    private val EXTENSIONS = setOf("json", "lock", "lockfile", "yaml", "yml", "txt", "mod")

    /**
     * The files of all uploads as name and text: a plain file as it is, a zip (base64url, as the browser sends binary
     * values) by the files read from it, named `archive.zip/path/in/zip`. The limits of the unpacked files apply to all
     * zips of the upload together.
     */
    fun expand(files: List<Triple<String, String?, String?>>): List<Pair<String, String>> {
        val budget = Budget()
        return files.flatMap { (name, text, base64) ->
            if (base64 == null) listOf(name to (text ?: "")) else unpack(name, decode(name, base64), budget)
        }
    }

    private fun decode(name: String, base64: String): ByteArray {
        // 4 characters per 3 bytes: checked before anything is decoded.
        if (base64.length * 3L / 4 > MAX_ARCHIVE_BYTES) throw tooLarge(name, MAX_ARCHIVE_BYTES)
        return try {
            Base64.getUrlDecoder().decode(base64)
        } catch (_: IllegalArgumentException) {
            throw UserException(ERROR_INVALID, name)
        }
    }

    private class Budget(var files: Int = 0, var bytes: Long = 0)

    private fun unpack(archive: String, bytes: ByteArray, budget: Budget): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entries = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++entries > MAX_ZIP_ENTRIES) throw UserException(ERROR_TOO_MANY_FILES, archive, MAX_FILES.toString())
                    val path = entry.name.replace('\\', '/').trimStart('/')
                    if (entry.isDirectory || !readable(path)) continue
                    if (++budget.files > MAX_FILES) throw UserException(ERROR_TOO_MANY_FILES, archive, MAX_FILES.toString())
                    val name = "$archive/$path"
                    result += name to read(zip, name, budget)
                }
            }
        } catch (_: ZipException) {
            throw UserException(ERROR_INVALID, archive)
        }
        if (result.isEmpty() && bytes.isNotEmpty() && !looksLikeZip(bytes)) throw UserException(ERROR_INVALID, archive)
        return result
    }

    /** No dot file or directory, no `__MACOSX`, one of the [EXTENSIONS]. */
    private fun readable(path: String): Boolean {
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.any { it.startsWith(".") || it == "__MACOSX" }) return false
        return parts.last().substringAfterLast('.', "").lowercase() in EXTENSIONS
    }

    /** The local file header of a zip (or the end record of an empty one). */
    private fun looksLikeZip(bytes: ByteArray) =
        bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    private fun read(zip: ZipInputStream, name: String, budget: Budget): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = zip.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            budget.bytes += n
            if (out.size() > LibraryListParser.MAX_FILE_BYTES) throw tooLarge(name, LibraryListParser.MAX_FILE_BYTES)
            if (budget.bytes > MAX_UNPACKED_BYTES) throw tooLarge(name.substringBefore('/'), MAX_UNPACKED_BYTES)
        }
        // Like the browser reads a plain file: UTF-8 without a byte order mark.
        return out.toString(Charsets.UTF_8).removePrefix("\uFEFF")
    }

    private fun tooLarge(name: String, max: Int) = UserException(ERROR_TOO_LARGE, name, (max / 1024 / 1024).toString())
}
