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

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import java.util.*

private val log = KotlinLogging.logger {}

class LoggerMemoryAppender : AppenderBase<ILoggingEvent?>() {
  private var lastLogEntryOrderNumber = -1

  var queue = FiFoBuffer<LoggingEventData>(QUEUE_SIZE)

  override fun append(event: ILoggingEvent?) {
    val eventData = LoggingEventData(event!!, ++lastLogEntryOrderNumber)
    queue.add(eventData)
  }

  /**
   * For testing purposes.
   *
   * @param event
   */
  fun append(event: LoggingEventData) {
    queue.add(event)
  }

  fun query(filter: LogFilter, locale: Locale? = null): List<LoggingEventData> {
    val result: MutableList<LoggingEventData> = ArrayList()
    var counter = 0
    //I18n i18n = CoreI18n.getDefault().get(locale);
    if (filter.isAscendingOrder) {
      for (i in 0 until queue.size) {
        val resultEvent = getResultEvent(filter, queue[i], locale) ?: continue
        result.add(resultEvent)
        if (++counter > filter.maxSize) break
      }
    } else {
      for (i in queue.size downTo 0) {
        val resultEvent = getResultEvent(filter, queue[i], locale) ?: continue
        result.add(resultEvent)
        if (++counter > filter.maxSize) break
      }
    }
    return result
  }

  private fun getResultEvent(filter: LogFilter, event: LoggingEventData?, locale: Locale?): LoggingEventData? {
    if (event == null) {
      return null
    }
    if (!event.level.matches(filter.threshold)) {
      return null
    }
    filter.lastReceivedLogOrderNumber?.let { lastReceivedLogOrderNumber ->
      if (event.id <= lastReceivedLogOrderNumber) {
        return null
      }
    }
    var logString: String? = null
    val message = event.message
    val localizedMessage = false
    /*if (message != null && message.startsWith("i18n=")) {
            I18nLogEntry i18nLogEntry = I18nLogEntry.parse(message);
            message = i18n.formatMessage(i18nLogEntry.getI18nKey(), (Object[])i18nLogEntry.getArgs());
            localizedMessage = true;
        }*/if (StringUtils.isNotBlank(filter.search)) {
      val sb = StringBuilder()
      sb.append(event.isoTimestamp)
      append(sb, event.level, true)
      append(sb, message, true)
      append(sb, event.javaClass, true)
      append(sb, event.stackTrace, filter.isShowStackTraces)
      logString = sb.toString()
    }
    if (logString == null || matches(logString, filter.search)) {
      var resultEvent: LoggingEventData = event
      if (localizedMessage) {
        // Need a clone
        resultEvent = event.clone()
        resultEvent.message = message
      }
      return resultEvent
    }
    return null
  }

  private fun append(sb: StringBuilder, value: Any?, append: Boolean) {
    if (!append || value == null) {
      return
    }
    sb.append("|#|").append(value)
  }

  private fun matches(str: String?, searchString: String?): Boolean {
    if (str.isNullOrBlank()) {
      return searchString.isNullOrBlank()
    }
    return if (searchString.isNullOrBlank()) {
      true
    } else str.contains(searchString, ignoreCase = true)
  }

  companion object {
    private const val MAX_RESULT_SIZE = 1000
    private const val QUEUE_SIZE = 10000
    private var instance: LoggerMemoryAppender? = null

    fun getInstance(): LoggerMemoryAppender {
      return instance!!
    }
  }

  /**
   * Initialized by logback on start-up (see logback-spring.xml).
   */
  init {
    if (instance != null) {
      log.warn { "*** LoggerMemoryAppender instantiated twice! Shouldn't occur. ***" }
    } else {
      instance = this
    }
  }
}
