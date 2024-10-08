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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.projectforge.Constants
import org.projectforge.ProjectForgeApp
import org.projectforge.SystemStatus
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.login.Login
import org.projectforge.business.login.LoginDefaultHandler
import org.projectforge.business.task.TaskDO
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserService
import org.projectforge.database.DatabaseSupport
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.ConfigXmlTest
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.I18nHelper.addBundleName
import org.projectforge.framework.persistence.api.HibernateUtils.databaseDialect
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner.Companion.setInternalSetUnitTestMode
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.setUser
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.PFUserDO.Companion.createCopy
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.jcr.RepoService
import org.projectforge.mail.SendMail.Companion.internalSetTestMode
import org.projectforge.plugins.core.AbstractPlugin.Companion.internalJunitTestMode
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.core.PluginsRegistry
import org.projectforge.registry.Registry
import org.projectforge.test.DatabaseHelper.clearDatabase
import org.projectforge.web.WicketSupport
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.BadSqlGrammarException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*
import javax.sql.DataSource

/**
 * Every test should finish with a valid database with test cases. If not, the test should call recreateDatabase() on afterAll!
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
@Component
abstract class AbstractTestBase protected constructor() {
    @JvmField
    protected val baseLog = KotlinLogging.logger {}

    @Autowired
    protected lateinit var applicationContext: ApplicationContext

    @Autowired
    protected lateinit var userService: UserService

    @Autowired
    protected lateinit var accessChecker: AccessChecker

    @Autowired
    lateinit var initTestDB: InitTestDB

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    @Autowired
    private lateinit var repoService: RepoService

    @Autowired
    private lateinit var systemStatus: SystemStatus

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    protected lateinit var persistenceService: PfPersistenceService

    @PostConstruct
    private fun postConstruct() {
        if (DatabaseSupport.getInstance() == null) {
            baseLog.info("************ Initializing DatabaseSupport with database dialect = " + databaseDialect)
            DatabaseSupport.setInstance(
                DatabaseSupport(
                    databaseDialect
                )
            )
        }
        if (!pluginsInitialized) {
            pluginsInitialized = true
            WicketSupport.register(applicationContext)
            var webRegistryClazz: Class<*>? = null
            try {
                // Wicket package not available for compilation.
                webRegistryClazz = Class.forName("org.projectforge.web.registry.WebRegistry")
                val webRegistry = webRegistryClazz.getMethod("getInstance").invoke(null)
                webRegistryClazz.getMethod("init").invoke(webRegistry)
            } catch (ex: ReflectiveOperationException) {
                if (webRegistryClazz == null) {
                    // Wicket not present in current plugin to test (OK)
                } else {
                    baseLog.error(ex.message, ex)
                }
            }
            pluginAdminService.initializeAllPluginsForUnitTest()
            addBundleName(Constants.RESOURCE_BUNDLE_NAME)
            // Register all resource bundles of the plugins
            for (plugin in PluginsRegistry.instance().plugins) {
                for (bundleName in plugin.resourceBundleNames) {
                    addBundleName(bundleName)
                }
            }
        }
    }

    protected var mCount: Int = 0

    private var testRepoDir: File? = null

    init {
        System.setProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR, File("target", "ProjectForgeTest").absolutePath)
    }

    /**
     * Override this as beforeAll, but non static.
     */
    protected open fun beforeAll() {
    }

    /**
     * Override this as afterAll, but non static.
     * Every test should finish with a valid database with test cases. If not, the test should call recreateDatabase() on afterAll!
     */
    protected open fun afterAll() {
    }

    @BeforeEach
    fun beforeEach() {
        if (instance == null) {
            instance = this // Store instance for afterAll method.
            // System.out.println("******** " + instance.getClass());
        }
        if (testRepoDir != null) {
            repoService.internalResetForJunitTestCases()
            repoService.init(testRepoDir!!)
            testRepoDir = null // Don't initialize twice.
        }
        if (!initialized) {
            initialized = true
            if (initTestDB.getUser(ADMIN) == null) {
                recreateDataBase()
            }
            beforeAll()
            systemStatus.upAndRunning = true
        }
    }


    /**
     * Will be called once (BeforeClass). You may it call in your test to get a fresh database in your test class for your
     * method.
     */
    fun recreateDataBase() {
        System.setProperty("user.timezone", "UTC")
        TimeZone.setDefault(DateHelper.UTC)
        Locale.setDefault(Locale.ENGLISH)
        baseLog.info("user.timezone is: " + System.getProperty("user.timezone"))
        val jdbc = JdbcTemplate(dataSource)
        try {
            jdbc.execute("CHECKPOINT DEFRAG")
        } catch (ex: BadSqlGrammarException) {
            // ignore
        }

        clearDatabase(persistenceService)

        Configuration(configurationService)
        ConfigXmlTest.createTestConfiguration()

        Registry.getInstance().init(applicationContext)

        try {
            initDb()
        } catch (e: BeansException) {
            baseLog.error("Something in setUp go wrong: " + e.message, e)
        }
        return
    }

    protected open fun initDb() {
        init(true)
    }

    /**
     * Init and reinitialise context before each run
     */
    fun init(createTestData: Boolean) {
        val loginHandler = applicationContext.getBean(LoginDefaultHandler::class.java)
        loginHandler.initialize()
        Login.getInstance().setLoginHandler(loginHandler)
        if (createTestData) {
            initTestDB.initDatabase()
        }
    }

    /**
     * Test cases using the jcr repo should init it. See DataTransferJCRCleanUpJobTest of plugin datatransfer as an example.
     *
     * @param modulName    Maven module name (dir) of your current tested module.
     * @param testRepoName Unique test repoName like "datatransferTestRepo"
     */
    protected fun initJCRTestRepo(modulName: String, testRepoName: String): File {
        val testUtils = TestUtils(modulName)
        testRepoDir = testUtils.deleteAndCreateTestFile(testRepoName)
        return testRepoDir!!
    }

    protected fun clearDatabase() {
        clearDatabase(persistenceService)
        userGroupCache.setExpired()
        initTestDB.clearUsers()
    }

    fun logon(username: String): PFUserDO {
        val user = userService.getInternalByUsername(username)
        if (user == null) {
            Assertions.fail<Any>("User not found: $username")
        }
        setUser(createCopy(user))
        return user
    }

    fun logon(user: PFUserDO): PFUserDO {
        setUser(user)
        return user
    }

    protected fun logoff() {
        setUser(null)
    }

    fun getGroup(groupName: String?): GroupDO {
        return initTestDB.getGroup(groupName)!!
    }

    fun getGroupId(groupName: String?): Long {
        return initTestDB.getGroup(groupName)!!.id!!
    }

    fun getTask(taskName: String): TaskDO {
        return initTestDB.getTask(taskName)!!
    }

    fun getUser(userName: String?): PFUserDO {
        return initTestDB.getUser(userName)!!
    }

    fun getUserId(userName: String?): Long {
        return initTestDB.getUser(userName)!!.id!!
    }

    protected fun logStart(name: String) {
        logStartPublic(name)
        mCount = 0
    }

    protected fun logEnd() {
        logEndPublic()
        mCount = 0
    }

    protected fun logDot() {
        log(".")
    }

    protected fun log(string: String?) {
        logPublic(string)
        if (++mCount % 40 == 0) {
            println("")
        }
    }

    protected fun assertAccessException(
        ex: AccessException, taskId: Long?, accessType: AccessType?,
        operationType: OperationType?
    ) {
        Assertions.assertEquals(accessType, ex.accessType)
        Assertions.assertEquals(operationType, ex.operationType)
        Assertions.assertEquals(taskId, ex.taskId)
    }

    protected fun assertUTCDate(
        date: Date, year: Int, month: Month?, day: Int, hour: Int,
        minute: Int, second: Int
    ) {
        val dateTime = from(date, DateHelper.UTC)
        Assertions.assertEquals(year, dateTime.year)
        Assertions.assertEquals(month, dateTime.month)
        Assertions.assertEquals(day, dateTime.dayOfMonth)
        Assertions.assertEquals(hour, dateTime.hour)
        Assertions.assertEquals(minute, dateTime.minute)
        Assertions.assertEquals(second, dateTime.second)
    }

    protected fun assertLocalDate(date: LocalDate, year: Int, month: Month?, day: Int) {
        Assertions.assertEquals(year, date.year)
        Assertions.assertEquals(month, date.month)
        Assertions.assertEquals(day, date.dayOfMonth)
    }

    fun createHistoryTester(): HistoryTester {
        return HistoryTester(persistenceService)
    }

    companion object {
        const val ADMIN: String = "PFAdmin"

        const val TEST_ADMIN_USER: String = "testSysAdmin"

        const val TEST_EMPLOYEE_USER: String = "testEmployee"

        val TEST_EMPLOYEE_USER_PASSWORD: CharArray = "testEmployee42".toCharArray()

        @JvmField
        val TEST_ADMIN_USER_PASSWORD: CharArray = "testSysAdmin42".toCharArray()

        const val TEST_FINANCE_USER: String = "testFinanceUser"

        const val TEST_HR_USER: String = "testHRUser"

        const val TEST_FULL_ACCESS_USER: String = "testFullAccessUser"

        @JvmField
        val TEST_FULL_ACCESS_USER_PASSWORD: CharArray = "testFullAccessUser42".toCharArray()

        const val TEST_GROUP: String = "testGroup"

        const val TEST_USER: String = "testUser"

        @JvmField
        val TEST_USER_PASSWORD: CharArray = "testUser42".toCharArray()

        const val TEST_USER2: String = "testUser2"

        const val TEST_DELETED_USER: String = "deletedTestUser"

        val TEST_DELETED_USER_PASSWORD: CharArray = "deletedTestUser42".toCharArray()

        const val TEST_PROJECT_MANAGER_USER: String = "testProjectManager"

        const val TEST_PROJECT_ASSISTANT_USER: String = "testProjectAssistant"

        const val TEST_CONTROLLING_USER: String = "testController"

        const val TEST_MARKETING_USER: String = "testMarketingUser"

        val ADMIN_GROUP: String = ProjectForgeGroup.ADMIN_GROUP.toString()

        @JvmField
        val FINANCE_GROUP: String = ProjectForgeGroup.FINANCE_GROUP.toString()

        val CONTROLLING_GROUP: String = ProjectForgeGroup.CONTROLLING_GROUP.toString()

        val PROJECT_MANAGER: String = ProjectForgeGroup.PROJECT_MANAGER.toString()

        @JvmField
        val PROJECT_ASSISTANT: String = ProjectForgeGroup.PROJECT_ASSISTANT.toString()

        val MARKETING_GROUP: String = ProjectForgeGroup.MARKETING_GROUP.toString()

        @JvmField
        val ORGA_GROUP: String = ProjectForgeGroup.ORGA_TEAM.toString()

        val HR_GROUP: String = ProjectForgeGroup.HR_GROUP.toString()

        lateinit var ADMIN_USER: PFUserDO

        private var pluginsInitialized = false

        private var initialized = false

        private var instance: AbstractTestBase? = null

        fun logStartPublic(name: String) {
            print("$name: ")
        }

        fun logEndPublic() {
            println(" (OK)")
        }

        fun logDotPublic() {
            logPublic(".")
        }

        fun logPublic(string: String?) {
            print(string)
        }

        fun logSingleEntryPublic(string: String?) {
            println(string)
        }

        /*    @SuppressWarnings("rawtypes")
    protected void assertHistoryEntry(final HistoryEntry entry, final Long entityId, final PFUserDO user,
                                      final EntityOpType type) {
        assertHistoryEntry(entry, entityId, user, type, null, null, null, null);
    }

    @SuppressWarnings("rawtypes")
    protected void assertHistoryEntry(final HistoryEntry entry, final Long entityId, final PFUserDO user,
                                      final EntityOpType type,
                                      final String propertyName, final Class<?> classType, final Object oldValue, final Object newValue) {
        assertEquals(user.getId().toString(), entry.getUserName());
        // assertEquals(AddressDO.class.getSimpleName(), entry.getClassName());
        assertEquals(null, entry.getUserComment());
        assertEquals(type, entry.getEntityOpType());
        assertEquals(entityId, entry.getEntityId());
        if (propertyName != null) {
            fail("TODO HISTORY History not yet implemented");
        }
    }*/
        @JvmStatic
        fun assertBigDecimal(v1: Int, v2: BigDecimal) {
            assertBigDecimal(BigDecimal(v1), v2)
        }

        fun assertBigDecimal(v1: BigDecimal, v2: BigDecimal) {
            Assertions.assertTrue(
                v1.compareTo(v2) == 0,
                "BigDecimal values are not equal: $v1 != $v2"
            )
        }

        @JvmStatic
        @BeforeAll
        fun _beforeAll() {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            setInternalSetUnitTestMode()
            ProjectForgeApp.internalSetJunitTestMode()
            internalJunitTestMode = true
            internalSetTestMode()
            initialized = false
        }

        @JvmStatic
        @AfterAll
        fun _afterAll() {
            instance!!.afterAll()
            instance = null
            initialized = false
        }
    }
}
