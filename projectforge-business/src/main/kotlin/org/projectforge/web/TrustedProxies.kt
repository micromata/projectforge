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

package org.projectforge.web

import mu.KotlinLogging
import java.net.InetAddress

private val log = KotlinLogging.logger {}

/**
 * Which hosts may tell ProjectForge the client's ip address via `X-Forwarded-For`.
 *
 * `X-Forwarded-For` is an ordinary request header, so any client can send one. Only a reverse proxy in front of
 * ProjectForge may be believed - otherwise a client picks its own ip address, which defeats everything keyed by
 * it: the brute force protection of [org.projectforge.business.login.LoginProtection] (a new fake ip per attempt
 * lifts the brake, a victim's ip blocks that victim) as well as every ip in the logs.
 *
 * Configured by `projectforge.security.trustedProxies`, see application.properties for the default.
 *
 * @see WebUtils.getClientIp
 */
class TrustedProxies(definition: String?) {
    private class Range(val address: ByteArray, val prefixLength: Int) {
        fun matches(candidate: ByteArray): Boolean {
            if (candidate.size != address.size) {
                return false // Don't compare an IPv4 address with an IPv6 one.
            }
            var remainingBits = prefixLength
            for (i in address.indices) {
                if (remainingBits <= 0) {
                    return true
                }
                if (remainingBits >= 8) {
                    if (candidate[i] != address[i]) {
                        return false
                    }
                    remainingBits -= 8
                } else {
                    // Compare the remaining bits of this byte only, the mask has its high bits set.
                    val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                    return (candidate[i].toInt() and mask) == (address[i].toInt() and mask)
                }
            }
            return true
        }
    }

    private val ranges: List<Range>

    /**
     * True, if nothing is configured: then no `X-Forwarded-For` is used at all.
     */
    val isEmpty: Boolean
        get() = ranges.isEmpty()

    init {
        ranges = definition?.split(',')?.mapNotNull { parseRange(it.trim()) } ?: emptyList()
    }

    /**
     * @param address The address of the host the request came from ([jakarta.servlet.ServletRequest.getRemoteAddr]).
     * @return True, if a `X-Forwarded-For` sent by this host may be believed.
     */
    fun isTrusted(address: String?): Boolean {
        if (address.isNullOrBlank() || ranges.isEmpty()) {
            return false
        }
        val bytes = parseAddress(address) ?: return false
        return ranges.any { it.matches(bytes) }
    }

    private fun parseRange(entry: String): Range? {
        if (entry.isEmpty()) {
            return null // Occurs on a trailing coma.
        }
        val address = parseAddress(entry.substringBefore('/'))
        if (address == null) {
            log.warn { "Ignoring trusted proxy '$entry' of projectforge.security.trustedProxies: not an ip address." }
            return null
        }
        val maxPrefixLength = address.size * 8
        val prefixLength = if (entry.contains('/')) {
            entry.substringAfter('/').toIntOrNull()?.takeIf { it in 0..maxPrefixLength } ?: run {
                log.warn { "Ignoring trusted proxy '$entry' of projectforge.security.trustedProxies: invalid prefix length (0..$maxPrefixLength expected)." }
                return null
            }
        } else {
            maxPrefixLength // A single host.
        }
        return Range(address, prefixLength)
    }

    companion object {
        /**
         * [InetAddress.getByName] would do a dns lookup for anything that isn't an ip literal, so a header value
         * would turn into a lookup of an attacker chosen name. Square brackets of an IPv6 literal (`[::1]:8080`
         * style) are accepted, a zone id (`fe80::1%eth0`) is dropped.
         *
         * @return The address as bytes (4 for IPv4, 16 for IPv6), or null if it isn't an ip literal.
         */
        internal fun parseAddress(address: String?): ByteArray? {
            var value = address?.trim() ?: return null
            if (value.isEmpty()) {
                return null
            }
            value = value.removeSurrounding("[", "]").substringBefore('%')
            if (value.isEmpty()) {
                return null
            }
            // Only literals: a digit or a colon at the start rules out a host name (a hostname may contain
            // digits, but must not start with a colon and must not consist of digits and dots only).
            if (!value[0].isDigit() && value[0] != ':') {
                return null
            }
            return try {
                // Since Java 22 an ip literal is required by getByName if the string looks like one, but on
                // older versions a dns lookup for e. g. "1.2.3.4.5" would happen, so check the format first.
                if (value.contains(':')) {
                    if (!value.all { it.isDigit() || it == ':' || it == '.' || it in 'a'..'f' || it in 'A'..'F' }) {
                        return null
                    }
                } else if (!value.all { it.isDigit() || it == '.' }) {
                    return null
                }
                InetAddress.getByName(value).address
            } catch (ex: Exception) {
                null
            }
        }
    }
}
