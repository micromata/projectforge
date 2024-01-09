/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.hc.core5.net.URLEncodedUtils
import java.net.InetAddress
import java.net.URI
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest

object WebUtils {
  @JvmStatic
  fun getClientIp(request: ServletRequest): String? {
    var remoteAddr: String? = null
    if (request is HttpServletRequest) {
      remoteAddr = request.getHeader("X-Forwarded-For")
    }
    if (remoteAddr != null) {
      if (remoteAddr.contains(",")) {
        // sometimes the header is of form client ip,proxy 1 ip,proxy 2 ip,...,proxy n ip,
        // we just want the client
        remoteAddr = remoteAddr.split(',')[0].trim({ it <= ' ' })
      }
      try {
        // If ip4/6 address string handed over, simply does pattern validation.
        InetAddress.getByName(remoteAddr)
      } catch (e: UnknownHostException) {
        remoteAddr = request.remoteAddr
      }

    } else {
      remoteAddr = request.remoteAddr
    }
    return remoteAddr
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
   * @return Absolute uri: "" -> "/", "/react" -> "/react", "/react/../rs/" -> "/rs"
   */
  fun normalizeUri(uriString: String?): String? {
    uriString ?: return null
    val path = URI(uriString).normalize().path ?: return null
    return if (path.contains("..")) {
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
    URLEncodedUtils.parse(URI(uriString), StandardCharsets.UTF_8).forEach {
      result.add(Pair(it.name, URLEncoder.encode(it.value, StandardCharsets.UTF_8)))
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
