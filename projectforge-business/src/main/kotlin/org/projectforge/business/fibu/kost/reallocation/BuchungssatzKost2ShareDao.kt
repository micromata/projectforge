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

package org.projectforge.business.fibu.kost.reallocation

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.springframework.stereotype.Service

/**
 * The kost2 shares of the accounting records. Readable by finance and controlling (the effective view is used by the
 * reporting and the contribution margin), writable by finance only. The internal selects run without access check:
 * the callers check the access to the records themselves.
 */
@Service
open class BuchungssatzKost2ShareDao :
    AbstractKost2ReallocationDao<BuchungssatzKost2ShareDO>(BuchungssatzKost2ShareDO::class.java, controllingMaySelect = true) {

    override fun select(filter: BaseSearchFilter): List<BuchungssatzKost2ShareDO> {
        return select(QueryFilter(filter))
    }

    override fun newInstance(): BuchungssatzKost2ShareDO {
        return BuchungssatzKost2ShareDO()
    }

    /** The active shares of the given records. */
    open fun selectActiveByRecordIds(recordIds: Collection<Long>): List<BuchungssatzKost2ShareDO> {
        return persistenceService.executeQueryBatched(
            "from BuchungssatzKost2ShareDO s where s.buchungssatz.id in :ids and $ACTIVE order by s.id",
            BuchungssatzKost2ShareDO::class.java,
            batchParam = "ids",
            batchValues = recordIds,
        )
    }

    /** The ids of the records (of the given months, year * 100 + month) with an active share on one of the kost2. */
    open fun selectActiveRecordIdsByKost2(
        kost2Ids: Collection<Long>, fromYearMonth: Int? = null, toYearMonth: Int? = null,
    ): Set<Long> {
        val period = if (fromYearMonth != null && toYearMonth != null) {
            " and (s.buchungssatz.year * 100 + s.buchungssatz.month) between :from and :to"
        } else ""
        val params = if (period.isEmpty()) emptyArray() else arrayOf(
            Pair<String, Any?>("from", fromYearMonth),
            Pair<String, Any?>("to", toYearMonth),
        )
        return persistenceService.executeQueryBatched(
            "select distinct s.buchungssatz.id from BuchungssatzKost2ShareDO s where s.kost2.id in :ids and $ACTIVE$period",
            java.lang.Long::class.java,
            "ids",
            kost2Ids,
            *params,
        ).map { it.toLong() }.toSet()
    }

    /** The ids of the records with active shares out of the given ones. */
    open fun selectRecordIdsWithActiveShares(recordIds: Collection<Long>): Set<Long> {
        return persistenceService.executeQueryBatched(
            "select distinct s.buchungssatz.id from BuchungssatzKost2ShareDO s where s.buchungssatz.id in :ids and $ACTIVE",
            java.lang.Long::class.java,
            batchParam = "ids",
            batchValues = recordIds,
        ).map { it.toLong() }.toSet()
    }

    /** All not deleted shares (active or superseded) of the given run or superseded by it. */
    open fun selectByRun(runId: Long): List<BuchungssatzKost2ShareDO> {
        return persistenceService.executeQuery(
            "from BuchungssatzKost2ShareDO s where s.deleted = false and (s.run.id = :runId or s.supersededByRun.id = :runId)",
            BuchungssatzKost2ShareDO::class.java,
            Pair("runId", runId),
        )
    }

    /** All shares (also deleted ones) written by the given run. */
    open fun selectAllOfRun(runId: Long): List<BuchungssatzKost2ShareDO> {
        return persistenceService.executeQuery(
            "from BuchungssatzKost2ShareDO s where s.run.id = :runId",
            BuchungssatzKost2ShareDO::class.java,
            Pair("runId", runId),
        )
    }

    /**
     * Discards the shares of the given records (active and superseded ones), e. g. on a DATEV reimport which changed
     * the kost2 or the amount of a record: the record counts on its own kost2 again. The runs may be re-applied.
     * Without access check: the caller (DATEV import) checks its own right.
     * @return The number of discarded shares.
     */
    open fun discardForRecords(recordIds: Collection<Long>): Int {
        if (recordIds.isEmpty()) {
            return 0
        }
        val shares = persistenceService.executeQueryBatched(
            "from BuchungssatzKost2ShareDO s where s.buchungssatz.id in :ids and s.deleted = false",
            BuchungssatzKost2ShareDO::class.java,
            batchParam = "ids",
            batchValues = recordIds,
        )
        shares.forEach { markAsDeleted(it, checkAccess = false) }
        return shares.size
    }

    companion object {
        private const val ACTIVE = "s.deleted = false and s.supersededByRun is null"
    }
}
