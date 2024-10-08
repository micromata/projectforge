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

package org.projectforge.framework.persistence.database

import jakarta.persistence.Persistence
import mu.KotlinLogging
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.login.Login
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.*
import org.projectforge.common.DatabaseDialect
import org.projectforge.common.task.TaskStatus
import org.projectforge.database.DatabaseExecutor
import org.projectforge.database.DatabaseResultRow
import org.projectforge.database.DatabaseSupport
import org.projectforge.database.jdbc.DatabaseExecutorImpl
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessCheckerImpl
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.HibernateUtils.databaseDialect
import org.projectforge.framework.persistence.search.HibernateSearchReindexer
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.setUser
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUser
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.sql.SQLException
import java.util.*
import java.util.function.Consumer
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Service
class DatabaseService {
    @Autowired
    private lateinit var hibernateSearchReindexer: HibernateSearchReindexer

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userPasswordDao: UserPasswordDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userRightDao: UserRightDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Value("\${hibernate.search.directory.root}")
    private val hibernateSearchDirectoryRoot: String? = null

    @Autowired
    lateinit var dataSource: DataSource
        private set

    @Autowired
    private lateinit var accessChecker: AccessChecker

    private var databaseSupport: DatabaseSupport? = null
        get() {
            if (field == null) {
                field = DatabaseSupport(dialect)
            }
            return field
        }

    private var databaseExecutor: DatabaseExecutor? = null


    /**
     * If the database is empty (user list is empty) then a admin user and ProjectForge root task will be created.
     *
     * @param adminUser The admin user with the desired username and the salted password (salt string included). All other
     * attributes and groups of the user are set by this method.
     */
    fun initializeDefaultData(adminUser: PFUserDO, adminUserTimezone: TimeZone) {
        log.info("Init admin user and root task.")
        if (databaseTablesWithEntriesExist()) {
            databaseNotEmpty()
        }
        persistenceService.runInTransaction { context ->
            val em = context.em
            val task = TaskDO()
            task.title = "Root"
            task.status = TaskStatus.N
            task.shortDescription = "ProjectForge root task"
            task.setCreated()
            task.setLastUpdate()
            em.persist(task)
            log.info("New object added (" + task.id + "): " + task.toString())

            // Use of taskDao does not work with maven test case: Could not synchronize database state with session?

            // Create Admin user
            adminUser.localUser = true
            adminUser.lastname = "Administrator"
            adminUser.description = "ProjectForge administrator"
            adminUser.timeZone = adminUserTimezone
            userDao.internalSave(adminUser, context)
            adminUser.addRight(UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.FIBU_COST_UNIT, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE))
            adminUser.addRight(UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.FIBU_ACCOUNTS, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.ORGA_CONTRACTS, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.ORGA_INCOMING_MAIL, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.ORGA_OUTGOING_MAIL, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.PM_PROJECT, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.READWRITE))
            adminUser.addRight(UserRightDO(UserRightId.PM_HR_PLANNING, UserRightValue.READWRITE))
            adminUser.rights!!.forEach(Consumer { obj: UserRightDO -> userRightDao.internalSave(obj, context) })

            setUser(adminUser) // Need to login the admin user for avoiding following access exceptions.

            internalCreateProjectForgeGroups(adminUser, context)

