/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationMode
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStats
import org.projectforge.common.DateFormatType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDayUtils
import java.math.BigDecimal
import java.time.LocalDate

class Vacation(var employee: Employee? = null,
               var startDate: LocalDate? = null,
               var startDateFormatted: String? = null,
               var endDate: LocalDate? = null,
               var endDateFormatted: String? = null,
               var workingDays: BigDecimal? = null,
               var workingDaysFormatted: String? = null,
               var status: VacationStatus? = null,
               var statusString: String? = null,
               var vacationMode: VacationMode? = null,
               var vacationModeString: String? = null,
               var replacement: Employee? = null,
               var manager: Employee? = null,
               var special: Boolean? = null,
               var specialFormatted: String? = null,
               var halfDayBegin: Boolean? = null,
               var halfDayEnd: Boolean? = null,
               var comment: String? = null,
               var vacationDaysLeftInYear: BigDecimal? = null,
               var vacationDaysLeftInYearString: String? = null
) : BaseDTO<VacationDO>() {
    constructor(src: VacationDO) : this() {
        this.copyFrom(src)
    }

    override fun copyFrom(src: VacationDO) {
        super.copyFrom(src)
        workingDays = VacationService.getVacationDays(src)
        workingDaysFormatted = VacationStats.format(workingDays)
        status?.let { statusString = translate(it.i18nKey) }
        vacationMode = src.getVacationmode()
        vacationMode?.let { vacationModeString = translate(it.i18nKey) }
        specialFormatted = if (special == true) {
            translate("yes")
        } else {
            translate("no")
        }
        startDateFormatted = startDate?.let {
            PFDayUtils.format(it, DateFormatType.DATE)
        }
        endDateFormatted = endDate?.let {
            PFDayUtils.format(it, DateFormatType.DATE)
        }
    }

    override fun copyTo(dest: VacationDO) {
        super.copyTo(dest)
    }
}
