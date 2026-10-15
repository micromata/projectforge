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

import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import java.time.LocalDate

// The team vacation calendar of projectforge-next (`/next/vacationExport`, VacationExportRest).

/** An employee or group as the pickers show it. */
class VacationExportRef(
    val id: Long,
    val displayName: String?,
)

/**
 * What the page shows and exports: the vacations of [employeeIds] and of the members of [groupIds] from [from] to
 * [until] (both inclusive). The Excel export ignores [until]: its sheets start in the month of [from].
 *
 * @param periodKind The kind of period the page pages through (e.g. `quarter`), only stored for next time.
 * @param dayWidth The width of the chart's day columns (`auto`, `fit`, `narrow`, …), only stored for next time.
 * @param showAvailabilities Shows and exports the availability entries of the same employees, too.
 */
class VacationExportRequest(
    var employeeIds: List<Long>? = null,
    var groupIds: List<Long>? = null,
    var periodKind: String? = null,
    var dayWidth: String? = null,
    var from: LocalDate? = null,
    var until: LocalDate? = null,
    var showAvailabilities: Boolean? = null,
)

/**
 * The page's initial state: the employees and groups chosen last time (only those still to be picked).
 *
 * @param unrestricted HR and finance staff, who may pick everybody; anybody else picks only from their own groups.
 * @param earliestDate The first day shown and exported: 1 January of the current year for anybody but HR and
 * finance staff (null: no limit), who don't see the vacations of previous years.
 */
class VacationExportInitial(
    val employees: List<VacationExportRef>,
    val groups: List<VacationExportRef>,
    val periodKind: String?,
    val dayWidth: String?,
    val showAvailabilities: Boolean,
    val unrestricted: Boolean,
    val earliestDate: LocalDate?,
    val favorites: List<VacationExportFavoriteInfo>,
)

/**
 * A favorite of the team vacation calendar: a named choice of employees and groups (the ids the user may still
 * pick, as [VacationExportSelection] applies them; the page tells by them whether its choice is the favorite's).
 */
class VacationExportFavoriteInfo(
    val id: Long,
    val name: String,
    val employeeIds: List<Long>,
    val groupIds: List<Long>,
)

/** A favorite applied: its employees and groups, only those the user may (still) pick. */
class VacationExportSelection(
    val employees: List<VacationExportRef>,
    val groups: List<VacationExportRef>,
)

/** The calendar of the period asked for: its [days] and one row per employee, sorted by name. */
class VacationExportView(
    val days: List<VacationChartDay>,
    val rows: List<VacationExportRow>,
)

/** @param availabilities Empty unless [VacationExportRequest.showAvailabilities]. */
class VacationExportRow(
    val employee: VacationExportRef,
    val vacations: List<VacationExportVacation>,
    val availabilities: List<VacationExportAvailability> = emptyList(),
)

/**
 * An availability entry of the team vacation calendar, cut to the period shown as [VacationExportVacation.of]. Only
 * the public comment: the internal one is for the employee and HR staff, not for the team.
 *
 * @param label What the entry is called for the logged-in user, as the Excel export writes it
 * (AvailabilityDao.labelFor: the type, e.g. "Workation (Reachable if needed)", only for the employee and HR staff).
 * @param absent The employee is away (AvailabilityStatus.absent), as on vacation.
 */
class VacationExportAvailability(
    val id: Long?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val label: String,
    val comment: String?,
    val absent: Boolean = false,
) {
    companion object {
        fun of(obj: AvailabilityDO, from: LocalDate, until: LocalDate, label: String) = VacationExportAvailability(
            obj.id,
            obj.startDate?.let { maxOf(it, from) },
            obj.endDate?.let { minOf(it, until) },
            label,
            obj.comment?.takeIf { it.isNotBlank() },
            obj.status?.absent == true,
        )
    }
}

class VacationExportVacation(
    val id: Long?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val halfDayBegin: Boolean,
    val halfDayEnd: Boolean,
    val status: VacationStatus?,
) {
    companion object {
        /**
         * The vacation cut to the period shown ([from]..[until]): its dates outside aren't sent (for a restricted
         * user that would be a previous year), and a half day cut off is no longer one.
         */
        fun of(obj: VacationDO, from: LocalDate, until: LocalDate): VacationExportVacation {
            val startDate = obj.startDate?.let { maxOf(it, from) }
            val endDate = obj.endDate?.let { minOf(it, until) }
            return VacationExportVacation(
                obj.id,
                startDate,
                endDate,
                obj.halfDayBegin == true && startDate == obj.startDate,
                obj.halfDayEnd == true && endDate == obj.endDate,
                obj.status,
            )
        }
    }
}
