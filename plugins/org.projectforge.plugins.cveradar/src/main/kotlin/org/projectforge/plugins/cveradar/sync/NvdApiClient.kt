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
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

/**
 * Client of the NVD API 2.0 (IMP-01, IMP-04): pages of at most [RESULTS_PER_PAGE] vulnerabilities, the rate limit of
 * the NVD kept by a pause between two requests, retries with backoff on 403/429/503 and on network errors. The API key
 * is sent as header only, it is never logged (IMP-05). Timeouts and the size limit of the answers: integration id
 * [CveRadarHttp.NVD].
 */
@Service
class NvdApiClient : NvdCveSource {
    @Autowired
    private lateinit var config: CveRadarConfig

    private var lastRequest: Long = 0

    @Synchronized
    override fun fetch(lastModStart: Instant, lastModEnd: Instant, startIndex: Int): NvdPage {
        val url = "${config.nvd.url.trim()}?lastModStartDate=${format(lastModStart)}" +
                "&lastModEndDate=${format(lastModEnd)}&resultsPerPage=$RESULTS_PER_PAGE&startIndex=$startIndex"
        var attempt = 0
        while (true) {
            pauseForRateLimit()
            val status = try {
                return NvdCveParser.parsePage(CveRadarHttp.execute(CveRadarHttp.NVD, buildRequest(url)))
            } catch (ex: CveRadarHttp.HttpException) {
                if (ex.status !in RETRY_STATUS) {
                    throw ex
                }
                ex.status
            } catch (ex: IOException) {
                log.warn { "NVD request failed (attempt ${attempt + 1}): ${ex.message}" }
                null
            }
            if (attempt >= BACKOFF_SECONDS.size) {
                throw IllegalStateException("NVD not reachable after ${attempt + 1} attempts (last status: $status).")
            }
            log.info { "NVD answered with status $status, retrying in ${BACKOFF_SECONDS[attempt]} s." }
            Thread.sleep(BACKOFF_SECONDS[attempt] * 1000L)
            attempt++
        }
    }

    private fun buildRequest(url: String): HttpGet {
        val request = HttpGet(url)
        config.nvd.apiKey.takeIf { it.isNotBlank() }?.let { request.setHeader("apiKey", it.trim()) }
        return request
    }

    /** Without key the NVD allows 5 requests in 30 s, with key 50. */
    private fun pauseForRateLimit() {
        val interval = if (config.nvd.apiKey.isBlank()) 6_500L else 700L
        val wait = lastRequest + interval - System.currentTimeMillis()
        if (wait > 0) {
            Thread.sleep(wait)
        }
        lastRequest = System.currentTimeMillis()
    }

    companion object {
        const val RESULTS_PER_PAGE = 2000

        private val RETRY_STATUS = setOf(403, 429, 503)

        private val BACKOFF_SECONDS = listOf(10L, 30L, 60L, 120L)

        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx").withZone(ZoneOffset.UTC)

        /** Extended ISO-8601 with offset, as the NVD expects it (`2026-10-01T00:00:00.000+00:00`), url encoded. */
        internal fun format(instant: Instant): String =
            URLEncoder.encode(TIMESTAMP_FORMAT.format(instant), StandardCharsets.UTF_8)
    }
}
