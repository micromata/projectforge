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

package org.projectforge.business.task

import jakarta.persistence.Tuple
import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserDao
import org.projectforge.common.i18n.UserException
import org.projectforge.common.task.TaskStatus
import org.projectforge.common.task.TimesheetBookingStatus
import org.projectforge.database.DatabaseSupport
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.QueryFilter.Companion.not
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.web.WicketSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.*
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class TaskDao : BaseDao<TaskDO>(TaskDO::class.java), Serializable { // Serializable needed for Wicket.
    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var taskTree: TaskTree

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    fun getTaskTree(): TaskTree {
        return TaskTreeHelper.getTaskTree()
    }

    /**
     * Checks constraint violation.
     */
    override fun onSaveOrModify(obj: TaskDO, context: PfPersistenceContext) {
        synchronized(this) {
            checkConstraintVioloation(obj, context)
        }
    }

    /**
     * @param task
     * @param parentTaskId If null, then task will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setParentTask(task: TaskDO, parentTaskId: Long): TaskDO {
        val parentTask = getOrLoad(parentTaskId)
        task.parentTask = parentTask
        return task
    }

    /**
     * @param task
     * @param predecessorId If null, then task will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setGanttPredecessor(task: TaskDO, predecessorId: Long) {
        val predecessor = getOrLoad(predecessorId)
        task.ganttPredecessor = predecessor
    }

    /**
     * @param task
     * @param responsibleUserId If null, then task will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setResponsibleUser(task: TaskDO, responsibleUserId: Long) {
        val user = userDao.getOrLoad(responsibleUserId)
        task.responsibleUser = user
    }

    /**
     * Gets the total duration of all time sheets of all tasks (excluding the child tasks).
     */
    fun readTotalDurations(): List<Array<Any>> {
        log.debug("Calculating duration for all tasks")
        val intervalInSeconds = DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime")
        if (intervalInSeconds != null) {
            val result = persistenceService.executeQuery(
                "select $intervalInSeconds, task.id from TimesheetDO where deleted=false group by task.id",
                Tuple::class.java,
            )
            // select intervalInSeconds, task.id from TimesheetDO where deleted=false group by task.id
            val list = mutableListOf<Array<Any>>()
            for (tuple in result) {
                list.add(arrayOf(tuple[0], tuple[1]))
            }
            return list
        }

        val result = persistenceService.executeQuery(
            "select startTime, stopTime, task.id from TimesheetDO where deleted=false order by task.id",
            Tuple::class.java,
        )
        // select startTime, stopTime, task.id from TimesheetDO where deleted=false order by task.id");
        val list = mutableListOf<Array<Any>>()
        if (!CollectionUtils.isEmpty(result)) {
            var currentTaskId: Long? = null
            var totalDuration: Long = 0
            for (oa in result) {
                val startTime = oa[0] as Date
                val stopTime = oa[1] as Date
                val taskId = oa[2] as Long
                val duration = (stopTime.time - startTime.time) / 1000
                if (currentTaskId == null || currentTaskId != taskId) {
                    if (currentTaskId != null) {
                        list.add(arrayOf(totalDuration, currentTaskId))
                    }
                    // New row.
                    currentTaskId = taskId
                    totalDuration = 0
                }
                totalDuration += duration
            }
            if (currentTaskId != null) {
                list.add(arrayOf(totalDuration, currentTaskId))
            }
        }
        return list
    }

    /**
     * Gets the total duration of all time sheets of the given task (excluding the child tasks).
     * @return Duration in seconds.
     */
    fun readTotalDuration(taskId: Long?): Long {
        log.debug { "Calculating duration for task $taskId" }
        val intervalInSeconds = DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime")
        if (intervalInSeconds != null) {
            // Expected type is Integer or Long.
            val value = persistenceService.selectSingleResult(
                "select $intervalInSeconds from TimesheetDO where task.id=:taskId and task.deleted=false group by task.id",
                Number::class.java,
                Pair("taskId", taskId),
            )
            // select DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime") from TimesheetDO where task.id = :taskId and deleted=false")
            return value?.toLong() ?: 0L
        }
        val result = persistenceService.executeNamedQuery(
            TimesheetDO.FIND_START_STOP_BY_TASKID,
            Tuple::class.java,
            Pair("taskId", taskId),
        )
        if (CollectionUtils.isEmpty(result)) {
            return 0L
        }
        var totalDuration: Long = 0
        for (oa in result) {
            val startTime = oa[0] as Date
            val stopTime = oa[1] as Date
            val duration = stopTime.time - startTime.time
            totalDuration += duration
        }
        return totalDuration / 1000
    }

    @Throws(AccessException::class)
    override fun getList(filter: BaseSearchFilter, context: PfPersistenceContext): List<TaskDO> {
        val myFilter = if (filter is TaskFilter) {
            filter
        } else {
            TaskFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        val col: MutableCollection<TaskStatus?> = ArrayList(4)
        if (myFilter.isNotOpened) {
            col.add(TaskStatus.N)
        }
        if (myFilter.isOpened) {
            col.add(TaskStatus.O)
        }
        if (myFilter.isClosed) {
            col.add(TaskStatus.C)
        }
        if (col.isNotEmpty()) {
            queryFilter.add(isIn<Any>("status", col))
        } else {
            // Note: Result set should be empty, because every task should has one of the following status values.
            queryFilter.add(
                not(isIn("status", TaskStatus.N, TaskStatus.O, TaskStatus.C))
            )
        }
        queryFilter.addOrder(asc("title"))
        if (log.isDebugEnabled) {
            log.debug(myFilter.toString())
        }
        return getList(queryFilter, context)
    }

    /**
     * Checks if the given task has already a sister task with the same title.
     *
     * @param task
     * @throws UserException
     */
    @Throws(UserException::class)
    private fun checkConstraintVioloation(task: TaskDO, context: PfPersistenceContext) {
        if (task.parentTaskId == null) {
            // Root task or task without parent task.
            if (!taskTree.isRootNode(task)) {
                // Task is not root task!
                throw UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_GIVEN)
            }
        } else {
            val others = if (task.id != null) {
                context.executeNamedQuery(
                    TaskDO.FIND_OTHER_TASK_BY_PARENTTASKID_AND_TITLE,
                    TaskDO::class.java,
                    Pair("parentTaskId", task.parentTaskId),
                    Pair("title", task.title),
                    Pair("id", task.id),
                )// Find other (different from this id).
            } else {
                context.executeNamedQuery(
                    TaskDO.FIND_BY_PARENTTASKID_AND_TITLE,
                    TaskDO::class.java,
                    Pair("parentTaskId", task.parentTaskId),
                    Pair("title", task.title),
                )
            }
            if (CollectionUtils.isNotEmpty(others)) {
                throw UserException(I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS)
            }
        }
    }

    override fun afterSaveOrModify(obj: TaskDO, context: PfPersistenceContext) {
        taskTree.addOrUpdateTaskNode(obj)
    }

    /**
     * Must be visible for TaskTree.
     */
    override fun hasUserSelectAccess(user: PFUserDO, obj: TaskDO, throwException: Boolean): Boolean {
        if (accessChecker.isUserMemberOfGroup(
                user, false, ProjectForgeGroup.ADMIN_GROUP, ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.CONTROLLING_GROUP
            )
        ) {
            return true
        }
        return super.hasUserSelectAccess(user, obj, throwException)
    }

    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    override fun hasAccess(
        user: PFUserDO, obj: TaskDO?, oldObj: TaskDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        return accessChecker.hasPermission(user, obj?.id, AccessType.TASKS, operationType, throwException)
    }

    override fun hasUpdateAccess(
        user: PFUserDO, obj: TaskDO, dbObj: TaskDO?,
        throwException: Boolean
    ): Boolean {
        requireNotNull(dbObj)
        if (taskTree.isRootNode(obj)) {
            if (obj.parentTaskId != null) {
                throw UserException(I18N_KEY_ERROR_CYCLIC_REFERENCE)
            }
            return accessChecker.isUserMemberOfGroup(
                user, throwException, ProjectForgeGroup.ADMIN_GROUP,
                ProjectForgeGroup.FINANCE_GROUP
            )
        }
        requireNotNull(dbObj.parentTaskId)
        if (obj.parentTaskId == null) {
            throw UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_GIVEN)
        }
        taskTree.getTaskNodeById(obj.parentTaskId)
            ?: throw UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND)
        // Checks cyclic and self reference. The parent task is not allowed to be a self reference.
        checkCyclicReference(obj)
        if (accessChecker.isUserMemberOfGroup(
                user, ProjectForgeGroup.ADMIN_GROUP,
                ProjectForgeGroup.FINANCE_GROUP
            )
        ) {
            return true
        }
        if (!accessChecker.hasPermission(
                user, obj.id, AccessType.TASKS, OperationType.UPDATE,
                throwException
            )
        ) {
            return false
        }
        if (dbObj.parentTaskId != obj.parentTaskId) {
            // User moves the object to another task:
            if (!hasInsertAccess(user, obj, throwException)) {
                // Inserting of object under new task not allowed.
                return false
            }
            // Deleting of object under old task not allowed.
            return accessChecker.hasPermission(
                user, dbObj.parentTaskId, AccessType.TASKS, OperationType.DELETE,
                throwException
            )
        }
        return true
    }

    fun hasAccessForKost2AndTimesheetBookingStatus(user: PFUserDO?, obj: TaskDO?): Boolean {
        if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
            return true
        }
        if (obj == null) {
            return false
        }
        val taskId = if (obj.id != null) obj.id else obj.parentTaskId
        val projekt = taskTree.getProjekt(taskId)
        // Parent task because id of current task is null and project can't be found.
        return projekt != null && userGroupCache.isUserProjectManagerOrAssistantForProject(projekt)
    }

    @Throws(AccessException::class)
    override fun checkInsertAccess(user: PFUserDO, obj: TaskDO) {
        super.checkInsertAccess(user, obj)
        if (!accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
            if (obj.protectTimesheetsUntil != null) {
                throw AccessException("task.error.protectTimesheetsUntilReadonly")
            }
            if (obj.protectionOfPrivacy) {
                throw AccessException("task.error.protectionOfPrivacyReadonly")
            }
        }
        if (!hasAccessForKost2AndTimesheetBookingStatus(user, obj)) {
            // Non project managers are not able to manipulate the following fields:
            if (StringUtils.isNotBlank(obj.kost2BlackWhiteList) || obj.kost2IsBlackList) {
                throw AccessException("task.error.kost2Readonly")
            }
            if (obj.timesheetBookingStatus != TimesheetBookingStatus.DEFAULT) {
                throw AccessException("task.error.timesheetBookingStatus2Readonly")
            }
        }
    }

    @Throws(AccessException::class)
    override fun checkUpdateAccess(user: PFUserDO, obj: TaskDO, dbObj: TaskDO) {
        super.checkUpdateAccess(user, obj, dbObj)
        if (!accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
            var ts1: Long? = null
            var ts2: Long? = null
            if (obj.protectTimesheetsUntil != null) {
                ts1 = obj.protectTimesheetsUntil!!.toEpochDay()
            }
            if (dbObj.protectTimesheetsUntil != null) {
                ts2 = dbObj.protectTimesheetsUntil!!.toEpochDay()
            }
            if (ts1 != ts2) {
                throw AccessException("task.error.protectTimesheetsUntilReadonly")
            }
            if (obj.protectionOfPrivacy != dbObj.protectionOfPrivacy) {
                throw AccessException("task.error.protectionOfPrivacyReadonly")
            }
        }
        if (!hasAccessForKost2AndTimesheetBookingStatus(user, obj)) {
            // Non project managers are not able to manipulate the following fields:
            if (obj.kost2BlackWhiteList != dbObj.kost2BlackWhiteList || obj.kost2IsBlackList != dbObj.kost2IsBlackList) {
                throw AccessException("task.error.kost2Readonly")
            }
            if (obj.timesheetBookingStatus != dbObj.timesheetBookingStatus) {
                throw AccessException("task.error.timesheetBookingStatus2Readonly")
            }
        }
    }

    override fun hasInsertAccess(user: PFUserDO, obj: TaskDO?, throwException: Boolean): Boolean {
        requireNotNull(obj) { "Given TaskDO as obj parameter mustn't be null." }
        // Checks if the task is orphan.
        val parent = taskTree.getTaskNodeById(obj.parentTaskId)
        if (parent == null) {
            if (taskTree.isRootNode(obj) && obj.deleted) {
                // Oups, the user has deleted the root task!
            } else {
                throw UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND)
            }
        }
        if (accessChecker.isUserMemberOfGroup(
                user, ProjectForgeGroup.ADMIN_GROUP,
                ProjectForgeGroup.FINANCE_GROUP
            )
        ) {
            return true
        }
        return accessChecker.hasPermission(
            user, obj.parentTaskId, AccessType.TASKS, OperationType.INSERT,
            throwException
        )
    }

    override fun hasDeleteAccess(
        user: PFUserDO, obj: TaskDO, dbObj: TaskDO?,
        throwException: Boolean
    ): Boolean {
        if (hasUpdateAccess(user, obj, dbObj, throwException)) {
            return true
        }
        if (accessChecker.isUserMemberOfGroup(
                user, ProjectForgeGroup.ADMIN_GROUP,
                ProjectForgeGroup.FINANCE_GROUP
            )
        ) {
            return true
        }
        return accessChecker.hasPermission(
            user, obj.parentTaskId, AccessType.TASKS, OperationType.DELETE,
            throwException
        )
    }

    override fun copyValues(src: TaskDO, dest: TaskDO, vararg ignoreFields: String): EntityCopyStatus? {
        var modified = super.copyValues(src, dest, *ignoreFields)
        // Priority value is null-able (may be was not copied from super.copyValues):
        if (dest.priority != src.priority) {
            dest.priority = src.priority
            modified = EntityCopyStatus.MAJOR
        }
        // User object is null-able:
        if (src.responsibleUser == null) {
            if (dest.responsibleUser != null) {
                dest.responsibleUser = src.responsibleUser
                modified = EntityCopyStatus.MAJOR
            }
        }
        return modified
    }

    private fun checkCyclicReference(obj: TaskDO) {
        if (obj.id == obj.parentTaskId) {
            // Self reference
            throw UserException(I18N_KEY_ERROR_CYCLIC_REFERENCE)
        }
        val parent = taskTree.getTaskNodeById(obj.parentTaskId)
            ?: // Task is orphan because it has no parent task.
            throw UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND)
        val node = taskTree.getTaskNodeById(obj.id)
        if (node.isParentOf(parent)) {
            // Cyclic reference because task is ancestor of itself.
            throw UserException(I18N_KEY_ERROR_CYCLIC_REFERENCE)
        }
    }

    /**
     * Checks only root task (can't be deleted).
     */
    override fun onDelete(obj: TaskDO, context: PfPersistenceContext) {
        if (taskTree.isRootNode(obj)) {
            throw UserException("task.error.couldNotDeleteRootTask")
        }
    }

    /**
     * Re-index all dependent objects only if the title was changed.
     */
    override fun wantsReindexAllDependentObjects(obj: TaskDO, dbObj: TaskDO): Boolean {
        if (!super.wantsReindexAllDependentObjects(obj, dbObj)) {
            return false
        }
        return !StringUtils.equals(obj.title, dbObj.title)
    }

    override fun newInstance(): TaskDO {
        return TaskDO()
    }

    companion object {
        const val I18N_KEY_ERROR_CYCLIC_REFERENCE: String = "task.error.cyclicReference"
        const val I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND: String = "task.error.parentTaskNotFound"
        const val I18N_KEY_ERROR_PARENT_TASK_NOT_GIVEN: String = "task.error.parentTaskNotGiven"
        const val I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS: String = "task.error.duplicateChildTasks"
        val ADDITIONAL_SEARCH_FIELDS =
            arrayOf("responsibleUser.username", "responsibleUser.firstname", "responsibleUser.lastname")
    }

    // Wicket workarround
    @Serial
    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(aInputStream: ObjectInputStream) {
        this.userDao = WicketSupport.get(UserDao::class.java)
        this.taskTree = TaskTree.getInstance()
    }

    // Wicket workarround
    @Serial
    @Throws(IOException::class)
    private fun writeObject(aOutputStream: ObjectOutputStream) {
        // Do nothing.
    }
}
