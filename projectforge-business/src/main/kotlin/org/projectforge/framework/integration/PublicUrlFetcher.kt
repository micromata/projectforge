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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.SystemDefaultDnsResolver
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.UnknownHostException

private val log = KotlinLogging.logger {}

/**
 * Resolves host names to public addresses only: a host with any address in a private, loopback, link-local
 * (e.g. the metadata service of a cloud, 169.254.169.254), unique-local, multicast or reserved range is refused as
 * unknown. Used by the connections themselves ([PooledHttpClients.getPublicOnly]), so the checked address is the one
 * connected to (no second lookup a DNS rebinding could answer differently).
 */
object PublicAddressResolver : DnsResolver {
    override fun resolve(host: String): Array<InetAddress> {
        val addresses = SystemDefaultDnsResolver.INSTANCE.resolve(host)
        if (addresses.isEmpty() || addresses.any { !isPublic(it) }) {
            throw UnknownHostException("$host: no public address.")
        }
        return addresses
    }

    override fun resolve(host: String, port: Int): List<InetSocketAddress> {
        return resolve(host).map { InetSocketAddress(it, port) }
    }

    override fun resolveCanonicalHostname(host: String): String {
        return SystemDefaultDnsResolver.INSTANCE.resolveCanonicalHostname(host)
    }

    /** Is the address a public one (of the internet)? */
    fun isPublic(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
        ) {
            return false
        }
        val bytes = address.address
        return when (address) {
            is Inet4Address -> isPublicV4(bytes)
            is Inet6Address -> isPublicV6(bytes)
            else -> false
        }
    }

    private fun isPublicV4(bytes: ByteArray): Boolean {
        val a = bytes[0].toInt() and 0xff
        val b = bytes[1].toInt() and 0xff
        val c = bytes[2].toInt() and 0xff
        return when {
            a == 0 || a == 10 || a == 127 || a >= 224 -> false // This network, private, loopback, multicast, reserved.
            a == 100 && b in 64..127 -> false // Shared address space (carrier-grade NAT).
            a == 169 && b == 254 -> false // Link-local, cloud metadata.
            a == 172 && b in 16..31 -> false
            a == 192 && b == 168 -> false
            a == 192 && b == 0 && (c == 0 || c == 2) -> false // IETF protocol assignments, documentation.
            a == 198 && (b == 18 || b == 19) -> false // Benchmarking.
            a == 198 && b == 51 && c == 100 -> false // Documentation.
            a == 203 && b == 0 && c == 113 -> false // Documentation.
            else -> true
        }
    }

    private fun isPublicV6(bytes: ByteArray): Boolean {
        fun u(i: Int) = bytes[i].toInt() and 0xff
        fun embeddedV4(offset: Int) = isPublicV4(bytes.copyOfRange(offset, offset + 4))
        return when {
            (u(0) and 0xfe) == 0xfc -> false // Unique local fc00::/7.
            (0..9).all { u(it) == 0 } && ((u(10) == 0 && u(11) == 0) || (u(10) == 0xff && u(11) == 0xff)) ->
                embeddedV4(12) // IPv4-compatible ::a.b.c.d, IPv4-mapped ::ffff:a.b.c.d.
            u(0) == 0x00 && u(1) == 0x64 && u(2) == 0xff && u(3) == 0x9b -> embeddedV4(12) // NAT64 64:ff9b::/96.
            u(0) == 0x20 && u(1) == 0x02 -> embeddedV4(2) // 6to4 2002::/16.
            u(0) == 0x20 && u(1) == 0x01 && u(2) == 0x00 && u(3) == 0x00 -> false // Teredo (hides the address).
            u(0) == 0x20 && u(1) == 0x01 && u(2) == 0x0d && u(3) == 0xb8 -> false // Documentation.
            u(0) == 0x01 && u(1) == 0x00 && (2..7).all { u(it) == 0 } -> false // Discard 100::/64.
            else -> true
        }
    }
}

