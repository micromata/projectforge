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

package org.projectforge.rest.dto

import org.projectforge.business.humanresources.HRPlanningDO
import org.projectforge.business.humanresources.HRPlanningEntryDO
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The planned week of one employee: user, week (its Monday) and the entries of that week. The DTO of the
 * hand-built projectforge-next edit page (`HRPlanningEntityRest`).
 */
class HRPlanning(
    var week: LocalDate? = null,
    var formattedWeekOfYear: String? = null,
    var totalHours: BigDecimal? = null,
    var totalUnassignedHours: BigDecimal? = null,
    var user: User? = null,
    var entries: MutableList<HRPlanningEntry>? = null,
) : BaseDTO<HRPlanningDO>() {

    /**
     * @see copyFrom
     */
    constructor(src: HRPlanningDO) : this() {
        copyFrom(src)
    }

    override fun copyFrom(src: HRPlanningDO) {
        super.copyFrom(src)
        formattedWeekOfYear = src.formattedWeekOfYear
        totalHours = src.totalHours
        totalUnassignedHours = src.totalUnassignedHours
        this.user = src.user?.let {
            User(it)
        }
    }

    /**
     * [copyFrom] plus the entries, for the edit page: it shows every entry and sends them all back on save.
     *
     * The deleted entries travel too: `HRPlanningDO.entries` has `autoUpdateCollectionEntries` but no
     * `@SoftDeleteCollection`, so the collection handler physically removes whatever a posted collection
     * leaves out.
     */
    fun copyFromWithCollections(src: HRPlanningDO) {
        copyFrom(src)
        entries = src.entries?.map { HRPlanningEntry().also { entry -> entry.copyFrom(it) } }?.toMutableList()
    }

    /**
     * Rebuilds the entries instead of appending to them, each with its back reference to [dest] — which the
     * collection handler needs for matching a posted entry against its database row.
     */
    override fun copyTo(dest: HRPlanningDO) {
        super.copyTo(dest)
        dest.entries = entries?.map { dto ->
            HRPlanningEntryDO().also {
                dto.copyTo(it)
                it.planning = dest
            }
        }?.toMutableList()
    }
}
