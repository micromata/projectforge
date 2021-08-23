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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.model.rest.RestPaths
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Is called for all requests by UserFilter and ensure valid 2FA for every request configured in [TwoFactorAuthenticationConfiguration].
 */
@Service
open class TwoFactorAuthenticationHandler {
  internal lateinit var configuration: TwoFactorAuthenticationConfiguration // internal for test case
  private lateinit var expiryPeriods: Array<ExpiryPeriod>
  private var uriMap = mutableMapOf<String, ExpiryPeriod>()
  private var ignoreUris = mutableListOf<String>()

  /**
   * Checks for the given request, if a 2FA is required and not expired. If a 2FA is required (because it's not yet given or expired
   * for the requested url, then a redirection to a 2FA is forced.
   * @return true, if no 2FA is required (the http filter chain should be continued) or false, if a 2FA is required and the filter
   * chain shouldn't be continued.
   */
  fun handleRequest(request: HttpServletRequest): Boolean {
    val expiryPeriod = matches(request.requestURI)
    return true
  }

  internal fun matches(uri: String): ExpiryPeriod? {
    if (uri.isEmpty()) {
      return null
    }
    synchronized(ignoreUris) {
      if (ignoreUris.contains(uri)) {
        return null
      }
    }
    var result: ExpiryPeriod? = null
    synchronized(uriMap) {
      result = uriMap[uri]
    }
    if (result != null) {
      return result
    }
    expiryPeriods.forEach { period ->
      period.regexArray.forEach { regex ->
        if (uri.matches(regex)) {
          synchronized(uriMap) {
            if (uriMap.size > 10000) {
              // Clean uriMap from time to time to get rid of old uris, not used very often or faulty uris.
              uriMap.clear()
            }
            uriMap[uri] = period
          }
          return period
        }
      }
    }
    synchronized(ignoreUris) {
      if (ignoreUris.size > 100000) {
        // Clean ignoreUris from time to time to get rid of old uris, not used very often or faulty uris.
        ignoreUris.clear()
      }
      ignoreUris.add(uri)
    }
    return null;
  }

  @PostConstruct
  internal fun postConstruct() {
    expiryPeriods = arrayOf(
      ExpiryPeriod(configuration.expiryPeriodMinutes1, AbstractCache.TICKS_PER_MINUTE, "minutes1"),
      ExpiryPeriod(configuration.expiryPeriodMinutes10, AbstractCache.TICKS_PER_MINUTE * 10, "minutes10"),
      ExpiryPeriod(configuration.expiryPeriodHours1, AbstractCache.TICKS_PER_HOUR, "hours1"),
      ExpiryPeriod(configuration.expiryPeriodHours8, AbstractCache.TICKS_PER_HOUR * 8, "hours8"),
      ExpiryPeriod(configuration.expiryPeriodDays30, AbstractCache.TICKS_PER_DAY * 30, "days30"),
      ExpiryPeriod(configuration.expiryPeriodDays90, AbstractCache.TICKS_PER_DAY * 90, "days90"),
    )
  }

  internal class ExpiryPeriod(regex: String?, val expiryMillis: Long, expiryPeriod: String) {
    val regexArray: Array<Regex>

    init {
      val list = mutableListOf<String>()
      regex?.split(';')?.forEach { value ->
        val exp = value.trim()
        // println("regex=$regex, exp=$exp")
        if (!exp.isBlank()) {
          if (!exp.startsWith("WRITE:") && exp[0].isUpperCase()) {
            // Proceed short cuts such as ADMIN, FINANCE, ...
            var found = false;
            TwoFactorAuthenticationConfiguration.shortCuts.forEach { shortCut, shortCutRegex ->
              if (!found && exp == shortCut) {
                found = true
                shortCutRegex.split(';').forEach { shortCutExp ->
                  // println("regex=$regex, exp=$exp, regexp=$shortCutRegex, shortCutExp: $shortCutExp")
                  addRegex(list, shortCutExp)
                }
              }
            }
            if (!found) {
              // Started with uppercase  but not of list "ADMIN, FINANCE, ..."
              log.warn { "Configuration error for projectforge.2fa.expiryPeriod.$expiryPeriod: '$exp' not defined." }
              addRegex(list, exp)
            }
          } else {
            // Regex
            addRegex(list, exp)
          }
        }
      }
      regexArray = list.map { it.toRegex() }.toTypedArray()
    }

    private fun addRegex(list: MutableList<String>, exp: String){
      if (exp.startsWith("WRITE:")) {
        val entity = exp.removePrefix("WRITE:").trim()
        // Add all write access rest calls of entity:
        list.add("/rs/$entity/${RestPaths.CLONE}")
        list.add("/rs/$entity/${RestPaths.DELETE}")
        list.add("/rs/$entity/${RestPaths.EDIT}")
        list.add("/rs/$entity/${RestPaths.FORCE_DELETE}")
        list.add("/rs/$entity/${RestPaths.MARK_AS_DELETED}")
        list.add("/rs/$entity/${RestPaths.SAVE}")
        list.add("/rs/$entity/${RestPaths.SAVE_OR_UDATE}")
        list.add("/rs/$entity/${RestPaths.UNDELETE}")
        list.add("/rs/$entity/${RestPaths.UPDATE}")
        list.add("/rs/$entity/${RestPaths.WATCH_FIELDS}")
      }
      if (exp.startsWith("/")) {
        list.add("^${exp.trim()}.*")
      } else {
        list.add(exp.trim())
      }
    }
  }
}
