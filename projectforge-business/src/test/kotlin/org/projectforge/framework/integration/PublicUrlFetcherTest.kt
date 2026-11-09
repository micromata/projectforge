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

import com.sun.net.httpserver.HttpServer
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger

class PublicUrlFetcherTest {
    @Test
    fun `internal, reserved and special addresses are not public`() {
        listOf(
            "127.0.0.1", "127.1.2.3", "0.0.0.0", "10.0.0.1", "172.16.0.1", "172.31.255.255", "192.168.1.1",
            "169.254.169.254", "100.64.0.1", "198.18.0.1", "224.0.0.1", "255.255.255.255", "192.0.2.1",
            "::1", "::", "fe80::1", "fc00::1", "fd12:3456::1", "::ffff:127.0.0.1", "::ffff:10.0.0.1", "::10.0.0.1",
            "64:ff9b::a00:1", "2002:a00:1::1", "2001:0:4136:e378::1", "2001:db8::1", "ff02::1",
        ).forEach { assertFalse(PublicAddressResolver.isPublic(InetAddress.getByName(it)), it) }
        listOf("8.8.8.8", "1.1.1.1", "172.32.0.1", "100.128.0.1", "2a00:1450:4001::200e", "2002:808:808::1")
            .forEach { assertTrue(PublicAddressResolver.isPublic(InetAddress.getByName(it)), it) }
    }

    @Test
    fun `the resolver refuses hosts with internal addresses`() {
        assertThrows(UnknownHostException::class.java) { PublicAddressResolver.resolve("localhost") }
        assertThrows(UnknownHostException::class.java) { PublicAddressResolver.resolve("127.0.0.1") }
        assertThrows(UnknownHostException::class.java) { PublicAddressResolver.resolve("2130706433") }
        assertThrows(UnknownHostException::class.java) { PublicAddressResolver.resolve("::1") }
    }

    @Test
    fun `only http and https on their standard ports, without user info`() {
        assertNotNull(PublicUrlFetcher.checkUrl("https://www.projectforge.org/docs"))
        assertNotNull(PublicUrlFetcher.checkUrl("http://example.com:80/"))
        assertNotNull(PublicUrlFetcher.checkUrl("https://example.com:443/"))
        listOf(
            "file:///etc/passwd", "ftp://example.com/", "gopher://example.com/", "javascript:alert(1)",
            "https://example.com:8443/", "http://example.com:22/", "https://user:secret@example.com/",
            "https:///path", "mailto:info@example.com", "not a url", "",
        ).forEach { assertNull(PublicUrlFetcher.checkUrl(it), it) }
    }

    @Test
    fun `a server in the internal network is never contacted`() {
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange ->
            requests.incrementAndGet()
            exchange.sendResponseHeaders(200, 2)
            exchange.responseBody.use { it.write("ok".toByteArray()) }
        }
        server.start()
        try {
            // A standard port is required, so the test server (random port) isn't reachable by an allowed url at all.
            // Its address is refused independently of the port:
            val port = server.address.port
            listOf("http://127.0.0.1:$port/", "http://localhost:$port/", "http://[::1]:$port/").forEach {
                assertNull(fetch(it), it)
            }
            listOf("http://127.0.0.1/", "http://localhost/", "http://[::1]/", "http://169.254.169.254/latest/meta-data/")
                .forEach { assertNull(fetch(it), it) }
            // The client itself (without the checks of the fetcher) doesn't connect to it either, also not by literal:
            listOf("http://127.0.0.1:$port/", "http://localhost:$port/").forEach { url ->
                assertThrows(java.io.IOException::class.java, {
                    PooledHttpClients.getPublicOnly(IntegrationConfig.LINK_PREVIEW).execute(HttpGet(url)) { it.code }
                }, url)
            }
            assertEquals(0, requests.get())
        } finally {
            server.stop(0)
        }
    }

    private fun fetch(url: String) = PublicUrlFetcher.fetch(
        IntegrationConfig.LINK_PREVIEW,
        url,
        accept = "*/*",
        acceptContentType = { true },
        maxBytes = 1024,
        maxMillis = 2_000,
    )
}
