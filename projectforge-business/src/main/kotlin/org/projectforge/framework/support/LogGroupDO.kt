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

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogNotify
import java.util.Date

/** The admin's view on a problem. */
enum class LogGroupStatus {
    /** Not yet looked at, or a [RESOLVED] problem that occurred again (see [LogGroupDO.reopenedAt]). */
    NEW,

    /** Known, someone takes care of it. Still reported by the notify rule of its event. */
    ACKNOWLEDGED,

    /** Never reported again, only counted. */
    IGNORED,

    /** Fixed: reopened as [NEW] if it occurs again. */
    RESOLVED,
}

/**
 * One problem of the log aggregation: all occurrences with the same fingerprint (see
 * [ErrorDigestCollector.keyOf]) over all periods and restarts. The occurrences per hour are in [LogBucketDO].
 *
 * Deliberately not a [org.projectforge.framework.persistence.entities.DefaultBaseDO] (no history, no BaseDao): the
 * rows are statistics, written in batches by [LogAggregationService] without a logged-in user. No user names are
 * stored here, the samples only contain the message, the stack trace and the request of one occurrence.
 */
@Entity
@Table(
    name = "T_PF_LOG_GROUP",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_pf_log_group_fingerprint", columnNames = ["fingerprint"])],
    indexes = [Index(name = "idx_t_pf_log_group_last_seen", columnList = "last_seen")],
)
@NamedQueries(
    NamedQuery(
        name = LogGroupDO.FIND_BY_FINGERPRINT,
        query = "from LogGroupDO g where g.fingerprint = :fingerprint",
    ),
    NamedQuery(
        name = LogGroupDO.SELECT_STATES,
        query = "select new org.projectforge.framework.support.LogGroupState(g.id, g.fingerprint, g.status, g.mutedUntil, g.overrideNotify, g.lastNotified, g.reopenedAt) from LogGroupDO g",
    ),
    NamedQuery(
        name = LogGroupDO.SELECT_ROWS,
        query = "select new org.projectforge.framework.support.LogGroupRow(g.id, g.code, g.category, g.level, g.location, g.exceptionClass, substring(g.sampleMessage, 1, 300), g.firstSeen, g.lastSeen, g.totalCount, g.status, g.mutedUntil, g.overrideNotify, g.reopenedAt) from LogGroupDO g order by g.lastSeen desc",
    ),
    NamedQuery(
        name = LogGroupDO.UPDATE_LAST_NOTIFIED,
        query = "update LogGroupDO g set g.lastNotified = :lastNotified where g.fingerprint in :fingerprints",
    ),
    NamedQuery(
        name = LogGroupDO.SELECT_IDS_LAST_SEEN_BEFORE,
        query = "select g.id from LogGroupDO g where g.lastSeen < :before",
    ),
    NamedQuery(
        name = LogGroupDO.DELETE_BY_IDS,
        query = "delete from LogGroupDO g where g.id in :ids",
    ),
    NamedQuery(
        name = LogGroupDO.CLEAR_SAMPLES_LAST_SEEN_BEFORE,
        query = "update LogGroupDO g set g.sampleMessage = null, g.sampleStackTrace = null, g.sampleRequest = null where g.lastSeen < :before and (g.sampleMessage is not null or g.sampleStackTrace is not null or g.sampleRequest is not null)",
    ),
)
open class LogGroupDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    /** Lowercase hex SHA-256 of the group key, see [LogAggregationService.fingerprintOf]. */
    @get:Column(length = 64, nullable = false)
    open var fingerprint: String? = null

    /** The [org.projectforge.common.logging.LogEvent.code]. */
    @get:Column(length = 255, nullable = false)
    open var code: String? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var category: LogCategory? = null

    /** The most severe level of all occurrences. */
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "log_level", length = 10, nullable = false)
    open var level: LogLevel? = null

    @get:Column(length = 255)
    open var location: String? = null

    @get:Column(name = "exception_class", length = 255)
    open var exceptionClass: String? = null

    /** Cleared after a while for privacy reasons, see [LogAggregationService.cleanup]. */
    @get:Column(name = "sample_message", length = 4000)
    open var sampleMessage: String? = null

    @get:Column(name = "sample_stacktrace", length = 10000)
    open var sampleStackTrace: String? = null

    /** Method and uri of a request, if any. */
    @get:Column(name = "sample_request", length = 1000)
    open var sampleRequest: String? = null

    @get:Column(name = "first_seen", nullable = false)
    open var firstSeen: Date? = null

    @get:Column(name = "last_seen", nullable = false)
    open var lastSeen: Date? = null

    @get:Column(name = "total_count", nullable = false)
    open var totalCount: Long = 0

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var status: LogGroupStatus = LogGroupStatus.NEW

    /** Not reported until then. */
    @get:Column(name = "muted_until")
    open var mutedUntil: Date? = null

    /** The admin's notify rule, replaces the one of the event. */
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "override_notify", length = 20)
    open var overrideNotify: LogNotify? = null

    /** When this problem was reported by the support error digest the last time. */
    @get:Column(name = "last_notified")
    open var lastNotified: Date? = null

    /** When this problem occurred again after it was [resolved][LogGroupStatus.RESOLVED] (a regression). */
    @get:Column(name = "reopened_at")
    open var reopenedAt: Date? = null

    companion object {
        internal const val FIND_BY_FINGERPRINT = "LogGroupDO_FindByFingerprint"
        internal const val SELECT_STATES = "LogGroupDO_SelectStates"
        internal const val SELECT_ROWS = "LogGroupDO_SelectRows"
        internal const val UPDATE_LAST_NOTIFIED = "LogGroupDO_UpdateLastNotified"
        internal const val SELECT_IDS_LAST_SEEN_BEFORE = "LogGroupDO_SelectIdsLastSeenBefore"
        internal const val DELETE_BY_IDS = "LogGroupDO_DeleteByIds"
        internal const val CLEAR_SAMPLES_LAST_SEEN_BEFORE = "LogGroupDO_ClearSamplesLastSeenBefore"
    }
}

