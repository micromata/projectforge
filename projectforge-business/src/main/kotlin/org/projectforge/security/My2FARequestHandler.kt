/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.Constants
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * Is called for all requests by UserFilter and ensure valid 2FA for every request configured in [My2FARequestConfiguration].
 */
@Service
open class My2FARequestHandler {
  @Autowired
  private lateinit var configuration: My2FARequestConfiguration

  private val shortCuts = mutableMapOf<String, String>()

  private var expiryPeriods = mutableListOf<My2FAExpiryPeriod>()

  // expiryPeriods are dirty, if shortCuts were modified.
  private var expiryPeriodsDirty = true

  /**
   * For caching matching urls.
   */
  internal var uriMap = mutableMapOf<String, My2FAExpiryPeriod>()

  /**
   * For caching expiry periods for entities (WRITE:<entity>). Key is the entity name.
   * If the key is given, but the expiry period is null, then no write protection is defined.
   */
  private var entitiesWriteAccessMap = mutableMapOf<String, My2FAExpiryPeriod?>()

  /**
   * For caching expiry periods for entities (ENTITY:<entity>). Key is the entity name.
   * If the key is given, but the expiry period is null, then no entity protection is defined.
   */
  private var entitiesAccessMap = mutableMapOf<String, My2FAExpiryPeriod?>()

  private var my2FAPage: My2FAPage? = null

  /**
   * Gets the remaining period until the current 2FA is expired, or null if no 2FA is required.
   * @return Remaining period in ms before expiring or 0, if expired. null if no 2FA is required.
   */
  fun getRemainingPeriod(request: HttpServletRequest): Long? {
    return getRemainingPeriod(request.requestURI)
  }

