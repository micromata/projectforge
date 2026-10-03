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

package org.projectforge.framework.integration

import mu.KotlinLogging
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * One shared, pooled Apache http client per integration point instead of a new client (and new connections)
 * per request. Timeouts and pool sizes are taken from [IntegrationConfig], so no request may block forever.
 * The clients are thread safe and closed on shutdown.
 */
object PooledHttpClients {
    private val clients = ConcurrentHashMap<String, CloseableHttpClient>()

    /**
     * @param id Id of the integration point, e.g. [IntegrationConfig.ICAL].
     * @param disableContentCompression For servers sending a Content-Length of the compressed body (Authentik).
     */
    @JvmStatic
    @JvmOverloads
    fun get(id: String, disableContentCompression: Boolean = false): CloseableHttpClient {
        val key = if (disableContentCompression) "$id(no-compression)" else id
        return clients.computeIfAbsent(key) { create(id, disableContentCompression) }
    }

    /**
     * Request factory for [org.springframework.web.client.RestTemplate] using the pooled client of the given id.
     */
    @JvmStatic
    @JvmOverloads
    fun requestFactory(id: String, disableContentCompression: Boolean = false): HttpComponentsClientHttpRequestFactory {
        return HttpComponentsClientHttpRequestFactory(get(id, disableContentCompression))
    }

    fun closeAll() {
        clients.values.forEach { client ->
            try {
                client.close(CloseMode.GRACEFUL)
            } catch (e: Exception) {
                log.warn { "Error while closing http client: ${e.message}" }
            }
        }
        clients.clear()
    }

    private fun create(id: String, disableContentCompression: Boolean): CloseableHttpClient {
        val timeouts = IntegrationConfig.get(id)
        log.info { "Creating pooled http client for '$id': $timeouts" }
        val connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(timeouts.connectTimeoutMs))
            .setSocketTimeout(Timeout.ofMilliseconds(timeouts.responseTimeoutMs))
            .setTimeToLive(CONNECTION_TTL)
            .build()
        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .setMaxConnTotal(timeouts.maxConnections)
            .setMaxConnPerRoute(timeouts.maxConnectionsPerRoute)
            .build()
        val requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeouts.connectionRequestTimeoutMs))
            .setResponseTimeout(Timeout.ofMilliseconds(timeouts.responseTimeoutMs))
            .build()
        val builder = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictExpiredConnections()
            .evictIdleConnections(IDLE_TIMEOUT)
        if (disableContentCompression) {
            builder.disableContentCompression()
        }
        return builder.build()
    }

    /**
     * Reads the stream completely, but not more than [maxBytes] (0: no limit).
     * @throws ResponseTooLargeException if the stream is larger.
     */
    @JvmStatic
    fun readLimited(input: InputStream, maxBytes: Long): ByteArray {
        if (maxBytes <= 0) {
            return input.readBytes()
        }
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) {
                break
            }
            total += read
            if (total > maxBytes) {
                throw ResponseTooLargeException(maxBytes)
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    class ResponseTooLargeException(val maxBytes: Long) :
        IOException("Response exceeds the limit of $maxBytes bytes.")

    private val CONNECTION_TTL = TimeValue.ofMinutes(5)

    private val IDLE_TIMEOUT = TimeValue.ofMinutes(1)
}
