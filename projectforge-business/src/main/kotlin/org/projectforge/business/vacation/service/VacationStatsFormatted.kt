/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.service

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

/**
 * Formats all values for the client.
 */
class VacationStatsFormatted(var stats: VacationStats) {

    @get:JsonProperty
    val year: Int?
        get() = stats.year

    @get:JsonProperty
    val remainingLeaveFromPreviousYear: String
        get() = VacationStats.format(stats.remainingLeaveFromPreviousYear)

    @get:JsonProperty
    val remainingLeaveFromPreviousYearAllocated: String
        get() = VacationStats.format(stats.remainingLeaveFromPreviousYearAllocated)

    @get:JsonProperty
    val remainingLeaveFromPreviousYearUnused: String
        get() = VacationStats.format(stats.remainingLeaveFromPreviousYearUnused, true, true, !stats.endOfVactionYearExceeded)

    @get:JsonProperty
    val totalLeaveIncludingCarry: String
        get() = VacationStats.format(stats.totalLeaveIncludingCarry)

    @get:JsonProperty
    val allocatedDaysInOverlapPeriod: String
        get() = VacationStats.format(stats.allocatedDaysInOverlapPeriod)

    @get:JsonProperty
    val vacationDaysInYearFromContract: String
        get() = VacationStats.format(stats.vacationDaysInYearFromContract)

    @get:JsonProperty
    val vacationDaysInProgressAndApproved: String
        get() = VacationStats.format(stats.vacationDaysInProgressAndApproved, true, true)

    @get:JsonProperty
    val vacationDaysLeftInYear: String
        get() = VacationStats.format(stats.vacationDaysLeftInYear)

    @get:JsonProperty
    val vacationDaysLeftInYearWithoutCarry: String
        get() = VacationStats.format(stats.vacationDaysLeftInYearWithoutCarry)

    @get:JsonProperty
    val vacationDaysInProgress: String
        get() = VacationStats.format(stats.vacationDaysInProgress, true, true)

    @get:JsonProperty
    val hasVacationDaysInProgress: Boolean
        get() = stats.vacationDaysInProgress != BigDecimal.ZERO

    @get:JsonProperty
    val vacationDaysApproved: String
        get() = VacationStats.format(stats.vacationDaysApproved, true, true)

    @get:JsonProperty
    val hasSpecialVacationDaysInProgress: Boolean
        get() = stats.specialVacationDaysInProgress != BigDecimal.ZERO

    @get:JsonProperty
    val specialVacationDaysInProgress: String
        get() = VacationStats.format(stats.specialVacationDaysInProgress, false, true)

    @get:JsonProperty
    val hasSpecialVacationDaysApproved: Boolean
        get() = stats.specialVacationDaysApproved != BigDecimal.ZERO

    @get:JsonProperty
    val specialVacationDaysApproved: String
        get() = VacationStats.format(stats.specialVacationDaysApproved, false, true)

    @get:JsonProperty
    val leaveAccountEntriesSum: String
        get() = VacationStats.format(stats.leaveAccountEntriesSum)
}
