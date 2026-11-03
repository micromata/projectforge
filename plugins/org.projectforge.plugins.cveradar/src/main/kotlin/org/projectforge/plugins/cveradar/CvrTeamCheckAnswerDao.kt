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

import org.projectforge.plugins.cveradar.model.CvrTeamCheckAnswerDO
import org.springframework.stereotype.Service

/**
 * The answers of the members to the team checks asking every member (CLS-03): written by
 * [CveRadarService.answerTeamCheck] only, which checks the membership against the stored check.
 */
@Service
class CvrTeamCheckAnswerDao : CvrBaseDao<CvrTeamCheckAnswerDO>(CvrTeamCheckAnswerDO::class.java) {
    override fun newInstance(): CvrTeamCheckAnswerDO {
        return CvrTeamCheckAnswerDO()
    }

    /** The answers (deleted ones not) of the given checks. */
    fun selectByChecks(checkIds: Collection<Long>): List<CvrTeamCheckAnswerDO> {
        if (checkIds.isEmpty()) {
            return emptyList()
        }
        return persistenceService.executeQuery(
            "select a from CvrTeamCheckAnswerDO a where a.teamCheck.id in :checkIds and a.deleted = false",
            CvrTeamCheckAnswerDO::class.java,
            Pair("checkIds", checkIds),
        )
    }

    /** The answer of the user to the check (answers aren't deleted). */
    fun findByCheckAndUser(checkId: Long, userId: Long): CvrTeamCheckAnswerDO? {
        return persistenceService.selectSingleResult(
            "select a from CvrTeamCheckAnswerDO a where a.teamCheck.id = :checkId and a.user.id = :userId",
            CvrTeamCheckAnswerDO::class.java,
            Pair("checkId", checkId),
            Pair("userId", userId),
        )
    }
}
