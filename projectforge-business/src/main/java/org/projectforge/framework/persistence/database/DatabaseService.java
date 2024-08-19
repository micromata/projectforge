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

package org.projectforge.framework.persistence.database;

import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.business.login.Login;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.user.*;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.database.DatabaseExecutor;
import org.projectforge.database.DatabaseResultRow;
import org.projectforge.database.DatabaseSupport;
import org.projectforge.database.jdbc.DatabaseExecutorImpl;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessCheckerImpl;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.history.HibernateSearchReindexer;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPasswordDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class DatabaseService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseService.class);

  public static final String DEFAULT_ADMIN_USER = "admin";

  private static final PFUserDO SYSTEM_ADMIN_PSEUDO_USER = new PFUserDO();

  static {
    SYSTEM_ADMIN_PSEUDO_USER.setUsername("System admin user only for internal usage");
  }

  @Autowired
  private HibernateSearchReindexer hibernateSearchReindexer;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserPasswordDao userPasswordDao;

  @Autowired
  private UserGroupCache userGroupCache;

  @Autowired
  private UserRightDao userRightDao;

  @Autowired
  private TaskTree taskTree;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AddressbookDao addressbookDao;

  @Value("${hibernate.search.default.indexBase}")
  private String hibernateIndexDir;

  @Autowired
  private DataSource dataSource;

  private DatabaseSupport databaseSupport;

  private DatabaseExecutor databaseExecutor;

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private EntityManager entityManager;

  /**
   * If the database is empty (user list is empty) then a admin user and ProjectForge root task will be created.
   *
   * @param adminUser The admin user with the desired username and the salted password (salt string included). All other
   *                  attributes and groups of the user are set by this method.
   */
  public void initializeDefaultData(final PFUserDO adminUser, final TimeZone adminUserTimezone) {
    log.info("Init admin user and root task.");
    if (databaseTablesWithEntriesExist()) {
      databaseNotEmpty();
    }

    final TaskDO task = new TaskDO();
    task.setTitle("Root");
    task.setStatus(TaskStatus.N);
    task.setShortDescription("ProjectForge root task");
    task.setCreated();
    task.setLastUpdate();
    entityManager.persist(task);
    log.info("New object added (" + task.getId() + "): " + task.toString());
    // Use of taskDao does not work with maven test case: Could not synchronize database state with session?

    // Create Admin user
    adminUser.setLocalUser(true);
    adminUser.setLastname("Administrator");
    adminUser.setDescription("ProjectForge administrator");
    adminUser.setTimeZone(adminUserTimezone);
    userDao.internalSave(adminUser);
    adminUser.addRight(new UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.FIBU_COST_UNIT, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE));
    adminUser.addRight(new UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.FIBU_ACCOUNTS, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.ORGA_CONTRACTS, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.ORGA_INCOMING_MAIL, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.ORGA_OUTGOING_MAIL, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.PM_PROJECT, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.READWRITE));
    adminUser.addRight(new UserRightDO(UserRightId.PM_HR_PLANNING, UserRightValue.READWRITE));
    adminUser.getRights().forEach(userRightDao::internalSave);

    ThreadLocalUserContext.setUser(adminUser); // Need to login the admin user for avoiding following access exceptions.

    internalCreateProjectForgeGroups(adminUser);

    taskTree.setExpired();
    userGroupCache.setExpired();

    log.info("Default data successfully initialized in database.");
  }

  public AddressbookDO insertGlobalAddressbook() {
    return insertGlobalAddressbook(null);
  }

  public AddressbookDO insertGlobalAddressbook(PFUserDO user) {
    log.info("Checking if global addressbook exists.");
    try {
      String selectGlobal = "SELECT * FROM t_addressbook WHERE pk = 1";
      SqlRowSet selectResult = jdbcTemplate.queryForRowSet(selectGlobal);
      if (selectResult != null && selectResult.getRow() > 0) {
        return addressbookDao.getGlobalAddressbook();
      }
    } catch (Exception e) {
      log.warn("Something went wrong while checking for global addressbook: " + e.getMessage());
    }
    log.info("Adding global addressbook.");
    String insertGlobal =
        "INSERT INTO t_addressbook(pk, created, deleted, last_update, description, title, owner_fk) "
            + "VALUES (1, CURRENT_TIMESTAMP, false, CURRENT_TIMESTAMP, 'The global addressbook', 'Global', "
            + (user != null && user.getId() != null ? user.getId() : ThreadLocalUserContext.getUserId()) + ")";
    jdbcTemplate.execute(insertGlobal);
    log.info("Adding global addressbook finished: " + insertGlobal);
    return addressbookDao.getGlobalAddressbook();
  }

  private void internalCreateProjectForgeGroups(final PFUserDO adminUser) {
    final Set<PFUserDO> adminUsers = new HashSet<>();
    adminUsers.add(adminUser);

    addGroup(ProjectForgeGroup.ADMIN_GROUP, "Administrators of ProjectForge", adminUsers);
    addGroup(ProjectForgeGroup.CONTROLLING_GROUP, "Users for having read access to the company's finances.", adminUsers);
    addGroup(ProjectForgeGroup.FINANCE_GROUP, "Finance and Accounting", adminUsers);
    addGroup(ProjectForgeGroup.MARKETING_GROUP, "Marketing users can download all addresses in excel format.", adminUsers);
    addGroup(ProjectForgeGroup.ORGA_TEAM, "The organization team has access to post in- and outbound, contracts etc..", adminUsers);
    addGroup(ProjectForgeGroup.HR_GROUP, "Users for having full access to the companies hr.", adminUsers);
    addGroup(ProjectForgeGroup.PROJECT_MANAGER,
        "Project managers have access to assigned orders and resource planning.", null);
    addGroup(ProjectForgeGroup.PROJECT_ASSISTANT, "Project assistants have access to assigned orders.", null);
  }

  private void addGroup(final ProjectForgeGroup projectForgeGroup, final String description, final Set<PFUserDO> users) {
    final GroupDO group = new GroupDO();
    group.setName(projectForgeGroup.toString());
    group.setDescription(description);
    if (users != null) {
      group.setAssignedUsers(users);
    }
    // group.setNestedGroupsAllowed(false);
    group.setLocalGroup(true); // Do not synchronize group with external user management system by default.
    groupDao.internalSave(group);
  }

  /**
   * @param user              The admin user with the desired username and the salted password (salt string included).
   * @param adminUserTimezone
   */
  public PFUserDO updateAdminUser(PFUserDO user, final TimeZone adminUserTimezone) {
    //Update test data user with data from setup page
    PFUserDO adminUser = userDao.getInternalByName(DEFAULT_ADMIN_USER);
    adminUser.setUsername(user.getUsername());
    adminUser.setLocalUser(true);
    adminUser.setTimeZone(adminUserTimezone);
    userDao.internalUpdate(adminUser);
    ThreadLocalUserContext.setUser(adminUser);
    userGroupCache.forceReload();
    return adminUser;
  }

  public void updatePasswords(PFUserDO user, String password, String passwordSalt) {
    UserPasswordDO passwordObj = new UserPasswordDO();
    passwordObj.setPasswordHash(password);
    passwordObj.setPasswordSalt(passwordSalt);
    userPasswordDao.saveOrUpdate(user.getId(), passwordObj);
  }

  public void afterCreatedTestDb(boolean blocking) {
    Thread rebuildThread = new Thread(() -> hibernateSearchReindexer.rebuildDatabaseSearchIndices());
    rebuildThread.start();
    if (blocking) {
      try {
        rebuildThread.join();
      } catch (InterruptedException e) {
        log.warn("reindex thread was interrupted: " + e.getMessage(), e);
      }
    }
    taskTree.setExpired();
    userGroupCache.setExpired();
    log.info("Database successfully initialized with test data.");
  }

  private void databaseNotEmpty() {
    final String msg = "Database seems to be not empty. Initialization of database aborted.";
    log.error(msg);
    throw new AccessException(msg);
  }

  public void updateSchema() {
    log.info("Start generating Schema...");
    Map<String, Object> props = new HashMap<>();
    props.put("hibernate.hbm2ddl.auto", "update");
    props.put("hibernate.search.default.indexBase", hibernateIndexDir);
    props.put("hibernate.connection.datasource", dataSource);
    try {
      Persistence.createEntityManagerFactory("org.projectforge.webapp", props);
    } catch (Exception e) {
      log.error("Exception while updateSchema:" + e.getMessage(), e);
      throw e;
    }
    log.info("Finished generating Schema...");
  }

  public DatabaseDialect getDialect() {
    return HibernateUtils.getDatabaseDialect();
  }

  private DatabaseSupport getDatabaseSupport() {
    if (databaseSupport == null) {
      databaseSupport = new DatabaseSupport(getDialect());
    }
    return databaseSupport;
  }

  private DatabaseExecutor getDatabaseExecutor() {
    if (databaseExecutor == null) {
      databaseExecutor = new DatabaseExecutorImpl();
      databaseExecutor.setDataSource(dataSource);
    }
    return databaseExecutor;
  }

  /**
   * Does nothing at default. Override this method for checking the access of the user, e. g. only admin user's should
   * be able to manipulate the database.
   *
   * @param writeaccess
   */
  protected void accessCheck(final boolean writeaccess) {
    if (ThreadLocalUserContext.getUser() == SYSTEM_ADMIN_PSEUDO_USER) {
      // No access check for the system admin pseudo user.
      return;
    }
    if (!Login.getInstance().isAdminUser(ThreadLocalUserContext.getUser())) {
      throw new AccessException(AccessCheckerImpl.I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF,
          ProjectForgeGroup.ADMIN_GROUP.getKey());
    }
    accessChecker.checkRestrictedOrDemoUser();
  }

  public boolean doesTableExist(final String table) {
    accessCheck(false);
    return internalDoesTableExist(table);
  }

  /**
   * Without check access.
   *
   * @param table
   * @return
   */
  public boolean internalDoesTableExist(final String table) {
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      jdbc.queryForInt("SELECT COUNT(*) FROM " + table);
    } catch (final Exception ex) {
      log.warn("Exception while checking count from table: " + table + " Exception: " + ex.getMessage());
      return false;
    }
    return true;
  }

  public boolean isTableEmpty(final String table) {
    accessCheck(false);
    return internalIsTableEmpty(table);
  }

  public boolean internalIsTableEmpty(final String table) {
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      return jdbc.queryForInt("SELECT COUNT(*) FROM " + table) == 0;
    } catch (final Exception ex) {
      return false;
    }
  }

  /**
   * Creates missing database indices of tables starting with 't_'.
   *
   * @return Number of successful created database indices.
   */
  public int createMissingIndices() {
    accessCheck(true);
    log.info("createMissingIndices called.");
    int counter = 0;
    // For user / time period search:
    try (Connection connection = dataSource.getConnection()) {
      final ResultSet reference = connection.getMetaData().getCrossReference(null, null, null, null, null, null);
      while (reference.next()) {
        final String fkTable = reference.getString("FKTABLE_NAME");
        final String fkCol = reference.getString("FKCOLUMN_NAME");
        if (fkTable.startsWith("t_")) {
          // Table of ProjectForge
          if (createIndex("idx_fk_" + fkTable + "_" + fkCol, fkTable, fkCol)) {
            counter++;
          }
        }
      }
    } catch (final SQLException ex) {
      log.error(ex.getMessage(), ex);
    }
    return counter;
  }

  /**
   * Creates the given database index if not already exists.
   *
   * @param name
   * @param table
   * @param attributes
   * @return true, if the index was created, false if an error has occured or the index already exists.
   */
  public boolean createIndex(final String name, final String table, final String attributes) {
    accessCheck(true);
    try {
      final String jdbcString = "CREATE INDEX " + name + " ON " + table + "(" + attributes + ");";
      execute(jdbcString, false);
      log.info(jdbcString);
      return true;
    } catch (final Throwable ex) {
      // Index does already exist (or an error has occurred).
      return false;
    }
  }

  /**
   * @param jdbcString
   * @see #execute(String, boolean)
   */
  public void execute(final String jdbcString) {
    execute(jdbcString, true);
  }

  /**
   * Executes the given String
   *
   * @param jdbcString
   * @param ignoreErrors If true (default) then errors will be caught and logged.
   * @return true if no error occurred (no exception was caught), otherwise false.
   */
  public void execute(final String jdbcString, final boolean ignoreErrors) {
    accessCheck(true);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    jdbc.execute(jdbcString, ignoreErrors);
    log.info(jdbcString);
  }

  public int queryForInt(final String jdbcQuery) {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    log.info(jdbcQuery);
    return jdbc.queryForInt(jdbcQuery);
  }

  public List<DatabaseResultRow> query(final String sql, final Object... args) {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    log.info(sql);
    return jdbc.query(sql, args);
  }

  /**
   * Will be called on shutdown.
   *
   * @see DatabaseSupport#getShutdownDatabaseStatement()
   */
  public void shutdownDatabase() {
    final String statement = getDatabaseSupport().getShutdownDatabaseStatement();
    if (statement == null) {
      return;
    }
    log.info("Executing database shutdown statement: " + statement);
    execute(statement);
  }

  public static PFUserDO __internalGetSystemAdminPseudoUser() {
    return SYSTEM_ADMIN_PSEUDO_USER;
  }

  /**
   * Needed for checking if user table is existing on startup. If not, the setup page will be called (see ProjectForge.jsx)
   *
   * @return
   */
  public boolean databaseTablesWithEntriesExist() {
    try {
      final String tableName = "T_PF_USER";
      return internalDoesTableExist(tableName) && !internalIsTableEmpty(tableName);
    } catch (final Exception ex) {
      log.error("Error while checking existing of user table with entries.", ex);
    }
    return false;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