/**
 * Fetches a url given by a user (e.g. for the preview of a link in the chat) without exposing the internal network
 * (SSRF): http and https on their standard ports only, public addresses only ([PublicAddressResolver]), redirects
 * followed by hand ([MAX_REDIRECTS]) with every hop checked again, the size and the total time limited.
 */
object PublicUrlFetcher {
    class Response(
        /** The url of the last hop (after redirects). */
        val url: URI,
        /** E.g. `text/html; charset=utf-8`, null if not given. */
        val contentType: String?,
        val body: ByteArray,
    )

    const val MAX_REDIRECTS = 3

    const val USER_AGENT = "Mozilla/5.0 (compatible; ProjectForge-LinkPreview/1.0; +https://www.projectforge.org)"

    /** The url as fetched: http or https with a host, the standard port, without user info. Null otherwise. */
    fun checkUrl(url: String?): URI? {
        val uri = try {
            URI(url?.trim() ?: return null).normalize()
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return null
        }
        if (uri.host.isNullOrBlank() || uri.rawUserInfo != null) {
            return null
        }
        if (uri.port != -1 && uri.port != (if (scheme == "https") 443 else 80)) {
            return null
        }
        return uri
    }

    /**
     * @param id The integration point (timeouts, see [IntegrationConfig]).
     * @param accept The accept header.
     * @param acceptContentType Accepts the content type of the response (e.g. html only)?
     * @param maxBytes The maximum size of the body.
     * @param maxMillis The maximum time of the whole fetch including redirects.
     * @param truncate Returns the first [maxBytes] of a larger body (e.g. the head of a large html page) instead of
     * nothing.
     * @return The response, null if the url isn't allowed, not reachable, not public, answered with an error, too large
     * or of an unaccepted type.
     */
    fun fetch(
        id: String,
        url: String,
        accept: String,
        acceptContentType: (String?) -> Boolean,
        maxBytes: Long,
        maxMillis: Long,
        truncate: Boolean = false,
    ): Response? {
        val deadline = System.currentTimeMillis() + maxMillis
        var uri = checkUrl(url) ?: return null
        val client = PooledHttpClients.getPublicOnly(id)
        try {
            repeat(MAX_REDIRECTS + 1) {
                // Checked before the request too: the client may take an address literal without asking the resolver.
                PublicAddressResolver.resolve(uri.host.removePrefix("[").removeSuffix("]"))
                val request = HttpGet(uri).also {
                    it.setHeader("Accept", accept)
                    it.setHeader("User-Agent", USER_AGENT)
                }
                var redirect: String? = null
                val response = client.execute(request) { response ->
                    when (response.code) {
                        in 300..399 -> {
                            redirect = response.getFirstHeader("Location")?.value
                            null
                        }

                        in 200..299 -> {
                            val entity = response.entity ?: return@execute null
                            val contentType = entity.contentType
                            if (!acceptContentType(contentType)) {
                                return@execute null
                            }
                            Response(uri, contentType, readLimited(entity.content, maxBytes, deadline, truncate))
                        }

                        else -> null
                    }
                }
                val location = redirect ?: return response
                uri = checkUrl(uri.resolve(location.trim()).toString()) ?: return null
            }
            log.debug { "Too many redirects: $url" }
        } catch (ex: IOException) {
            log.debug { "Url not fetched: $url: ${ex.message}" }
        } catch (ex: IllegalArgumentException) {
            log.debug { "Url not fetched (invalid): $url: ${ex.message}" }
        }
        return null
    }

    /** As [PooledHttpClients.readLimited], also limited in time: a server sending slowly can't keep the thread. */
    private fun readLimited(input: InputStream, maxBytes: Long, deadline: Long, truncate: Boolean): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            if (System.currentTimeMillis() > deadline) {
                throw IOException("Time limit exceeded.")
            }
            val read = input.read(buffer)
            if (read < 0) {
                break
            }
            if (total + read > maxBytes) {
                if (!truncate) {
                    throw PooledHttpClients.ResponseTooLargeException(maxBytes)
                }
                out.write(buffer, 0, (maxBytes - total).toInt())
                break
            }
            total += read
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
