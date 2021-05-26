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
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.CoreConstants
import org.apache.commons.lang3.ClassUtils
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * For easier serialization: JSON
 */
class LoggingEventData(event: ILoggingEvent, val id: Int) : Cloneable {
  val level: LogLevel = LogLevel.getLevel(event)
  var message: String? = event.formattedMessage
  val messageObjectClass: Class<*> = event.message.javaClass
  val loggerName: String? = event.loggerName
  val timestampMillis: Long = event.timeStamp
  val isoTimestamp: String
    get() = isoDateTimeFormatterMinutes.format(Instant.ofEpochMilli(timestampMillis))
  val javaClass: String = event.callerData[0].className
  val javaClassSimpleName: String? = ClassUtils.getShortClassName(event.callerData[0].className)
  val lineNumber = event.callerData[0].lineNumber
  val methodName: String = event.callerData[0].methodName
  var stackTrace: String? = null
    private set
  var user: String? = null
  var ip: String? = null
  var userAgent: String? = null
  var session: String? = null

  init {
    val throwableProxy = event.throwableProxy
    if (throwableProxy != null) {
      val writer = StringWriter()
      val printWriter = PrintWriter(writer)
      printWriter.append(ThrowableProxyUtil.asString(throwableProxy))
      printWriter.append(CoreConstants.LINE_SEPARATOR)
      stackTrace = writer.toString()
    }
    val mdcMap = event.mdcPropertyMap
    user = mdcMap[MDC_USER]
    session = mdcMap[MDC_SESSION]
    ip = mdcMap[MDC_IP]
    userAgent = mdcMap[MDC_USER_AGENT]
  }

  public override fun clone(): LoggingEventData {
    val clone: LoggingEventData? = try {
      super.clone() as LoggingEventData
    } catch (ex: CloneNotSupportedException) {
      throw UnsupportedOperationException(this::class.java.canonicalName + " isn't cloneable: " + ex.message, ex)
    }
    return clone!!
  }

  companion object {
    private val isoDateTimeFormatterMinutes =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
  }
}
