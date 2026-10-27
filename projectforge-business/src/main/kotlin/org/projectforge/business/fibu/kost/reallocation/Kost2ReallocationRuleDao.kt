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

/** The rules A -> B of the reallocation runs, finance only. */
@Service
open class Kost2ReallocationRuleDao :
    AbstractKost2ReallocationDao<Kost2ReallocationRuleDO>(Kost2ReallocationRuleDO::class.java) {

    override fun select(filter: BaseSearchFilter): List<Kost2ReallocationRuleDO> {
        return select(QueryFilter(filter))
    }

    override fun newInstance(): Kost2ReallocationRuleDO {
        return Kost2ReallocationRuleDO()
    }

    /** The not deleted rules of the given run (without access check). */
    open fun selectByRun(runId: Long): List<Kost2ReallocationRuleDO> {
        return persistenceService.executeQuery(
            "from Kost2ReallocationRuleDO r where r.run.id = :runId and r.deleted = false order by r.id",
            Kost2ReallocationRuleDO::class.java,
            Pair("runId", runId),
        )
    }
}
