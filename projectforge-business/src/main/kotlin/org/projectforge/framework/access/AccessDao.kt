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

package org.projectforge.framework.access

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.JoinType
import org.hibernate.Hibernate
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.GroupDao
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class AccessDao : BaseDao<GroupTaskAccessDO>(GroupTaskAccessDO::class.java) {
    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var groupDao: GroupDao

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    init {
        this.supportAfterUpdate = true
    }

    /**
     * @param access
     * @param taskId If null, then task will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setTask(access: GroupTaskAccessDO, taskId: Long) {
        val task = taskDao.findOrLoad(taskId)
        access.task = task
    }

    /**
     * @param access
     * @param groupId If null, then group will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setGroup(access: GroupTaskAccessDO, groupId: Long) {
        val group = groupDao.findOrLoad(groupId)
        access.group = group
    }

    /**
     * Loads all GroupTaskAccessDO (not deleted ones) without any access checking.
     *
     * @return
     */
    override fun selectAll(checkAccess: Boolean): List<GroupTaskAccessDO> {
        if (checkAccess) {
            checkLoggedInUserSelectAccess()
        }
        val list = persistenceService.runReadOnly { context ->
            val em = context.em
            val cb: CriteriaBuilder = em.criteriaBuilder
            val cr = cb.createQuery(GroupTaskAccessDO::class.java)
            val root: From<GroupTaskAccessDO, GroupTaskAccessDO> = cr.from(doClass)
            root.fetch<GroupTaskAccessDO, Any>("accessEntries", JoinType.LEFT)
            cr.select(root).where(
                cb.equal(root.get<Boolean>("deleted"), true)
            )
            cr.orderBy(
                cb.asc(root.get<TaskDO>("task").get<Long>("id")),
                cb.asc(root.get<GroupDO>("group").get<Long>("id")),
            )
                .distinct(true)
            em.createQuery(cr).resultList
        }
        return filterAccess(list, checkAccess = checkAccess, callAfterLoad = true)

        // from GroupTaskAccessDO g join fetch g.accessEntries where deleted=false order by g.task.id, g.group.id");
    }

    fun getEntry(task: TaskDO, group: GroupDO): GroupTaskAccessDO? {
        requireNotNull(task.id)
        requireNotNull(group.id)
        val access = persistenceService.selectNamedSingleResult(
            GroupTaskAccessDO.FIND_BY_TASK_AND_GROUP,
            GroupTaskAccessDO::class.java,
            Pair("taskId", task.id),
            Pair("groupId", group.id),
        )
        if (access != null) {
            checkLoggedInUserSelectAccess(access)
        }
        return access
    }

    override fun select(filter: BaseSearchFilter): List<GroupTaskAccessDO> {
        val myFilter = if (filter is AccessFilter) {
            filter
        } else {
            AccessFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        if (myFilter.taskId != null) {
            var descendants: List<Long>? = null
            var ancestors: List<Long>? = null
            val node = taskTree.getTaskNodeById(myFilter.taskId)
            if (myFilter.isIncludeDescendentTasks) {
                descendants = node?.descendantIds
            }
            if (myFilter.isInherit || myFilter.isIncludeAncestorTasks) {
                ancestors = node?.ancestorIds
            }
            if (descendants != null || ancestors != null) {
                val taskIds = mutableListOf<Long>()
                if (descendants != null) {
                    taskIds.addAll(descendants)
                }
                if (ancestors != null) {
                    taskIds.addAll(ancestors)
                }
                node?.id?.let { id ->
                    taskIds.add(id)
                }
                queryFilter.add(isIn<Any>("task.id", taskIds))
            } else {
                queryFilter.add(eq("task.id", myFilter.taskId))
            }
        }
        if (myFilter.groupId != null) {
            val group = GroupDO()
            group.id = myFilter.groupId
            queryFilter.add(eq("group", group))
        }
        val qlist = select(queryFilter)
        var list: List<GroupTaskAccessDO>
        if (myFilter.taskId != null && myFilter.isInherit && !myFilter.isIncludeAncestorTasks) {
            // Now we have to remove all inherited entries of ancestor nodes which are not declared as recursive.
            list = ArrayList()
            val taskNode = taskTree.getTaskNodeById(myFilter.taskId)
            if (taskNode == null) { // Paranoia
                list = qlist
            } else {
                for (access in qlist) {
                    if (!access.isRecursive) {
                        val accessNode = taskTree.getTaskNodeById(access.taskId)
                        // && myFilter.getTaskId().equals(access.getTaskId()) == false) {
                        if (accessNode?.isParentOf(taskNode) == true) {
                            // This entry is not recursive and inherited, therefore this entry will be ignored.
                            continue
                        }
                    }
                    list.add(access)
                }
            }
        } else {
            list = qlist
        }
        if (myFilter.userId != null) {
            val result: MutableList<GroupTaskAccessDO> = ArrayList()
            for (access in list) {
                if (userGroupCache.isUserMemberOfGroup(myFilter.userId, access.groupId)) {
                    result.add(access)
                }
            }
            return result
        }
        return list
    }

    /**
     * @return Always true, no generic select access needed for group task access objects.
     * @see org.projectforge.framework.persistence.api.BaseDao.hasSelectAccess
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    /**
     * @return false, if no admin user and the context user is not member of the group. Also deleted entries are only
     * visible for admin users.
     * @see org.projectforge.framework.persistence.api.BaseDao.hasSelectAccess
     */
    override fun hasUserSelectAccess(user: PFUserDO, obj: GroupTaskAccessDO, throwException: Boolean): Boolean {
        var result = accessChecker.isUserMemberOfAdminGroup(user)
        if (!result && !obj.deleted) {
            result = userGroupCache.isUserMemberOfGroup(user.id, obj.groupId)
        }
        if (throwException && !result) {
            throw AccessException(AccessType.GROUP, OperationType.SELECT)
        }
        return result
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.hasAccess
     */
    override fun hasAccess(
        user: PFUserDO, obj: GroupTaskAccessDO?, oldObj: GroupTaskAccessDO?,
        operationType: OperationType, throwException: Boolean
    ): Boolean {
        return accessChecker.hasPermission(
            user, obj?.taskId, AccessType.TASK_ACCESS_MANAGEMENT, operationType,
            throwException
        )
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.hasUpdateAccess
     */
    override fun hasUpdateAccess(
        user: PFUserDO, obj: GroupTaskAccessDO, dbObj: GroupTaskAccessDO?,
        throwException: Boolean
    ): Boolean {
        requireNotNull(dbObj)
        requireNotNull(dbObj.taskId)
        requireNotNull(obj.taskId)
        if (!accessChecker.hasPermission(
                user,
                obj.taskId,
                AccessType.TASK_ACCESS_MANAGEMENT,
                OperationType.UPDATE,
                throwException
            )
        ) {
            return false
        }
        if (dbObj.taskId != obj.taskId) {
            // User moves the object to another task:
            if (!accessChecker.hasPermission(
                    user,
                    obj.taskId,
                    AccessType.TASK_ACCESS_MANAGEMENT,
                    OperationType.INSERT,
                    throwException
                )
            ) {
                // Inserting of object under new task not allowed.
                return false
            }
            if (!accessChecker.hasPermission(
                    user,
                    dbObj.taskId,
                    AccessType.TASK_ACCESS_MANAGEMENT,
                    OperationType.DELETE,
                    throwException
                )
            ) {
                // Deleting of object under old task not allowed.
                return false
            }
        }
        return true
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.prepareHibernateSearch
     */
    override fun prepareHibernateSearch(
        obj: GroupTaskAccessDO,
        operationType: OperationType,
    ) {
        val task = obj.task
        if (task != null && !Hibernate.isInitialized(task)) {
            Hibernate.initialize(obj.task)
            obj.task = taskTree.getTaskById(task.id)
        }
        val group = obj.group
        if (group != null && !Hibernate.isInitialized(group)) {
            obj.group = groupDao.findOrLoad(obj.groupId!!)
        }
    }

    override fun afterInsertOrModify(obj: GroupTaskAccessDO, operationType: OperationType) {
        taskTree.setGroupTaskAccess(obj)
    }

    override fun getBackupObject(dbObj: GroupTaskAccessDO): GroupTaskAccessDO {
        val access = GroupTaskAccessDO()
        for (dbEntry in dbObj.accessEntries!!) {
            val entry = AccessEntryDO(dbEntry.accessType!!)
            entry.accessSelect = dbEntry.accessSelect
            entry.accessInsert = dbEntry.accessInsert
            entry.accessUpdate = dbEntry.accessUpdate
            entry.accessDelete = dbEntry.accessDelete
            access.addAccessEntry(entry)
        }
        return access
    }

    override fun afterDelete(obj: GroupTaskAccessDO) {
        taskTree.removeGroupTaskAccess(obj)
    }

    override fun afterUndelete(obj: GroupTaskAccessDO) {
        taskTree.setGroupTaskAccess(obj)
    }

    override fun hasHistoryAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user, throwException)
    }

    override fun newInstance(): GroupTaskAccessDO {
        return GroupTaskAccessDO()
    }

    companion object {
        val ADDITIONAL_SEARCH_FIELDS = arrayOf("task.title", "group.name")
    }
}
