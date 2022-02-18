/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.vacation.model.LeaveAccountEntryDO
import org.projectforge.business.vacation.service.VacationStats
import org.projectforge.common.DateFormatType
import org.projectforge.framework.time.PFDayUtils
import java.math.BigDecimal
import java.time.LocalDate

class LeaveAccountEntry(var employee: Employee? = null,
                        var date: LocalDate? = null,
                        var dateFormatted: String? = null,
                        var amount: BigDecimal? = null,
                        var amountFormatted: String? = null,
                        var description: String? = null
) : BaseDTO<LeaveAccountEntryDO>() {

    /**
     * @see copyFrom
     */
    constructor(src: LeaveAccountEntryDO) : this() {
        this.copyFrom(src)
    }

    override fun copyFrom(src: LeaveAccountEntryDO) {
        super.copyFrom(src)
        dateFormatted = date?.let {
            PFDayUtils.format(it, DateFormatType.DATE)
        }
        amountFormatted = VacationStats.format(amount)
    }

    override fun copyTo(dest: LeaveAccountEntryDO) {
        super.copyTo(dest)
    }
}
