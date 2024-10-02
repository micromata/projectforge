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

package org.projectforge.test

import mu.KotlinLogging
import org.apache.commons.lang3.Validate
import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.*
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.setUser
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.time.DateHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class InitTestDB {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var databaseService: DatabaseService

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userRightDao: UserRightDao

    private val groupMap = mutableMapOf<String, GroupDO>()

    private val userMap = mutableMapOf<String, PFUserDO>()

    private val taskMap = mutableMapOf<String, TaskDO>()

    fun putUser(user: PFUserDO) {
        userMap[user.username!!] = user
    }

    fun addUserInTrans(username: String?): PFUserDO {
        return persistenceService.runInTransaction { context: PfPersistenceContext ->
            addUser(
                username,
                null,
                context
            )
        }
    }

    fun addUser(username: String?, context: PfPersistenceContext): PFUserDO {
        return addUser(username, null, context)
    }

    fun addUser(username: String?, password: CharArray?, context: PfPersistenceContext): PFUserDO {
        var user = PFUserDO()
        user.username = username
        user.locale = Locale.ENGLISH
        user.dateFormat = "dd/MM/yyyy"
        user.email = "devnull@localhost"
        user = addUser(user, context)
        if (password != null) {
            userService.encryptAndSavePassword(user, password, false, context)
        }
        return user
    }

    fun addUser(user: PFUserDO, context: PfPersistenceContext): PFUserDO {
        user.rights?.let { userRights ->
            val savedSet = mutableSetOf<UserRightDO>()
            savedSet.addAll(userRights)
            userRights.clear()
            userService.save(user, context) // Save user without rights
            savedSet.forEach { right ->
                // Now, save the rights.
                userRightDao.internalSave(right, context)
            }
        }
        putUser(user)
        if (user.username == AbstractTestBase.ADMIN) {
            AbstractTestBase.ADMIN_USER = user
        }
        return user
    }

    fun getUser(userName: String?): PFUserDO? {
        return userMap[userName]
    }

    fun clearUsers() {
        userMap.clear()
    }

    fun putGroup(group: GroupDO) {
        groupMap[group.name!!] = group
    }

    fun addGroup(groupname: String, context: PfPersistenceContext, vararg usernames: String): GroupDO {
        val group = GroupDO()
        group.name = groupname
        group.assignedUsers = usernames.mapNotNull { username -> getUser(username) }.toMutableSet()
        groupDao.internalSave(group, context)
        putGroup(group)
        userGroupCache.setExpired()
        return group
    }

    fun getGroup(groupName: String?): GroupDO? {
        return groupMap[groupName]
    }

    fun putTask(task: TaskDO) {
        taskMap[task.title!!] = task
    }

    fun addTask(taskName: String, parentTaskName: String?, context: PfPersistenceContext): TaskDO {
        Validate.isTrue(taskName.length <= TaskDO.TITLE_LENGTH)
        return addTask(taskName, parentTaskName, null, context)
    }

    fun addTask(
        taskName: String?,
        parentTaskName: String?,
        shortDescription: String?,
        context: PfPersistenceContext
    ): TaskDO {
        val task = TaskDO()
        task.title = taskName
        if (parentTaskName != null) {
            task.parentTask = getTask(parentTaskName)
        }
        if (shortDescription != null) {
            task.shortDescription = shortDescription
        }
        val id: Serializable? = taskDao.internalSave(task, context)
        // Test if the task is saved correctly:
        taskDao.internalGetById(id, context).let { savedTask ->
            requireNotNull(savedTask)
            putTask(savedTask)
            return savedTask
        }
    }

    fun getTask(taskName: String): TaskDO? {
        return taskMap[taskName]
    }

    fun addTimesheet(
        user: PFUserDO, task: TaskDO, startTime: Date,
        stopTime: Date, description: String?,
        context: PfPersistenceContext
    ): TimesheetDO {
        val timesheet = TimesheetDO()
        timesheet.description = description
        timesheet.startTime = startTime
        timesheet.stopTime = stopTime
        timesheet.task = task
        timesheet.user = user
        timesheetDao.internalSave(timesheet, context)
        return timesheet
    }

    fun addProjekt(
        kunde: KundeDO?, projektNummer: Int, projektName: String?,
        context: PfPersistenceContext,
        vararg kost2ArtIds: Long
    ): ProjektDO {
        val projekt = ProjektDO()
        projekt.nummer = projektNummer
        projekt.name = projektName
        if (kunde != null) {
            projektDao.setKunde(projekt, kunde.id, context)
        }
        projektDao.save(projekt, context)
        kost2ArtIds.forEach { id ->
            val kost2 = Kost2DO()
            kost2.projekt = projekt
            kost2.nummernkreis = 5
            if (kunde != null) {
                kost2.bereich = kunde.id!!.toInt()
            }
            kost2.teilbereich = projekt.nummer
            kost2Dao.setKost2Art(kost2, id, context)
            kost2Dao.save(kost2, context)
        }
        return projekt
    }

    fun initDatabase() {
        val origUser = user
        try {
            persistenceService.runInTransaction<Any?> { context: PfPersistenceContext ->
                val initUser = PFUserDO()
                initUser.username = "Init-database-pseudo-user"
                initUser.id = -1L
                initUser.addRight(UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE))
                setUser(initUser)
                initConfiguration(context)
                initUsers(context)
                databaseService.insertGlobalAddressbook(AbstractTestBase.ADMIN_USER, context)
                initGroups(context)
                null
            }
            initTaskTree()
            initAccess()
            persistenceService.runInTransaction<Any?> { context: PfPersistenceContext ->
                initKost2Arts(context)
                initEmployees(context)
                null
            }
        } finally {
            setUser(origUser)
        }
    }

    private fun initEmployees(context: PfPersistenceContext) {
        val user = addUser(AbstractTestBase.TEST_EMPLOYEE_USER, AbstractTestBase.TEST_EMPLOYEE_USER_PASSWORD, context)
        val e = EmployeeDO()
        e.user = user
        employeeDao.internalSave(e, context)
    }

    private fun initConfiguration(context: PfPersistenceContext) {
        configurationDao.checkAndUpdateDatabaseEntries(context)
        val entry = configurationDao.getEntry(ConfigurationParam.DEFAULT_TIMEZONE, context)
        entry.timeZone = DateHelper.EUROPE_BERLIN
        configurationDao.internalUpdate(entry, context)
    }

    private fun initUsers(context: PfPersistenceContext) {
        addUser(AbstractTestBase.ADMIN, context)
        addUser(AbstractTestBase.TEST_ADMIN_USER, AbstractTestBase.TEST_ADMIN_USER_PASSWORD, context)
        var user = PFUserDO()
        user.username = AbstractTestBase.TEST_FINANCE_USER
        user //
            .addRight(UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.FIBU_ACCOUNTS, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.FIBU_COST_UNIT, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.PM_PROJECT, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.PM_HR_PLANNING, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.ORGA_CONTRACTS, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE)) //
        addUser(user, context)
        user = PFUserDO()
        user.username = AbstractTestBase.TEST_HR_USER
        user //
            .addRight(UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE)) //
        addUser(user, context)
        user = PFUserDO()
        user.username = AbstractTestBase.TEST_FULL_ACCESS_USER
        user //
            .addRight(UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.FIBU_ACCOUNTS, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.FIBU_COST_UNIT, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.PM_PROJECT, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.PM_HR_PLANNING, UserRightValue.READWRITE)) //
            .addRight(UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE)) //
        user = addUser(user, context)
        userService.encryptAndSavePassword(user, AbstractTestBase.TEST_FULL_ACCESS_USER_PASSWORD, false, context)
        addUser(AbstractTestBase.TEST_USER, AbstractTestBase.TEST_USER_PASSWORD, context)
        addUser(AbstractTestBase.TEST_USER2, context)
        user = addUser(AbstractTestBase.TEST_DELETED_USER, AbstractTestBase.TEST_DELETED_USER_PASSWORD, context)
        userService.markAsDeleted(user, context)
        addUser("user1", context)
        addUser("user2", context)
        addUser("user3", context)
        addUser(AbstractTestBase.TEST_CONTROLLING_USER, context)
        addUser(AbstractTestBase.TEST_MARKETING_USER, context)
        addUser(AbstractTestBase.TEST_PROJECT_MANAGER_USER, context)
        user = PFUserDO()
        user.username = AbstractTestBase.TEST_PROJECT_ASSISTANT_USER
        user.addRight(UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.PARTLYREADWRITE))
        addUser(user, context)
    }

    private fun initGroups(context: PfPersistenceContext) {
        addGroup(
            AbstractTestBase.ADMIN_GROUP, context,
            "PFAdmin", AbstractTestBase.TEST_ADMIN_USER, AbstractTestBase.TEST_FULL_ACCESS_USER
        )
        addGroup(
            AbstractTestBase.FINANCE_GROUP, context,
            AbstractTestBase.TEST_FINANCE_USER, AbstractTestBase.TEST_FULL_ACCESS_USER
        )
        addGroup(
            AbstractTestBase.CONTROLLING_GROUP, context,
            AbstractTestBase.TEST_CONTROLLING_USER, AbstractTestBase.TEST_FULL_ACCESS_USER
        )
        addGroup(
            AbstractTestBase.HR_GROUP,
            context,
            AbstractTestBase.TEST_FULL_ACCESS_USER,
            AbstractTestBase.TEST_HR_USER
        )
        addGroup(AbstractTestBase.ORGA_GROUP, context, AbstractTestBase.TEST_FULL_ACCESS_USER)
        addGroup(
            AbstractTestBase.PROJECT_MANAGER, context,
            AbstractTestBase.TEST_PROJECT_MANAGER_USER, AbstractTestBase.TEST_FULL_ACCESS_USER
        )
        addGroup(
            AbstractTestBase.PROJECT_ASSISTANT, context,
            AbstractTestBase.TEST_PROJECT_ASSISTANT_USER, AbstractTestBase.TEST_FULL_ACCESS_USER
        )
        addGroup(
            AbstractTestBase.MARKETING_GROUP, context,
            AbstractTestBase.TEST_MARKETING_USER, AbstractTestBase.TEST_FULL_ACCESS_USER
        )
        addGroup(AbstractTestBase.TEST_GROUP, context, AbstractTestBase.TEST_USER, AbstractTestBase.TEST_USER2)
        addGroup("group1", context, "user1", "user2")
        addGroup("group2", context, "user1")
        addGroup("group3", context)
    }

    private fun initKost2Arts(context: PfPersistenceContext) {
        addKost2Art(0L, "Akquise", context)
        addKost2Art(1L, "Research", context)
        addKost2Art(2L, "Realization", context)
        addKost2Art(3L, "Systemadministration", context)
        addKost2Art(4L, "Travel costs", context)
    }

    private fun addKost2Art(id: Long, name: String, context: PfPersistenceContext) {
        val kost2Art = Kost2ArtDO()
        kost2Art.id = id
        kost2Art.name = name
        kost2ArtDao.internalSave(kost2Art, context)
    }

    private fun initTaskTree() {
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext ->
            log.debug { "Setting taskTree.expired: $taskTree" }
            taskTree.clear()
            log.debug { "TaskTree after reload: $taskTree" }
            if (taskTree.rootTaskNode == null) {
                addTask("root", null, context)
            } else {
                putTask(taskTree.rootTaskNode.task)
            }
            addTask("1", "root", context)
            addTask("1.1", "1", context)
            addTask("1.2", "1", context)
            addTask("1.1.1", "1.1", context)
            addTask("1.1.2", "1.1", context)
            addTask("2", "root", context)
            addTask("2.1", "2", context)
            addTask("2.2", "2", context)
        }
    }

    fun createGroupTaskAccess(group: GroupDO?, task: TaskDO?, context: PfPersistenceContext): GroupTaskAccessDO {
        requireNotNull(group)
        requireNotNull(task)
        val access = GroupTaskAccessDO()
        access.group = group
        access.task = task
        accessDao.internalSave(access, context)
        return access
    }

    fun createGroupTaskAccess(
        group: GroupDO?, task: TaskDO?, accessType: AccessType?,
        accessSelect: Boolean, accessInsert: Boolean, accessUpdate: Boolean, accessDelete: Boolean,
        context: PfPersistenceContext
    ): GroupTaskAccessDO {
        val access = createGroupTaskAccess(group, task, context)
        val entry = access.ensureAndGetAccessEntry(accessType)
        entry.setAccess(accessSelect, accessInsert, accessUpdate, accessDelete)
        accessDao.internalUpdate(access, context)
        return access
    }

    private fun initAccess() {
        persistenceService.runInTransaction { context ->
            val access = createGroupTaskAccess(getGroup("group1"), getTask("1"), context)
            val entry = access.ensureAndGetAccessEntry(AccessType.TASKS)
            entry.setAccess(true, true, true, true)
            accessDao.internalUpdate(access, context)
        }
        // Access entries must be saved (flushed) before adding tasks.
        persistenceService.runInTransaction { context ->
            // Create some test tasks with test access:
            addTask("testAccess", "root", context)
            addTask(
                "ta_1_siud",
                "testAccess",
                "Testuser has all access rights: select, insert, update, delete",
                context
            )
            addTask("ta_1_1", "ta_1_siud", context)
        }
        persistenceService.runInTransaction { context ->
            var access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_1_siud"), context)
            setAllAccessEntries(access, true, true, true, true, context)
            addTask("ta_2_siux", "testAccess", "Testuser has the access rights: select, insert, update", context)
            addTask("ta_2_1", "ta_2_siux", context)
            access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_2_siux"), context)
            setAllAccessEntries(access, true, true, true, false, context)
            addTask("ta_3_sxxx", "testAccess", "Testuser has only select rights: select", context)
            addTask("ta_3_1", "ta_3_sxxx", context)
            access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_3_sxxx"), context)
            setAllAccessEntries(access, true, false, false, false, context)
            addTask("ta_4_xxxx", "testAccess", "Testuser has no rights.", context)
            addTask("ta_4_1", "ta_4_xxxx", context)
            access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_4_xxxx"), context)
            setAllAccessEntries(access, false, false, false, false, context)
            addTask("ta_5_sxux", "testAccess", "Testuser has select and update rights.", context)
            addTask("ta_5_1", "ta_5_sxux", context)
            access = createGroupTaskAccess(getGroup(AbstractTestBase.TEST_GROUP), getTask("ta_5_sxux"), context)
            setAllAccessEntries(access, true, false, true, false, context)
        }
    }

    private fun setAllAccessEntries(
        access: GroupTaskAccessDO, selectAccess: Boolean,
        insertAccess: Boolean,
        updateAccess: Boolean, deleteAccess: Boolean,
        context: PfPersistenceContext
    ) {
        var entry = access.ensureAndGetAccessEntry(AccessType.TASK_ACCESS_MANAGEMENT)
        entry.setAccess(selectAccess, insertAccess, updateAccess, deleteAccess)
        entry = access.ensureAndGetAccessEntry(AccessType.TASKS)
        entry.setAccess(selectAccess, insertAccess, updateAccess, deleteAccess)
        entry = access.ensureAndGetAccessEntry(AccessType.TIMESHEETS)
        entry.setAccess(selectAccess, insertAccess, updateAccess, deleteAccess)
        entry = access.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS)
        entry.setAccess(selectAccess, insertAccess, updateAccess, deleteAccess)
        accessDao.internalUpdate(access, context)
    }
}
