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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.projectforge.business.PfCaches
import org.projectforge.business.timesheet.TimesheetDO
import java.math.BigDecimal
import java.util.*

// Json ignore: These are properties by calendar events, not by timesheets. They exist after switching from calendar events to timesheets.
@JsonIgnoreProperties(value = ["reminderDuration", "reminderDurationUnit"])
class Timesheet(
    var task: Task? = null,
    var location: String? = null,
    var reference: String? = null,
    var tag: String? = null,
    var description: String? = null,
    var user: User? = null,
    var kost2: Kost2? = null,
    var startTime: Date? = null,
    var stopTime: Date? = null,
    /**
     * A counter (incremented by one for each recent entry) usable by React as key.
     */
    var counter: Int? = null
) : BaseDTO<TimesheetDO>() {
    var timeSavedByAI: BigDecimal? = null
    var timeSavedByAIUnit: TimesheetDO.TimeSavedByAIUnit? = TimesheetDO.TimeSavedByAIUnit.PERCENTAGE
    var timeSavedByAIDescription: String? = null

    /**
     * Whether AI time-savings tracking is enabled in this installation (transient, server set). The
     * hand-built edit page has no UILayout to hide the AI fields from, so it reads this flag from the
     * DTO to gate the section — the counterpart of `baseDao.timeSavingsByAIEnabled` guarding the
     * UILayout in [org.projectforge.rest.TimesheetPagesRest.createEditLayout].
     */
    var timeSavingsByAIEnabled: Boolean = false

    /**
     * The configured note shown below the edit form (transient, server set), or null where none is
     * configured — the counterpart of the [org.projectforge.ui.UIAlert] the UILayout adds to
     * `layoutBelowActions` in [org.projectforge.rest.TimesheetPagesRest.createEditLayout]. The
     * hand-built edit page has no UILayout to carry it, so it reads the text from the DTO. Set only
     * when [timeSavingsByAIEnabled], as in the UILayout.
     */
    var timeSavingsByAINote: String? = null

    /**
     * The tags to choose from (transient, server set), or null/empty where none is configured. Like
     * [timeSavingsByAIEnabled], the hand-built edit page has no UILayout to read the `tag` select's
     * values from, so it takes them from here — the counterpart of `createTagUISelect` returning null
     * (and no field at all) when there is nothing to choose. Includes the sheet's own tag even after it
     * was dropped from the configuration (see `TimesheetDao.getTags`).
     */
    var tags: List<String>? = null

    /**
     * The lean row of the hand-built next list: exactly the columns of `timesheet.page.tsx` (see
     * `TimesheetListRow`) and nothing else, so `JsonInclude.Include.NON_NULL` keeps the rest — the tag,
     * the AI fields, the counter — off the wire (see [BaseDTO.copyFrom4ListRow]). The counterpart of the
     * nested `Timesheet4ListExport` the legacy React list reads; the next client gets this flat shape
     * because [org.projectforge.rest.TimesheetPagesRest.newDTO] returns a non-null DTO (see
     * [org.projectforge.rest.core.AbstractDTOPagesRest.createListRow]).
     */
    override fun copyFrom4ListRow(src: TimesheetDO) {
        id = src.id
        deleted = src.deleted
        // The two timestamps every next list offers as a column, hidden until switched on
        // (`lib/page-def/audit-columns.ts`).
        copyAuditFieldsFrom(src)
        // Populate task/user/kost2 from the in-memory caches by their FK ids before reading them: otherwise
        // each row would lazy load its task from the DB, an N+1 over the page (getListByIds loads by IN(...)).
        PfCaches.instance.initialize(src)
        startTime = src.startTime
        stopTime = src.stopTime
        location = src.location
        reference = src.reference
        description = src.description
        // The columns show a name (and, for the cost unit, its formatted number), not a whole entity — so
        // the id and display name only. Not the DTOs' copyFrom, which would nest the task's parent chain.
        src.task?.let { task = Task(id = it.id, displayName = it.displayName) }
        src.user?.let { user = User(id = it.id, displayName = it.displayName) }
        src.kost2?.let { kost2 = Kost2().also { dto -> dto.copyFromMinimal(it) } }
    }
}
