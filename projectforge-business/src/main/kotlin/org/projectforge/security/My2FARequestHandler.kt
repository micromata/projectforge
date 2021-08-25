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
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.model.rest.RestPaths
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * Is called for all requests by UserFilter and ensure valid 2FA for every request configured in [My2FARequestConfiguration].
 */
@Service
open class My2FARequestHandler {
  @Autowired
  internal lateinit var configuration: My2FARequestConfiguration // internal for test case

  private lateinit var expiryPeriods: Array<ExpiryPeriod>

  /**
   * For caching matching urls.
   */
  internal var uriMap = mutableMapOf<String, ExpiryPeriod>()

  /**
   * For caching expiry periods for entities (WRITE:<entity>). Key is the entity name.
   * If the key is given, but the expiry period is null, then no write protection is defined.
   */
  private var entitiesWriteAccessMap = mutableMapOf<String, ExpiryPeriod?>()

  /**
   * Gets the remaining period until the current 2FA is expired, or null if no 2FA is required.
   * @return Remaining period in ms before expiring or 0, if expired. null if no 2FA is required.
   */
  fun getRemainingPeriod(request: HttpServletRequest): Long? {
    return getRemainingPeriod(request.requestURI)
  }

  internal fun getRemainingPeriod(uri: String): Long? {
    val expiryPeriod = matchesUri(uri) ?: return null // No expiryPeriod matches: return null
    return expiryPeriod.remainingPeriod()
  }

  /**
   * Gets the remaining period until the current 2FA is expired, or null if no 2FA is required.
   * @return Remaining period in ms before expiring or 0, if expired. null if no 2FA is required.
   */
  fun getRemainingPeriod4WriteAccess(entity: String): Long? {
    val expiryPeriod = matchesEntity(entity) ?: return null // No expiryPeriod matches: return null
    return expiryPeriod.remainingPeriod()
  }

  /**
   * Checks for the given request, if a 2FA is required and not expired. If a 2FA is required (because it's not yet given or expired
   * for the requested url, then a redirection to a 2FA is forced.
   * @return true, if no 2FA is required (the http filter chain should be continued) or false, if a 2FA is required and the filter
   * chain shouldn't be continued.
   */
  fun handleRequest(request: HttpServletRequest, response: HttpServletResponse): Boolean {
    val expiryPeriod = matchesUri(request.requestURI) ?: return true // No expiryPeriod matches: return true.
    if (expiryPeriod.valid(request.requestURI)) {
      return true
    }
    response.sendRedirect("/react/2FA")
    return false
  }

  /**
   * Checks, if a expiry period for the given entity is configured. If so, the expiration of any 2FA of the
   * logged-in user is checked.
   * @return true, if a 2FA is now required before continuing, false, if no 2FA is required for this write access.
   */
  fun twoFactorRequiredForWriteAccess(entity: String): Boolean {
    val period = matchesEntity(entity) ?: return false
    return !period.valid("write access of $entity")
  }

  internal fun matchesEntity(entity: String): ExpiryPeriod? {
    synchronized(entitiesWriteAccessMap) {
      if (entitiesWriteAccessMap.containsKey(entity)) {
        return entitiesWriteAccessMap[entity]
      }
    }
    expiryPeriods.forEach { period ->
      synchronized(entitiesWriteAccessMap) {
        if (entitiesWriteAccessMap.size > 1000) { // Paranoia setting
          // Clean uriMap from time to time to get rid of old uris, not used very often or faulty uris.
          entitiesWriteAccessMap.clear()
        }
        if (period.writeAccessEntities.any { it == entity }) {
          entitiesWriteAccessMap[entity] = period
          entitiesWriteAccessMap[entity] = period
          return period
        } else {
          entitiesWriteAccessMap[entity] = null
        }
      }
    }
    return null
  }

