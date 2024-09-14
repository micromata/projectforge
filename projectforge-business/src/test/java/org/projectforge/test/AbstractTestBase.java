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

package org.projectforge.test;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.projectforge.Constants;
import org.projectforge.ProjectForgeApp;
import org.projectforge.SystemStatus;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.LoginDefaultHandler;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.service.UserService;
import org.projectforge.database.DatabaseExecutor;
import org.projectforge.database.DatabaseSupport;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner;
import org.projectforge.framework.persistence.jpa.PfPersistenceService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.jcr.RepoService;
import org.projectforge.mail.SendMail;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.plugins.core.PluginsRegistry;
import org.projectforge.registry.Registry;
import org.projectforge.web.WicketSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Every test should finish with a valid database with test cases. If not, the test should call recreateDatabase() on afterAll!
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class})
@Component
public abstract class AbstractTestBase {
    protected static final org.slf4j.Logger baseLog = org.slf4j.LoggerFactory
            .getLogger(AbstractTestBase.class);

    public static final String ADMIN = "PFAdmin";

    public static final String TEST_ADMIN_USER = "testSysAdmin";

    public static final String TEST_EMPLOYEE_USER = "testEmployee";

    public static final char[] TEST_EMPLOYEE_USER_PASSWORD = "testEmployee42".toCharArray();

    public static final char[] TEST_ADMIN_USER_PASSWORD = "testSysAdmin42".toCharArray();

    public static final String TEST_FINANCE_USER = "testFinanceUser";

    public static final String TEST_HR_USER = "testHRUser";

    public static final String TEST_FULL_ACCESS_USER = "testFullAccessUser";

    public static final char[] TEST_FULL_ACCESS_USER_PASSWORD = "testFullAccessUser42".toCharArray();

    public static final String TEST_GROUP = "testGroup";

    public static final String TEST_USER = "testUser";

    public static final char[] TEST_USER_PASSWORD = "testUser42".toCharArray();

    public static final String TEST_USER2 = "testUser2";

    public static final String TEST_DELETED_USER = "deletedTestUser";

    public static final char[] TEST_DELETED_USER_PASSWORD = "deletedTestUser42".toCharArray();

    public static final String TEST_PROJECT_MANAGER_USER = "testProjectManager";

    public static final String TEST_PROJECT_ASSISTANT_USER = "testProjectAssistant";

    public static final String TEST_CONTROLLING_USER = "testController";

    public static final String TEST_MARKETING_USER = "testMarketingUser";

    public static final String ADMIN_GROUP = ProjectForgeGroup.ADMIN_GROUP.toString();

    public static final String FINANCE_GROUP = ProjectForgeGroup.FINANCE_GROUP.toString();

    public static final String CONTROLLING_GROUP = ProjectForgeGroup.CONTROLLING_GROUP.toString();

    public static final String PROJECT_MANAGER = ProjectForgeGroup.PROJECT_MANAGER.toString();

    public static final String PROJECT_ASSISTANT = ProjectForgeGroup.PROJECT_ASSISTANT.toString();

    public static final String MARKETING_GROUP = ProjectForgeGroup.MARKETING_GROUP.toString();

    public static final String ORGA_GROUP = ProjectForgeGroup.ORGA_TEAM.toString();

    public static final String HR_GROUP = ProjectForgeGroup.HR_GROUP.toString();

    @Autowired
    protected ApplicationContext applicationContext;

    public static PFUserDO ADMIN_USER;

    @Autowired
    protected UserService userService;

    @Autowired
    protected AccessChecker accessChecker;

    @Autowired
    public InitTestDB initTestDB;

    @Autowired
    private DataSource dataSource;

    private DatabaseExecutor databaseExecutor;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private PluginAdminService pluginAdminService;

    @Autowired
    private RepoService repoService;

    @Autowired
    private SystemStatus systemStatus;

    @Autowired
    private UserGroupCache userGroupCache;

    @Autowired
    protected PfPersistenceService persistenceService;

    private static boolean pluginsInitialized = false;

