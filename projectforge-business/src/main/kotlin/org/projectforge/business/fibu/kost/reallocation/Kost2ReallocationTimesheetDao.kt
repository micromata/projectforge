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

/** The log of the time sheets moved by the reallocation runs, finance only. */
@Service
open class Kost2ReallocationTimesheetDao :
    AbstractKost2ReallocationDao<Kost2ReallocationTimesheetDO>(Kost2ReallocationTimesheetDO::class.java) {

    override fun select(filter: BaseSearchFilter): List<Kost2ReallocationTimesheetDO> {
        return select(QueryFilter(filter))
    }

    override fun newInstance(): Kost2ReallocationTimesheetDO {
        return Kost2ReallocationTimesheetDO()
    }

    /** The not deleted log entries of the given run (without access check). */
    open fun selectByRun(runId: Long): List<Kost2ReallocationTimesheetDO> {
        return persistenceService.executeQuery(
            "from Kost2ReallocationTimesheetDO t where t.run.id = :runId and t.deleted = false order by t.id",
            Kost2ReallocationTimesheetDO::class.java,
            Pair("runId", runId),
        )
    }

    /** The not deleted log entries of the given runs (without access check). */
    open fun selectByRuns(runIds: Collection<Long>): List<Kost2ReallocationTimesheetDO> {
        return persistenceService.executeQueryBatched(
            "from Kost2ReallocationTimesheetDO t where t.run.id in :runIds and t.deleted = false",
            Kost2ReallocationTimesheetDO::class.java,
            batchParam = "runIds",
            batchValues = runIds,
        )
    }

    /** The not deleted log entries of the given time sheets (without access check). */
    open fun selectByTimesheets(timesheetIds: Collection<Long>): List<Kost2ReallocationTimesheetDO> {
        return persistenceService.executeQueryBatched(
            "from Kost2ReallocationTimesheetDO t where t.timesheet.id in :timesheetIds and t.deleted = false",
            Kost2ReallocationTimesheetDO::class.java,
            batchParam = "timesheetIds",
            batchValues = timesheetIds,
        )
    }
}
