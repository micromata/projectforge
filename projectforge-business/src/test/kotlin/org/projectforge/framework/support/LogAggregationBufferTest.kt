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

package org.projectforge.framework.support

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.Constants
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogLevel

class LogAggregationBufferTest {
    @Test
    fun `counts per problem and hour, prefers a sample with stack trace`() {
        val buffer = LogAggregationBuffer()
        buffer.add(occurrence(HOUR + 10, user = "a", stackTrace = "trace"))
        buffer.add(occurrence(HOUR + 20, user = "b", level = LogLevel.WARN))
        buffer.add(occurrence(2 * HOUR + 5, user = "a", level = LogLevel.FATAL))
        buffer.add(occurrence(2 * HOUR + 6, event = OTHER))
        val groups = buffer.drain(now = 2 * HOUR + 10).associateBy { it.event.code }
        Assertions.assertEquals(2, groups.size)
        groups["test.buffer"]!!.let {
            Assertions.assertEquals(3, it.count)
            Assertions.assertEquals(LogLevel.FATAL, it.level, "The most severe level.")
            Assertions.assertEquals(HOUR + 10, it.firstMillis)
            Assertions.assertEquals(2 * HOUR + 5, it.lastMillis)
            Assertions.assertEquals("trace", it.sample.stackTrace, "The one with a stack trace, not the latest.")
            Assertions.assertEquals(mapOf(HOUR to 2, 2 * HOUR to 1), it.hours)
            Assertions.assertEquals(mapOf(HOUR to 2, 2 * HOUR to 1), it.distinctUsers)
        }
        Assertions.assertEquals(mapOf(2 * HOUR to 0), groups["test.buffer.other"]!!.distinctUsers, "No user known.")
        Assertions.assertTrue(buffer.drain().isEmpty(), "Drained.")
    }

    @Test
    fun `distinct users are counted over the flushes of an hour`() {
        val buffer = LogAggregationBuffer()
        buffer.add(occurrence(HOUR + 1, user = "a"))
        buffer.add(occurrence(HOUR + 2, user = "b"))
        Assertions.assertEquals(2, buffer.drain(now = HOUR + 3).single().distinctUsers[HOUR])
        buffer.add(occurrence(HOUR + 4, user = "a"))
        buffer.add(occurrence(HOUR + 5, user = "c"))
        buffer.drain(now = HOUR + 6).single().let {
            Assertions.assertEquals(2, it.count)
            Assertions.assertEquals(3, it.distinctUsers[HOUR], "a, b and c in this hour, a only once.")
        }
        // Two hours later, the users of the first hour are forgotten:
        buffer.drain(now = 3 * HOUR)
        buffer.add(occurrence(HOUR + 7, user = "d"))
        Assertions.assertEquals(1, buffer.drain(now = 3 * HOUR).single().distinctUsers[HOUR])

        val small = LogAggregationBuffer(maxUsersPerHour = 2)
        listOf("a", "b", "c").forEach { small.add(occurrence(HOUR, user = it)) }
        Assertions.assertEquals(2, small.drain(now = HOUR).single().distinctUsers[HOUR], "Bounded.")
    }

    @Test
    fun `bounded, and restored after a failed write`() {
        val buffer = LogAggregationBuffer(maxGroups = 2)
        buffer.add(occurrence(HOUR, location = "A:1"))
        buffer.add(occurrence(HOUR, location = "B:1"))
        buffer.add(occurrence(HOUR, location = "C:1"))
        Assertions.assertEquals(2, buffer.size)
        Assertions.assertEquals(1, buffer.dropped)
        val failed = buffer.drain(now = HOUR)

        buffer.add(occurrence(2 * HOUR, location = "A:1", stackTrace = "new"))
        buffer.add(occurrence(2 * HOUR, location = "D:1"))
        buffer.restore(failed)
        Assertions.assertEquals(2, buffer.size)
        Assertions.assertEquals(2, buffer.dropped, "B doesn't fit any more.")
        buffer.drain(now = 2 * HOUR).first { it.location == "A:1" }.let {
            Assertions.assertEquals(2, it.count, "Merged with the restored one.")
            Assertions.assertEquals(HOUR, it.firstMillis)
            Assertions.assertEquals(mapOf(HOUR to 1, 2 * HOUR to 1), it.hours)
            Assertions.assertEquals("new", it.sample.stackTrace)
        }
    }

    @Test
    fun `hour of a timestamp`() {
        Assertions.assertEquals(HOUR, PendingLogGroup.hourOf(HOUR))
        Assertions.assertEquals(HOUR, PendingLogGroup.hourOf(2 * HOUR - 1))
        Assertions.assertEquals(-HOUR, PendingLogGroup.hourOf(-1), "Before 1970 as well.")
    }

    private fun occurrence(
        millis: Long,
        event: LogEvent = EVENT,
        location: String = "Foo:1",
        user: String? = null,
        stackTrace: String? = null,
        level: LogLevel = LogLevel.ERROR,
    ) = ErrorOccurrence(
        timestampMillis = millis,
        level = level,
        event = event,
        exceptionClass = null,
        message = "Something failed",
        location = location,
        stackTrace = stackTrace,
        user = user,
    )

    companion object {
        private const val HOUR = Constants.MILLIS_PER_HOUR
        private val EVENT = LogEvent("test.buffer", LogCategory.BUG)
        private val OTHER = LogEvent("test.buffer.other", LogCategory.BUG)
    }
}
