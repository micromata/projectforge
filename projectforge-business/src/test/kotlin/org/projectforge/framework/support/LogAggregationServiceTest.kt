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
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogNotify
import org.springframework.beans.factory.annotation.Autowired

class LogAggregationServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var logAggregationService: LogAggregationService

    @Test
    fun `flush writes groups and hourly buckets, merges later flushes`() {
        val event = LogEvent("test.aggregation.flush", LogCategory.EXTERNAL, notify = LogNotify.DIGEST_IF_NEW)
        val now = System.currentTimeMillis()
        val hour = PendingLogGroup.hourOf(now)
        logAggregationService.add(occurrence(event, hour + 1, user = "a", stackTrace = "trace", level = LogLevel.WARN))
        logAggregationService.add(occurrence(event, hour + 2, user = "b"))
        logAggregationService.add(occurrence(event, hour - Constants.MILLIS_PER_HOUR, user = "a"))
        logAggregationService.flush()

        val key = ErrorDigestCollector.keyOf(occurrence(event, now))
        group(key).let {
            Assertions.assertEquals(event.code, it.code)
            Assertions.assertEquals(LogCategory.EXTERNAL, it.category)
            Assertions.assertEquals(LogLevel.ERROR, it.level)
            Assertions.assertEquals(3, it.totalCount)
            Assertions.assertEquals("trace", it.sampleStackTrace)
            Assertions.assertEquals("GET /rs/test", it.sampleRequest)
            Assertions.assertEquals(hour - Constants.MILLIS_PER_HOUR, it.firstSeen!!.time)
            Assertions.assertEquals(LogGroupStatus.NEW, it.status)
        }
        Assertions.assertEquals(listOf(1 to 1, 2 to 2), buckets(key).map { it.occurrences to it.distinctUsers })
        Assertions.assertNotNull(logAggregationService.stateOf(key), "Known after the flush.")
        Assertions.assertTrue(logAggregationService.stateOf(key)!!.unreported)

        logAggregationService.add(occurrence(event, hour + 3, user = "c"))
        logAggregationService.flush()
        group(key).let {
            Assertions.assertEquals(4, it.totalCount)
            Assertions.assertEquals("trace", it.sampleStackTrace, "Kept: the new sample has no stack trace.")
        }
        logAggregationService.add(
            occurrence(event, hour + 4, message = "M".repeat(5000), stackTrace = "S".repeat(20000)),
        )
        logAggregationService.flush()
        group(key).let {
            Assertions.assertEquals(4000, it.sampleMessage!!.length, "Truncated to the column.")
            Assertions.assertEquals(10000, it.sampleStackTrace!!.length)
        }
        Assertions.assertEquals(listOf(1 to 1, 4 to 3), buckets(key).map { it.occurrences to it.distinctUsers })

        logAggregationService.markNotified(listOf(key))
        Assertions.assertNotNull(group(key).lastNotified)
        Assertions.assertFalse(logAggregationService.stateOf(key)!!.unreported)
    }

    @Test
    fun `a resolved problem that occurs again is a regression`() {
        val event = LogEvent("test.aggregation.regression", LogCategory.BUG)
        val now = System.currentTimeMillis()
        val key = ErrorDigestCollector.keyOf(occurrence(event, now))
        logAggregationService.add(occurrence(event, now))
        logAggregationService.flush()
        logAggregationService.markNotified(listOf(key))
        updateGroup(key) { it.status = LogGroupStatus.RESOLVED }
        logAggregationService.reloadStates()
        Assertions.assertEquals(LogGroupStatus.RESOLVED, logAggregationService.stateOf(key)!!.status)

        Thread.sleep(5) // reopenedAt after lastNotified
        logAggregationService.add(occurrence(event, System.currentTimeMillis()))
        logAggregationService.flush()
        group(key).let {
            Assertions.assertEquals(LogGroupStatus.NEW, it.status)
            Assertions.assertNotNull(it.reopenedAt)
        }
        Assertions.assertTrue(logAggregationService.stateOf(key)!!.unreported, "Reported again by the digest.")
    }

    @Test
    fun `nothing is counted while writing`() {
        val event = LogEvent("test.aggregation.guard", LogCategory.BUG)
        val key = ErrorDigestCollector.keyOf(occurrence(event, 0))
        // An error logged by the database access of the flush itself, e.g. by Hibernate:
        logAggregationService.flushing.set(true)
        try {
            logAggregationService.add(occurrence(event, System.currentTimeMillis()))
        } finally {
            logAggregationService.flushing.set(false)
        }
        logAggregationService.flush()
        Assertions.assertNull(logAggregationService.stateOf(key))
    }

    @Test
    fun `nothing is written after close`() {
        // Own instance: the shared one must stay open. Closed, it doesn't touch the (not injected) database.
        val service = LogAggregationService()
        service.close()
        val event = LogEvent("test.aggregation.closed", LogCategory.BUG)
        service.add(occurrence(event, System.currentTimeMillis()))
        service.flush()
        service.markNotified(listOf(ErrorDigestCollector.keyOf(occurrence(event, 0))))
        Assertions.assertEquals(1, service.buffer.size, "Kept in memory, not written.")
    }

    @Test
    fun `cleanup removes old buckets, samples and problems`() {
        val now = System.currentTimeMillis()
        val day = Constants.MILLIS_PER_DAY
        val old = LogEvent("test.aggregation.old", LogCategory.BUG)
        val quiet = LogEvent("test.aggregation.quiet", LogCategory.BUG)
        val recent = LogEvent("test.aggregation.recent", LogCategory.BUG)
        logAggregationService.add(occurrence(old, now - 200 * day, stackTrace = "trace"))
        logAggregationService.add(occurrence(quiet, now - 100 * day, stackTrace = "trace"))
        logAggregationService.add(occurrence(quiet, now - 40 * day))
        logAggregationService.add(occurrence(recent, now - 1000, stackTrace = "trace"))
        logAggregationService.flush()
        val (oldKey, quietKey, recentKey) = listOf(old, quiet, recent).map { ErrorDigestCollector.keyOf(occurrence(it, now)) }
        Assertions.assertEquals(2, buckets(quietKey).size)

        logAggregationService.cleanup(now)
        Assertions.assertNull(findGroup(oldKey), "Not seen for 180 days.")
        Assertions.assertNull(logAggregationService.stateOf(oldKey))
        group(quietKey).let {
            Assertions.assertNull(it.sampleStackTrace, "Not seen for 30 days: no samples.")
            Assertions.assertNull(it.sampleMessage)
            Assertions.assertEquals(2, it.totalCount, "The counts are kept.")
        }
        Assertions.assertEquals(1, buckets(quietKey).size, "The bucket of 100 days ago is gone.")
        Assertions.assertEquals("trace", group(recentKey).sampleStackTrace)
    }

    private fun findGroup(key: String): LogGroupDO? = persistenceService.selectNamedSingleResult(
        LogGroupDO.FIND_BY_FINGERPRINT, LogGroupDO::class.java,
        "fingerprint" to LogAggregationService.fingerprintOf(key),
    )

    private fun group(key: String): LogGroupDO = findGroup(key)!!

    private fun buckets(key: String): List<LogBucketDO> = persistenceService.executeNamedQuery(
        LogBucketDO.FIND_BY_GROUP, LogBucketDO::class.java, "groupId" to group(key).id,
    )

    private fun updateGroup(key: String, block: (LogGroupDO) -> Unit) {
        persistenceService.runInTransaction { context ->
            context.selectNamedSingleResult(
                LogGroupDO.FIND_BY_FINGERPRINT, LogGroupDO::class.java,
                "fingerprint" to LogAggregationService.fingerprintOf(key), attached = true,
            )!!.let(block)
        }
    }

    private fun occurrence(
        event: LogEvent,
        millis: Long,
        user: String? = null,
        stackTrace: String? = null,
        level: LogLevel = LogLevel.ERROR,
        message: String = "Something failed",
    ) = ErrorOccurrence(
        timestampMillis = millis,
        level = level,
        event = event,
        exceptionClass = null,
        message = message,
        location = "Foo:1",
        stackTrace = stackTrace,
        user = user,
        request = "GET /rs/test",
        groupByCode = true,
    )
}
