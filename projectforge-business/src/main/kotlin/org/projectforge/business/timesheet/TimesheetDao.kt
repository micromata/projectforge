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

package org.projectforge.business.timesheet

import jakarta.annotation.PostConstruct
import jakarta.persistence.Tuple
import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.Validate
import org.apache.commons.lang3.builder.ToStringBuilder
import org.projectforge.business.common.AutoCompletionUtils
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserDao
import org.projectforge.common.extensions.isZeroOrNull
import org.projectforge.common.i18n.MessageParam
import org.projectforge.common.i18n.UserException
import org.projectforge.common.task.TaskStatus
import org.projectforge.common.task.TimesheetBookingStatus
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.and
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ge
import org.projectforge.framework.persistence.api.QueryFilter.Companion.gt
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.QueryFilter.Companion.le
import org.projectforge.framework.persistence.api.QueryFilter.Companion.lt
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.SQLHelper.getYearsByTupleOfDate
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.projectforge.framework.utils.NumberHelper.isEqual
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class TimesheetDao : BaseDao<TimesheetDO>(TimesheetDO::class.java) {
    @Value("\${projectforge.timesheets.timeSavingsByAI.enabled}")
    var timeSavingsByAIEnabled = false
        private set

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var taskTree: TaskTree

    @PostConstruct
    fun init() {
        AITimeSavings.timeSavingsByAIEnabled = timeSavingsByAIEnabled
    }

    /**
     * Return list of configured tags including any already given tag in time sheet.
     */
    @JvmOverloads
    fun getTags(currentTag: String? = null): List<String>? {
        val tags = Configuration.instance.getStringValue(ConfigurationParam.TIMESHEET_TAGS)?.split(";")
        if (currentTag.isNullOrBlank()) {
            return tags
        }
        if (tags.isNullOrEmpty()) {
            return listOf(currentTag)
        }
        return if (tags.contains(currentTag)) {
            tags
        } else {
            tags + currentTag
        }
    }

    open fun showTimesheetsOfOtherUsers(): Boolean {
        return accessChecker.isLoggedInUserMemberOfGroup(
            ProjectForgeGroup.CONTROLLING_GROUP,
            ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.HR_GROUP,
            ProjectForgeGroup.ORGA_TEAM,
            ProjectForgeGroup.PROJECT_MANAGER,
            ProjectForgeGroup.PROJECT_ASSISTANT
        )
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    /**
     * List of all years with time sheets of the given user: select min(startTime), max(startTime) from t_timesheet where
     * user=?.
     */
    open fun getYears(userId: Long?): IntArray {
        val minMaxDate = persistenceService.selectNamedSingleResult(
            TimesheetDO.SELECT_MIN_MAX_DATE_FOR_USER,
            Tuple::class.java,
            Pair("userId", userId),
        )
        return getYearsByTupleOfDate(minMaxDate)
    }

    /**
     * @param userId If null, then task will be set to null;
     * @see BaseDao.findOrLoad
     */
    open fun setUser(sheet: TimesheetDO, userId: Long?) {
        userId ?: return
        val user = userDao.findOrLoad(userId)
        sheet.user = user
    }

    /**
     * @param taskId If null, then task will be set to null;
     * @see TaskTree.getTaskById
     */
    open fun setTask(sheet: TimesheetDO, taskId: Long?) {
        val task = taskTree.getTaskById(taskId)
        sheet.task = task
    }

    /**
     * @param kost2Id If null, then kost2 will be set to null;
     * @see BaseDao.findOrLoad
     */
    open fun setKost2(sheet: TimesheetDO, kost2Id: Long?) {
        kost2Id ?: return
        val kost2 = kost2Dao.findOrLoad(kost2Id)
        sheet.kost2 = kost2
    }

    /**
     * Gets the available Kost2DO's for the given time sheet. The task must already be assigned to this time sheet.
     *
     * @return Available list of Kost2DO's or null, if not exist.
     */
    open fun getKost2List(timesheet: TimesheetDO?): List<Kost2DO>? {
        return if (timesheet?.taskId == null) {
            null
        } else taskTree.getKost2List(timesheet.taskId)
    }

    private fun buildQueryFilter(filter: TimesheetFilter): QueryFilter {
        val queryFilter = QueryFilter(filter)
        if (filter.userId != null) {
            queryFilter.add(eq("user.id", filter.userId))
        }
        if (filter.startTime != null && filter.stopTime != null) {
            queryFilter.add(
                and(
                    ge("stopTime", filter.startTime), le("startTime", filter.stopTime)
                )
            )
        } else if (filter.startTime != null) {
            queryFilter.add(ge("startTime", filter.startTime))
        } else if (filter.stopTime != null) {
            queryFilter.add(le("startTime", filter.stopTime))
        }
        if (filter.taskId != null) {
            if (filter.isRecursive) {
                val node = taskTree.getTaskNodeById(filter.taskId)!!
                val taskIds = node.descendantIds
                taskIds.add(node.id)
                queryFilter.add(isIn<Any>("task.id", taskIds))
                if (log.isDebugEnabled) {
                    log.debug { "search in tasks: $taskIds" }
                }
            } else {
                queryFilter.add(eq("task.id", filter.taskId))
            }
        }
        if (filter.orderType == OrderDirection.DESC) {
            queryFilter.addOrder(desc("startTime"))
        } else {
            queryFilter.addOrder(asc("startTime"))
        }
        if (log.isDebugEnabled) {
            log.debug(ToStringBuilder.reflectionToString(filter))
        }
        return queryFilter
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.selectForSearchDao
     */
    override fun selectForSearchDao(filter: BaseSearchFilter, checkAccess: Boolean): List<TimesheetDO> {
        val timesheetFilter = TimesheetFilter(filter)
        if (filter.modifiedByUserId == null) {
            timesheetFilter.userId = ThreadLocalUserContext.loggedInUserId
        }
        return select(timesheetFilter, checkAccess)
    }

    /**
     * Gets the list filtered by the given filter.
     */
    @Throws(AccessException::class)
    override fun select(filter: BaseSearchFilter): List<TimesheetDO> {
        return internalGetList(filter, true)
    }

    fun internalGetList(
        filter: BaseSearchFilter?,
        checkAccess: Boolean,
    ): List<TimesheetDO> {
        val myFilter = if (filter is TimesheetFilter) {
            filter
        } else {
            TimesheetFilter(filter)
        }
        if (myFilter.stopTime != null) {
            val dateTime = from(myFilter.stopTime).endOfDay
            myFilter.stopTime = dateTime.utilDate
        }
        if (accessChecker.isLoggedInUserMemberOfGroup(
                ProjectForgeGroup.CONTROLLING_GROUP, ProjectForgeGroup.FINANCE_GROUP
            )
        ) {
            // Financial staff needs sometimes to query a lot of time sheets for exporting, statistics etc.
            myFilter.maxRows = 100000
        }
        val queryFilter = buildQueryFilter(myFilter)
        var result = select(queryFilter, checkAccess = checkAccess)
        if (myFilter.isOnlyBillable) {
            val list: List<TimesheetDO> = result
            result = ArrayList()
            for (entry in list) {
                if (entry.kost2?.kost2Art?.fakturiert == true) {
                    result.add(entry)
                }
            }
        }
        return result
    }

    /**
     * Rechecks the time sheet overlaps.
     */
    override fun afterInsertOrModify(obj: TimesheetDO, operationType: OperationType) {
        taskTree.resetTotalDuration(obj.taskId!!)
    }

    /**
     * Checks the start and stop time. If seconds or millis is not null, a RuntimeException will be thrown.
     */
    override fun onInsertOrModify(obj: TimesheetDO, operationType: OperationType) {
        validateTimestamp(obj.startTime, "startTime")
        validateTimestamp(obj.stopTime, "stopTime")
        validateTimeSavingsByAI(obj)?.let { throw UserException(it) }
        if (obj.duration < 60000) {
            throw UserException("timesheet.error.zeroDuration") // "Duration of time sheet must be at minimum 60s!
        }
        if (obj.duration > MAXIMUM_DURATION) {
            throw UserException("timesheet.error.maximumDurationExceeded")
        }
        Validate.isTrue(obj.startTime!!.before(obj.stopTime), "Stop time of time sheet is before start time!")
        if (Configuration.instance.isCostConfigured) {
            val kost2List = taskTree.getKost2List(obj.taskId)
            val kost2Id = obj.kost2Id
            if (kost2Id == null) {
                // Check, if there is any cost definition in any descendant task:
                val taskNode = taskTree.getTaskNodeById(obj.taskId)
                if (taskNode != null) {
                    val descendents = taskNode.descendantIds
                    for (taskId in descendents) {
                        if (CollectionUtils.isNotEmpty(taskTree.getKost2List(taskId))) {
                            // But Kost2 is available for sub task, so user should book his time sheet
                            // on a sub task with kost2s.
                            throw UserException("timesheet.error.kost2NeededChooseSubTask")
                        }
                    }
                }
            }
            if (!kost2List.isNullOrEmpty()) {
                if (kost2Id == null) {
                    throw UserException("timesheet.error.kost2Required")
                }
                var kost2IdFound = false
                kost2List.forEach { kost2 ->
                    if (isEqual(kost2Id, kost2.id)) {
                        kost2IdFound = true
                        return@forEach
                    }
                }
                if (!kost2IdFound) {
                    throw UserException("timesheet.error.invalidKost2") // Kost2Id of time sheet is not available in the task's kost2 list!
                }
            } else {
                if (kost2Id != null) {
                    throw UserException("timesheet.error.invalidKost2") // Kost2Id can't be given for task without any kost2 entries!
                }
            }
        }
    }

    override fun onUpdate(obj: TimesheetDO, dbObj: TimesheetDO) {
        if (compareValues(obj.taskId, dbObj.taskId) != 0) {
            taskTree.resetTotalDuration(dbObj.taskId!!)
        }
    }

    override fun prepareHibernateSearch(obj: TimesheetDO, operationType: OperationType) {
        val user = obj.user
        if (user != null && !HibernateUtils.isFullyInitialized(user)) {
            obj.user = userGroupCache.getUser(user.id)
        }
        val task = obj.task
        if (task != null && !HibernateUtils.isFullyInitialized(task)) {
            obj.task = taskTree.getTaskById(task.id)
        }
    }

    fun validateTimeSavingsByAI(timesheet: TimesheetDO): String? {
        return validateTimeSavingsByAI(timesheet.timeSavedByAI, timesheet.timeSavedByAIUnit)
    }

    fun validateTimeSavingsByAI(timeSavedByAI: BigDecimal?, unit: TimesheetDO.TimeSavedByAIUnit?): String? {
        if (timeSavedByAI.isZeroOrNull()) {
            return null
        }
        unit ?: return "timesheet.ai.timeSavedByAI.error.unitMissing"
        if (unit == TimesheetDO.TimeSavedByAIUnit.PERCENTAGE) {
            if (timeSavedByAI!! < BigDecimal.ZERO || timeSavedByAI > BigDecimal(99)) {
                return "timesheet.ai.timeSavedByAI.error.invalidPercentage"
            }
        }
        // All values (also negative ones) are allowed for hours.
        return null
    }

    private fun validateTimestamp(date: Date?, name: String) {
        if (date == null) {
            return
        }
        val cal = Calendar.getInstance()
        cal.time = date
        Validate.isTrue(cal[Calendar.MILLISECOND] == 0, "Millis of $name is not 0!")
        Validate.isTrue(cal[Calendar.SECOND] == 0, "Seconds of $name is not 0!")
        val m = cal[Calendar.MINUTE]
        Validate.isTrue(m % 5 == 0, "Minutes of $name must be 00, 5, 10, ..., 55")
    }

    /**
     * Checks if the time sheet overlaps with another time sheet of the same user. Should be checked on every insert or
     * update (also undelete). For time collision detection deleted time sheets are ignored.
     *
     * @return The existing time sheet with the time period collision.
     */
    open fun hasTimeOverlap(timesheet: TimesheetDO, throwException: Boolean): Boolean {
        val begin = System.currentTimeMillis()
        requireNotNull(timesheet.user)
        val queryFilter = QueryFilter()
        queryFilter.add(eq("user", timesheet.user!!))
        queryFilter.add(eq("deleted", false))
        timesheet.stopTime?.let { queryFilter.add(lt("startTime", it)) }
        timesheet.startTime?.let { queryFilter.add(gt("stopTime", it)) }
        if (timesheet.id != null) {
            // Update time sheet, do not compare with itself.
            queryFilter.add(ne("id", timesheet.id!!))
        }
        val list = select(queryFilter)
        if (list.isNotEmpty()) {
            val ts = list[0]
            if (throwException) {
                log.info("Time sheet collision detected of time sheet $timesheet with existing time sheet $ts")
                val startTime = DateHelper.formatIsoTimestamp(ts.startTime)
                val stopTime = DateHelper.formatIsoTimestamp(ts.stopTime)
                throw UserException(
                    "timesheet.error.timeperiodOverlapDetection", MessageParam(
                        ts.id
                    ), MessageParam(startTime), MessageParam(stopTime)
                )
            }
            val end = System.currentTimeMillis()
            log.info("TimesheetDao.hasTimeOverlap took: " + (end - begin) + " ms.")
            return true
        }
        val end = System.currentTimeMillis()
        log.info("TimesheetDao.hasTimeOverlap took: " + (end - begin) + " ms.")
        return false
    }

    /**
     * return Always true, no generic select access needed for address objects.
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    override fun hasAccess(
        user: PFUserDO, obj: TimesheetDO?, oldObj: TimesheetDO?, operationType: OperationType, throwException: Boolean
    ): Boolean {
        require(obj != null) { "Given timesheet as obj parameter mustn't be null." }
        if (accessChecker.userEquals(user, obj.user)) {
            // Own time sheet
            if (!accessChecker.hasPermission(
                    user, obj.taskId, AccessType.OWN_TIMESHEETS, operationType, throwException
                )
            ) {
                return false
            }
        } else {
            // Foreign time sheet
            if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
                return true
            }
            if (!accessChecker.hasPermission(
                    user, obj.taskId, AccessType.TIMESHEETS, operationType, throwException
                )
            ) {
                return false
            }
        }
        return if (operationType == OperationType.DELETE) {
            // UPDATE and INSERT is already checked, SELECT will be ignored.
            checkTimesheetProtection(user, obj, null, operationType, throwException)
        } else true
    }

    /**
     * User can always see his own time sheets. But if he has no access then the location and description values are
     * hidden (empty strings).
     */
    override fun hasUserSelectAccess(user: PFUserDO, obj: TimesheetDO, throwException: Boolean): Boolean {
        if (!hasAccess(user, obj, null, OperationType.SELECT, false)) {
            // User has no access by definition.
            if (accessChecker.userEquals(user, obj.user) || accessChecker.isUserMemberOfGroup(
                    user,
                    ProjectForgeGroup.PROJECT_MANAGER
                )
            ) {
                if (!accessChecker.userEquals(user, obj.user)) {
                    // Check protection of privacy for foreign time sheets:
                    val pathToRoot = taskTree.getPathToRoot(obj.taskId)
                    for (node in pathToRoot) {
                        if (node.task.protectionOfPrivacy) {
                            return false
                        }
                    }
                }
                // An user should see his own time sheets, but the values should be hidden.
                // A project manager should also see all time sheets, but the values should be hidden.
                // em.detach(obj)
                obj.description = HIDDEN_FIELD_MARKER
                obj.location = HIDDEN_FIELD_MARKER
                log.debug { "User has no access to own time sheet (or project manager): $obj" }
                return true
            }
        }
        return super.hasUserSelectAccess(user, obj, throwException)
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: TimesheetDO, throwException: Boolean): Boolean {
        return hasAccess(user, obj, null, OperationType.SELECT, throwException)
    }

    override fun hasUpdateAccess(
        user: PFUserDO, obj: TimesheetDO, dbObj: TimesheetDO?, throwException: Boolean
    ): Boolean {
        requireNotNull(dbObj)
        requireNotNull(dbObj.taskId)
        requireNotNull(obj.taskId)
        if (!hasAccess(user, obj, dbObj, OperationType.UPDATE, throwException)) {
            return false
        }
        if (dbObj.userId != obj.userId) {
            // User changes the owner of the time sheet:
            if (!hasAccess(user, dbObj, null, OperationType.DELETE, throwException)) {
                // Deleting of time sheet of another user is not allowed.
                return false
            }
        }
        if (dbObj.taskId != obj.taskId) {
            // User moves the object to another task:
            if (!hasAccess(user, obj, null, OperationType.INSERT, throwException)) {
                // Inserting of object under new task not allowed.
                return false
            }
            if (!hasAccess(user, dbObj, null, OperationType.DELETE, throwException)) {
                // Deleting of object under old task not allowed.
                return false
            }
        }
        if (hasTimeOverlap(obj, throwException)) {
            return false
        }
        var result = checkTimesheetProtection(user, obj, dbObj, OperationType.UPDATE, throwException)
        if (result) {
            result = checkTaskBookable(obj, dbObj, OperationType.UPDATE, throwException)
        }
        return result
    }

    override fun hasInsertAccess(user: PFUserDO, obj: TimesheetDO?, throwException: Boolean): Boolean {
        requireNotNull(obj) { "Given timesheet as obj parameter mustn't be null." }
        if (!hasAccess(user, obj, null, OperationType.INSERT, throwException)) {
            return false
        }
        if (hasTimeOverlap(obj, throwException)) {
            return false
        }
        var result = checkTimesheetProtection(user, obj, null, OperationType.INSERT, throwException)
        if (result) {
            result = checkTaskBookable(obj, null, OperationType.INSERT, throwException)
        }
        return result
    }

    /**
     * Checks whether the time sheet is book-able or not. The checks are:
     *
     *  1. Only for update mode: If the time sheet is unmodified in start and stop time, kost2, task and user then return
     * true without further checking.
     *  1. Is the task or any of the ancestor tasks closed or deleted?
     *  1. Has the task or any of the ancestor tasks the TimesheetBookingStatus.TREE_CLOSED?
     *  1. Is the task not a leaf node and has this task or ancestor task the booking status ONLY_LEAFS?
     *  1. Does any of the descendant task node has an assigned order position?
     *
     *
     * @param timesheet    The time sheet to insert or update.
     * @param oldTimesheet The origin time sheet from the data base (could be null, if no update is done).
     * @return True if none of the rules above matches.
     */
    @JvmOverloads
    open fun checkTaskBookable(
        timesheet: TimesheetDO,
        oldTimesheet: TimesheetDO?,
        operationType: OperationType,
        throwException: Boolean,
        checkTaskTreeRefresh: Boolean = false,
    ): Boolean {
        if (operationType == OperationType.UPDATE) {
            if (timesheet.startTime!!.time == oldTimesheet!!.startTime!!.time && timesheet.stopTime!!.time == oldTimesheet.stopTime!!.time && timesheet.kost2Id == oldTimesheet.kost2Id && timesheet.taskId == oldTimesheet.taskId && timesheet.userId == oldTimesheet.userId) {
                // Only minor fields are modified (description, location etc.).
                return true
            }
        }
        val taskNode = taskTree.getTaskNodeById(timesheet.taskId, checkTaskTreeRefresh)
        // 1. Is the task or any of the ancestor tasks closed, deleted or has the booking status TREE_CLOSED?
        var node: TaskNode? = taskNode
        do {
            val task = node!!.task
            var errorMessage: String? = null
            if (task.deleted) {
                errorMessage = "timesheet.error.taskNotBookable.taskDeleted"
            } else if (!task.status.isIn(TaskStatus.O, TaskStatus.N)) {
                errorMessage = "timesheet.error.taskNotBookable.taskNotOpened"
            } else if (task.timesheetBookingStatus == TimesheetBookingStatus.TREE_CLOSED) {
                errorMessage = "timesheet.error.taskNotBookable.treeClosedForBooking"
            }
            if (errorMessage != null) {
                if (throwException) {
                    throw AccessException(errorMessage, task.title + " (#" + task.id + ")")
                }
                return false
            }
            node = node.parent
        } while (node != null)
        // 2. Has the task the booking status NO_BOOKING?
        var bookingStatus = taskNode!!.task.timesheetBookingStatus
        node = taskNode
        while (bookingStatus == TimesheetBookingStatus.INHERIT && node?.parent != null) {
            node = node.parent
            bookingStatus = node.task.timesheetBookingStatus
        }
        if (bookingStatus == TimesheetBookingStatus.NO_BOOKING) {
            if (throwException) {
                throw AccessException(
                    "timesheet.error.taskNotBookable.taskClosedForBooking",
                    taskNode.task.title + " (#" + taskNode.id + ")"
                )
            }
            return false
        }
        if (taskNode.hasChildren()) {
            // 3. Is the task not a leaf node and has this task or ancestor task the booking status ONLY_LEAFS?
            node = taskNode
            do {
                val task = node!!.task
                if (task.timesheetBookingStatus == TimesheetBookingStatus.ONLY_LEAFS) {
                    if (throwException) {
                        throw AccessException(
                            "timesheet.error.taskNotBookable.onlyLeafsAllowedForBooking",
                            taskNode.task.title + " (#" + taskNode.id + ")"
                        )
                    }
                    return false
                }
                node = node.parent
            } while (node != null)
            // 4. Does any of the descendant task node has an assigned order position?
            for (child in taskNode.children) {
                if (taskTree.hasOrderPositions(child.id, true)) {
                    if (throwException) {
                        throw AccessException(
                            "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks",
                            taskNode.task.title + " (#" + taskNode.id + ")"
                        )
                    }
                    return false
                }
            }
        }
        return true
    }

    /**
     * Checks if there exists any time sheet protection on the corresponding task or one of the ancestor tasks. If the
     * times sheet is protected and the duration of this time sheet is modified, and AccessException will be thrown. <br></br>
     * Checks insert, update and delete operations. If an existing time sheet has to be modified, the check will only be
     * done, if any modifications of the time stamps is done (e. g. descriptions of the task are allowed if the start and
     * stop time is untouched).
     *
     * @param oldTimesheet   (null for delete and insert)
     * @param throwException If true and the time sheet protection is violated then an AccessException will be thrown.
     * @return true, if no time sheet protection is violated or if the logged in user is member of the finance group.
     * @see ProjectForgeGroup.FINANCE_GROUP
     */
    open fun checkTimesheetProtection(
        user: PFUserDO?,
        timesheet: TimesheetDO,
        oldTimesheet: TimesheetDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP) && !accessChecker.userEquals(
                user,
                timesheet.user
            )
        ) {
            // Member of financial group are able to book foreign time sheets.
            return true
        }
        if (operationType == OperationType.UPDATE) {
            if (timesheet.startTime!!.time == oldTimesheet!!.startTime!!.time && timesheet.stopTime!!.time == oldTimesheet.stopTime!!.time && timesheet.kost2Id == oldTimesheet.kost2Id) {
                return true
            }
        }
        val taskNode = taskTree.getTaskNodeById(timesheet.taskId)
        requireNotNull(taskNode)
        val list = taskNode.pathToRoot
        list.add(0, taskTree.rootTaskNode)
        for (node in list) {
            val date = node.task.protectTimesheetsUntil ?: continue
            val dateTime = from(date).endOfDay
            //New and existing startdate have to be checked for protection
            if (oldTimesheet != null && oldTimesheet.startTime!!.before(dateTime.utilDate) || timesheet.startTime!!.before(
                    dateTime.utilDate
                )
            ) {
                if (throwException) {
                    throw AccessException(
                        "timesheet.error.timesheetProtectionVioloation",
                        node.task.title + " (#" + node.taskId + ")",
                        DateHelper.formatIsoDate(dateTime.utilDate)
                    )
                }
                return false
            }
        }
        return true
    }

    /**
     * Get all locations of the user's time sheet (not deleted ones) with modification date within last year.
     */
    @Deprecated("Used by deprecated Wicket pages.")
    open fun getLocationAutocompletion(searchString: String?): List<String> {
        checkLoggedInUserSelectAccess()
        val oneYearAgo = now().minusDays(365)
        return persistenceService.executeNamedQuery(
            TimesheetDO.SELECT_USED_LOCATIONS_BY_USER_AND_LOCATION_SEARCHSTRING,
            String::class.java,
            Pair("lastUpdate", oneYearAgo.utilDate),
            Pair("locationSearch", "%${searchString?.lowercase() ?: ""}%"),
            Pair("userId", ThreadLocalUserContext.loggedInUserId),
        )
    }

    /**
     * Get all used references of time sheets with given task id or used in any sub task.
     */
    open fun getUsedReferences(taskId: Long): List<String> {
        checkLoggedInUserSelectAccess()
        return persistenceService.executeNamedQuery(
            TimesheetDO.SELECT_REFERENCES_BY_TASK_ID,
            String::class.java,
            Pair("taskIds", taskTree.getAncestorAndDescendantTaskIs(taskId, true)),
        )
    }

    open fun getUsedReferences(taskId: Long, search: String?): List<String> {
        return AutoCompletionUtils.filter(getUsedReferences(taskId), search)
    }

    /**
     * Get all locations of the user's time sheet (not deleted ones) with modification date within last year.
     *
     * @param sinceDate Limit the result to the recent locations of time sheet updated after sinceDate.
     * @return result as Json object.
     */
    open fun getRecentLocation(sinceDate: Date?): Collection<String> {
        checkLoggedInUserSelectAccess()
        log.debug { "Get recent locations from the database." }
        return persistenceService.executeNamedQuery(
            TimesheetDO.SELECT_RECENT_USED_LOCATIONS_BY_USER_AND_LAST_UPDATE,
            String::class.java,
            Pair("userId", ThreadLocalUserContext.requiredLoggedInUserId),
            Pair("lastUpdate", sinceDate),
        )
    }

    override fun newInstance(): TimesheetDO {
        return TimesheetDO()
    }

    companion object {
        /**
         * Maximum allowed duration of time sheets is 14 hours.
         */
        const val MAXIMUM_DURATION = (1000 * 3600 * 14).toLong()
        const val HIDDEN_FIELD_MARKER = "[...]"

        private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "user.id",
            "user.username",
            "user.firstname",
            "user.lastname",
            "kost2.nummer",
            "kost2.description",
            "kost2.projekt.name"
        )
    }
}
