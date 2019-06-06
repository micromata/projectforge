package org.projectforge.business.calendar

import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.admin.model.TeamCalDO

open class TeamCalendar(val id: Int?,
                   val title: String?,
                   var access: ACCESS? = null) {
    enum class ACCESS { OWNER, FULL, READ, MINIMAL, NONE }

    constructor(teamCalDO: TeamCalDO, userId: Int, teamCalCache: TeamCalCache) : this(teamCalDO.id, teamCalDO.title) {
        val right = teamCalCache.teamCalRight
        access =
                when {
                    right.isOwner(userId, teamCalDO) -> ACCESS.OWNER
                    right.hasFullAccess(teamCalDO, userId) -> ACCESS.FULL
                    right.hasReadonlyAccess(teamCalDO, userId) -> ACCESS.FULL
                    right.hasMinimalAccess(teamCalDO, userId) -> ACCESS.MINIMAL
                    else -> ACCESS.NONE // Only admins get calendarIds with none access.
                }
    }
}
