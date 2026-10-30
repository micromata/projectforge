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


package org.projectforge.plugins.cveradar

import org.projectforge.plugins.cveradar.model.CvrFindingBucket
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrFindingCounts
import org.projectforge.plugins.cveradar.model.CvrFindingStatus
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * The findings (MAT-02): read by the readers and changed by the security team ([CveRadarAccess]), but which one a user
 * may see depends on the instance ([CvrInventoryService]), so the service asks for them with `checkAccess = false`;
 * the matching ([CvrMatchingService]) writes them.
 */
@Service
class CvrFindingDao : CvrBaseDao<CvrFindingDO>(CvrFindingDO::class.java) {

    override fun newInstance(): CvrFindingDO = CvrFindingDO()

    /** The findings (deleted ones not) of a vulnerability. */
    fun selectByVulnerability(vulnerabilityId: Long): List<CvrFindingDO> {
        return persistenceService.executeQuery(
            "select f from CvrFindingDO f where f.vulnerability.id = :id and f.deleted = false",
            CvrFindingDO::class.java,
            Pair("id", vulnerabilityId),
        )
    }

    /** All open findings (deleted ones not), for the counts of the inventory tree. */
    fun selectAllOpen(): List<CvrFindingDO> {
        return persistenceService.executeQuery(
            "select f from CvrFindingDO f where f.deleted = false and f.status in :status",
            CvrFindingDO::class.java,
            Pair("status", CvrFindingStatus.OPEN),
        )
    }

    /**
     * The findings whose risk acceptance, or request of one, has its last day before [today] (MAT-05): accepted ones,
     * and requests in review nobody approved in time.
     */
    fun selectExpiredRiskAcceptances(today: LocalDate): List<CvrFindingDO> {
        return persistenceService.executeQuery(
            "select f from CvrFindingDO f where f.deleted = false and f.riskExpires < :today and " +
                    "(f.status = :accepted or (f.status = :review and f.riskRequestedBy is not null))",
            CvrFindingDO::class.java,
            Pair("accepted", CvrFindingStatus.RISK_ACCEPTED),
            Pair("review", CvrFindingStatus.IN_REVIEW),
            Pair("today", today),
        )
    }

    /** The findings (deleted ones not) of the given vulnerabilities and components; null: not restricted. */
    fun selectInScope(vulnerabilityIds: Collection<Long>?, componentIds: Collection<Long>?): List<CvrFindingDO> {
        val base = "select f from CvrFindingDO f where f.deleted = false"
        return when {
            vulnerabilityIds != null -> persistenceService.executeQueryBatched(
                "$base and f.vulnerability.id in :ids", CvrFindingDO::class.java, "ids", vulnerabilityIds,
            ).filter { componentIds == null || it.componentId in componentIds }

            componentIds != null -> persistenceService.executeQueryBatched(
                "$base and f.component.id in :ids", CvrFindingDO::class.java, "ids", componentIds,
            )

            else -> persistenceService.executeQuery(base, CvrFindingDO::class.java)
        }
    }

    /** The ids of the vulnerabilities with a finding still open or in progress, for the filter of the list. */
    fun selectVulnerabilityIdsWithFindings(): List<Long> {
        return persistenceService.executeQuery(
            "select distinct f.vulnerability.id from CvrFindingDO f where f.deleted = false and f.status in :status",
            Long::class.javaObjectType,
            Pair("status", CvrFindingStatus.PENDING),
        )
    }

    /**
     * Per vulnerability the findings for the page of a list (TRI-10): the open ones by certainty, and the ones in
     * progress and done. Counted in the database, as most findings are done after a while.
     */
    fun countsByVulnerabilities(vulnerabilityIds: Collection<Long>): Map<Long, CvrFindingCounts> {
        if (vulnerabilityIds.isEmpty()) {
            return emptyMap()
        }
        val rows = persistenceService.executeQueryBatched(
            "select f.vulnerability.id, f.status, f.certainty, count(f) from CvrFindingDO f " +
                    "where f.deleted = false and f.status <> :discarded and f.vulnerability.id in :ids " +
                    "group by f.vulnerability.id, f.status, f.certainty",
            Array<Any?>::class.java,
            "ids",
            vulnerabilityIds,
            Pair("discarded", CvrFindingStatus.DISCARDED),
        )
        return rows.groupBy { it[0] as Long }.mapValues { (_, list) ->
            fun count(predicate: (CvrFindingStatus?, CvrFindingCertainty?) -> Boolean) =
                list.filter { predicate(it[1] as CvrFindingStatus?, it[2] as CvrFindingCertainty?) }
                    .sumOf { (it[3] as Number).toInt() }
            CvrFindingCounts(
                affected = count { s, c -> s?.bucket == CvrFindingBucket.OPEN && c == CvrFindingCertainty.AFFECTED },
                possible = count { s, c -> s?.bucket == CvrFindingBucket.OPEN && c != CvrFindingCertainty.AFFECTED },
                inProgress = count { s, _ -> s?.bucket == CvrFindingBucket.IN_PROGRESS },
                done = count { s, _ -> s?.bucket == CvrFindingBucket.DONE },
            )
        }
    }

    companion object {
        /**
         * Serialises the changes of findings (by hand, the expiry job and the matching): each reads a finding and writes
         * it back, so two at a time would lose one of the changes, e.g. one of two approvals.
         */
        val LOCK = Any()
    }
}
