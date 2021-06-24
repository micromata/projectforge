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

import java.util.*

/**
 * LogSubscriptions may be registered for collecting log message for users of special functionality (e. g. used by Merlin plugin).
 * Please take care to not expose to much log messages for the users!
 * LogSubscriptions will be deleted automatically after 1 hour of inactivity.
 * @param user Subscribes only log events for this specific user (caused by this user).
 * @param packages Subscribes only log messages of these packages.
 */
class LogSubscription(val user: String, vararg packages: String) {
  private val queue = LogQueue(100)
  private var lastActivity = System.currentTimeMillis()
  private val packageArray: Array<out String> = packages
  val id = ++counter

  fun query(filter: LogFilter, locale: Locale? = null): List<LoggingEventData> {
    return queue.query(filter, locale)
  }

  internal fun processEvent(eventData: LoggingEventData) {
    if (!matches(eventData)) {
      return
    }
    queue.add(eventData)
    lastActivity = eventData.timestampMillis
  }

  internal fun matches(user: String, packages: Array<out String>): Boolean {
    return this.user == user && packages.contentEquals(this.packageArray)
  }

  private fun matches(eventData: LoggingEventData): Boolean {
    if (eventData.user != user) {
      return false
    }
    packageArray.forEach {
      if (eventData.loggerName?.startsWith(it) == true) {
        return true
      }
    }
    return false
  }

  val expired: Boolean
    get() = System.currentTimeMillis() - lastActivity > LIFETIME_MS

  companion object {
    private const val LIFETIME_MS = 60 * 60 * 1000 // On hour
    private var counter = 0

    fun ensureSubscription(user: String, vararg packages: String): LogSubscription {
      return LoggerMemoryAppender.getInstance().ensureSubscription(match = { subscription ->
        subscription.matches(user, packages)
      },
        create = { -> LogSubscription(user, *packages) })
    }

    fun getSubscription(user: String, vararg packages: String): LogSubscription? {
      return LoggerMemoryAppender.getInstance().getSubscription { subscription -> subscription.matches(user, packages) }
    }

    fun getSubscription(id: Int): LogSubscription? {
      return LoggerMemoryAppender.getInstance().getSubscription { subscription -> subscription.id == id }
    }
  }
}
