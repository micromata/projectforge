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

package org.projectforge.rest

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.Constants
import org.projectforge.SystemAlertMessage
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.user.UserLocale
import org.projectforge.common.DateFormatType
import org.projectforge.framework.access.AccessChecker
import org.projectforge.jira.JiraUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.PFDateCompatibilityUtils
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.time.TimeNotation
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.pub.SystemStatusRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import jakarta.servlet.http.HttpServletRequest

/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping("${Rest.URL}/userStatus")
open class UserStatusRest {

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var systemStatusRest: SystemStatusRest

  @Autowired
  private lateinit var employeeDao: EmployeeDao

  @Autowired
  private lateinit var sessionCsrfService: SessionCsrfService

  data class UserData(
    var username: String? = null,
    var organization: String? = null,
    var fullname: String? = null,
    var lastName: String? = null,
    var firstName: String? = null,
    var userId: Long? = null,
    var employeeId: Long? = null,
    var locale: String? = null,
    var timeZone: String? = null,
    var dateFormat: String? = null,
    var dateFormatShort: String? = null,
    var timestampFormatMinutes: String? = null,
    var timestampFormatSeconds: String? = null,
    var timestampFormatMillis: String? = null,
    var jsDateFormat: String? = null,
    var jsDateFormatShort: String? = null,
    var jsTimestampFormatMinutes: String? = null,
    var jsTimestampFormatSeconds: String? = null,
    var firstDayOfWeek: DayOfWeek? = null,
    var timeNotation: TimeNotation? = null,
    var currency: String? = Constants.CURRENCY_SYMBOL,
    var thousandSeparator: Char? = null,
    var decimalSeparator: Char? = null,
  ) {
    /**
     * 0 - Sunday, 1 - Monday, ...
     */
    @get:JsonProperty
    val firstDayOfWeekSunday0: Int?
      get() = PFDateCompatibilityUtils.getCompatibilityDayOfWeekSunday0Value(firstDayOfWeek)

    /**
     * 1 - Monday, ..., 7 - Sunday
     */
    @get:JsonProperty
    val isoFirstDayOfWeekValue: Int?
      get() = PFDayUtils.getISODayOfWeekValue(firstDayOfWeek)

  }

  data class Result(
    val userData: UserData,
    val systemData: SystemStatusRest.SystemData,
    val alertMessage: String? = null,
    /**
     * CSRF token of the user's session, sent by projectforge-next in every state-changing call
     * (header [org.projectforge.rest.core.RestCsrfProtection.CSRF_TOKEN_HEADER]).
     *
     * The UILayout based clients get it per page inside `FormLayoutData.serverData` instead; next has no
     * such envelope, so it takes the token from here - the one call it makes on every app start.
     */
    val csrfToken: String? = null,
    /**
     * True if the user is a member of the admin group.
     *
     * Needed by projectforge-next, which declares its menus in the frontend and therefore has to decide itself
     * whether to offer an admin only action (e. g. rebuilding the whole search index). The UILayout based clients
     * don't need it: there the server omits such entries from the page menu (see AbstractPagesRest).
     */
    val adminUser: Boolean = false,
    /**
     * The JIRA configuration projectforge-next needs to turn issue keys into links itself (see
     * [JiraUtils.getClientConfig]), sent only where JIRA is configured. The UILayout based clients don't need it:
     * there the server emits the finished links (see JiraSupport / JiraIssuesPanel).
     */
    val jira: JiraUtils.JiraClientConfig? = null,
  )

  @GetMapping
  fun loginTest(request: HttpServletRequest): ResponseEntity<Result> {
    val user = LoginService.getUser(request) ?: return ResponseEntity(HttpStatus.UNAUTHORIZED)
    var employeeId: Long? = user.getTransientAttribute("employeeId") as Long?
    if (employeeId == null) {
      employeeId = employeeDao.findEmployeeIdByByUserId(user.id) ?: -1
      user.setTransientAttribute("employeeId", employeeId) // Avoid multiple calls of db
    }
    val firstDayOfWeek = ThreadLocalUserContext.firstDayOfWeek
    val userData = UserData(
      username = user.username,
      organization = user.organization,
      fullname = user.getFullname(),
      firstName = user.firstname,
      lastName = user.lastname,
      userId = user.id,
      employeeId = employeeId,
      locale = ThreadLocalUserContext.localeAsString,
      timeZone = ThreadLocalUserContext.timeZone.id,
      timeNotation = DateFormats.ensureAndGetDefaultTimeNotation(),
      dateFormat = DateFormats.getFormatString(DateFormatType.DATE),
      dateFormatShort = DateFormats.getFormatString(DateFormatType.DATE_SHORT),
      timestampFormatMinutes = DateFormats.getFormatString(DateFormatType.DATE_TIME_MINUTES),
      timestampFormatSeconds = DateFormats.getFormatString(DateFormatType.DATE_TIME_SECONDS),
      timestampFormatMillis = DateFormats.getFormatString(DateFormatType.DATE_TIME_MILLIS),
      firstDayOfWeek = firstDayOfWeek,
      thousandSeparator = DecimalFormatSymbols(UserLocale.determineUserLocale()).groupingSeparator,
      decimalSeparator = DecimalFormatSymbols(UserLocale.determineUserLocale()).decimalSeparator,

      )
    userData.jsDateFormat = convertToJavascriptFormat(userData.dateFormat)
    userData.jsDateFormatShort = convertToJavascriptFormat(userData.dateFormatShort)
    userData.jsTimestampFormatMinutes = convertToJavascriptFormat(userData.timestampFormatMinutes)
    userData.jsTimestampFormatSeconds = convertToJavascriptFormat(userData.timestampFormatSeconds)

    val systemData = systemStatusRest.systemData
    // Rest clients authenticated by token have no session and need no CSRF token (see RestCsrfProtection).
    val csrfToken = request.getSession(false)?.let { sessionCsrfService.createServerData(request).csrfToken }
    return ResponseEntity<Result>(
      Result(
        userData,
        systemData,
        SystemAlertMessage.alertMessage,
        csrfToken,
        accessChecker.isLoggedInUserMemberOfAdminGroup,
        JiraUtils.getClientConfig().takeIf { it.configured },
      ),
      HttpStatus.OK
    )
  }

  companion object {
    /**
     * 'dd.MM.yyyy HH:mm:ss' -> 'DD.MM.YYYY HH:mm:ss'.
     */
    fun convertToJavascriptFormat(dateFormat: String?): String? {
      if (dateFormat == null) return null
      return dateFormat.replace('d', 'D', false)
        .replace('y', 'Y', false)
    }
  }
}
