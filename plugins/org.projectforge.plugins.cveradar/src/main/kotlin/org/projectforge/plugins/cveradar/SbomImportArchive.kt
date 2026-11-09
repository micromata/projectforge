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

import com.fasterxml.jackson.core.JsonProcessingException
import org.projectforge.common.i18n.UserException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipException
import java.util.zip.ZipFile

/**
 * The zip of a bulk import of SBOMs (INV-35), e.g. the output of a tool scanning all repositories and container images
 * of a project: every JSON file in it is read as a stream ([SbomStreamReader]), the SBOMs among them are kept with
 * their libraries only. Everything else (logs, the raw reports of the scanners, other JSON) is skipped and counted.
 * As in [LibraryArchive], files and directories starting with a dot and `__MACOSX` are never read. The zip is untrusted
 * (SEC-05): the number of entries, the unpacked size of every file (counted while reading, not taken from the header)
 * and of all of them together, the compression ratio and the number of libraries are limited.
 */
object SbomImportArchive {
    const val MAX_ENTRIES = 20_000
    const val MAX_ENTRY_BYTES = 1024L * 1024 * 1024

    /** All files read together unpack to at most so many times the zip. */
    const val MAX_TOTAL_RATIO = 20

    /** A file unpacks to at most so many times its packed size (or 1 MB, for small ones). */
    const val MAX_ENTRY_RATIO = 100
    private const val MIN_RATIO_BASE = 1024L * 1024

    /** The libraries of all SBOMs together, before they are merged per target. */
    const val MAX_LIBRARIES = 1_000_000

    const val ERROR_INVALID = "plugins.cveradar.sbomImport.error.invalid"
    const val ERROR_TOO_MANY_ENTRIES = "plugins.cveradar.sbomImport.error.tooManyEntries"
    const val ERROR_TOO_LARGE = "plugins.cveradar.sbomImport.error.entryTooLarge"
    const val ERROR_RATIO = "plugins.cveradar.sbomImport.error.ratio"
    const val ERROR_TOO_MANY_LIBRARIES = "plugins.cveradar.sbomImport.error.tooManyLibraries"

    class Result(
        val documents: List<SbomImportDocument>,
        /** Files that aren't JSON (logs, ...). */
        val otherFiles: Int,
        /** JSON that isn't an SBOM (the raw report of a scanner, ...). */
        val notSbom: Int,
        /** JSON that couldn't be read (empty, broken). */
        val invalid: Int,
    )

    /**
     * Reads the zip [file]; [progress] is told the packed bytes read so far and in all, [isActive] is asked between the
     * files (false: stopped, what was read so far is returned).
     */
    fun read(file: Path, progress: (read: Long, total: Long) -> Unit = { _, _ -> }, isActive: () -> Boolean = { true }): Result {
        val total = file.toFile().length()
        val budget = Budget(total * MAX_TOTAL_RATIO)
        val documents = mutableListOf<SbomImportDocument>()
        var otherFiles = 0
        var notSbom = 0
        var invalid = 0
        var libraries = 0
        try {
            ZipFile(file.toFile()).use { zip ->
                if (zip.size() > MAX_ENTRIES) throw UserException(ERROR_TOO_MANY_ENTRIES, MAX_ENTRIES.toString())
                var read = 0L
                for (entry in zip.entries()) {
                    if (!isActive()) break
                    val path = entry.name.replace('\\', '/').trimStart('/')
                    read += entry.compressedSize.coerceAtLeast(0)
                    if (entry.isDirectory || !readable(path)) {
                        if (!entry.isDirectory) otherFiles++
                        continue
                    }
                    if (!path.lowercase().endsWith(".json")) {
                        otherFiles++
                        continue
                    }
                    // A scan that failed, e.g. of an image that couldn't be pulled.
                    if (entry.size == 0L) {
                        invalid++
                        continue
                    }
                    val limit = minOf(MAX_ENTRY_BYTES, maxOf(entry.compressedSize, MIN_RATIO_BASE) * MAX_ENTRY_RATIO)
                    val document = try {
                        LimitedInputStream(zip.getInputStream(entry), path, limit, budget).use { SbomStreamReader.read(it) }
                    } catch (_: JsonProcessingException) {
                        invalid++
                        continue
                    } catch (ex: IOException) {
                        if (ex is ZipException) throw ex
                        invalid++
                        continue
                    }
                    if (document == null) {
                        notSbom++
                    } else {
                        libraries += document.entries.size
                        if (libraries > MAX_LIBRARIES) throw UserException(ERROR_TOO_MANY_LIBRARIES, MAX_LIBRARIES.toString())
                        documents.add(SbomImportDocument(path, document))
                    }
                    progress(read, total)
                }
            }
        } catch (_: ZipException) {
            throw UserException(ERROR_INVALID)
        }
        return Result(documents, otherFiles, notSbom, invalid)
    }

    /** No dot file or directory, no `__MACOSX`. */
    private fun readable(path: String): Boolean {
        val parts = path.split('/').filter { it.isNotEmpty() }
        return parts.isNotEmpty() && parts.none { it.startsWith(".") || it == "__MACOSX" }
    }

    private class Budget(val max: Long, var bytes: Long = 0)

    /** Counts what is read; above the limit of the file (size, ratio) or of all files the import stops. */
    private class LimitedInputStream(
        input: InputStream,
        private val name: String,
        private val limit: Long,
        private val budget: Budget,
    ) : FilterInputStream(input) {
        private var count = 0L

        override fun read(): Int {
            val b = super.read()
            if (b >= 0) counted(1)
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) counted(n.toLong())
            return n
        }

        private fun counted(n: Long) {
            count += n
            budget.bytes += n
            if (count > MAX_ENTRY_BYTES) throw UserException(ERROR_TOO_LARGE, name, (MAX_ENTRY_BYTES / 1024 / 1024).toString())
            if (count > limit || budget.bytes > budget.max) throw UserException(ERROR_RATIO, name)
        }
    }
}
