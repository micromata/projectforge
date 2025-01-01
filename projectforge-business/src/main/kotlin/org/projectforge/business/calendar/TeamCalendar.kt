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

package org.projectforge.business.calendar

import org.projectforge.business.common.DataobjectAccessType
import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.framework.i18n.translate

open class TeamCalendar(val id: Long?,
                        val title: String?,
                        var access: ACCESS? = null,
                        var externalSubscription: Boolean = false) {
    enum class ACCESS { OWNER, FULL, READ, MINIMAL, NONE }

    constructor(teamCalDO: TeamCalDO, userId: Long, teamCalCache: TeamCalCache) : this(teamCalDO.id, teamCalDO.title) {
        val right = teamCalCache.teamCalRight
        access =
                when (right.getAccessType(teamCalDO, userId)) {
                    DataobjectAccessType.OWNER -> ACCESS.OWNER
                    DataobjectAccessType.FULL -> ACCESS.FULL
                    DataobjectAccessType.READONLY -> ACCESS.READ
                    DataobjectAccessType.MINIMAL -> ACCESS.MINIMAL
                    else -> ACCESS.NONE // Only admins get calendarIds with none access.
                }
        externalSubscription = teamCalDO.externalSubscription
    }

    companion object {
        /**
         * Calendar id of pseudo calendar (birthdays of favorite peoples).
         */
        const val BIRTHDAYS_FAVS_CAL_ID = -2L

        /**
         * Calendar id of pseudo calendar (birthdays of non-favorite peoples).
         */
        const val BIRTHDAYS_ALL_CAL_ID = -3L

        fun createFavoritesBirthdaysPseudoCalendar() : TeamCalendar {
            return TeamCalendar(TeamCalendar.BIRTHDAYS_FAVS_CAL_ID,
                    translate("calendar.birthdays.favorites"),
                    ACCESS.READ)
        }

        fun createAllBirthdaysPseudoCalendar() : TeamCalendar {
            return TeamCalendar(TeamCalendar.BIRTHDAYS_ALL_CAL_ID,
                    translate("calendar.birthdays.all"),
                    ACCESS.READ)
        }
    }
}