  internal fun matchesUri(uri: String): ExpiryPeriod? {
    if (uri.isEmpty()) {
      return null
    }
    var result: ExpiryPeriod?
    val paths = getParentPaths(uri)
    synchronized(uriMap) {
      paths.forEach { path ->
        result = uriMap[path]
        if (result != null) {
          // uri or parent path matches:
          return result
        }
      }
    }
    expiryPeriods.forEach { period ->
      period.regexArray.forEach { regex ->
        // Search for the shortest matching parent path:
        paths.forEach { uriPath ->
          if (uriPath.matches(regex)) {
            synchronized(uriMap) {
              if (uriMap.size > 10000) {
                // Clean uriMap from time to time to get rid of old uris, not used very often or faulty uris.
                uriMap.clear()
              }
              uriMap[uriPath] = period
            }
            return period
          }
        }
      }
    }
    return null
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

  fun printConfiguration(): String {
    val sb = StringBuilder()
    sb.appendLine("  *")
    sb.appendLine("  * Please refer documentation: https://projectforge.org/docs/adminguide/#securityconfig")
    sb.appendLine("  *")
    expiryPeriods.forEach { period ->
      sb.appendLine(period.expiryPeriod)
      sb.appendLine("  config value=${period.regex}")
      period.regexArray.forEach {
        sb.appendLine("    $it")
      }
      period.writeAccessEntities.forEach { entity ->
        sb.appendLine("    WRITE:$entity")
      }
    }
    return sb.toString()
  }

  /**
   * For checking the configuration (user Administration -> System for checking).
   */
  fun printAllEndPoints(endpoints: List<String>): String {
    val sorted = endpoints.filter { it.isNotBlank() }.sorted()
    val map = mutableMapOf<String, MutableList<String>>()
    val unmatched = mutableListOf<String>()
    sorted.forEach { uri ->
      val period = matchesUri(uri)
      if (period == null) {
        unmatched.add(uri)
      } else {
        var list = map[period.expiryPeriod]
        if (list == null) {
          list = mutableListOf()
          map[period.expiryPeriod] = list
        }
        list.add(uri)
      }
    }
    val sb = StringBuilder()
    expiryPeriods.forEach { period ->
      sb.appendLine("  ${period.expiryPeriod}")
      map[period.expiryPeriod]?.forEach { uri ->
        sb.appendLine("  + ${uri}")
      }
    }
    sb.appendLine("  unmatched")
    unmatched.forEach { uri ->
      sb.appendLine("  - ${uri}")
    }
    return sb.toString()
  }

  companion object {
    @JvmField
    val MY_2FA_URL = MenuItemDefId.MY_2FA.url!!

    /**
     * For getting the shortest matching url.
     * /rs/user/save -> '/rs/user/save', '/rs/user', '/rs'
     * @return Parent paths of uri including uri itself.
     */
    internal fun getParentPaths(uri: String): List<String> {
      val result = mutableListOf<String>()
      /*var pos = uri.lastIndexOf('/', uri.length - 1)
      while (pos > 0) {
        result.add(uri.substring(0, pos))
        pos = uri.lastIndexOf('/', pos - 1)
      }*/
      var pos = uri.indexOf('/', 1)
      while (pos >= 0 && pos < uri.length) {
        result.add(uri.substring(0, pos))
        pos = uri.indexOf('/', pos + 1)
      }
      result.add(uri)
      // println("$uri: ${result.joinToString { it }}")
      return result
    }
  }

  internal class ExpiryPeriod(val regex: String?, val expiryMillis: Long, val expiryPeriod: String) {
    val regexArray: Array<Regex>
    val writeAccessEntities = mutableListOf<String>()

    init {
      val list = mutableListOf<String>()
      regex?.split(';')?.forEach { value ->
        val exp = value.trim()
        // println("regex=$regex, exp=$exp")
        if (exp.isNotBlank()) { // ignore blank expressions.
          if (!exp.startsWith("WRITE:") && exp[0].isUpperCase()) {
            // Proceed shortcuts such as ADMIN, FINANCE, ...
            var found = false
            My2FARequestConfiguration.shortCuts.forEach { (shortCut, shortCutRegex) ->
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

    /**
     * Compares [org.projectforge.framework.persistence.user.api.UserContext.lastSuccessful2FA] of
     * [ThreadLocalUserContext.getUserContext] given time stamp with current time in millis and [expiryMillis].
     * @param action Only for logging the demanded user action if 2FA is required.
     * @return true if the time stamp (epoch ms) of UserContext isn't null and isn't expired.
     */
    fun valid(action: String): Boolean {
      val user = ThreadLocalUserContext.getUserContext()
      val lastSuccessful2FA = user?.lastSuccessful2FA
      if (lastSuccessful2FA != null && lastSuccessful2FA > System.currentTimeMillis() - expiryMillis) {
        return true
      }
      log.info { "2FA is required for user '${user.user?.username}' for period '$expiryPeriod' for: $action" }
      return false
    }

    /**
     * Compares [org.projectforge.framework.persistence.user.api.UserContext.lastSuccessful2FA] of
     * [ThreadLocalUserContext.getUserContext] given time stamp with current time in millis and [expiryMillis].
     * @return Remaining period in ms if last successful 2FA isn't expired, or 0, if 2FA is expired or never done before.
     */
    fun remainingPeriod(): Long {
      val user = ThreadLocalUserContext.getUserContext()
      val lastSuccessful2FA = user?.lastSuccessful2FA
      val limit = System.currentTimeMillis() - expiryMillis
      return if (lastSuccessful2FA != null && lastSuccessful2FA > limit) {
        lastSuccessful2FA - limit
      } else {
        return 0 // 2FA is expired or was never done before.
      }
    }

    private fun addRegex(list: MutableList<String>, exp: String) {
      if (exp.startsWith("WRITE:")) {
        val entity = exp.removePrefix("WRITE:").trim()
        writeAccessEntities.add(entity)
        // Add all write access rest calls of entity:
        list.add("^/rs/$entity/${RestPaths.CLONE}.*")
        list.add("^/rs/$entity/${RestPaths.DELETE}.*")
        list.add("^/rs/$entity/${RestPaths.EDIT}.*")
        list.add("^/rs/$entity/${RestPaths.FORCE_DELETE}.*")
        list.add("^/rs/$entity/${RestPaths.MARK_AS_DELETED}.*")
        list.add("^/rs/$entity/${RestPaths.SAVE}.*")
        list.add("^/rs/$entity/${RestPaths.SAVE_OR_UDATE}.*")
        list.add("^/rs/$entity/${RestPaths.UNDELETE}.*")
        list.add("^/rs/$entity/${RestPaths.UPDATE}.*")
        list.add("^/rs/$entity/${RestPaths.WATCH_FIELDS}.*")
        return
      }
      if (exp.startsWith("/")) {
        list.add("^${exp.trim()}.*")
      } else {
        list.add(exp.trim())
      }
    }
  }
}
