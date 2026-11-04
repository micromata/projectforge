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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.projectforge.framework.integration.PooledHttpClients
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipFile

private val log = KotlinLogging.logger {}

/** The advisories of the ecosystems of OSV.dev. */
interface OsvSource {
    /** The ids of the advisories of the ecosystem modified since [since] with their modification time. */
    fun modifiedSince(ecosystem: String, since: Instant): List<Pair<Instant, String>>

    /** The document of one advisory, null if it doesn't exist (any more). */
    fun advisory(ecosystem: String, id: String): String?

    /** All documents of the ecosystem one by one, except the ids [skip] refuses (not even read). */
    fun all(ecosystem: String, skip: (String) -> Boolean, consumer: (String) -> Unit)
}

/**
 * Reads the public bucket of OSV.dev (https://google.github.io/osv.dev/data/#data-dumps): per ecosystem
 * `modified_id.csv` (`<modified>,<id>` per line), `<id>.json` and `all.zip`. Only files of advisories are fetched, never
 * anything of the inventory is sent (IMP-13). The zip is downloaded into a temporary file and read entry by entry,
 * each one limited in size. Timeouts and the size limit: integration id [CveRadarHttp.OSV].
 */
@Service
class OsvClient : OsvSource {
    @Autowired
    private lateinit var config: CveRadarConfig

    override fun modifiedSince(ecosystem: String, since: Instant): List<Pair<Instant, String>> {
        val result = mutableListOf<Pair<Instant, String>>()
        // Sorted by the modification time, the latest first: reading (and downloading) stops at the first one older
        // than [since], the list of a large ecosystem is never read as a whole.
        CveRadarHttp.readLines(CveRadarHttp.OSV, url(ecosystem, "modified_id.csv"), maxBytesLimit = MAX_LIST_BYTES) { line ->
            val (time, id) = line.split(',', limit = 2).takeIf { it.size == 2 } ?: return@readLines true
            val modified = OsvParser.parseTimestamp(time) ?: return@readLines true
            if (modified < since) return@readLines false
            result.add(modified to id.trim())
            true
        }
        return result
    }

    override fun advisory(ecosystem: String, id: String): String? =
        try {
            CveRadarHttp.execute(CveRadarHttp.OSV, HttpGet(url(ecosystem, "${encode(id)}.json")), maxBytesLimit = MAX_ENTRY_BYTES)
        } catch (ex: CveRadarHttp.HttpException) {
            if (ex.status != 404) throw ex
            null
        } catch (_: PooledHttpClients.ResponseTooLargeException) {
            // As in the zip: skipped for good, it would fail on every run.
            log.warn { "OSV advisory $id of $ecosystem skipped: larger than $MAX_ENTRY_BYTES bytes." }
            null
        }

    override fun all(ecosystem: String, skip: (String) -> Boolean, consumer: (String) -> Unit) {
        val file = Files.createTempFile("cveradar-osv-", ".zip")
        try {
            CveRadarHttp.download(CveRadarHttp.OSV, url(ecosystem, "all.zip"), file)
            ZipFile(file.toFile()).use { zip ->
                zip.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".json") }.forEach { entry ->
                    if (skip(entry.name.removeSuffix(".json").substringAfterLast('/'))) return@forEach
                    val json = try {
                        zip.getInputStream(entry).use { PooledHttpClients.readLimited(it, MAX_ENTRY_BYTES) }.toString(Charsets.UTF_8)
                    } catch (_: PooledHttpClients.ResponseTooLargeException) {
                        log.warn { "OSV advisory ${entry.name} of $ecosystem skipped: larger than $MAX_ENTRY_BYTES bytes." }
                        return@forEach
                    }
                    consumer(json)
                }
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun url(ecosystem: String, file: String) = CveRadarHttp.url(config.osv.url, "/${encode(ecosystem)}/$file")

    private fun encode(text: String) = URLEncoder.encode(text, Charsets.UTF_8).replace("+", "%20")

    companion object {
        /** The document of one advisory at most (the largest ones list some thousand versions). */
        private const val MAX_ENTRY_BYTES = 5L * 1024 * 1024
        private const val MAX_LIST_BYTES = 200L * 1024 * 1024
    }
}