    @PostConstruct
    private void postConstruct() {
        if (DatabaseSupport.getInstance() == null) {
            baseLog.info("************ Initializing DatabaseSupport with database dialect = " + HibernateUtils.getDatabaseDialect());
            DatabaseSupport.setInstance(new DatabaseSupport(HibernateUtils.getDatabaseDialect()));
        }
        if (!pluginsInitialized) {
            pluginsInitialized = true;
            WicketSupport.register(applicationContext);
            Class<?> webRegistryClazz = null;
            try {
                // Wicket package not available for compilation.
                webRegistryClazz = Class.forName("org.projectforge.web.registry.WebRegistry");
                Object webRegistry = webRegistryClazz.getMethod("getInstance").invoke(null);
                webRegistryClazz.getMethod("init").invoke(webRegistry);
            } catch (ReflectiveOperationException ex) {
                if (webRegistryClazz == null) {
                    // Wicket not present in current plugin to test (OK)
                } else {
                    baseLog.error(ex.getMessage(), ex);
                }
            }
            pluginAdminService.initializeAllPluginsForUnitTest();
            I18nHelper.addBundleName(Constants.RESOURCE_BUNDLE_NAME);
            // Register all resource bundles of the plugins
            for (AbstractPlugin plugin : PluginsRegistry.instance().getPlugins()) {
                for (String bundleName : plugin.getResourceBundleNames()) {
                    I18nHelper.addBundleName(bundleName);
                }
            }
        }
    }

    protected int mCount = 0;

    private static boolean initialized = false;

    private static AbstractTestBase instance = null;

    private File testRepoDir = null;

