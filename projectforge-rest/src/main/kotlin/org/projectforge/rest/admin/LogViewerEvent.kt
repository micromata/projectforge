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

package org.projectforge.rest.admin

import org.projectforge.common.DateFormatType
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.logging.LoggingEventData
import org.projectforge.framework.time.PFDateTime

/**
 * @param userLocalTime If true, the timestamp of the log event will be displayed in user's time zone (for user view). If false (default), UTC is used.
 */
class LogViewerEvent(event: LoggingEventData, userFriendlyTime: Boolean = false) {
  val id = event.id

  @PropertyInfo(i18nKey = "timestamp")
  val timestamp = if (userFriendlyTime) {
    PFDateTime.from(event.timestampMillis).format(DateFormatType.DATE_TIME_SECONDS)
  } else {
    event.isoTimestamp
  }

  @PropertyInfo(i18nKey = "system.admin.logViewer.level")
  val level = event.level

  @PropertyInfo(i18nKey = "system.admin.logViewer.message")
  val message = event.message

  @PropertyInfo(i18nKey = "system.admin.logViewer.stacktrace")
  val stackTrace = event.stackTrace

  @PropertyInfo(i18nKey = "system.admin.logViewer.user")
  val user = "${event.user ?: ""}@${event.ip ?: ""}"

  @PropertyInfo(i18nKey = "system.admin.logViewer.userAgent")
  val userAgent = event.userAgent
}