            taskTree.setExpired()
            userGroupCache.setExpired()
        }
        log.info("Default data successfully initialized in database.")
    }

    @JvmOverloads
    fun insertGlobalAddressbook(user: PFUserDO? = null): AddressbookDO? {
        return persistenceService.runInTransaction { context: PfPersistenceContext ->
            insertGlobalAddressbook(
                user,
                context,
            )
        }
    }

    fun insertGlobalAddressbook(user: PFUserDO?, context: PfPersistenceContext): AddressbookDO? {
        log.info("Checking if global addressbook exists.")
        val addressbook = addressbookDao.getGlobalAddressbook(context)
        if (addressbook != null) {
            return addressbook
        }
        log.info("Adding global addressbook.")
        val insertGlobal =
            "INSERT INTO t_addressbook(pk, created, deleted, last_update, description, title, owner_fk) VALUES (:id, :created, :deleted, :lastUpdate, :description, :title, :owner)"
        val ownerId = user?.id ?: ThreadLocalUserContext.loggedInUserId
        val now = Date()
        val result = context.executeNativeUpdate(
            insertGlobal,
            Pair("id", AddressbookDao.GLOBAL_ADDRESSBOOK_ID),
            Pair("created", now),
            Pair("deleted", false),
            Pair("lastUpdate", now),
            Pair("description", AddressbookDao.GLOBAL_ADDRESSBOOK_DESCRIPTION),
            Pair("title", AddressbookDao.GLOBAL_ADDRESSBOOK_TITLE),
            Pair("owner", ownerId),
        );
        log.info("Adding global addressbook finished: $insertGlobal, result: $result")
        return addressbookDao.getGlobalAddressbook(context)
    }

    private fun internalCreateProjectForgeGroups(adminUser: PFUserDO, context: PfPersistenceContext) {
        val adminUsers: MutableSet<PFUserDO> = HashSet()
        adminUsers.add(adminUser)

        addGroup(ProjectForgeGroup.ADMIN_GROUP, "Administrators of ProjectForge", adminUsers, context)
        addGroup(
            ProjectForgeGroup.CONTROLLING_GROUP,
            "Users for having read access to the company's finances.",
            adminUsers,
            context,
        )
        addGroup(ProjectForgeGroup.FINANCE_GROUP, "Finance and Accounting", adminUsers, context)
        addGroup(
            ProjectForgeGroup.MARKETING_GROUP,
            "Marketing users can download all addresses in excel format.",
            adminUsers,
            context,
        )
        addGroup(
            ProjectForgeGroup.ORGA_TEAM,
            "The organization team has access to post in- and outbound, contracts etc..",
            adminUsers,
            context,
        )
        addGroup(ProjectForgeGroup.HR_GROUP, "Users for having full access to the companies hr.", adminUsers, context)
        addGroup(
            ProjectForgeGroup.PROJECT_MANAGER,
            "Project managers have access to assigned orders and resource planning.", null, context
        )
        addGroup(
            ProjectForgeGroup.PROJECT_ASSISTANT,
            "Project assistants have access to assigned orders.",
            null,
            context
        )
    }

    private fun addGroup(
        projectForgeGroup: ProjectForgeGroup,
        description: String,
        users: MutableSet<PFUserDO>?,
        context: PfPersistenceContext
    ) {
        val group = GroupDO()
        group.name = projectForgeGroup.toString()
        group.description = description
        if (!users.isNullOrEmpty()) {
            group.assignedUsers = users
        }
        // group.setNestedGroupsAllowed(false);
        group.localGroup = true // Do not synchronize group with external user management system by default.
        groupDao.internalSave(group, context)
    }

    /**
     * @param user              The admin user with the desired username and the salted password (salt string included).
     * @param adminUserTimezone
     */
    fun updateAdminUser(user: PFUserDO, adminUserTimezone: TimeZone): PFUserDO {
        //Update test data user with data from setup page
        val adminUser = userDao.getInternalByName(DEFAULT_ADMIN_USER)
        adminUser!!.username = user.username
        adminUser.localUser = true
        adminUser.timeZone = adminUserTimezone
        userDao.internalUpdateInTrans(adminUser)
        setUser(adminUser)
        userGroupCache.forceReload()
        return adminUser
    }

    fun afterCreatedTestDb(blocking: Boolean) {
        val rebuildThread = Thread { hibernateSearchReindexer.rebuildDatabaseSearchIndices() }
        rebuildThread.start()
        if (blocking) {
            try {
                rebuildThread.join()
            } catch (e: InterruptedException) {
                log.warn("reindex thread was interrupted: " + e.message, e)
            }
        }
        taskTree.setExpired()
        userGroupCache.setExpired()
        log.info("Database successfully initialized with test data.")
    }

    private fun databaseNotEmpty() {
        val msg = "Database seems to be not empty. Initialization of database aborted."
        log.error(msg)
        throw AccessException(msg)
    }

    fun updateSchema() {
        log.info("Start generating Schema...")
        val props: MutableMap<String?, Any?> = HashMap()
        props["hibernate.hbm2ddl.auto"] = "update"
        props["hibernate.search.backend.directory.root"] = hibernateSearchDirectoryRoot
        props["hibernate.connection.datasource"] = dataSource
        try {
            Persistence.createEntityManagerFactory("org.projectforge.webapp", props)
        } catch (e: Exception) {
            log.error("Exception while updateSchema:" + e.message, e)
            throw e
        }
        log.info("Finished generating Schema...")
    }

    val dialect: DatabaseDialect
        get() = databaseDialect

    private fun getDatabaseExecutor(): DatabaseExecutor {
        if (databaseExecutor == null) {
            databaseExecutor = DatabaseExecutorImpl()
            databaseExecutor!!.setDataSource(dataSource)
        }
        return databaseExecutor!!
    }

    /**
     * Does nothing at default. Override this method for checking the access of the user, e. g. only admin user's should
     * be able to manipulate the database.
     *
     * @param writeaccess
     */
    protected fun accessCheck(writeaccess: Boolean) {
        if (loggedInUser === SYSTEM_ADMIN_PSEUDO_USER) {
            // No access check for the system admin pseudo user.
            return
        }
        if (!Login.getInstance().isAdminUser(loggedInUser)) {
            throw AccessException(
                AccessCheckerImpl.I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF,
                ProjectForgeGroup.ADMIN_GROUP.key
            )
        }
        accessChecker.checkRestrictedOrDemoUser()
    }

    fun doesTableExist(table: String): Boolean {
        accessCheck(false)
        return internalDoesTableExist(table)
    }

    /**
     * Without check access.
     *
     * @param table
     * @return
     */
    fun internalDoesTableExist(table: String): Boolean {
        val jdbc = getDatabaseExecutor()
        try {
            jdbc.queryForInt("SELECT COUNT(*) FROM $table")
        } catch (ex: Exception) {
            log.warn("Exception while checking count from table: " + table + " Exception: " + ex.message)
            return false
        }
        return true
    }

    fun isTableEmpty(table: String): Boolean {
        accessCheck(false)
        return internalIsTableEmpty(table)
    }

    fun internalIsTableEmpty(table: String): Boolean {
        val jdbc = getDatabaseExecutor()
        return try {
            jdbc.queryForInt("SELECT COUNT(*) FROM $table") == 0
        } catch (ex: Exception) {
            false
        }
    }

    /**
     * Creates missing database indices of tables starting with 't_'.
     *
     * @return Number of successful created database indices.
     */
    fun createMissingIndices(): Int {
        accessCheck(true)
        log.info("createMissingIndices called.")
        var counter = 0
        // For user / time period search:
        try {
            dataSource.connection.use { connection ->
                val reference = connection.metaData.getCrossReference(null, null, null, null, null, null)
                while (reference.next()) {
                    val fkTable = reference.getString("FKTABLE_NAME")
                    val fkCol = reference.getString("FKCOLUMN_NAME")
                    if (fkTable.startsWith("t_")) {
                        // Table of ProjectForge
                        if (createIndex("idx_fk_" + fkTable + "_" + fkCol, fkTable, fkCol)) {
                            counter++
                        }
                    }
                }
            }
        } catch (ex: SQLException) {
            log.error(ex.message, ex)
        }
        return counter
    }

    /**
     * Creates the given database index if not already exists.
     *
     * @param name
     * @param table
     * @param attributes
     * @return true, if the index was created, false if an error has occured or the index already exists.
     */
    fun createIndex(name: String, table: String, attributes: String): Boolean {
        accessCheck(true)
        try {
            val jdbcString = "CREATE INDEX $name ON $table($attributes);"
            execute(jdbcString, false)
            log.info(jdbcString)
            return true
        } catch (ex: Throwable) {
            // Index does already exist (or an error has occurred).
            return false
        }
    }

    /**
     * Executes the given String
     *
     * @param jdbcString
     * @param ignoreErrors If true (default) then errors will be caught and logged.
     * @return true if no error occurred (no exception was caught), otherwise false.
     */
    /**
     * @param jdbcString
     * @see .execute
     */
    @JvmOverloads
    fun execute(jdbcString: String?, ignoreErrors: Boolean = true) {
        accessCheck(true)
        val jdbc = getDatabaseExecutor()
        jdbc.execute(jdbcString, ignoreErrors)
        log.info(jdbcString)
    }

    fun queryForInt(jdbcQuery: String?): Int {
        accessCheck(false)
        val jdbc = getDatabaseExecutor()
        log.info(jdbcQuery)
        return jdbc.queryForInt(jdbcQuery)
    }

    fun query(sql: String?, vararg args: Any?): List<DatabaseResultRow> {
        accessCheck(false)
        val jdbc = getDatabaseExecutor()
        log.info(sql)
        return jdbc.query(sql, *args)
    }

    /**
     * Will be called on shutdown.
     *
     * @see DatabaseSupport.getShutdownDatabaseStatement
     */
    fun shutdownDatabase() {
        val statement = databaseSupport!!.shutdownDatabaseStatement ?: return
        log.info("Executing database shutdown statement: $statement")
        execute(statement)
    }

    /**
     * Needed for checking if user table is existing on startup. If not, the setup page will be called (see ProjectForge.jsx)
     *
     * @return
     */
    fun databaseTablesWithEntriesExist(): Boolean {
        try {
            val tableName = "T_PF_USER"
            return internalDoesTableExist(tableName) && !internalIsTableEmpty(tableName)
        } catch (ex: Exception) {
            log.error("Error while checking existing of user table with entries.", ex)
        }
        return false
    }

    companion object {
        const val DEFAULT_ADMIN_USER: String = "admin"

        private val SYSTEM_ADMIN_PSEUDO_USER = PFUserDO()

        init {
            SYSTEM_ADMIN_PSEUDO_USER.username = "System admin user only for internal usage"
        }

        @JvmStatic
        fun __internalGetSystemAdminPseudoUser(): PFUserDO {
            return SYSTEM_ADMIN_PSEUDO_USER
        }
    }
}
