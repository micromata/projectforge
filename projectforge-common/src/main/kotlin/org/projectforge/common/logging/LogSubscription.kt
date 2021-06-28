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
 * @param title Is also the identifier. Each user may have only one subscription with this title (first come - first serve).
 * @param user Subscribes only log events for this specific user (caused by this user).
 * @param packages Subscribes only log messages of these packages.
 */
class LogSubscription @JvmOverloads constructor(
  val title: String,
  val user: String,
  val matcher: LogEventMatcher,
  val maxSize: Int = 1000
) {
  private val queue = LogQueue(maxSize)
  private var lastActivity = System.currentTimeMillis()
  private var lastReceivedLogOrderNumber: Long? = null
  val id = ++counter

  val size
    get() = queue.size

  val lastEntryNumber: Long?
    get() = queue.newestEntry?.id

  /**
   * Resets the standard value of [LogFilter.lastReceivedLogOrderNumber] to last received value.
   * If no value is explicit set, any query will return only entries newer than the current one.
   * This method doesn't clear the queue, because e. g. Merlin serial execution data may miss some log entries
   * if user calls this method in parallel.
   */
  fun reset() {
    lastReceivedLogOrderNumber = queue.newestEntryId
  }

  fun query(filter: LogFilter, locale: Locale? = null): List<LoggingEventData> {
    if (filter.lastReceivedLogOrderNumber == null && lastReceivedLogOrderNumber != null) {
      filter.lastReceivedLogOrderNumber = lastReceivedLogOrderNumber
    }
    return queue.query(filter, locale)
  }

  internal fun processEvent(eventData: LoggingEventData) {
    if (!matches(eventData)) {
      return
    }
    queue.add(eventData)
    lastActivity = eventData.timestampMillis
  }

  private fun matches(eventData: LoggingEventData): Boolean {
    if (eventData.user != user) {
      return false
    }
    return matcher.matches(eventData)
  }

  val expired: Boolean
    get() = System.currentTimeMillis() - lastActivity > LIFETIME_MS

  companion object {
    private const val LIFETIME_MS = 60 * 60 * 1000 // On hour
    private var counter = 0

    @JvmStatic
    fun getSubscription(id: Int): LogSubscription? {
      return LoggerMemoryAppender.getInstance().getSubscription(id)
    }

    @JvmStatic
    fun ensureSubscription(
      title: String,
      user: String,
      create: (title: String, user: String) -> LogSubscription
    ): LogSubscription {
      return LoggerMemoryAppender.getInstance().ensureSubscription(title = title, user = user, create = create)
    }
  }
}
