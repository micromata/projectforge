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

package org.projectforge.business.vacation.model

import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.persistence.api.impl.CustomResultFilter

/**
 * The vacations a user who isn't HR or finance staff sees in the vacation list: their own ones, the ones they
 * manage or substitute for, and the vacations of the substitutes named in one of their own vacations which overlap
 * that vacation. A rejected (or otherwise inactive) vacation of their own doesn't reveal its substitutes' leave.
 *
 * Only the list is restricted this way, the read right ([org.projectforge.business.vacation.repository.VacationDao])
 * stays as it is.
 *
 * @param employeeId The logged-in user's employee; null: nothing matches.
 * @param ownVacations The vacations of that employee (any period).
 */
class VacationVisibilityFilter(
    private val employeeId: Long?,
    ownVacations: List<VacationDO>,
) : CustomResultFilter<VacationDO> {
    /** The active own vacations by the employee id of each of their substitutes. */
    private val ownVacationsBySubstitute: Map<Long, List<VacationDO>> =
        ownVacations.filter { it.status in VacationService.CONFLICT_STATUS }
            .flatMap { vacation -> vacation.allReplacements.mapNotNull { it.id }.map { it to vacation } }
            .groupBy({ it.first }, { it.second })

    override fun match(list: MutableList<VacationDO>, element: VacationDO): Boolean {
        employeeId ?: return false
        if (element.employee?.id == employeeId || element.manager?.id == employeeId ||
            element.allReplacements.any { it.id == employeeId }
        ) {
            return true
        }
        val ownVacations = ownVacationsBySubstitute[element.employee?.id] ?: return false
        return ownVacations.any { it.hasOverlap(element) }
    }
}
