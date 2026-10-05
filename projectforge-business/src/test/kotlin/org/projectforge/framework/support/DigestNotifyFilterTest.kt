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
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogNotify
import java.util.Date

class DigestNotifyFilterTest {
    /** The database's view, by group key (the codes of the test events). */
    private class FakeStates : LogGroupStates {
        val states = mutableMapOf<String, LogGroupState>()
        val notified = mutableListOf<String>()

        override fun stateOf(key: String) = states[key]

        override fun markNotified(keys: Collection<String>) {
            notified.addAll(keys)
            keys.forEach { key -> states[key]?.let { states[key] = it.withLastNotified(Date(NOW)) } }
        }

        fun set(
            event: LogEvent,
            status: LogGroupStatus = LogGroupStatus.NEW,
            mutedUntil: Long? = null,
            overrideNotify: LogNotify? = null,
            lastNotified: Long? = null,
            reopenedAt: Long? = null,
        ) {
            states[event.code] = LogGroupState(
                1L, event.code, status, mutedUntil?.let { Date(it) }, overrideNotify, lastNotified?.let { Date(it) },
                reopenedAt?.let { Date(it) },
            )
        }
    }

    @Test
    fun `ignored, muted and overridden problems`() {
        val states = FakeStates()
        val filter = DigestNotifyFilter(states)
        states.set(IGNORED_EVENT, status = LogGroupStatus.IGNORED)
        states.set(MUTED_EVENT, mutedUntil = NOW + 1000)
        states.set(MUTE_EXPIRED_EVENT, mutedUntil = NOW - 1000)
        states.set(SILENCED_EVENT, overrideNotify = LogNotify.NONE)
        filter.apply(snapshot(IGNORED_EVENT, MUTED_EVENT, MUTE_EXPIRED_EVENT, SILENCED_EVENT, UNKNOWN_EVENT), NOW).let {
            Assertions.assertEquals(listOf("test.muteExpired", "test.unknown"), it.groups.map { g -> g.key })
            Assertions.assertEquals(3, it.suppressed)
        }
        Assertions.assertEquals(listOf("test.muteExpired", "test.unknown"), states.notified)
    }

    @Test
    fun `DIGEST_IF_NEW is taken from the database and reported again after a regression`() {
        val states = FakeStates()
        val filter = DigestNotifyFilter(states)
        states.set(ONCE_EVENT, lastNotified = NOW - 10_000)
        Assertions.assertTrue(filter.apply(snapshot(ONCE_EVENT), NOW).groups.isEmpty(), "Reported before a restart.")

        states.set(ONCE_EVENT, lastNotified = NOW - 10_000, reopenedAt = NOW - 5_000)
        Assertions.assertEquals(1, filter.apply(snapshot(ONCE_EVENT), NOW).groups.size, "Resolved, but back again.")
        Assertions.assertTrue(filter.apply(snapshot(ONCE_EVENT), NOW).groups.isEmpty(), "And reported now.")

        // Raised by an admin: always reported.
        states.set(ONCE_EVENT, lastNotified = NOW, overrideNotify = LogNotify.DIGEST)
        Assertions.assertEquals(1, filter.apply(snapshot(ONCE_EVENT), NOW).groups.size)
    }

    @Test
    fun `without database state, DIGEST_IF_NEW is remembered in memory`() {
        val states = FakeStates()
        val filter = DigestNotifyFilter(states)
        Assertions.assertEquals(1, filter.apply(snapshot(ONCE_EVENT), NOW).groups.size)
        Assertions.assertTrue(filter.apply(snapshot(ONCE_EVENT), NOW).groups.isEmpty())
    }

    @Test
    fun `the override decides whether an occurrence is collected`() {
        val occurrence = occurrence(ONCE_EVENT)
        Assertions.assertEquals(LogNotify.DIGEST_IF_NEW, DigestNotifyFilter.notifyOf(occurrence, null))
        val state = LogGroupState(1L, "fp", LogGroupStatus.NEW, null, LogNotify.IMMEDIATE, null, null)
        Assertions.assertEquals(LogNotify.IMMEDIATE, DigestNotifyFilter.notifyOf(occurrence, state))
    }

    private fun snapshot(vararg events: LogEvent): ErrorDigestCollector.Snapshot {
        val collector = ErrorDigestCollector()
        events.forEach { collector.add(occurrence(it)) }
        return collector.drain()
    }

    private fun occurrence(event: LogEvent) = ErrorOccurrence(
        timestampMillis = NOW,
        level = LogLevel.ERROR,
        event = event,
        exceptionClass = null,
        message = "Failed",
        location = "Foo:1",
        groupByCode = true,
    )

    companion object {
        private const val NOW = 1_000_000_000L
        private val IGNORED_EVENT = LogEvent("test.ignored", LogCategory.BUG)
        private val MUTED_EVENT = LogEvent("test.muted", LogCategory.BUG)
        private val MUTE_EXPIRED_EVENT = LogEvent("test.muteExpired", LogCategory.BUG)
        private val SILENCED_EVENT = LogEvent("test.silenced", LogCategory.BUG)
        private val UNKNOWN_EVENT = LogEvent("test.unknown", LogCategory.BUG)
        private val ONCE_EVENT = LogEvent("test.once", LogCategory.EXTERNAL, notify = LogNotify.DIGEST_IF_NEW)
    }
}