/**
 * A [LogGroupDO] without stack trace, request and full message, for the list of the dashboard
 * ([LogGroupAdminService]).
 */
class LogGroupRow(
    val id: Long,
    val code: String,
    val category: LogCategory,
    val level: LogLevel,
    val location: String?,
    val exceptionClass: String?,
    /** The first characters of the sample message. */
    val message: String?,
    val firstSeen: Date,
    val lastSeen: Date,
    val totalCount: Long,
    val status: LogGroupStatus,
    val mutedUntil: Date?,
    val overrideNotify: LogNotify?,
    val reopenedAt: Date?,
)

/**
 * What the support error digest needs to know of a [LogGroupDO], kept in memory by [LogAggregationService].
 */
class LogGroupState(
    val id: Long,
    val fingerprint: String,
    val status: LogGroupStatus,
    val mutedUntil: Date?,
    val overrideNotify: LogNotify?,
    val lastNotified: Date?,
    val reopenedAt: Date?,
) {
    fun muted(now: Long = System.currentTimeMillis()): Boolean =
        status == LogGroupStatus.IGNORED || (mutedUntil?.time ?: 0L) > now

    /** Not yet reported, or reopened since it was reported the last time. */
    val unreported: Boolean
        get() = lastNotified == null || reopenedAt?.after(lastNotified) == true

    fun withLastNotified(date: Date) = LogGroupState(id, fingerprint, status, mutedUntil, overrideNotify, date, reopenedAt)

    companion object {
        fun of(group: LogGroupDO) = LogGroupState(
            group.id!!, group.fingerprint!!, group.status, group.mutedUntil, group.overrideNotify, group.lastNotified,
            group.reopenedAt,
        )
    }
}
