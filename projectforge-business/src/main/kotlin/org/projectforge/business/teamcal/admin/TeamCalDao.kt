/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.admin

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.admin.right.TeamCalRight
import org.projectforge.business.teamcal.externalsubscription.SubscriptionUpdateInterval
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.history.DisplayHistoryConvertContext
import org.projectforge.framework.persistence.history.FlatDisplayHistoryEntry
import org.projectforge.framework.persistence.history.HistoryEntry
import org.projectforge.framework.persistence.history.HistoryFormatUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUser
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Service
class TeamCalDao : BaseDao<TeamCalDO>(TeamCalDO::class.java) {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var historyFormatUtils: HistoryFormatUtils

    @Autowired
    private lateinit var teamCalCache: TeamCalCache

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userService: UserService

    init {
        userRightId = UserRightId.PLUGIN_CALENDAR
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    fun setOwner(calendar: TeamCalDO, userId: Long?) {
        val user = userDao.findOrLoad(userId)
        calendar.owner = user
    }

    override fun newInstance(): TeamCalDO {
        return TeamCalDO()
    }

    override fun select(filter: BaseSearchFilter): List<TeamCalDO> {
        val myFilter = if (filter is TeamCalFilter) filter
        else {
            TeamCalFilter(filter)
        }
        val user = loggedInUser
        val queryFilter = QueryFilter(myFilter)
        queryFilter.addOrder(asc("title"))
        val list = select(queryFilter)
        if (myFilter.isDeleted) {
            // No further filtering, show all deleted calendars.
            return list
        }
        val result: MutableList<TeamCalDO> = ArrayList()
        val right = userRight as TeamCalRight?
        val userId = user!!.id
        val adminAccessOnly = (myFilter.isAdmin
                && accessChecker.isUserMemberOfAdminGroup(user))
        for (cal in list) {
            val isOwn = right!!.isOwner(user, cal)
            if (isOwn) {
                // User is owner.
                if (adminAccessOnly) {
                    continue
                }
                if (myFilter.isAll || myFilter.isOwn) {
                    // Calendar matches the filter:
                    result.add(cal)
                }
            } else {
                // User is not owner.
                if (myFilter.isAll || myFilter.isOthers || adminAccessOnly) {
                    if ((myFilter.isFullAccess && right.hasFullAccess(cal, userId))
                        || (myFilter.isReadonlyAccess && right.hasReadonlyAccess(cal, userId))
                        || (myFilter.isMinimalAccess && right.hasMinimalAccess(cal, userId))
                    ) {
                        // Calendar matches the filter:
                        if (!adminAccessOnly) {
                            result.add(cal)
                        }
                    } else if (adminAccessOnly) {
                        result.add(cal)
                    }
                }
            }
        }
        return result
    }

    override fun onInsertOrModify(obj: TeamCalDO, operationType: OperationType) {
        val interval = obj.externalSubscriptionUpdateInterval
        if (interval != null && interval < SubscriptionUpdateInterval.FIFTEEN_MINUTES.interval) {
            // Ensures a minimal interval length of 15 minutes.
            obj.externalSubscriptionUpdateInterval = SubscriptionUpdateInterval.FIFTEEN_MINUTES.interval
        }
    }

    val allCalendarsWithFullAccess: List<TeamCalDO>
        /**
         * Gets a list of all calendars with full access of the current logged-in user as well as the calendars owned by the
         * current logged-in user.
         *
         * @return
         */
        get() {
            val filter = TeamCalFilter()
            filter.setOwnerType(TeamCalFilter.OwnerType.ALL)
            filter.setFullAccess(true).setReadonlyAccess(false).setMinimalAccess(false)
            return select(filter)
        }

    override fun customizeHistoryEntry(context: DisplayHistoryConvertContext<*>) {
        historyFormatUtils.replaceGroupAndUserIdsValues(context.requiredDisplayHistoryEntry)
    }

    /**
     * Calls [TeamCalCache.setExpired].
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.afterSaveOrModify
     */
    override fun afterInsertOrModify(obj: TeamCalDO, operationType: OperationType) {
        teamCalCache.setExpired()
    }

    override fun afterInsert(obj: TeamCalDO) {
        if (obj.externalSubscription) {
            teamEventExternalSubscriptionCache.updateCache(obj)
        }
    }

    override fun afterUpdate(obj: TeamCalDO, dbObj: TeamCalDO?, isModified: Boolean) {
        if (dbObj != null && obj.externalSubscription
            && !StringUtils.equals(obj.externalSubscriptionUrl, dbObj.externalSubscriptionUrl)
        ) {
            // only update if the url has changed!
            teamEventExternalSubscriptionCache.updateCache(obj)
        }
        // if calendar is present in subscription cache and is not an external subscription anymore -> cleanup!
        if (!obj.externalSubscription
            && teamEventExternalSubscriptionCache.isExternalSubscribedCalendar(obj.id)
        ) {
            obj.externalSubscriptionCalendarBinary = null
            obj.externalSubscriptionUrl = null
            obj.externalSubscriptionUpdateInterval = null
            obj.externalSubscriptionHash = null
            teamEventExternalSubscriptionCache.updateCache(obj, true)
        }
    }

    private val teamEventExternalSubscriptionCache: TeamEventExternalSubscriptionCache
        get() = applicationContext.getBean(TeamEventExternalSubscriptionCache::class.java)

    companion object {
        @JvmStatic
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "usersgroups", "owner.username",
            "owner.firstname",
            "owner.lastname"
        )
    }
}
