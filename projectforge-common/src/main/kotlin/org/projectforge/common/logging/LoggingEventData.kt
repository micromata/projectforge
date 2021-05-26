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
import java.text.SimpleDateFormat
import java.util.*

/**
 * For easier serialization: JSON
 */
class LoggingEventData(event: ILoggingEvent) : Cloneable {
  var id = 0
  var level: LogLevel = LogLevel.getLevel(event)
  var message: String? = event.formattedMessage
  var messageObjectClass: String? = event.message.javaClass.toString()
    private set
  var loggerName: String? = event.loggerName
    private set
  var logDate: String? = getIsoLogDate(event.timeStamp)
    private set
  var javaClass: String? = null
  var javaClassSimpleName: String? = null
    private set
  var lineNumber = 0
    private set
  var methodName: String? = null
    private set
  var stackTrace: String? = null
    private set

  init {
    val info = event.callerData[0]
    val throwableProxy = event.throwableProxy
    if (throwableProxy != null) {
      val writer = StringWriter()
      val printWriter = PrintWriter(writer)
      printWriter.append(ThrowableProxyUtil.asString(throwableProxy))
      printWriter.append(CoreConstants.LINE_SEPARATOR)
      stackTrace = writer.toString()
    }
    if (info != null) {
      javaClass = info.className
      javaClassSimpleName = ClassUtils.getShortClassName(info.className)
      lineNumber = info.lineNumber
      methodName = info.methodName
    }
  }

  private fun getIsoLogDate(millis: Long): String {
    synchronized(ISO_DATEFORMAT) { return ISO_DATEFORMAT.format(Date(millis)) }
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
    private val ISO_DATEFORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  }
}
