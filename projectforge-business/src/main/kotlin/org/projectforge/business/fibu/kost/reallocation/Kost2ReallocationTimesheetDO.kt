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

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * The log of a time sheet moved by a [Kost2ReallocationDO]: the old and new task and kost2. It isn't an input of the
 * calculation of a run, but it is needed for the revert and for the re-apply (the moved time sheets don't show their
 * old kost2 anymore). [millis] is the attendance duration of the time sheet (after the overlap split, before the working
 * time fraction, see `MonthlyEmployeeReport.kost2TimesheetAttendances`) in the month of the salary export.
 */
@Entity
@Table(
    name = "t_fibu_kost2_reallocation_timesheet",
    indexes = [
        Index(name = "idx_fk_t_fibu_kost2_reallocation_timesheet_run_fk", columnList = "run_fk"),
        Index(name = "idx_fk_t_fibu_kost2_reallocation_timesheet_timesheet_fk", columnList = "timesheet_fk"),
    ]
)
open class Kost2ReallocationTimesheetDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "run_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var run: Kost2ReallocationDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "timesheet_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var timesheet: TimesheetDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    @get:Column(nullable = false)
    open var year: Int? = null

    /** 1-based. */
    @get:Column(nullable = false)
    open var month: Int? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "old_task_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var oldTask: TaskDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "new_task_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var newTask: TaskDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "old_kost2_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var oldKost2: Kost2DO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "new_kost2_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var newKost2: Kost2DO? = null

    @get:Column(nullable = false)
    open var millis: Long = 0
}
