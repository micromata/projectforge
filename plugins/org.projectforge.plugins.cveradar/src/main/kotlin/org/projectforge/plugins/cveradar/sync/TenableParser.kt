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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.plugins.cveradar.CvrDeviceService
import java.util.Date

/**
 * Reads the responses of the Tenable.sc REST API (`/rest/asset`, `/rest/analysis` with the tool `vulndetails`).
 * Tenable.sc sends most numbers as strings; everything is read leniently and is untrusted (SEC-05).
 */
object TenableParser {
    private val objectMapper = ObjectMapper()

    class Asset(val id: String, val name: String)

    /**
     * [totalRecords] is null if the response doesn't tell it; [returned] counts all results of the page, also those
     * without plugin id.
     */
    class VulnPage(val totalRecords: Int?, val returned: Int, val vulns: List<CvrDeviceService.ScanVuln>)

    /** The usable and manageable assets (asset lists) of the user. */
    fun parseAssets(json: String): List<Asset> {
        val response = checkError(objectMapper.readTree(json)).path("response")
        val nodes = if (response.isArray) {
            response.toList()
        } else {
            response.path("usable").toList() + response.path("manageable").toList()
        }
        return nodes.mapNotNull { node ->
            val id = node.text("id") ?: return@mapNotNull null
            val name = node.text("name") ?: return@mapNotNull null
            Asset(id, name)
        }.distinctBy { it.id }
    }

    fun parseVulns(json: String): VulnPage {
        val response = checkError(objectMapper.readTree(json)).path("response")
        val results = response.path("results")
        val vulns = results.mapNotNull { node ->
            val pluginId = node.text("pluginID") ?: return@mapNotNull null
            CvrDeviceService.ScanVuln(
                pluginId = pluginId,
                pluginName = node.text("pluginName"),
                severity = node.path("severity").let { it.text("id") ?: it.textValue() }?.toIntOrNull(),
                cveIds = node.text("cve")?.split(',')?.map { it.trim().uppercase() }?.filter { it.isNotEmpty() }
                    ?: emptyList(),
                firstSeen = epoch(node.text("firstSeen")),
                lastSeen = epoch(node.text("lastSeen")),
                hostname = node.text("dnsName") ?: node.text("netbiosName")?.substringAfterLast('\\'),
            )
        }
        return VulnPage(response.text("totalRecords")?.toIntOrNull(), results.size(), vulns)
    }

    /** The request body of `/rest/analysis` for the findings of an asset with the given severities. */
    fun vulnQuery(assetId: String, severities: String, startOffset: Int, endOffset: Int): String {
        val query = mapOf(
            "type" to "vuln",
            "sourceType" to "cumulative",
            "query" to mapOf(
                "type" to "vuln",
                "tool" to "vulndetails",
                "startOffset" to startOffset,
                "endOffset" to endOffset,
                "filters" to listOf(
                    mapOf("filterName" to "asset", "operator" to "=", "value" to mapOf("id" to assetId)),
                    mapOf("filterName" to "severity", "operator" to "=", "value" to severities),
                ),
            ),
        )
        return objectMapper.writeValueAsString(query)
    }

    private fun checkError(root: JsonNode): JsonNode {
        val code = root.text("error_code")
        if (code != null && code != "0") {
            throw IllegalStateException("Tenable.sc error $code: ${root.text("error_msg")?.take(300)}")
        }
        return root
    }

    private fun epoch(value: String?): Date? {
        val seconds = value?.toLongOrNull()?.takeIf { it > 0 } ?: return null
        return Date(seconds * 1000)
    }

    private fun JsonNode.text(field: String): String? {
        val node = get(field) ?: return null
        if (node.isNull || node.isContainerNode) return null
        return node.asText().trim().takeIf { it.isNotEmpty() }
    }
}
