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


package org.projectforge.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class LogQueueContextTest {
    private val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

    @Test
    fun `reads the accepted events around an event, oldest first`() {
        val queue = LogQueue(5)
        // 1 drops out: the queue holds the last five (2..6).
        (1L..6L).forEach { queue.add(event(it, if (it % 2 == 0L) "kai" else "anna")) }
        fun messages(events: List<LoggingEventData>) = events.map { it.message }

        val all = queue.context(4, 1, 1) { _, _ -> true }!!
        Assertions.assertEquals("message 4", all.hit.message)
        Assertions.assertEquals(listOf("message 3"), messages(all.before))
        Assertions.assertEquals(listOf("message 5"), messages(all.after))
        Assertions.assertTrue(all.moreBefore)
        Assertions.assertTrue(all.moreAfter)

        val sameUser = queue.context(4, 5, 5) { event, hit -> event.user == hit.user }!!
        Assertions.assertEquals(listOf("message 2"), messages(sameUser.before))
        Assertions.assertEquals(listOf("message 6"), messages(sameUser.after))
        Assertions.assertFalse(sameUser.moreBefore)
        Assertions.assertFalse(sameUser.moreAfter)

        Assertions.assertNull(queue.context(1, 1, 1) { _, _ -> true }, "No longer in the queue.")
    }

    private fun event(id: Long, user: String): LoggingEventData {
        val event = LoggingEvent(javaClass.name, loggerContext.getLogger("org.projectforge.Foo"), Level.INFO, "message $id", null, null)
            .apply { callerData = arrayOf(StackTraceElement("org.projectforge.Foo", "bar", "Foo.kt", 42)) }
        return LoggingEventData(event, id).also { it.user = user }
    }
}
