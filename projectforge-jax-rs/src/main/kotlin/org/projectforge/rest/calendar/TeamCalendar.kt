package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.admin.model.TeamCalDO

open class TeamCalendar(val id: Int?,
                   val title: String?,
                   var access: CalendarConfigServicesRest.ACCESS? = null) {
    constructor(teamCalDO: TeamCalDO, userId: Int, teamCalCache: TeamCalCache) : this(teamCalDO.id, teamCalDO.title) {
        val right = teamCalCache.teamCalRight
        access =
                when {
                    right.isOwner(userId, teamCalDO) -> CalendarConfigServicesRest.ACCESS.OWNER
                    right.hasFullAccess(teamCalDO, userId) -> CalendarConfigServicesRest.ACCESS.FULL
                    right.hasReadonlyAccess(teamCalDO, userId) -> CalendarConfigServicesRest.ACCESS.FULL
                    right.hasMinimalAccess(teamCalDO, userId) -> CalendarConfigServicesRest.ACCESS.MINIMAL
                    else -> CalendarConfigServicesRest.ACCESS.NONE // Only admins get calendarIds with none access.
                }
    }
}
