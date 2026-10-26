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

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.springframework.stereotype.Service

/**
 * The team checks (CLS-03): created and changed by the security team ([CveRadarRight]); a member of the asked group
 * answers through [CveRadarService.answerTeamCheck], which checks the membership against the stored check.
 */
@Service
class CvrTeamCheckDao : BaseDao<CvrTeamCheckDO>(CvrTeamCheckDO::class.java) {
    init {
        userRightId = CveRadarRightId.PLUGIN_CVERADAR
    }

    override fun newInstance(): CvrTeamCheckDO {
        return CvrTeamCheckDO()
    }

    /** The checks of a case (deleted ones not). */
    fun selectByCase(caseId: Long): List<CvrTeamCheckDO> {
        return persistenceService.executeQuery(
            "from CvrTeamCheckDO t where t.cvrCase.id = :caseId and t.deleted = false order by t.subject",
            CvrTeamCheckDO::class.java,
            Pair("caseId", caseId),
        )
    }

    /** The open checks (deleted ones not) of the given groups, for the members' own view. */
    fun selectByGroups(groupIds: Collection<Long>): List<CvrTeamCheckDO> {
        if (groupIds.isEmpty()) {
            return emptyList()
        }
        return persistenceService.executeQuery(
            "from CvrTeamCheckDO t where t.group.id in :groupIds and t.deleted = false order by t.subject",
            CvrTeamCheckDO::class.java,
            Pair("groupIds", groupIds),
        )
    }
}
