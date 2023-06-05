/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class LogEventLoggerNameMatcherTest {
  private var counter: Long = 0
  @Test
  fun formatBytesTest() {
    val matcher1 = LogEventLoggerNameMatcher("org.projectforge.common", "de.micromata.merlin|hurzel")
      .withBlocked("org.projectforge.common.i18n", "de.micromata.merlin|blocked")
    Assertions.assertTrue(matcher1.matches(createLogEventData("org.projectforge.common.Test", "Hello world")))
    Assertions.assertTrue(matcher1.matches(createLogEventData("de.micromata.merlin.Test", "Hello hurzel!")))
    Assertions.assertFalse(matcher1.matches(createLogEventData("org.projectforge.common.i18n.Test", "Hello hurzel!")), "org.projectforge.common.i18n should be blocked.")
    Assertions.assertFalse(matcher1.matches(createLogEventData("de.micromata.merlin.Test", "Hello blocked hurzel!")), "de.micromata.common should be blocked, if message contains 'blocked'.")
  }

  private fun createLogEventData(loggerName: String, message: String): LoggingEventData {
    val callerData = arrayOf(StackTraceElement(loggerName, "foo", "foo.kt", 42))
    val event = Mockito.mock(ILoggingEvent::class.java)
    Mockito.`when`(event.loggerName).thenReturn(loggerName)
    Mockito.`when`(event.message).thenReturn(message)
    Mockito.`when`(event.formattedMessage).thenReturn(message)
    Mockito.`when`(event.level).thenReturn(Level.INFO)
    Mockito.`when`(event.callerData).thenReturn(callerData)
    return LoggingEventData(event, counter++)
  }
}
