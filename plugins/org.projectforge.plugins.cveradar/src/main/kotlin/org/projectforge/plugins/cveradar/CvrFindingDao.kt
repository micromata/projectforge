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

import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrFindingStatus
import org.springframework.stereotype.Service

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

    /** All findings (deleted ones not), for the counts of the inventory tree. */
    fun selectAllOpen(): List<CvrFindingDO> {
        return persistenceService.executeQuery(
            "select f from CvrFindingDO f where f.deleted = false and f.status = :status",
            CvrFindingDO::class.java,
            Pair("status", CvrFindingStatus.NEW),
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

    /** The ids of the vulnerabilities with an open finding, for the filter of the list. */
    fun selectVulnerabilityIdsWithFindings(): List<Long> {
        return persistenceService.executeQuery(
            "select distinct f.vulnerability.id from CvrFindingDO f where f.deleted = false and f.status = :status",
            Long::class.javaObjectType,
            Pair("status", CvrFindingStatus.NEW),
        )
    }

    /** Per vulnerability the open findings by certainty, for the page of a list: (affected, possible). */
    fun countsByVulnerabilities(vulnerabilityIds: Collection<Long>): Map<Long, Pair<Int, Int>> {
        if (vulnerabilityIds.isEmpty()) {
            return emptyMap()
        }
        return persistenceService.executeQueryBatched(
            "select f from CvrFindingDO f where f.deleted = false and f.status = :status and f.vulnerability.id in :ids",
            CvrFindingDO::class.java,
            "ids",
            vulnerabilityIds,
            Pair("status", CvrFindingStatus.NEW),
        ).groupBy { it.vulnerabilityId!! }.mapValues { (_, findings) ->
            val affected = findings.count { it.certainty == CvrFindingCertainty.AFFECTED }
            affected to (findings.size - affected)
        }
    }
}
