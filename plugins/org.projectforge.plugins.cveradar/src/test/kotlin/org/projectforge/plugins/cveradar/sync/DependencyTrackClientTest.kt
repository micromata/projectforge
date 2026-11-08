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

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.framework.integration.SyncStats
import org.projectforge.framework.integration.SyncStatsRegistry
import org.projectforge.plugins.cveradar.CveRadarConfig
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The client of Dependency-Track against a local stub server with answers shaped like the ones of Dependency-Track
 * 5.2.1: the key in the header, all pages of a list, errors without the key, the probe.
 */
class DependencyTrackClientTest {
    private lateinit var server: HttpServer

    private val requests = CopyOnWriteArrayList<Pair<String, String?>>()

    private var permissions = DependencyTrackClient.PERMISSIONS

    private var versionAnswer = """{"application":"Dependency-Track","version":"5.2.1"}"""

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val key = exchange.requestHeaders.getFirst("X-Api-Key")
            requests.add(exchange.requestURI.toString() to key)
            val path = exchange.requestURI.path
            when {
                path == "/api/version" -> answer(exchange, 200, versionAnswer)
                key != API_KEY -> answer(exchange, 401, "Unauthorized")
                path == "/api/v1/team/self" -> answer(
                    exchange, 200,
                    """{"name":"projectforge","permissions":[${permissions.joinToString(",") { """{"name":"$it"}""" }}]}""",
                )
                path == "/api/v1/finding/project/p1" -> {
                    val query = query(exchange)
                    val size = query.getValue("pageSize").toInt()
                    val page = query.getValue("pageNumber").toInt()
                    val from = (page - 1) * size
                    val entries = (from until minOf(from + size, TOTAL)).joinToString(",") { """{"n":$it}""" }
                    exchange.responseHeaders.add("X-Total-Count", "$TOTAL")
                    answer(exchange, 200, "[$entries]")
                }
                path == "/api/v1/plain" -> answer(exchange, 200, """[{"n":1},{"n":2}]""")
                path == "/api/v1/full" -> answer(exchange, 200, (1..100).joinToString(",", "[", "]") { """{"n":$it}""" })
                path == "/api/v1/moved" -> {
                    exchange.responseHeaders.add("Location", "/api/v1/team/self")
                    answer(exchange, 302, "")
                }
                else -> answer(exchange, 404, "Not found")
            }
        }
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop(0)
    }

    @Test
    fun `all pages of a list are read, the key is sent with every request`() {
        val findings = client().getAll("/api/v1/finding/project/p1?suppressed=true")
        assertEquals((0 until TOTAL).toList(), findings.map { it.path("n").intValue() })
        assertEquals(3, requests.size, "230 entries in pages of 100")
        assertTrue(requests.all { it.second == API_KEY })
        assertTrue(requests.first().first.contains("suppressed=true&pageSize=100&pageNumber=1"))
    }

    @Test
    fun `a list without total is read once`() {
        assertEquals(2, client().getAll("/api/v1/plain").size)
        assertEquals(1, requests.size)
    }

    @Test
    fun `a full page without total fails, as the list might be incomplete`() {
        assertThrows<IllegalStateException> { client().getAll("/api/v1/full") }
        assertEquals(1, requests.size)
    }

    @Test
    fun `a redirect isn't followed, the key isn't sent to the redirect target`() {
        val ex = assertThrows<CveRadarHttp.HttpException> { client().get("/api/v1/moved") }
        assertEquals(302, ex.status)
        assertEquals(1, requests.size)
    }

    @Test
    fun `the key is sent trimmed, the version is read without key`() {
        val client = client(apiKey = " $API_KEY\n")
        assertEquals("projectforge", client.team().name)
        assertEquals("5.2.1", client.version())
        assertEquals(listOf(API_KEY, null), requests.map { it.second })
    }

    @Test
    fun `a wrong key is refused, the message doesn't contain the key`() {
        val ex = assertThrows<CveRadarHttp.HttpException> { client(apiKey = "wrong-key-value").team() }
        assertEquals(401, ex.status)
        assertFalse(ex.message!!.contains("wrong-key-value"))
    }

    @Test
    fun `the probe reads version and team, a missing permission counts as error`() {
        val client = client()
        val result = client.probe()
        assertEquals("5.2.1", result.version)
        assertTrue(result.missingPermissions.isEmpty())
        assertEquals("5.2.1", client.lastVersion)
        assertEquals(SyncStats.Status.SUCCESS, SyncStatsRegistry.get(DependencyTrackClient.SYNC_TYPE).lastStatus)

        permissions = DependencyTrackClient.PERMISSIONS - "VULNERABILITY_ANALYSIS"
        assertEquals(setOf("VULNERABILITY_ANALYSIS"), client.probe().missingPermissions)
        assertEquals(SyncStats.Status.ERRORS, SyncStatsRegistry.get(DependencyTrackClient.SYNC_TYPE).lastStatus)

        permissions = DependencyTrackClient.PERMISSIONS
        versionAnswer = """{"application":"Dependency-Track"}"""
        assertNull(client.probe().version)
        assertEquals(SyncStats.Status.ERRORS, SyncStatsRegistry.get(DependencyTrackClient.SYNC_TYPE).lastStatus)

        val wrongKey = client(apiKey = "wrong")
        assertThrows<CveRadarHttp.HttpException> { wrongKey.probe() }
        assertEquals(SyncStats.Status.ABORTED, SyncStatsRegistry.get(DependencyTrackClient.SYNC_TYPE).lastStatus)
        assertNull(wrongKey.lastVersion, "a failed probe leaves no version")
    }

    @Test
    fun `unconfigured, nothing is requested`() {
        val config = CveRadarConfig()
        assertFalse(config.deptrack.isConfigured())
        assertThrows<IllegalStateException> { DependencyTrackClient(config).version() }
        assertTrue(requests.isEmpty())
        assertNull(DependencyTrackClient(config).lastVersion)
    }

    @Test
    fun `the key and the secret are not shown by toString`() {
        val deptrack = CveRadarConfig.DependencyTrack().apply {
            url = "https://dtrack.example.org"
            apiKey = "secret-key"
            webhookSecret = "secret-hook"
        }
        assertTrue(deptrack.isConfigured())
        assertFalse(deptrack.toString().contains("secret"))
    }

    private fun client(apiKey: String = API_KEY): DependencyTrackClient {
        val config = CveRadarConfig()
        // With a trailing slash, as an administrator may give it.
        config.deptrack.url = "http://127.0.0.1:${server.address.port}/"
        config.deptrack.apiKey = apiKey
        return DependencyTrackClient(config)
    }

    private fun query(exchange: HttpExchange): Map<String, String> =
        exchange.requestURI.query.split('&').associate { it.substringBefore('=') to it.substringAfter('=') }

    private fun answer(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    companion object {
        private const val API_KEY = "test-api-key"

        private const val TOTAL = 230
    }
}
