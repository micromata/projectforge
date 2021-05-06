/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.config

import mu.KotlinLogging
import org.projectforge.common.StringHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.core.SessionCsrfCache
import org.projectforge.ui.ValidationError
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.io.InputStream
import java.net.InetAddress
import java.net.UnknownHostException
import javax.servlet.Filter
import javax.servlet.FilterRegistration
import javax.servlet.ServletContext
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

object RestUtils {
  @JvmStatic
  fun registerFilter(
    sc: ServletContext,
    name: String,
    filterClass: Class<out Filter?>,
    isMatchAfter: Boolean,
    vararg patterns: String?
  ): FilterRegistration {
    val filterRegistration: FilterRegistration = sc.addFilter(name, filterClass)
    filterRegistration.addMappingForUrlPatterns(null, isMatchAfter, *patterns)
    log.info(
      "Registering filter '" + name + "' of class '" + filterClass.name + "' for urls: " + StringHelper.listToString(
        ", ",
        *patterns
      )
    )
    return filterRegistration
  }

  /**
   * Checks the CSRF token. If the user is logged in by an authenticationToken [RestAuthenticationInfo.loggedInByAuthenticationToken] and the CSRF token is missed no check will be done.
   * Therefore pure Rest clients may not care about the CSRF token.
   */
  @JvmStatic
  fun checkCsrfToken(
    request: HttpServletRequest,
    sessionCsrfCache: SessionCsrfCache,
    csrfToken: String?,
    logInfo: String,
    logData: Any?
  ): ValidationError? {
    if (csrfToken.isNullOrBlank() && ThreadLocalUserContext.getUserContext()?.loggedInByAuthenticationToken == true) {
      if (log.isDebugEnabled) {
        log.debug { "User '${ThreadLocalUserContext.getUser()?.username}' logged in by rest call, not by session." }
      }
      return null
    }
    if (!sessionCsrfCache.checkToken(request, csrfToken)) {
      log.warn("Check of CSRF token failed, a validation error will be shown. $logInfo declined: ${logData}")
      return ValidationError.create("errorpage.csrfError")
    }
    return null
  }

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

  fun downloadFile(filename: String, inputStream: InputStream): ResponseEntity<InputStreamResource> {
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/octet-stream"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filename.replace('"', '_')}\"")
      .body(InputStreamResource(inputStream))
  }

  fun downloadFile(filename: String, content: String): ResponseEntity<String> {
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/octet-stream"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filename.replace('"', '_')}\"")
      .body(content)
  }

  fun downloadFile(filename: String, resource: ByteArrayResource): ResponseEntity<Resource> {
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/octet-stream"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filename.replace('"', '_')}\"")
      .body(resource)
  }

  fun downloadFile(filename: String, ba: ByteArray): ResponseEntity<Resource> {
    return downloadFile(filename, ByteArrayResource(ba))
  }

  fun badRequest(message: String): ResponseEntity<String> {
    return ResponseEntity.badRequest().body(message)
  }
}
