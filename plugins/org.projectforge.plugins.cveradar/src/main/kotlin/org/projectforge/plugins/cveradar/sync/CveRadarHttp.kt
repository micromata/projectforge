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

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import org.projectforge.framework.integration.IntegrationConfig
import org.projectforge.framework.integration.PooledHttpClients

/**
 * The HTTP calls of the connectors: pooled clients with the timeouts of [IntegrationConfig] (ids [TENABLE],
 * [GRAYLOG]) and a size limit of every response (EXT-06, SEC-05). Error messages never contain request headers.
 */
internal object CveRadarHttp {
    const val TENABLE = "cveradar-tenable"
    const val GRAYLOG = "cveradar-graylog"

    /** Used, if `projectforge.integration.clients.<id>.maxResponseBytes` isn't given. */
    private const val DEFAULT_MAX_RESPONSE_BYTES = 50L * 1024 * 1024

    class HttpException(message: String) : RuntimeException(message)

    fun execute(integrationId: String, request: HttpUriRequestBase, jsonBody: String? = null): String {
        jsonBody?.let { request.entity = StringEntity(it, ContentType.APPLICATION_JSON) }
        val maxBytes = IntegrationConfig.get(integrationId).maxResponseBytes.takeIf { it > 0 }
            ?: DEFAULT_MAX_RESPONSE_BYTES
        return PooledHttpClients.get(integrationId).execute(request) { response ->
            val body = response.entity?.content?.use { PooledHttpClients.readLimited(it, maxBytes) }
                ?.toString(Charsets.UTF_8) ?: ""
            if (response.code !in 200..299) {
                throw HttpException("${request.method} ${request.requestUri}: HTTP ${response.code} ${body.take(300)}")
            }
            body
        }
    }

    /** The url without trailing slash plus the path. */
    fun url(baseUrl: String, path: String): String = "${baseUrl.trim().trimEnd('/')}$path"
}
