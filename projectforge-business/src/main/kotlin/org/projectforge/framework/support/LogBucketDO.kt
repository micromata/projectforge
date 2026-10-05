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
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * The occurrences of a [LogGroupDO] in one hour, for trends and spikes.
 */
@Entity
@Table(
    name = "T_PF_LOG_BUCKET",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_pf_log_bucket_group_start", columnNames = ["group_fk", "bucket_start"])],
    indexes = [Index(name = "idx_t_pf_log_bucket_start", columnList = "bucket_start")],
)
@NamedQueries(
    NamedQuery(
        name = LogBucketDO.FIND_BY_GROUP_AND_START,
        query = "from LogBucketDO b where b.groupId = :groupId and b.bucketStart = :bucketStart",
    ),
    NamedQuery(
        name = LogBucketDO.FIND_BY_GROUP,
        query = "from LogBucketDO b where b.groupId = :groupId order by b.bucketStart",
    ),
    NamedQuery(
        name = LogBucketDO.SELECT_SINCE,
        query = "select new org.projectforge.framework.support.LogBucketRow(b.groupId, b.bucketStart, b.occurrences, b.distinctUsers) from LogBucketDO b where b.bucketStart >= :since",
    ),
    NamedQuery(
        name = LogBucketDO.SELECT_BY_GROUPS_BETWEEN,
        query = "select new org.projectforge.framework.support.LogBucketRow(b.groupId, b.bucketStart, b.occurrences, b.distinctUsers) from LogBucketDO b where b.groupId in :groupIds and b.bucketStart >= :since and b.bucketStart < :until",
    ),
    NamedQuery(
        name = LogBucketDO.DELETE_BEFORE,
        query = "delete from LogBucketDO b where b.bucketStart < :before",
    ),
    NamedQuery(
        name = LogBucketDO.DELETE_BY_GROUP_IDS,
        query = "delete from LogBucketDO b where b.groupId in :groupIds",
    ),
)
open class LogBucketDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    /** The [LogGroupDO.id]: a plain id, the buckets are only read and deleted per group. */
    @get:Column(name = "group_fk", nullable = false)
    open var groupId: Long? = null

    /** The start of the hour. */
    @get:Column(name = "bucket_start", nullable = false)
    open var bucketStart: Date? = null

    @get:Column(nullable = false)
    open var occurrences: Int = 0

    /** Of the users of the occurrences, as far as known (see [LogAggregationBuffer]). */
    @get:Column(name = "distinct_users", nullable = false)
    open var distinctUsers: Int = 0

    companion object {
        internal const val FIND_BY_GROUP_AND_START = "LogBucketDO_FindByGroupAndStart"
        internal const val FIND_BY_GROUP = "LogBucketDO_FindByGroup"
        internal const val SELECT_SINCE = "LogBucketDO_SelectSince"
        internal const val SELECT_BY_GROUPS_BETWEEN = "LogBucketDO_SelectByGroupsBetween"
        internal const val DELETE_BEFORE = "LogBucketDO_DeleteBefore"
        internal const val DELETE_BY_GROUP_IDS = "LogBucketDO_DeleteByGroupIds"
    }
}

/** The counts of a [LogBucketDO], for the trends of the dashboard ([LogGroupAdminService]). */
class LogBucketRow(val groupId: Long, val bucketStart: Date, val occurrences: Int, val distinctUsers: Int)
