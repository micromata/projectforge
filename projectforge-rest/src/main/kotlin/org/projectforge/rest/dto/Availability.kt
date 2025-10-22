/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.availability.model.AvailabilityDO
import org.projectforge.common.DateFormatType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDayUtils
import java.time.LocalDate

class Availability(
    var employee: Employee? = null,
    var startDate: LocalDate? = null,
    var startDateFormatted: String? = null,
    var endDate: LocalDate? = null,
    var endDateFormatted: String? = null,
    var availabilityType: String? = null,
    var availabilityTypeFormatted: String? = null,
    var percentage: Int? = null,
    var replacement: Employee? = null,
    var otherReplacements: List<Employee>? = null,
    var otherReplacementsAsString: String? = null,
    var comment: String? = null,
    /**
     * Availabilities of substitutes overlapping this availability. Only set in detail view.
     */
    var conflictingAvailabilities: List<Availability>? = null,
    /**
     * If at least one day of the availability period isn't covered by any substitute (replacement).
     */
    var conflict: Boolean? = null,
) : BaseDTO<AvailabilityDO>() {
    constructor(src: AvailabilityDO) : this() {
        this.copyFrom(src)
    }

    override fun copyFrom(src: AvailabilityDO) {
        super.copyFrom(src)
        availabilityType = src.availabilityType
        availabilityType?.let {
            availabilityTypeFormatted = translate(it)
        }
        percentage = src.percentage
        startDateFormatted = startDate?.let {
            PFDayUtils.format(it, DateFormatType.DATE)
        }
        endDateFormatted = endDate?.let {
            PFDayUtils.format(it, DateFormatType.DATE)
        }
        otherReplacements = Employee.toEmployeeList(src.otherReplacements)?.also {
            otherReplacementsAsString = it.joinToString { it.displayName ?: "???" }
        }
    }

    override fun copyTo(dest: AvailabilityDO) {
        super.copyTo(dest)
        dest.otherReplacements = Employee.toEmployeeDOList(otherReplacements)
    }
}