    protected AbstractTestBase() {
        System.setProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR, new File("target", "ProjectForgeTest").getAbsolutePath());
    }

    @BeforeAll
    public static void _beforeAll() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        MyJpaWithExtLibrariesScanner.setInternalSetUnitTestMode();
        ProjectForgeApp.internalSetJunitTestMode();
        AbstractPlugin.setInternalJunitTestMode(true);
        SendMail.internalSetTestMode();
        initialized = false;
    }

    @AfterAll
    public static void _afterAll() {
        instance.afterAll();
        instance = null;
        initialized = false;
    }

    /**
     * Override this as beforeAll, but non static.
     */
    protected void beforeAll() {

    }

    /**
     * Override this as afterAll, but non static.
     * Every test should finish with a valid database with test cases. If not, the test should call recreateDatabase() on afterAll!
     */
    protected void afterAll() {

    }

    @BeforeEach
    void beforeEach() {
        if (instance == null) {
            instance = this; // Store instance for afterAll method.
            // System.out.println("******** " + instance.getClass());
        }
        if (testRepoDir != null) {
            repoService.internalResetForJunitTestCases();
            repoService.init(testRepoDir);
            testRepoDir = null; // Don't initialize twice.
        }
        if (!initialized) {
            initialized = true;
            if (getUser(ADMIN) == null) {
                recreateDataBase();
            }
            beforeAll();
            systemStatus.setUpAndRunning(true);
        }
    }


    /**
     * Will be called once (BeforeClass). You may it call in your test to get a fresh database in your test class for your
     * method.
     */
    public void recreateDataBase() {
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(DateHelper.UTC);
        Locale.setDefault(Locale.ENGLISH);
        baseLog.info("user.timezone is: " + System.getProperty("user.timezone"));
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            jdbc.execute("CHECKPOINT DEFRAG");
        } catch (final org.springframework.jdbc.BadSqlGrammarException ex) {
            // ignore
        }

        DatabaseHelper.clearDatabase(persistenceService);

        new Configuration(configurationService);
        ConfigXmlTest.createTestConfiguration();

        Registry.getInstance().init(applicationContext);

        try {
            initDb();
        } catch (BeansException e) {
            baseLog.error("Something in setUp go wrong: " + e.getMessage(), e);
        }
        return;
    }

    protected void initDb() {
        init(true);
    }

    /**
     * Init and reinitialise context before each run
     */
    public void init(final boolean createTestData) {
        final LoginDefaultHandler loginHandler = applicationContext.getBean(LoginDefaultHandler.class);
        loginHandler.initialize();
        Login.getInstance().setLoginHandler(loginHandler);
        if (createTestData) {
            initTestDB.initDatabase();
        }
    }

    /**
     * Test cases using the jcr repo should init it. See DataTransferJCRCleanUpJobTest of plugin datatransfer as an example.
     *
     * @param modulName    Maven module name (dir) of your current tested module.
     * @param testRepoName Unique test repoName like "datatransferTestRepo"
     */
    protected File initJCRTestRepo(String modulName, String testRepoName) {
        final TestUtils testUtils = new TestUtils(modulName);
        testRepoDir = testUtils.deleteAndCreateTestFile("cleanUpTestRepo");
        return testRepoDir;
    }

    protected void clearDatabase() {
        DatabaseHelper.clearDatabase(persistenceService);
        userGroupCache.setExpired();
        initTestDB.clearUsers();
    }

    public PFUserDO logon(final String username) {
        final PFUserDO user = userService.getInternalByUsername(username);
        if (user == null) {
            fail("User not found: " + username);
        }
        ThreadLocalUserContext.setUser(PFUserDO.Companion.createCopy(user));
        return user;
    }

    public void logon(final PFUserDO user) {
        ThreadLocalUserContext.setUser(user);
    }

    protected void logoff() {
        ThreadLocalUserContext.setUser(null);
    }

    public GroupDO getGroup(final String groupName) {
        return initTestDB.getGroup(groupName);
    }

    public Long getGroupId(final String groupName) {
        return initTestDB.getGroup(groupName).getId();
    }

    public TaskDO getTask(final String taskName) {
        return initTestDB.getTask(taskName);
    }

    public PFUserDO getUser(final String userName) {
        return initTestDB.getUser(userName);
    }

    public Long getUserId(final String userName) {
        return initTestDB.getUser(userName).getId();
    }

    protected void logStart(final String name) {
        logStartPublic(name);
        mCount = 0;
    }

    protected void logEnd() {
        logEndPublic();
        mCount = 0;
    }

    protected void logDot() {
        log(".");
    }

    protected void log(final String string) {
        logPublic(string);
        if (++mCount % 40 == 0) {
            System.out.println("");
        }
    }

    public static void logStartPublic(final String name) {
        System.out.print(name + ": ");
    }

    public static void logEndPublic() {
        System.out.println(" (OK)");
    }

    public static void logDotPublic() {
        logPublic(".");
    }

    public static void logPublic(final String string) {
        System.out.print(string);
    }

    public static void logSingleEntryPublic(final String string) {
        System.out.println(string);
    }

    protected void assertAccessException(final AccessException ex, final Long taskId, final AccessType accessType,
                                         final OperationType operationType) {
        assertEquals(accessType, ex.getAccessType());
        assertEquals(operationType, ex.getOperationType());
        assertEquals(taskId, ex.getTaskId());
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

    public static void assertBigDecimal(final int v1, final BigDecimal v2) {
        assertBigDecimal(new BigDecimal(v1), v2);
    }

    public static void assertBigDecimal(final BigDecimal v1, final BigDecimal v2) {
        assertTrue(v1.compareTo(v2) == 0, "BigDecimal values are not equal: " + v1 + " != " + v2);
    }

    protected void assertUTCDate(final Date date, final int year, final Month month, final int day, final int hour,
                                 final int minute, final int second) {
        PFDateTime dateTime = PFDateTime.from(date, DateHelper.UTC);
        assertEquals(year, dateTime.getYear());
        assertEquals(month, dateTime.getMonth());
        assertEquals(day, dateTime.getDayOfMonth());
        assertEquals(hour, dateTime.getHour());
        assertEquals(minute, dateTime.getMinute());
        assertEquals(second, dateTime.getSecond());
    }

    protected void assertLocalDate(final LocalDate date, final int year, final Month month, final int day) {
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDayOfMonth());
    }
}
