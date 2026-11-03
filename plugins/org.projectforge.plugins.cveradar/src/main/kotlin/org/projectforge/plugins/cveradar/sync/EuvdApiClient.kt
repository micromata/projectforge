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
import java.time.LocalDate
import java.util.Locale

private val log = KotlinLogging.logger {}

/**
 * Client of the public API of the EUVD of the ENISA (`/search`, no key needed, IMP-16): pages of at most [PAGE_SIZE]
 * entries, a pause between two requests (the rate limit isn't documented), retries with backoff on 429/5xx and on
 * network errors. Timeouts and the size limit of the answers: integration id [CveRadarHttp.EUVD].
 */
@Service
class EuvdApiClient : EuvdSource {
    @Autowired
    private lateinit var config: CveRadarConfig

    private var lastRequest: Long = 0

    @Synchronized
    override fun search(from: LocalDate?, to: LocalDate?, minScore: Double?, exploited: Boolean, page: Int): EuvdPage {
        val url = StringBuilder(CveRadarHttp.url(config.euvd.url, "/search"))
            .append("?size=").append(PAGE_SIZE).append("&page=").append(page)
        from?.let { url.append("&fromDate=").append(it) }
        to?.let { url.append("&toDate=").append(it) }
        if (minScore != null && minScore > 0) {
            url.append("&fromScore=").append(String.format(Locale.US, "%.1f", minScore)).append("&toScore=10")
        }
        if (exploited) {
            url.append("&exploited=true")
        }
        var attempt = 0
        while (true) {
            pause()
            val status = try {
                return EuvdParser.parsePage(CveRadarHttp.execute(CveRadarHttp.EUVD, HttpGet(url.toString())))
            } catch (ex: CveRadarHttp.HttpException) {
                if (ex.status != 429 && ex.status < 500) {
                    throw ex
                }
                ex.status
            } catch (ex: IOException) {
                log.warn { "EUVD request failed (attempt ${attempt + 1}): ${ex.message}" }
                null
            }
            if (attempt >= BACKOFF_SECONDS.size) {
                throw IllegalStateException("EUVD not reachable after ${attempt + 1} attempts (last status: $status).")
            }
            log.info { "EUVD answered with status $status, retrying in ${BACKOFF_SECONDS[attempt]} s." }
            Thread.sleep(BACKOFF_SECONDS[attempt] * 1000L)
            attempt++
        }
    }

    private fun pause() {
        val wait = lastRequest + config.euvd.pauseMillis - System.currentTimeMillis()
        if (wait > 0) {
            Thread.sleep(wait)
        }
        lastRequest = System.currentTimeMillis()
    }

    companion object {
        /** The EUVD answers 50 entries at most, whatever is asked. */
        const val PAGE_SIZE = 50

        private val BACKOFF_SECONDS = listOf(10L, 30L, 60L, 120L)
    }
}
