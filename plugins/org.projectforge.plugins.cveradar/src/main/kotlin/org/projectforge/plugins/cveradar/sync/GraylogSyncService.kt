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
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.projectforge.plugins.cveradar.CvrDeviceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64

private val log = KotlinLogging.logger {}

/**
 * Reads the versions and check results of the client agents from Graylog (EXT-04, EXT-05): the messages of the
 * configured query within the configured period, the newest value per device wins.
 */
@Service
class GraylogSyncService {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var deviceService: CvrDeviceService

    class Summary(val messages: Int, val devices: Int) {
        override fun toString(): String = "$messages messages, $devices devices updated"
    }

    fun sync(): Summary {
        val graylog = config.graylog
        check(graylog.isConfigured()) { "Graylog isn't configured." }
        val request = HttpPost(CveRadarHttp.url(graylog.url, "/api/views/search/messages"))
        val auth = Base64.getEncoder().encodeToString("${graylog.token.trim()}:token".toByteArray())
        request.setHeader("Authorization", "Basic $auth")
        request.setHeader("Accept", "application/json")
        request.setHeader("X-Requested-By", "ProjectForge")
        val records = GraylogParser.parse(
            CveRadarHttp.execute(CveRadarHttp.GRAYLOG, request, GraylogParser.request(graylog)), graylog,
        )
        val summary = Summary(records.size, deviceService.applyAgentRecords(records))
        log.info { "Graylog sync: $summary" }
        return summary
    }
}
