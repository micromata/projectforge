/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa

import org.hibernate.stat.spi.StatisticsImplementor
import org.projectforge.common.extensions.format

class PersistenceCallsStats() {
    var queryCount = 0L
    var updateCount = 0L
    var insertCount = 0L
    var deleteCount = 0L
    var fetchCount = 0L
    var collectionFetchCount = 0L
    var collectionRecreateCount = 0L
    var collectionRemoveCount = 0L
    var collectionUpdateCount = 0L
    var secondLevelCacheHitCount = 0L
    var secondLevelCachePutCount = 0L
    var secondLevelCacheMissCount = 0L

    internal constructor(statistics: StatisticsImplementor) : this() {
        // Now, let's get the statistics:
        queryCount = statistics.queryExecutionCount
        updateCount = statistics.entityUpdateCount
        insertCount = statistics.entityInsertCount
        deleteCount = statistics.entityDeleteCount
        collectionFetchCount = statistics.collectionFetchCount
        collectionRecreateCount = statistics.collectionRecreateCount
        collectionRemoveCount = statistics.collectionRemoveCount
        collectionUpdateCount = statistics.collectionUpdateCount
        // entityLoadCount is the number of entity loads (including cache access)
        fetchCount = statistics.entityFetchCount

        secondLevelCacheHitCount = statistics.secondLevelCacheHitCount
        secondLevelCachePutCount = statistics.secondLevelCachePutCount
        secondLevelCacheMissCount = statistics.secondLevelCacheMissCount
    }

    /**
     * Returns the difference between the current and the given statistics.
     */
    fun getStats(statistics: StatisticsImplementor): PersistenceCallsStats {
        val stats = PersistenceCallsStats()
        stats.queryCount = statistics.queryExecutionCount - queryCount
        stats.fetchCount = statistics.entityFetchCount - fetchCount
        stats.updateCount = statistics.entityUpdateCount - updateCount
        stats.insertCount = statistics.entityInsertCount - insertCount
        stats.deleteCount = statistics.entityDeleteCount - deleteCount
        stats.collectionFetchCount = statistics.collectionFetchCount - collectionFetchCount
        stats.collectionUpdateCount = statistics.collectionUpdateCount - collectionUpdateCount
        stats.collectionRemoveCount = statistics.collectionRemoveCount - collectionRemoveCount
        stats.collectionRecreateCount = statistics.collectionRecreateCount - collectionRecreateCount
        stats.secondLevelCacheHitCount = statistics.secondLevelCacheHitCount - secondLevelCacheHitCount
        stats.secondLevelCachePutCount = statistics.secondLevelCachePutCount - secondLevelCachePutCount
        stats.secondLevelCacheMissCount = statistics.secondLevelCacheHitCount - secondLevelCacheMissCount
        return stats
    }

    fun append(sb: StringBuilder, statistics: StatisticsImplementor) {
        val sb2 = StringBuilder()
        val stats = getStats(statistics)
        var total = append(sb2, "query", stats.queryCount)
        total += append(sb2, "fetch", stats.fetchCount)
        total += append(sb2, "update", stats.updateCount)
        total += append(sb2, "insert", stats.insertCount)
        total += append(sb2, "delete", stats.deleteCount)
        total += append(sb2, "collectionFetch", stats.collectionFetchCount)
        total += append(sb2, "collectionUpdate", stats.collectionUpdateCount)
        total += append(sb2, "collectionRemove", stats.collectionRemoveCount)
        total += append(sb2, "collectionRecreate", stats.collectionRecreateCount)
        sb.append("total=").append(total.format())
        sb.append(sb2.toString())
        append(sb, "secondLevelCacheHitCount", stats.secondLevelCacheHitCount)
        append(sb, "secondLevelCachePutCount", stats.secondLevelCachePutCount)
        append(sb, "secondLevelCacheMissCount", stats.secondLevelCacheHitCount)
        sb.append("}")
    }

    private fun append(sb: StringBuilder, key: String, value: Long): Long {
        if (value == 0L) return 0
        sb.append(",").append(key).append("=").append(value.format())
        return value
    }
}
