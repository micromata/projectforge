/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.model.CalEventDO
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.*

/**
 * @author Florian Blumenstein
 */
@Entity
@AUserRightId(value = "EMPLOYEE_VACATION", checkAccess = false)
@Table(name = "t_employee_vacation_calendar", uniqueConstraints = [UniqueConstraint(columnNames = ["vacation_id", "calendar_id"])])
open class VacationCalendarDO : DefaultBaseDO() {

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "vacation_id", nullable = false)
    open var vacation: VacationDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "calendar_id", nullable = false)
    open var calendar: TeamCalDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "event_id")
    open var event: CalEventDO? = null

}
