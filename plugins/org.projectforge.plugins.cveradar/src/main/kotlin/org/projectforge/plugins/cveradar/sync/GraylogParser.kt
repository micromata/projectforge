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

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.projectforge.plugins.cveradar.CvrDeviceService
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Date

/**
 * Reads the messages of the client agents from the response of the Graylog search API
 * (`/api/views/search/messages`: `schema` with the fields, `datarows` with the values in their order). The field
 * names come from the configuration only.
 */
object GraylogParser {
    private val objectMapper = ObjectMapper()

    private val PASSED = setOf("passed", "pass", "true", "ok", "success", "1")

    fun fields(config: CveRadarConfig.Graylog): List<String> {
        return listOf(
            "timestamp", config.userField, config.serialField, config.hostField, config.versionField,
            config.checkIdField, config.checkResultField,
        ).filter { it.isNotBlank() }.distinct()
    }

    /** The request body of the search. */
    fun request(config: CveRadarConfig.Graylog): String {
        val body = mutableMapOf<String, Any>(
            "query_string" to mapOf("type" to "elasticsearch", "query_string" to config.query),
            "timerange" to mapOf("type" to "relative", "range" to config.rangeSeconds),
            "fields_in_order" to fields(config),
            "limit" to config.limit,
        )
        if (config.streamId.isNotBlank()) {
            body["streams"] = listOf(config.streamId.trim())
        }
        return objectMapper.writeValueAsString(body)
    }

    fun parse(json: String, config: CveRadarConfig.Graylog): List<CvrDeviceService.AgentRecord> {
        val root = objectMapper.readTree(json)
        val columns = root.path("schema").mapIndexed { index, node -> node.path("field").asText() to index }.toMap()
        fun index(field: String): Int? = field.takeIf { it.isNotBlank() }?.let { columns[it] }
        val iTimestamp = index("timestamp")
        val iUser = index(config.userField) ?: return emptyList()
        val iSerial = index(config.serialField)
        val iHost = index(config.hostField)
        val iVersion = index(config.versionField)
        val iCheckId = index(config.checkIdField)
        val iCheckResult = index(config.checkResultField)
        return root.path("datarows").mapNotNull { row ->
            fun value(i: Int?): String? {
                i ?: return null
                val node = row.get(i) ?: return null
                if (node.isNull) return null
                return node.asText().trim().takeIf { it.isNotEmpty() }
            }
            val username = value(iUser) ?: return@mapNotNull null
            val checkId = value(iCheckId)
            CvrDeviceService.AgentRecord(
                timestamp = parseTimestamp(value(iTimestamp)) ?: return@mapNotNull null,
                username = username,
                serialNumber = value(iSerial),
                hostname = value(iHost),
                version = value(iVersion),
                checkId = checkId,
                checkPassed = if (checkId == null) null else value(iCheckResult)?.lowercase()?.let { PASSED.contains(it) },
            )
        }
    }

    private fun parseTimestamp(value: String?): Date? {
        value ?: return null
        return try {
            Date.from(Instant.parse(value))
        } catch (ex: Exception) {
            try {
                Date.from(OffsetDateTime.parse(value).toInstant())
            } catch (ex2: Exception) {
                null
            }
        }
    }
}
