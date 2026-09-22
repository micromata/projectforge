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
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

object WebUtils {
  /**
   * Set on startup from `projectforge.security.trustedProxies`, see
   * [org.projectforge.web.TrustedProxiesConfiguration]. Until then nothing is trusted: `X-Forwarded-For` is only
   * ever used after the configuration was read.
   */
  @JvmStatic
  var trustedProxies = TrustedProxies(null)

  /**
   * @return The ip address of the client, taken from `X-Forwarded-For` if (and only if) the request came from a
   * trusted proxy, otherwise the address of the host the request came from.
   * @see TrustedProxies
   */
  @JvmStatic
  fun getClientIp(request: ServletRequest): String? {
    val remoteAddr = request.remoteAddr
    if (request !is HttpServletRequest) {
      return remoteAddr
    }
    val forwardedFor = request.getHeader("X-Forwarded-For") ?: return remoteAddr
    if (!trustedProxies.isTrusted(remoteAddr)) {
      // Anybody may send this header, so it is only believed if a proxy of ours sent it. Info and not warn: a
      // client (or a proxy of the client's side) sending one isn't an incident, it just isn't authoritative.
      log.info { "Ignoring X-Forwarded-For '${forwardedFor.take(100)}': '$remoteAddr' isn't a trusted proxy (projectforge.security.trustedProxies)." }
      return remoteAddr
    }
    // The header is of the form "client, proxy 1, ..., proxy n": the client is the first entry. Note that this
    // first entry is the one the client itself may have sent (a proxy appends), which is why the whole header is
    // only used at all if a trusted proxy handed it over.
    val clientAddr = forwardedFor.substringBefore(',').trim()
    if (TrustedProxies.parseAddress(clientAddr) == null) {
      log.info { "Ignoring X-Forwarded-For '${forwardedFor.take(100)}': '$clientAddr' isn't an ip address." }
      return remoteAddr
    }
    return clientAddr
  }

  /**
   * @return the uri of the request with normalized path.
   * @see normalizeUri
   */
  fun getNormalizedUri(request: HttpServletRequest): String? {
    return normalizeUri(request.requestURI)
  }

  /**
   * If an invalid relative url is found, "<invalid>" is returned (e. g. for "../react", because .. cannot
   * be resolved.
   *
   * Only a `..` of its own is a path segment that couldn't be resolved; two dots inside a file name are not
   * (Next.js build chunks such as `/next/_next/static/chunks/0b4s9fzw~-kl..js` are legal names, and treating them
   * as an attack made every page load of the app a suspicious request).
   * @return Absolute uri: "" -> "/", "/react" -> "/react", "/react/../rs/" -> "/rs"
   */
  fun normalizeUri(uriString: String?): String? {
    uriString ?: return null
    val path = URI(uriString).normalize().path ?: return null
    return if (path.split('/').any { it == ".." }) {
      "<invalid>" // login required
    } else if (path.startsWith("/")) {
      path
    } else {
      "/$path"
    }
  }

  fun parseQueryParams(uriString: String?): MutableList<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    uriString ?: return result
    val query = URI(uriString).rawQuery ?: return result
    query.split("&").forEach { param ->
      val parts = param.split("=", limit = 2)
      val name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
      val value = if (parts.size > 1) parts[1] else ""
      result.add(Pair(name, URLEncoder.encode(URLDecoder.decode(value, StandardCharsets.UTF_8), StandardCharsets.UTF_8)))
    }
    return result
  }

  fun queryParamsToString(params: List<Pair<String, String>>, withQuestionMarkPrefix: Boolean = true): String {
    if (params.isEmpty()) {
      return ""
    }
    val prefix = if (withQuestionMarkPrefix) "?" else ""
    return params.joinToString(separator = "&", prefix = prefix) { "${it.first}=${it.second}" }
  }
}
