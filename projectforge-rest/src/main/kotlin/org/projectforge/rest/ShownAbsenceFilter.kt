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

package org.projectforge.rest

import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import java.time.LocalDate

/**
 * Filters the absences of a list (vacations, availability entries) by the part the logged-in user may see of them
 * ([org.projectforge.business.availability.AbsenceService.shownTo]): the query selects by their real dates, so an
 * entry clipped for the viewer (e.g. of a group mate, from today on) would tell its real begin by whether it matches a
 * period ending before the part shown, or an earlier year.
 * @param shown The entry as the viewer may see it, null for nothing of it.
 * @param until The end of the period filtered for, if any: the part shown must begin on or before.
 * @param year The year filtered for, if any: the part shown must begin or end in it.
 */
class ShownAbsenceFilter<O>(
    private val shown: (O) -> O?,
    private val startDate: (O) -> LocalDate?,
    private val endDate: (O) -> LocalDate?,
    private val until: LocalDate? = null,
    private val year: Int? = null,
) : CustomResultFilter<O> {
    override fun match(list: MutableList<O>, element: O): Boolean {
        val entry = shown(element) ?: return false
        val start = startDate(entry)
        if (until != null && start?.isAfter(until) == true) {
            return false
        }
        return year == null || start?.year == year || endDate(entry)?.year == year
    }
}
