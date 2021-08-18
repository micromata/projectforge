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
import org.projectforge.business.user.filter.UserFilter
import javax.servlet.http.HttpServletRequest

private val accessLog = KotlinLogging.logger("de.micromata.projectforge.accessLog")
private val securityLog = KotlinLogging.logger("de.micromata.projectforge.securityLog")

object SecurityLogging {
  /**
   * Writes info message to accessLog.
   * @param request Needed for logging client ip (remoteAddr), user-agent, username.
   * @param caller Source class
   * @return The build message including request infos.
   */
  @JvmStatic
  fun logAccessInfo(request: HttpServletRequest, caller: Class<*>): String {
    return logInfo(request, caller, logAccess = true, logSecurity = false)
  }

  /**
   * Writes info message to accessLog and/or securityLog.
   * @param request Needed for logging client ip (remoteAddr), user-agent, username.
   * @param caller Source class
   * @param logAccess If true, the log info message will be written to accessLog.
   * @param logSecurity If true, the log info message will be written to securityLog.
   * @return The build message including request infos.
   */
  @JvmStatic
  @JvmOverloads
  fun logInfo(
    request: HttpServletRequest,
    caller: Class<*>,
    logAccess: Boolean = true,
    logSecurity: Boolean = false,
  ): String {
    val msg = "${getLogInfo(request)}, ${getMessagePart(caller)}"
    if (logAccess) {
      accessLog.info(msg)
    }
    if (logSecurity) {
      securityLog.info(msg)
    }
    return msg
  }

  /**
   * Writes warn message to securityLog.
   * @param request Needed for logging client ip (remoteAddr), user-agent, username.
   * @param caller Source class
   * @param title The title of the message (start string encapsulated in *** <TITLE> ***.
   * @param msg Optional message attached to the end of the logging message.
   * @return The build message including request infos.
   */
  @JvmStatic
  fun logSecurityWarn(
    request: HttpServletRequest,
    caller: Class<*>,
    title: String,
    msg: String? = null
  ): String {
    return logWarn(request, caller, title, logAccess = false, logSecurity = true, msg = msg)
  }

  /**
   * Writes warn message to securityLog.
   * @param caller Source class
   * @param title The title of the message (start string encapsulated in *** <TITLE> ***.
   * @param msg Optional message attached to the end of the logging message.
   * @return The build message including request infos.
   */
  @JvmStatic
  fun logSecurityWarn(caller: Class<*>, title: String, msg: String? = null): String {
    val msg = "*** $title *** ${getMessagePart(caller, msg)}"
    securityLog.warn(msg)
    return msg
  }

  /**
   * Writes warn message to accessLog and/or securityLog.
   * @param request Needed for logging client ip (remoteAddr), user-agent, username.
   * @param caller Source class
   * @param title The title of the message (start string encapsulated in *** <TITLE> ***.
   * @param logAccess If true, the log warn message will be written to accessLog.
   * @param logSecurity If true, the log warn message will be written to securityLog.
   * @param msg Optional message attached to the end of the logging message.
   * @return The build message including request infos.
   */
  @JvmStatic
  @JvmOverloads
  fun logWarn(
    request: HttpServletRequest,
    caller: Class<*>,
    title: String,
    logAccess: Boolean = false,
    logSecurity: Boolean = true,
    msg: String? = null,
  ): String {
    val msg = "*** $title *** ${getLogInfo(request)} ${getMessagePart(caller, msg)}"
    if (logAccess) {
      accessLog.warn(msg)
    }
    if (logSecurity) {
      securityLog.warn(msg)
    }
    return msg
  }

  private fun getLogInfo(request: HttpServletRequest): String {
    val username = UserFilter.getUserContext(request, false)?.user?.username
    return "ip=[${request.remoteAddr}], user=[${username ?: "???"}], url=[${request.requestURL}], method=[${request.method}], agent=[${
      request.getHeader(
        "User-Agent"
      )
    }]"
  }

  private fun getMessagePart(caller: Class<*>, info: String? = null): String {
    return if (info.isNullOrBlank()) {
      "source=[${caller.name}]"
    } else {
      "info=[$info], source=[${caller.name}]"
    }
  }
}