  fun getRemainingPeriod(uri: String): Long? {
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
   * for the requested url, then a redirection to a 2FA is forced (if [sendRedirect] is true). If [sendRedirect]
   * is false, the caller has to proceed the redirection to the 2FA page.
   * @param sendRedirect If true (default), a redirection is done, if 2FA is required.
   * @return null, if no 2FA is required (the http filter chain should be continued) or the expiry period in millis, if a 2FA is required
   * and the filter chain shouldn't be continued.
   */
  @JvmOverloads
  fun handleRequest(request: HttpServletRequest, response: HttpServletResponse, sendRedirect: Boolean = true): Long? {
    val expiryPeriod = matchesUri(request.requestURI) ?: return null // No expiryPeriod matches: return true.
    if (expiryPeriod.valid(request.requestURI, request)) {
      return null
    }
    if (my2FAPage == null) {
      throw java.lang.Exception("my2FAPage not set (should be My2FAPageRest)!")
    }
    if (sendRedirect) {
      my2FAPage!!.redirect(request, response, expiryPeriod.expiryMillis)
    }
    return expiryPeriod.expiryMillis
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

  private fun getExpiryPeriodForShortCut(shortCut: My2FAShortCut): My2FAExpiryPeriod? {
    synchronized(shortCuts) {
      if (expiryPeriodsDirty) {
        reload()
      }
    }
    expiryPeriods.forEach { period ->
      if (period.usedShortCuts.contains(shortCut.name)) {
        return period
      }
    }
    return null
  }

  private fun matchesEntity(entity: String): My2FAExpiryPeriod? {
    synchronized(entitiesWriteAccessMap) {
      if (entitiesWriteAccessMap.containsKey(entity)) {
        return entitiesWriteAccessMap[entity]
      }
    }
    synchronized(shortCuts) {
      if (expiryPeriodsDirty) {
        reload()
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

  internal fun matchesUri(uri: String): My2FAExpiryPeriod? {
    if (uri.isEmpty()) {
      return null
    }
    synchronized(shortCuts) {
      if (expiryPeriodsDirty) {
        reload()
      }
    }
    var result: My2FAExpiryPeriod?
    val normalizedUri = WebUtils.normalizeUri(uri) ?: uri
    if (no2FAUrl(normalizedUri)) {
      // /rs/userStatus and /rs/2FA/ should never require 2FA.
      return null
    }
    val paths = getParentPaths(normalizedUri)
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
    // invoiceSelected: massUpate
    return null
  }

  /**
   * Should be called, if any modification was done.
   */
  fun reload() {
    val periods = mutableListOf<My2FAExpiryPeriod>()
    addPeriod(periods, configuration.expiryPeriodMinutes1, AbstractCache.TICKS_PER_MINUTE, "minutes1")
    addPeriod(periods, configuration.expiryPeriodMinutes10, AbstractCache.TICKS_PER_MINUTE * 10, "minutes10")
    addPeriod(periods, configuration.expiryPeriodHours1, AbstractCache.TICKS_PER_HOUR, "hours1")
    addPeriod(periods, configuration.expiryPeriodHours8, AbstractCache.TICKS_PER_HOUR * 8, "hours8")
    addPeriod(periods, configuration.expiryPeriodDays30, AbstractCache.TICKS_PER_DAY * 30, "days30")
    addPeriod(periods, configuration.expiryPeriodDays90, AbstractCache.TICKS_PER_DAY * 90, "days90")
    expiryPeriods = periods
    expiryPeriodsDirty = false
    // Clear caches:
    synchronized(entitiesWriteAccessMap) { // Should be empty because reload is done before app is available.
      entitiesWriteAccessMap.clear()
    }
    synchronized(uriMap) {
      uriMap.clear()
    }
  }

  private fun addPeriod(
    periods: MutableList<My2FAExpiryPeriod>,
    regex: String?,
    expireMillis: Long,
    expiryPeriod: String
  ) {
    periods.add(My2FAExpiryPeriod(regex, expireMillis, expiryPeriod, shortCuts))
  }

  fun printConfiguration(): String {
    val sb = StringBuilder()
    sb.appendLine("  *")
    sb.appendLine("  * Please refer documentation: ${Constants.WEB_DOCS_ADMIN_GUIDE_SECURITY_CONFIG_LINK}")
    sb.appendLine("  *")
    synchronized(shortCuts) {
      if (expiryPeriodsDirty) {
        reload()
      }
    }
    expiryPeriods.forEach { period ->
      sb.appendLine(period.expiryPeriod)
      sb.appendLine("  config value=${period.regex}")
      period.regexArray.sortedBy { it.pattern }.forEach {
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
      if (no2FAUrl(uri)) {
        sb.appendLine("  - ${uri} (ignored: no-2FA-url, OK)")
      } else {
        sb.appendLine("  - ${uri}")
      }
    }
    return sb.toString()
  }

  fun printResolvedShortCuts(): String {
    val sb = StringBuilder()
    My2FAShortCut.values().forEach { shortCut ->
      sb.appendLine("  ${shortCut.name}")
      shortCuts[shortCut.name]?.split(";")?.sorted()?.forEach { value ->
        if (value.isNotBlank()) {
          sb.appendLine("    $value")
        }
      }
    }
    return sb.toString()
  }

  companion object {
    @JvmField
    val MY_2FA_URL = MenuItemDefId.MY_2FA.url!!

    private val NO_2FA_URLS = arrayOf("/rs/menu", "/rs/userStatus", "/rs/2FA", "/rsPublic", "/rs/webauthn/webAuthn")

    private val NO_2FA_URL_MATCHERS = arrayOf("^/rs/\\w*/autosearch".toRegex(), "^/rs/\\w*/autocomplete".toRegex())

    private fun no2FAUrl(normalizedUri: String): Boolean {
      NO_2FA_URLS.forEach { url ->
        if (normalizedUri.startsWith(url)) {
          // /rs/userStatus, /rs/2FA/, /rs/menu etc. should never require 2FA.
          return true
        }
      }
      NO_2FA_URL_MATCHERS.forEach { regexp ->
        if (normalizedUri.matches(regexp)) {
          // /rs/user/autosearch, /rs/group/autosearch, ...
          return true
        }
      }
      return false
    }

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
        result.add(0, uri.substring(0, pos))
        pos = uri.indexOf('/', pos + 1)
      }
      result.add(0, uri)
      // println("$uri: ${result.joinToString { it }}")
      return result
    }
  }

  /**
   * Register (appends) the given values to the specified shortCut.
   * @param shortCut to create or modify by appending (separated by ';').
   * @param values List of values to append (each value may contain several values separated by ';' or might be a single
   * value.
   * @return this for chaining.
   */
  fun registerShortCutValues(shortCut: My2FAShortCut, vararg values: String)
      : My2FARequestHandler {
    synchronized(shortCuts) {
      val sb = StringBuilder()
      shortCuts[shortCut.name]?.let {
        sb.append(it)
        if (!it.endsWith(";")) {
          sb.append(";")
        }
      }
      values.forEach { value ->
        sb.append(value)
        if (!value.endsWith(";")) {
          sb.append(";")
        }
      }
      shortCuts[shortCut.name] = sb.toString()
      expiryPeriodsDirty = true
    }
    return this
  }

  init {
    registerShortCutValues(My2FAShortCut.ALL, "/wa", "/rs/")
  }

  fun getShortCutResolved(shortCut: My2FAShortCut): String? {
    return shortCuts[shortCut.name]
  }

  fun register(page: My2FAPage) {
    my2FAPage = page
  }

  fun internalSet4UnitTests(configuration: My2FARequestConfiguration) {
    this.configuration = configuration
  }
}
