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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.login.LoginService
import org.projectforge.model.rest.RestPaths
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

internal class My2FAExpiryPeriod(
  val regex: String?,
  val expiryMillis: Long,
  val expiryPeriod: String,
  shortCuts: Map<String, String>
) {
  val regexArray: Array<Regex>
  val writeAccessEntities = mutableListOf<String>()
  val usedShortCuts = mutableSetOf<String>()

  init {
    val list = mutableListOf<String>()
    regex?.split(';')?.forEach { value ->
      val exp = value.trim()
      // println("regex=$regex, exp=$exp")
      if (exp.isNotBlank()) { // ignore blank expressions.
        if (!exp.startsWith("WRITE:") && exp[0].isUpperCase()) {
          // Proceed shortcuts such as ADMIN, FINANCE, ...
          var found = false
          shortCuts.forEach { (shortCut, shortCutRegex) ->
            if (!found && exp == shortCut) {
              found = true
              usedShortCuts.add(shortCut)
              shortCutRegex.split(';').forEach { shortCutExp ->
                // println("regex=$regex, exp=$exp, regexp=$shortCutRegex, shortCutExp: $shortCutExp")
                addRegex(list, shortCutExp)
              }
            }
          }
          if (!found) {
            // Started with uppercase  but not of list "ADMIN, FINANCE, ..."
            log.error { "Configuration error for projectforge.2fa.expiryPeriod.$expiryPeriod: '$exp' not defined." }
            //
            // ********** SECURITY SHUTDOWN **********
            //
            // ProjectForge might not work as expected, so shutdown and inform the administrator.
            //
            SecurityShutdown.shutdownSystemOnFatalError(
              log, "Configuration error in projectforge.2fa.expiryPeriod.$expiryPeriod:",
              "'$exp' not defined."
            )
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
  internal fun valid(action: String, request: HttpServletRequest? = null): Boolean {
    var userContext = ThreadLocalUserContext.userContext
    if (userContext == null && request != null) {
      userContext = LoginService.getUserContext(request, false)
    }
    if (userContext == null) {
      return false
    }
    val lastSuccessful2FA = userContext.lastSuccessful2FA
    if (lastSuccessful2FA != null && lastSuccessful2FA > System.currentTimeMillis() - expiryMillis) {
      return true
    }
    log.info { "2FA is required for user '${userContext.user?.username}' for period '$expiryPeriod' for: $action" }
    return false
  }

  /**
   * Compares [org.projectforge.framework.persistence.user.api.UserContext.lastSuccessful2FA] of
   * [ThreadLocalUserContext.getUserContext] given time stamp with current time in millis and [expiryMillis].
   * @return Remaining period in ms if last successful 2FA isn't expired, or 0, if 2FA is expired or never done before.
   */
  fun remainingPeriod(): Long {
    val user = ThreadLocalUserContext.userContext
    val lastSuccessful2FA = user?.lastSuccessful2FA
    val limit = System.currentTimeMillis() - expiryMillis
    return if (lastSuccessful2FA != null && lastSuccessful2FA > limit) {
      lastSuccessful2FA - limit
    } else {
      return 0 // 2FA is expired or was never done before.
    }
  }

  private fun addRegex(list: MutableList<String>, exp: String) {
    if (exp.isBlank()) {
      // Occurs on split (..;..;) by trailing ';'
      return
    }
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
      list.add("^/rs/${entity}Selected/.*")
      return
    }
    if (exp.startsWith("/")) {
      list.add("^${exp.trim()}.*")
    } else {
      list.add(exp.trim())
    }
  }
}
