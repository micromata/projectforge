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

package org.projectforge.business.humanresources

import mu.KotlinLogging
import org.apache.commons.lang3.builder.ToStringBuilder
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.history.HistoryFormatUtils
import org.projectforge.framework.persistence.history.HistoryLoadContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Objects

private val log = KotlinLogging.logger {}

/**
 * @author Mario Groß (m.gross@micromata.de)
 */
@Service
class HRPlanningDao protected constructor() : BaseDao<HRPlanningDO>(HRPlanningDO::class.java) {
    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var userDao: UserDao

    init {
        userRightId = USER_RIGHT_ID
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    /**
     * @param sheet
     * @param projektId If null, then projekt will be set to null;
     */
    fun setProjekt(sheet: HRPlanningEntryDO, projektId: Long?) {
        val projekt = projektDao.findOrLoad(projektId)
        sheet.projekt = projekt
    }

    /**
     * @param sheet
     * @param userId If null, then user will be set to null;
     */
    fun setUser(sheet: HRPlanningDO, userId: Long?) {
        val user = userDao.findOrLoad(userId)
        sheet.user = user
    }

    /**
     * Does an entry with the same user and week of year already exist?
     *
     * @param planning
     * @return If week or user id is not given, return false.
     */
    fun doesEntryAlreadyExist(planning: HRPlanningDO?): Boolean {
        Objects.requireNonNull<HRPlanningDO?>(planning)
        return doesEntryAlreadyExist(planning!!.id, planning.userId, planning.week)
    }

    /**
     * Does an entry with the same user and week of year already exist?
     *
     * @param planningId Id of the current planning or null if new.
     * @param userId
     * @param week
     * @return If week or user id is not given, return false.
     */
    fun doesEntryAlreadyExist(planningId: Long?, userId: Long?, week: LocalDate?): Boolean {
        if (week == null || userId == null) {
            return false
        }
        val other: HRPlanningDO?
        if (planningId == null) {
            // New entry
            other = persistenceService.selectNamedSingleResult<HRPlanningDO>(
                HRPlanningDO.FIND_BY_USER_AND_WEEK,
                HRPlanningDO::class.java,
                "userId" to userId,
                "week" to week
            )
        } else {
            // Entry already exists. Check collision:
            other = persistenceService.selectNamedSingleResult<HRPlanningDO>(
                HRPlanningDO.FIND_OTHER_BY_USER_AND_WEEK,
                HRPlanningDO::class.java,
                "userId" to userId,
                "week" to week,
                "id" to planningId,
            )
        }
        return other != null
    }

    fun getEntry(user: PFUserDO, week: LocalDate): HRPlanningDO? {
        return getEntry(user.id, week)
    }

    fun getEntry(userId: Long?, week: LocalDate): HRPlanningDO? {
        var day = PFDay.Companion.from(week)
        if (!day.isBeginOfWeek) {
            log.error("Date is not begin of week, try to change date: " + day.isoString)
            day = day.beginOfWeek
        }
        val planning = persistenceService.selectNamedSingleResult<HRPlanningDO>(
            HRPlanningDO.FIND_BY_USER_AND_WEEK,
            HRPlanningDO::class.java,
            "userId" to userId,
            "week" to day.localDate,
        )
        if (planning == null) {
            return null
        }
        if (accessChecker.hasLoggedInUserSelectAccess(userRightId, planning, false)) {
            return planning
        } else {
            return null
        }
    }

    override fun select(filter: BaseSearchFilter): List<HRPlanningDO> {
        val myFilter = filter as HRPlanningFilter
        if (myFilter.getStopDay() != null) {
            val dateTime = PFDateTime.Companion.fromOrNow(myFilter.getStopDay()).endOfDay
            myFilter.setStopDay(dateTime.localDate)
        }
        val queryFilter = buildQueryFilter(myFilter)
        return select(queryFilter)
    }

    private fun entryHasUpdates(entry: HRPlanningEntryDO, existingPlanning: HRPlanningDO?): Boolean {
        if (entry.id == null) {
            return true
        }
        if (existingPlanning != null) {
            for (existingEntry in existingPlanning.entries!!) {
                if (!existingEntry.deleted && existingEntry.id == entry.id) {
                    return !existingEntry.hasNoFieldChanges(entry)
                }
            }
        }
        return false
    }

    fun buildQueryFilter(filter: HRPlanningFilter): QueryFilter {
        val queryFilter = QueryFilter(filter)
        if (filter.getUserId() != null) {
            val user = PFUserDO()
            user.id = filter.getUserId()
            queryFilter.add(QueryFilter.Companion.eq("user", user))
        }
        if (filter.getStartDay() != null && filter.getStopDay() != null) {
            queryFilter.add(QueryFilter.Companion.between<LocalDate>("week", filter.getStartDay(), filter.getStopDay()))
        } else if (filter.getStartDay() != null) {
            queryFilter.add(QueryFilter.Companion.ge<LocalDate>("week", filter.getStartDay()))
        } else if (filter.getStopDay() != null) {
            queryFilter.add(QueryFilter.Companion.le<LocalDate>("week", filter.getStopDay()))
        }
        if (filter.getProjektId() != null) {
            queryFilter.add(QueryFilter.Companion.eq("projekt.id", filter.getProjektId()))
        }
        queryFilter.addOrder(SortProperty.Companion.desc("week"))
        if (log.isDebugEnabled()) {
            log.debug(ToStringBuilder.reflectionToString(filter))
        }
        return queryFilter
    }

    /**
     *
     *  * Checks week date on: monday, 0:00:00.000 and if check fails then the date will be set to.
     *  * Check deleted entries and re-adds them instead of inserting a new entry, if exist.
     *
     */
    override fun onInsertOrModify(obj: HRPlanningDO, operationType: OperationType) {
        var day: PFDay = PFDay.fromOrNull(obj.week) ?: return
        if (!day.isBeginOfWeek) {
            log.error("Date is not begin of week, try to change date: " + day.isoString)
            day = day.beginOfWeek
            obj.week = day.date
        }

        if (!accessChecker.isLoggedInUserMemberOfGroup(
                ProjectForgeGroup.HR_GROUP,
                ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.CONTROLLING_GROUP
            )
        ) {
            var existingPlanning: HRPlanningDO? = null
            if (obj.id != null) {
                existingPlanning = find(obj.id, false)
            }
            for (entry in obj.entries!!) {
                val projekt = entry.projekt
                if (entryHasUpdates(entry, existingPlanning) && projekt != null) {
                    var userHasRightForProject = false
                    val userId = ThreadLocalUserContext.loggedInUser!!.id
                    val headOfBusinessManagerId =
                        if (projekt.headOfBusinessManager != null) projekt.headOfBusinessManager!!.id else null
                    val projectManagerId = if (projekt.projectManager != null) projekt.projectManager!!.id else null
                    val salesManageId = if (projekt.salesManager != null) projekt.salesManager!!.id else null
                    if (userId != null && (userId == headOfBusinessManagerId || userId == projectManagerId || userId == salesManageId)) {
                        userHasRightForProject = true
                    }

                    if (projekt.projektManagerGroup != null
                        && userGroupCache.isUserMemberOfGroup(userId, projekt.projektManagerGroupId)
                    ) {
                        userHasRightForProject = true
                    }
                    if (!userHasRightForProject) {
                        throw UserException("hr.planning.entry.error.noRightForProject", projekt.name)
                    }
                }
            }
        }
    }

    override fun prepareHibernateSearch(obj: HRPlanningDO, operationType: OperationType) {
        val entries = obj.entries
        if (entries != null) {
            for (entry in entries) {
                projektDao.initializeProjektManagerGroup(entry.projekt)
            }
        }
        val user = obj.user
        if (user != null) {
            obj.user = userGroupCache.getUser(user.id)
        }
    }

    override fun newInstance(): HRPlanningDO {
        return HRPlanningDO()
    }

    /**
     * Gets history entries of super and adds all history entries of the HRPlanningEntryDO children.
     */
    override fun addOwnHistoryEntries(obj: HRPlanningDO, context: HistoryLoadContext) {
        obj.entries?.forEach { position ->
            var prefix = if (position.projekt != null) position.projektName else position.status.toString()
            if (prefix == null) {
                prefix = "" // Just in case.
            }
            historyService.loadAndMergeHistory(position, context) { entry ->
                HistoryFormatUtils.setPropertyNameForListEntries(entry, prefix)
            }
        }
    }

    override val additionalHistorySearchDOs: Array<Class<*>>
        get() = ADDITIONAL_SEARCH_DOS

    companion object {
        val ADDITIONAL_SEARCH_FIELDS = arrayOf<String>(
            "entries.projekt.name", "entries.projekt.kunde.name", "user.username", "user.firstname",
            "user.lastname"
        )

        val USER_RIGHT_ID: UserRightId = UserRightId.PM_HR_PLANNING

        private val log: Logger = LoggerFactory.getLogger(HRPlanningDao::class.java)

        private val ADDITIONAL_SEARCH_DOS = arrayOf<Class<*>>(HRPlanningEntryDO::class.java)
    }
}
