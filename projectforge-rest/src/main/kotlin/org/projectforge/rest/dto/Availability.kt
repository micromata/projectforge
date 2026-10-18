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
import org.projectforge.business.availability.AvailabilityLocation
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStats
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal
import java.time.LocalDate

class Availability(
    var user: User? = null,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var type: String? = null,
    var status: AvailabilityStatus? = null,
    var statusAsString: String? = null,
    var location: AvailabilityLocation? = null,
    var locationAsString: String? = null,
    var comment: String? = null,
    var internalComment: String? = null,
    /** Whether the logged-in user may choose the user (HR staff only). Read-only, edit view only. */
    var userEditable: Boolean? = null,
    /** Whether the internal comment is offered (to the user and HR staff only). Read-only, edit view only. */
    var internalCommentVisible: Boolean? = null,
    /** The configured types (and the stored one). Read-only, edit view only. */
    var availableTypes: List<String>? = null,
    /** Optional: who stands in for the user during the period. */
    var substitutes: List<User>? = null,
    var substitutesAsString: String? = null,
    /** On a day of the period all substitutes are away (ConflictingAvailabilitiesCache). Read-only. */
    var conflict: Boolean? = null,
    /**
     * The entry of somebody else begins earlier than [startDate] tells: shown from that day on only, as its begin
     * isn't told to the logged-in user (AbsenceVisibility). Read-only.
     */
    var startHidden: Boolean? = null,
    /** The working days of the period (weekends and holidays not counted), as of a vacation. Read-only. */
    var workingDays: BigDecimal? = null,
    var workingDaysFormatted: String? = null,
    /** The weekends and holidays of the period, for the list's days bar; null for one it doesn't draw. List only. */
    var nonWorkingDays: List<LocalDate>? = null,
    /** The days without any substitute on duty, for the list's days bar; only on a conflict. List only. */
    var uncoveredDays: List<LocalDate>? = null,
    /** The substitutes' vacations overlapping the period, as the user may see them. Read-only, edit view only. */
    var substituteVacations: List<Vacation>? = null,
    /** The substitutes' availability entries overlapping the period, ditto. Read-only, edit view only. */
    var substituteAvailabilities: List<VacationReplacementAvailability>? = null,
    /** The days of the chart of the substitutes' absences. Read-only, edit view only. */
    var chartDays: List<VacationChartDay>? = null,
) : BaseDTO<AvailabilityDO>() {

    override fun copyFrom(src: AvailabilityDO) {
        super.copyFrom(src)
        status?.let { statusAsString = translate(it.i18nKey) }
        location?.let { locationAsString = translate(it.i18nKey) }
        val start = startDate
        val end = endDate
        if (start != null && end != null) {
            workingDays = VacationService.getVacationDays(start, end)
            workingDaysFormatted = VacationStats.format(workingDays)
        }
        substitutes = src.substitutes?.map { User().apply { copyFromMinimal(it) } }?.sortedBy { it.displayName }
        substitutesAsString = substitutes?.joinToString { it.displayName ?: "???" }
    }

    /** The substitutes by their ids only. */
    override fun copyTo(dest: AvailabilityDO) {
        super.copyTo(dest)
        dest.substitutes = substitutes?.mapNotNull { it.id }?.map { id -> PFUserDO().also { it.id = id } }?.toMutableSet()
    }
}
