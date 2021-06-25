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

package org.projectforge.common.logging

/**
 * Used for configuring LogSubscriptions.
 */
interface LogEventMatcher {
  fun matches(event: LoggingEventData): Boolean
}

/**
 * Logger's name of the log event must start with at least of one of the given loggerNameBeginnings.
 *
 * LoggerNameBeginnings may also contain a '|' character. If given, the string after '|' must be part of the event message (case insensitive),
 * e. g. "de.micromata.merlin|template" matches logging events, if logger name starts with "de.micromata.merlin" and the message contains the
 * string "template".
 *
 * @param loggerNameBeginnings List of names ("de.micromata.merlin", "org.projectforge.business.timesheet.TimesheetDao", ...)
 */
class LogEventLoggerNameMatcher(vararg loggerNameBeginnings: String) : LogEventMatcher {
  private val loggerNameBeginningsArray = loggerNameBeginnings
  private var blockedLoggerNameBeginningsArray: Array<out String>? = null

  /**
   * If any entry of this array matches, the logEventData is ignored (Blocked vs. allowed list).
   * @return this for chaining.
   */
  fun withBlocked(vararg blockedLoggerNameBeginningsArray: String): LogEventLoggerNameMatcher {
    this.blockedLoggerNameBeginningsArray = blockedLoggerNameBeginningsArray
    return this
  }

  override fun matches(eventData: LoggingEventData): Boolean {
    return matches(eventData, loggerNameBeginningsArray) && !matches(eventData, blockedLoggerNameBeginningsArray)
  }

  private fun matches(eventData: LoggingEventData, array: Array<out String>?): Boolean {
    array ?: return false
    array.forEach {
      val pkg = it.substringBefore("|")
      val msgPart = it.substringAfter("|", "")
      if (eventData.loggerName?.startsWith(pkg) == true && (msgPart.isEmpty() || eventData.message?.contains(
          msgPart,
          ignoreCase = true
        ) == true)
      ) {
        return true
      }
    }
    return false
  }
}
