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

package org.projectforge.business.vacation.model

import mu.KotlinLogging
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Filters vacation entries by given date period.
 */
class VacationPeriodFilter(val periodBegin: LocalDate, val periodEnd: LocalDate?) : CustomResultFilter<VacationDO> {

    override fun match(list: MutableList<VacationDO>, element: VacationDO): Boolean {
        val startDate = element.startDate
        val endDate = element.endDate
        if (startDate == null || endDate == null) {
            log.warn { "This shouldn't occur: start and/or end date of vacation is null: ${ToStringUtil.toJsonString(element)}" }
            return false
        }
        return !endDate.isBefore(periodBegin) && (periodEnd == null || !startDate.isAfter(periodEnd))
    }
}
