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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.Constants
import org.projectforge.business.privacyprotection.CronPrivacyProtectionJob
import org.projectforge.business.privacyprotection.IPrivacyProtectionJob
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * The status of the problems, as far as the support error digest needs it ([DigestNotifyFilter]).
 * The keys are the group keys of [ErrorDigestCollector.keyOf].
 */
interface LogGroupStates {
    /** Null if the problem isn't in the database (yet). */
    fun stateOf(key: String): LogGroupState?

    /** The problems were reported by the digest. */
    fun markNotified(keys: Collection<String>)
}

/**
 * Counts every collected occurrence (see [ErrorOccurrenceFactory]) in the database, per problem ([LogGroupDO]) and
 * hour ([LogBucketDO]), whether it is mailed or not: the base of the admin dashboard and of a problem's status,
 * which survives restarts (ignored, muted, resolved, already reported).
 *
 * Never written per occurrence: [add] only counts in memory ([LogAggregationBuffer]), [flush] writes all of it in
 * one transaction every 30 seconds (and by the digest before it is sent). If the database fails, the counts are kept
 * for the next try, bounded. Errors logged while writing aren't counted again.
 *
 * Cleaned up by the daily privacy protection job ([execute]).
 */
@Service
class LogAggregationService : LogGroupStates, IPrivacyProtectionJob {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var cronPrivacyProtectionJob: CronPrivacyProtectionJob

    @Value("\${projectforge.support.logAggregation.enabled:true}")
    var enabled: Boolean = true
        internal set

    @Value("\${projectforge.support.logAggregation.bucketRetentionDays:90}")
    internal var bucketRetentionDays: Int = 90

    @Value("\${projectforge.support.logAggregation.sampleRetentionDays:30}")
    internal var sampleRetentionDays: Int = 30

    @Value("\${projectforge.support.logAggregation.groupRetentionDays:180}")
    internal var groupRetentionDays: Int = 180

    internal val buffer = LogAggregationBuffer()

    /** Fingerprint -> state. Loaded by the first flush, not at log time. */
    private val states = ConcurrentHashMap<String, LogGroupState>()

    @Volatile
    private var statesLoaded = false

    /** Set while writing: errors of the database access itself are not counted again. */
    internal val flushing: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    /** Only the first failure of a row is logged, not every 30 seconds. */
    @Volatile
    private var lastFlushFailed = false

    /** Set by [close]: the database may be closed, nothing is written anymore. */
    @Volatile
    private var closed = false

    @PostConstruct
    internal fun init() {
        if (!enabled) {
            log.info { "Log aggregation disabled (projectforge.support.logAggregation.enabled=false)." }
            return
        }
        cronPrivacyProtectionJob.register(this)
    }

    /** Only counts in memory, cheap enough for every log event. */
    fun add(occurrence: ErrorOccurrence, key: String = ErrorDigestCollector.keyOf(occurrence)) {
        if (!enabled || flushing.get()) {
            return
        }
        buffer.add(occurrence, key)
    }

    override fun stateOf(key: String): LogGroupState? = states[fingerprintOf(key)]

