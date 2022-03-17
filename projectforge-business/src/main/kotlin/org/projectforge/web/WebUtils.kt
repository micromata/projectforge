/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import java.net.InetAddress
import java.net.UnknownHostException
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
}
