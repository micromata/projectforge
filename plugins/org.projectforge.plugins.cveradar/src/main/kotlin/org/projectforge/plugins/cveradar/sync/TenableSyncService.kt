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
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.projectforge.plugins.cveradar.CvrDeviceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Imports the findings of Tenable.sc (EXT-01 – EXT-03): every asset (asset list) whose name matches
 * [CveRadarConfig.Tenable.assetPattern] is a client device with the serial number and the user name of the pattern's
 * groups; its findings of the configured severities replace those of the last sync.
 */
@Service
class TenableSyncService {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var deviceService: CvrDeviceService

    class Summary(var assets: Int = 0, var skippedAssets: Int = 0, var failedAssets: Int = 0) {
        val result = CvrDeviceService.ScanResult()

        override fun toString(): String {
            return "$assets assets ($skippedAssets not matching, $failedAssets failed), ${result.newFindings} new, " +
                    "${result.reopened} reopened, ${result.resolved} resolved findings"
        }
    }

    fun sync(): Summary {
        val tenable = config.tenable
        check(tenable.isConfigured()) { "Tenable.sc isn't configured." }
        val pattern = Regex(tenable.assetPattern)
        val summary = Summary()
        val syncTime = Date()
        val assets = TenableParser.parseAssets(call(HttpGet(CveRadarHttp.url(tenable.url, "/rest/asset?fields=id,name"))))
        assets.forEach { asset ->
            val match = pattern.matchEntire(asset.name)
            val serial = match?.groups?.get("serial")?.value?.trim()
            val user = match?.groups?.get("user")?.value?.trim()
            if (serial.isNullOrEmpty() || user.isNullOrEmpty()) {
                summary.skippedAssets++
                return@forEach
            }
            try {
                val vulns = readVulns(asset.id, tenable.severities)
                val result = deviceService.applyScanAsset(
                    CvrDeviceService.ScanAsset(asset.id, asset.name, serial, user), vulns, syncTime,
                )
                summary.assets++
                summary.result.newFindings += result.newFindings
                summary.result.reopened += result.reopened
                summary.result.resolved += result.resolved
            } catch (ex: Exception) {
                // Its findings stay as they are; resolved is only what a successful sync doesn't report (EXT-02).
                log.error { "Reading the findings of Tenable asset #${asset.id} failed: ${ex.message}" }
                summary.failedAssets++
            }
        }
        log.info { "Tenable sync: $summary" }
        return summary
    }

    private fun readVulns(assetId: String, severities: String): List<CvrDeviceService.ScanVuln> {
        val vulns = mutableListOf<CvrDeviceService.ScanVuln>()
        var offset = 0
        while (true) {
            val request = HttpPost(CveRadarHttp.url(config.tenable.url, "/rest/analysis"))
            val page = TenableParser.parseVulns(
                call(request, TenableParser.vulnQuery(assetId, severities, offset, offset + PAGE_SIZE))
            )
            vulns.addAll(page.vulns)
            offset += PAGE_SIZE
            if (page.vulns.isEmpty() || offset >= page.totalRecords || offset >= MAX_FINDINGS_PER_ASSET) {
                break
            }
        }
        return vulns
    }

    private fun call(request: HttpUriRequestBase, body: String? = null): String {
        val tenable = config.tenable
        request.setHeader("x-apikey", "accesskey=${tenable.accessKey.trim()}; secretkey=${tenable.secretKey.trim()};")
        request.setHeader("Accept", "application/json")
        return CveRadarHttp.execute(CveRadarHttp.TENABLE, request, body)
    }

    companion object {
        private const val PAGE_SIZE = 1000

        /** Protection against endless paging. */
        private const val MAX_FINDINGS_PER_ASSET = 20000
    }
}