    override fun markNotified(keys: Collection<String>) {
        if (!enabled || closed || keys.isEmpty()) {
            return
        }
        val now = Date()
        val fingerprints = keys.map { fingerprintOf(it) }.filter { states.containsKey(it) }
        if (fingerprints.isEmpty()) {
            return
        }
        runGuarded("mark reported problems") {
            persistenceService.runInNewTransaction { context ->
                fingerprints.chunked(CHUNK_SIZE).forEach { chunk ->
                    context.executeNamedUpdate(
                        LogGroupDO.UPDATE_LAST_NOTIFIED, "lastNotified" to now, "fingerprints" to chunk,
                    )
                }
            }
            fingerprints.forEach { fp -> states.computeIfPresent(fp) { _, state -> state.withLastNotified(now) } }
        }
    }

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MILLIS, initialDelay = FLUSH_INTERVAL_MILLIS)
    fun scheduledFlush() {
        flush()
    }

    /**
     * Writes the rest and stops writing. Called on shutdown by [SupportErrorDigest] after its last digest, before
     * the database is closed (see [org.projectforge.ShutdownService]).
     */
    @Synchronized
    fun close() {
        flush()
        closed = true
    }

    /** Writes everything counted since the last call. */
    @Synchronized
    fun flush() {
        if (!enabled || closed) {
            return
        }
        val groups = buffer.drain()
        if (groups.isEmpty()) {
            // Nothing to write: no db access (e.g. on shutdown, when the db may already be closed).
            return
        }
        if (!statesLoaded) {
            runGuarded("load the problem states") { reloadStates() }
        }
        val written = runGuarded("write ${groups.size} problems") {
            persistenceService.runInNewTransaction { context ->
                groups.map { write(context, it) }
            }
        }
        if (written == null) {
            buffer.restore(groups)
        } else {
            written.forEach { states[it.fingerprint] = it }
        }
    }

    private fun write(context: PfPersistenceContext, pending: PendingLogGroup): LogGroupState {
        val fingerprint = fingerprintOf(pending.key)
        val now = Date()
        val group = context.selectNamedSingleResult(
            LogGroupDO.FIND_BY_FINGERPRINT, LogGroupDO::class.java, "fingerprint" to fingerprint, attached = true,
        ) ?: LogGroupDO().also {
            it.fingerprint = fingerprint
            it.firstSeen = Date(pending.firstMillis)
            it.lastSeen = Date(pending.lastMillis)
            it.level = pending.level
            it.status = LogGroupStatus.NEW
        }
        group.code = pending.event.code.take(255)
        group.category = pending.event.category
        group.location = pending.location.take(255)
        group.exceptionClass = pending.exceptionClass?.take(255)
        group.totalCount += pending.count
        if (pending.firstMillis < group.firstSeen!!.time) group.firstSeen = Date(pending.firstMillis)
        if (pending.lastMillis > group.lastSeen!!.time) group.lastSeen = Date(pending.lastMillis)
        if (pending.level.ordinal < group.level!!.ordinal) group.level = pending.level
        val sample = pending.sample
        if (sample.stackTrace != null || group.sampleStackTrace == null) {
            group.sampleMessage = sample.message?.take(MAX_MESSAGE)
            group.sampleStackTrace = sample.stackTrace?.take(MAX_STACKTRACE)
            group.sampleRequest = sample.request?.take(MAX_REQUEST)
        }
        if (group.status == LogGroupStatus.RESOLVED) {
            // A regression: the digest reports it again (see LogGroupState.unreported).
            group.status = LogGroupStatus.NEW
            group.reopenedAt = now
        }
        if (group.id == null) {
            context.insert(group)
        }
        val groupId = group.id!!
        pending.hours.forEach { (hour, occurrences) ->
            val start = Date(hour)
            val users = pending.distinctUsers[hour] ?: 0
            val bucket = context.selectNamedSingleResult(
                LogBucketDO.FIND_BY_GROUP_AND_START, LogBucketDO::class.java,
                "groupId" to groupId, "bucketStart" to start, attached = true,
            )
            if (bucket == null) {
                context.insert(LogBucketDO().also {
                    it.groupId = groupId
                    it.bucketStart = start
                    it.occurrences = occurrences
                    it.distinctUsers = users
                })
            } else {
                bucket.occurrences += occurrences
                // The buffer counts the users over all flushes of the hour (until a restart).
                bucket.distinctUsers = maxOf(bucket.distinctUsers, users)
            }
        }
        return LogGroupState.of(group)
    }

    /**
     * The daily cleanup (privacy protection): hourly counts, samples, and problems not seen for long, with their
     * status. Then the states are reloaded, so changes of other sources (e.g. the dashboard) are taken over as well.
     */
    override fun execute() {
        if (!enabled) {
            return
        }
        runGuarded("clean up") { cleanup() }
    }

    @Synchronized
    internal fun cleanup(now: Long = System.currentTimeMillis()) {
        fun daysAgo(days: Int) = Date(now - days * Constants.MILLIS_PER_DAY)
        persistenceService.runInNewTransaction { context ->
            val buckets = context.executeNamedUpdate(LogBucketDO.DELETE_BEFORE, "before" to daysAgo(bucketRetentionDays))
            val groupIds = context.executeNamedQuery(
                LogGroupDO.SELECT_IDS_LAST_SEEN_BEFORE, Long::class.javaObjectType,
                "before" to daysAgo(groupRetentionDays),
            )
            groupIds.chunked(CHUNK_SIZE).forEach { chunk ->
                context.executeNamedUpdate(LogBucketDO.DELETE_BY_GROUP_IDS, "groupIds" to chunk)
                context.executeNamedUpdate(LogGroupDO.DELETE_BY_IDS, "ids" to chunk)
            }
            val samples = context.executeNamedUpdate(
                LogGroupDO.CLEAR_SAMPLES_LAST_SEEN_BEFORE, "before" to daysAgo(sampleRetentionDays),
            )
            log.info { "Log aggregation cleaned up: $buckets hourly counts, ${groupIds.size} problems and $samples samples." }
        }
        reloadStates()
    }

    /**
     * Changes problems of the dashboard ([LogGroupAdminService]). Under the lock of [flush], which would otherwise
     * overwrite the change with the problem it loaded before; the digest sees the change at once.
     * @return The number of problems found and changed.
     */
    @Synchronized
    fun modifyGroups(ids: Collection<Long>, change: (LogGroupDO) -> Unit): Int {
        if (ids.isEmpty()) {
            return 0
        }
        val count = persistenceService.runInNewTransaction { context ->
            ids.distinct().mapNotNull { context.find(LogGroupDO::class.java, it, attached = true) }.onEach(change).size
        }
        reloadStates()
        return count
    }

    fun reloadStates() {
        val loaded = persistenceService.runReadOnly { context ->
            context.executeNamedQuery(LogGroupDO.SELECT_STATES, LogGroupState::class.java)
        }.associateBy { it.fingerprint }
        states.keys.retainAll(loaded.keys)
        states.putAll(loaded)
        statesLoaded = true
    }

    /** Runs the block with the [flushing] guard; null on failure, which is logged once until it works again. */
    private fun <T> runGuarded(what: String, block: () -> T): T? {
        flushing.set(true)
        try {
            return block().also {
                if (lastFlushFailed) {
                    lastFlushFailed = false
                    log.info { "Log aggregation works again." }
                }
            }
        } catch (t: Throwable) {
            if (!lastFlushFailed) {
                lastFlushFailed = true
                log.warn(t) { "Log aggregation couldn't $what (kept in memory, retried every 30s): ${t.message}" }
            }
            return null
        } finally {
            flushing.set(false)
        }
    }

    companion object {
        internal const val FLUSH_INTERVAL_MILLIS = 30 * Constants.MILLIS_PER_SECOND

        private const val CHUNK_SIZE = 1000
        private const val MAX_MESSAGE = 4000
        private const val MAX_STACKTRACE = 10000
        private const val MAX_REQUEST = 1000

        /** The [LogGroupDO.fingerprint] of a group key: lowercase hex SHA-256. */
        fun fingerprintOf(key: String): String =
            MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
