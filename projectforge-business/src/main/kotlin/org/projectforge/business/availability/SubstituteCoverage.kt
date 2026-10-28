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

package org.projectforge.business.availability

import java.time.LocalDate

/**
 * The days of a period no substitute is on duty: every substitute is away on the day. Shared by vacations
 * (VacationService) and availability entries ([ConflictingAvailabilitiesCache]).
 */
object SubstituteCoverage {
    /** A period a substitute is away (on vacation or not available), both days included. */
    data class Absence(val start: LocalDate?, val end: LocalDate?)

    /**
     * The days from [start] to [end] on which all substitutes are away. None, if there is no substitute or one of
     * them isn't away at all (an empty list): a substitute who isn't away in the period is on duty all the time.
     * Calendar days, as for the conflict flag.
     * @param absencesBySubstitute The absences of each substitute.
     */
    fun uncoveredDays(
        start: LocalDate,
        end: LocalDate,
        absencesBySubstitute: List<List<Absence>>,
    ): Sequence<LocalDate> {
        if (absencesBySubstitute.isEmpty() || absencesBySubstitute.any { it.isEmpty() }) {
            return emptySequence()
        }
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { it <= end }
            .take(10000) // Paranoia limit against endless periods.
            .filter { date ->
                absencesBySubstitute.all { absences ->
                    absences.any { (from, until) -> from != null && until != null && date in from..until }
                }
            }
    }
}
