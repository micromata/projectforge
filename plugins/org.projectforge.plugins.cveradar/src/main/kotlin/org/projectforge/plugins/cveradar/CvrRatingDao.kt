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

import org.projectforge.plugins.cveradar.model.CvrRatingDO
import org.springframework.stereotype.Service

/** The ratings of the cases by the security team: read by the readers, written by the security team ([CveRadarAccess]). */
@Service
class CvrRatingDao : CvrBaseDao<CvrRatingDO>(CvrRatingDO::class.java) {
    override fun newInstance(): CvrRatingDO = CvrRatingDO()

    /** The ratings (deleted ones not) of a case, oldest first. */
    fun selectByCase(caseId: Long): List<CvrRatingDO> {
        return persistenceService.executeQuery(
            "select r from CvrRatingDO r join fetch r.user where r.cvrCase.id = :caseId and r.deleted = false order by r.created",
            CvrRatingDO::class.java,
            Pair("caseId", caseId),
        )
    }

    /** The rating of the user for the case, a deleted one included (unique per case and user). */
    fun findByCaseAndUser(caseId: Long, userId: Long): CvrRatingDO? {
        return persistenceService.selectSingleResult(
            "from CvrRatingDO r where r.cvrCase.id = :caseId and r.user.id = :userId",
            CvrRatingDO::class.java,
            Pair("caseId", caseId),
            Pair("userId", userId),
        )
    }

    /** The ratings (deleted ones not) of the cases, by case id. */
    fun selectByCases(caseIds: Collection<Long>): Map<Long, List<CvrRatingDO>> {
        if (caseIds.isEmpty()) {
            return emptyMap()
        }
        return persistenceService.executeQueryBatched(
            "select r from CvrRatingDO r join fetch r.user where r.cvrCase.id in :caseIds and r.deleted = false",
            CvrRatingDO::class.java,
            "caseIds",
            caseIds,
        ).groupBy { it.cvrCase!!.id!! }
    }
}
