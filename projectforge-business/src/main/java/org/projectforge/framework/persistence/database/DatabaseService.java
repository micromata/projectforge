/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.business.login.Login;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.*;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.common.StringHelper;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.continuousdb.Table;
import org.projectforge.continuousdb.*;
import org.projectforge.continuousdb.hibernate.TableAttributeHookImpl;
import org.projectforge.continuousdb.jdbc.DatabaseExecutorImpl;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessCheckerImpl;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.history.HibernateSearchReindexer;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
  private ApplicationContext applicationContext;

  @PersistenceContext
  private EntityManager em;

  @Autowired
  private HibernateSearchReindexer hibernateSearchReindexer;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserRightDao userRightDao;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private AddressbookDao addressbookDao;

  @Value("${hibernate.search.default.indexBase}")
  private String hibernateIndexDir;

  @Autowired
  private DataSource dataSource;

  private DatabaseSupport databaseSupport;

  private DatabaseExecutor databaseExecutor;

  private SystemUpdater systemUpdater;

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private PfEmgrFactory emf;

  @Autowired
  private EntityManager entityManager;

  @PostConstruct
  public void initialize() {
    TableAttribute.register(new TableAttributeHookImpl());

    DatabaseCoreUpdates.setApplicationContext(this.applicationContext);
    final SortedSet<UpdateEntry> updateEntries = new TreeSet<>(DatabaseCoreUpdates.getUpdateEntries());
    getSystemUpdater().setUpdateEntries(updateEntries);
  }

  /**
   * If the database is empty (user list is empty) then a admin user and ProjectForge root task will be created.
   *
   * @param adminUser The admin user with the desired username and the salted password (salt string included). All other
   *                  attributes and groups of the user are set by this method.
   */
  public void initializeDefaultData(final PFUserDO adminUser, final TimeZone adminUserTimezone) {
    log.info("Init admin user and root task.");
    if (databaseTablesWithEntriesExists()) {
      databaseNotEmpty();
    }

    TenantDO defaultTenant = tenantService.getDefaultTenant();

    final TaskDO task = new TaskDO();
    task.setTitle("Root");
    task.setStatus(TaskStatus.N);
    task.setShortDescription("ProjectForge root task");
    task.setCreated();
    task.setLastUpdate();
    task.setTenant(defaultTenant);
    entityManager.persist(task);
    log.info("New object added (" + task.getId() + "): " + task.toString());
    // Use of taskDao does not work with maven test case: Could not synchronize database state with session?

    // Create Admin user
    adminUser.setLocalUser(true);
    adminUser.setLastname("Administrator");
    adminUser.setDescription("ProjectForge administrator");
    adminUser.setTimeZone(adminUserTimezone);
    adminUser.setTenant(defaultTenant);
    adminUser.setSuperAdmin(true);
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

    ThreadLocalUserContext.setUser(getUserGroupCache(), adminUser); // Need to login the admin user for avoiding following access exceptions.
    TenantRegistryMap.getInstance().clear();
    TenantRegistryMap.getInstance().getTenantRegistry();

    TenantDao tenantDao = applicationContext.getBean(TenantDao.class);
    Set<TenantDO> tenantsToAssign = new HashSet<>();
    tenantsToAssign.add(defaultTenant);
    tenantDao.internalAssignTenants(adminUser, tenantsToAssign, null, false, false);

    internalCreateProjectForgeGroups(defaultTenant, adminUser);

    TaskTreeHelper.getTaskTree().setExpired();
    getUserGroupCache().setExpired();
    TenantRegistryMap.getInstance().setAllUserGroupCachesAsExpired();

    log.info("Default data successfully initialized in database.");
  }

  public TenantRegistry getTenantRegistry() {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  public UserGroupCache getUserGroupCache() {
    return getTenantRegistry().getUserGroupCache();
  }

  public TenantDO insertDefaultTenant() {
    log.info("Checking if default tenant exists.");
    try {
      String selectDefaultTenant = "SELECT * FROM t_tenant WHERE pk = 1";
      SqlRowSet selectResult = jdbcTemplate.queryForRowSet(selectDefaultTenant);
      if (selectResult != null && selectResult.getRow() > 0) {
        return tenantService.getDefaultTenant();
      }
    } catch (Exception e) {
      log.warn("Something went wrong while checking for default tenant: " + e.getMessage());
    }
    log.info("Adding default tenant.");
    String insertDefaultTenant = "INSERT INTO t_tenant(PK, CREATED, DELETED, LAST_UPDATE, DEFAULT_TENANT, NAME, SHORTNAME, DESCRIPTION, TENANT_ID) "
            + "VALUES (1,'2016-03-17 14:00:00',FALSE,'2016-03-17 14:00:00',TRUE,'Default tenant','Default tenant','defaultTenant',1)";
    jdbcTemplate.execute(insertDefaultTenant);
    log.info("Adding default tenant finished.");
    tenantService.resetTenantTableStatus();
    return tenantService.getDefaultTenant();
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
            "INSERT INTO t_addressbook(pk, created, deleted, last_update, description, title, tenant_id, owner_fk) "
                    + "VALUES (1, CURRENT_TIMESTAMP, false, CURRENT_TIMESTAMP, 'The global addressbook', 'Global', 1, "
                    + (user != null && user.getId() != null ? user.getId() : ThreadLocalUserContext.getUserId()) + ")";
    jdbcTemplate.execute(insertGlobal);
    log.info("Adding global addressbook finished.");
    return addressbookDao.getGlobalAddressbook();
  }

  /**
   * Only for internal usage by {@link TenantDao} or {@link InitDatabaseDao}!
   *
   * @param tenant
   * @param adminUser
   */
  public void internalCreateProjectForgeGroups(final TenantDO tenant, final PFUserDO adminUser) {
    final Set<PFUserDO> adminUsers = new HashSet<>();
    adminUsers.add(adminUser);
    Set<PFUserDO> adminUsersForNewTenants = null;
    if (tenant.isDefault()) {
      // Assign admin user for almost all groups only for initialization of a new ProjectForge installation. For new tenants the admin user
      // is only assigned to the admin group for the new tenant.
      adminUsersForNewTenants = adminUsers;
    }

    addGroup(ProjectForgeGroup.ADMIN_GROUP, "Administrators of ProjectForge", tenant, adminUsers);
    addGroup(ProjectForgeGroup.CONTROLLING_GROUP, "Users for having read access to the company's finances.", tenant,
            adminUsersForNewTenants);
    addGroup(ProjectForgeGroup.FINANCE_GROUP, "Finance and Accounting", tenant, adminUsersForNewTenants);
    addGroup(ProjectForgeGroup.MARKETING_GROUP, "Marketing users can download all addresses in excel format.", tenant,
            null);
    addGroup(ProjectForgeGroup.ORGA_TEAM, "The organization team has access to post in- and outbound, contracts etc..",
            tenant,
            adminUsersForNewTenants);
    addGroup(ProjectForgeGroup.HR_GROUP, "Users for having full access to the companies hr.",
            tenant,
            adminUsersForNewTenants);
    addGroup(ProjectForgeGroup.PROJECT_MANAGER,
            "Project managers have access to assigned orders and resource planning.", tenant, null);
    addGroup(ProjectForgeGroup.PROJECT_ASSISTANT, "Project assistants have access to assigned orders.", tenant, null);
  }

  private void addGroup(final ProjectForgeGroup projectForgeGroup, final String description, final TenantDO tenant,
                        final Set<PFUserDO> users) {
    final GroupDO group = new GroupDO();
    group.setName(projectForgeGroup.toString());
    group.setDescription(description);
    if (users != null) {
      group.setAssignedUsers(users);
    }
    group.setTenant(tenant);
    // group.setNestedGroupsAllowed(false);
    group.setLocalGroup(true); // Do not synchronize group with external user management system by default.
    groupDao.internalSave(group);
  }

  /**
   * @param adminUser         The admin user with the desired username and the salted password (salt string included).
   * @param adminUserTimezone
   */
  public PFUserDO updateAdminUser(PFUserDO user, final TimeZone adminUserTimezone) {
    //Update test data user with data from setup page
    PFUserDO adminUser = userDao.getInternalByName(DEFAULT_ADMIN_USER);
    adminUser.setUsername(user.getUsername());
    adminUser.setPassword(user.getPassword());
    adminUser.setPasswordSalt(user.getPasswordSalt());
    adminUser.setLocalUser(true);
    adminUser.setTimeZone(adminUserTimezone);
    adminUser.setTenant(tenantService.getDefaultTenant());
    adminUser.setSuperAdmin(true);
    userDao.internalUpdate(adminUser);
    ThreadLocalUserContext.setUser(getUserGroupCache(), adminUser);
    TenantRegistryMap.getInstance().clear();
    UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    userGroupCache.forceReload();
    return adminUser;
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
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    taskTree.setExpired();
    TenantRegistryMap.getInstance().setAllUserGroupCachesAsExpired();
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
    return HibernateUtils.getDialect();
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

  public boolean doTablesExist(final Class<?>... entities) {
    accessCheck(false);
    for (final Class<?> entity : entities) {
      if (!internalDoesTableExist(new Table(entity).getName())) {
        return false;
      }
    }
    return true;
  }

  public boolean doExist(final Table... tables) {
    accessCheck(false);
    for (final Table table : tables) {
      if (!internalDoesTableExist(table.getName())) {
        return false;
      }
    }
    return true;
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

  public boolean doesTableAttributeExist(final String table, final String attribute) {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      jdbc.queryForInt("SELECT COUNT(" + attribute + ") FROM " + table);
    } catch (final Exception ex) {
      return false;
    }
    return true;
  }

  /**
   * @param entityClass
   * @param properties
   * @return false if at least one property of the given entity doesn't exist in the database, otherwise true.
   */
  public boolean doTableAttributesExist(final Class<?> entityClass, final String... properties) {
    accessCheck(false);
    final Table table = new Table(entityClass);
    return doTableAttributesExist(table, properties);
  }

  /**
   * @param table
   * @param properties
   * @return false if at least one property of the given table doesn't exist in the database, otherwise true.
   */
  public boolean doTableAttributesExist(final Table table, final String... properties) {
    accessCheck(false);
    for (final String property : properties) {
      final TableAttribute attr = TableAttribute.createTableAttribute(table.getEntityClass(), property);
      if (attr == null) {
        // Transient or getter method not found.
        return false;
      }
      if (!doesTableAttributeExist(table.getName(), attr.getName())) {
        return false;
      }
    }
    return true;
  }

  public boolean isTableEmpty(final Class<?> entity) {
    return isTableEmpty(new Table(entity).getName());
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
   * @param table
   * @return true, if the table is successfully dropped or does not exist.
   */
  public boolean dropTable(final String table) {
    accessCheck(true);
    if (!doesTableExist(table)) {
      // Table is already dropped or does not exist.
      return true;
    }
    if (!isTableEmpty(table)) {
      // Table is not empty.
      log.warn("Could not drop table '" + table + "' because the table is not empty.");
      return false;
    }
    execute("DROP TABLE " + table);
    return true;
  }

  /**
   * @param table
   * @param attribute
   * @return
   */
  public boolean dropTableAttribute(final String table, final String attribute) {
    accessCheck(true);
    execute("ALTER TABLE " + table + " DROP COLUMN " + attribute);
    return true;
  }

  /**
   * @param table
   * @param attribute
   * @param length
   * @return
   */
  public boolean alterTableColumnVarCharLength(final String table, final String attribute, final int length) {
    accessCheck(true);
    execute(getDatabaseSupport().alterTableColumnVarCharLength(table, attribute, length), false);
    return true;
  }

  public void buildCreateTableStatement(final StringBuffer buf, final Table table) {
    buf.append("CREATE TABLE " + table.getName() + " (\n");
    boolean first = true;
    for (final TableAttribute attr : table.getAttributes()) {
      if (attr.getType().isIn(TableAttributeType.LIST, TableAttributeType.SET)) {
        // Nothing to be done here.
        continue;
      }
      if (first) {
        first = false;
      } else {
        buf.append(",\n");
      }
      buf.append("  ");
      buildAttribute(buf, attr);
    }
    final TableAttribute primaryKey = table.getPrimaryKey();
    if (primaryKey != null) {
      buf.append(getDatabaseSupport().getPrimaryKeyTableSuffix(primaryKey));
    }
    // Create foreign keys if exist
    for (final TableAttribute attr : table.getAttributes()) {
      if (StringUtils.isNotEmpty(attr.getForeignTable())) {
        // foreign key (user_fk) references t_pf_user(pk)
        buf.append(",\n  FOREIGN KEY (").append(attr.getName()).append(") REFERENCES ").append(attr.getForeignTable())
                .append("(")
                .append(attr.getForeignAttribute()).append(")");
      }
    }
    final UniqueConstraint[] uniqueConstraints = table.getUniqueConstraints();
    if (uniqueConstraints != null && uniqueConstraints.length > 0) {
      for (final UniqueConstraint uniqueConstraint : uniqueConstraints) {
        final String[] columnNames = uniqueConstraint.columnNames();
        if (columnNames.length > 0) {
          buf.append(",\n  UNIQUE (");
          String separator = "";
          for (final String columnName : columnNames) {
            buf.append(separator).append(columnName);
            separator = ",";
          }
          buf.append(")");
        }
      }
    }
    for (final TableAttribute attr : table.getAttributes()) {
      if (!attr.isUnique()) {
        continue;
      }
      buf.append(",\n  UNIQUE (").append(attr.getName()).append(")");
    }
    buf.append("\n);\n");
  }

  public String getAttribute(final Class entityClass, final String property) {
    TableAttribute attr = TableAttribute.createTableAttribute(entityClass, property);

    if (attr == null)
      return "";

    final Column columnAnnotation = attr.getAnnotation(Column.class);
    if (columnAnnotation != null && StringUtils.isNotEmpty(columnAnnotation.columnDefinition())) {
      return columnAnnotation.columnDefinition();
    } else {
      return getDatabaseSupport().getType(attr);
    }
  }

  private void buildAttribute(final StringBuffer buf, final TableAttribute attr) {
    buf.append(attr.getName()).append(" ");
    final Column columnAnnotation = attr.getAnnotation(Column.class);
    if (columnAnnotation != null && StringUtils.isNotEmpty(columnAnnotation.columnDefinition())) {
      buf.append(columnAnnotation.columnDefinition());
    } else {
      buf.append(getDatabaseSupport().getType(attr));
    }
    boolean primaryKeyDefinition = false; // For avoiding double 'not null' definition.
    if (attr.isPrimaryKey()) {
      final String suffix = getDatabaseSupport().getPrimaryKeyAttributeSuffix(attr);
      if (StringUtils.isNotEmpty(suffix)) {
        buf.append(suffix);
        primaryKeyDefinition = true;
      }
    }
    if (!primaryKeyDefinition) {
      getDatabaseSupport().addDefaultAndNotNull(buf, attr);
    }
  }

  public void buildForeignKeyConstraint(final StringBuffer buf, final String table, final TableAttribute attr) {
    buf.append("ALTER TABLE ").append(table).append(" ADD CONSTRAINT ").append(table).append("_").append(attr.getName())
            .append(" FOREIGN KEY (").append(attr.getName()).append(") REFERENCES ").append(attr.getForeignTable())
            .append("(")
            .append(attr.getForeignAttribute()).append(");\n");
  }

  public boolean createTable(final Table table) {
    accessCheck(true);
    if (doExist(table)) {
      log.info("Table '" + table.getName() + "' does already exist.");
      return false;
    }
    final StringBuffer buf = new StringBuffer();
    buildCreateTableStatement(buf, table);
    execute(buf.toString());
    return true;
  }

  public boolean createSequence(final String name, final boolean ignoreErrors) {
    accessCheck(true);
    final String sql = getDatabaseSupport().createSequence(name);
    if (sql != null) {
      execute(sql, ignoreErrors);
    }
    return true;
  }

  public void buildAddTableAttributesStatement(final StringBuffer buf, final String table,
                                               final TableAttribute... attributes) {
    for (final TableAttribute attr : attributes) {
      if (doesTableAttributeExist(table, attr.getName())) {
        buf.append("-- Does already exist: ");
      }
      buf.append("ALTER TABLE ").append(table).append(" ADD COLUMN ");
      buildAttribute(buf, attr);
      buf.append(";\n");
    }
    for (final TableAttribute attr : attributes) {
      if (attr.getForeignTable() != null) {
        if (doesTableAttributeExist(table, attr.getName())) {
          buf.append("-- Column does already exist: ");
        }
        buildForeignKeyConstraint(buf, table, attr);
      }
    }
  }

  public void buildAddTableAttributesStatement(final StringBuffer buf, final String table,
                                               final Collection<TableAttribute> attributes) {
    buildAddTableAttributesStatement(buf, table, attributes.toArray(new TableAttribute[0]));
  }

  /**
   * @param entityClass
   * @param attributeNames Property names of the attributes to create.
   * @return
   */
  public boolean addTableAttributes(final Class<?> entityClass, final String... attributeNames) {
    return addTableAttributes(new Table(entityClass), attributeNames);
  }

  /**
   * @param table
   * @param attributeNames Property names of the attributes to create.
   * @return
   */
  public boolean addTableAttributes(final Table table, final String... attributeNames) {

    final ArrayList<TableAttribute> list = new ArrayList<>();
    for (String attributeName : attributeNames) {
      final TableAttribute attr = TableAttribute.createTableAttribute(table.getEntityClass(), attributeName);
      if (attr == null) {
        log.debug("Property '" + table.getName() + "." + attributeName + "' is transient.");
        continue;
      }
      list.add(attr);
    }
    final TableAttribute[] attributes = list.toArray(new TableAttribute[0]);
    return addTableAttributes(table, attributes);
  }

  public boolean addTableAttributes(final String table, final TableAttribute... attributes) {
    // splitting in multiple commands is required for HSQL
    for (TableAttribute att : attributes) {
      final StringBuffer buf = new StringBuffer();
      buildAddTableAttributesStatement(buf, table, att);
      this.execute(buf.toString());
    }

    return true;
  }

  public boolean addTableAttributes(final Table table, final TableAttribute... attributes) {
    return addTableAttributes(table.getName(), attributes);
  }

  public void buildAddUniqueConstraintStatement(final StringBuffer buf, final String table, final String constraintName,
                                                final String... attributes) {
    buf.append("ALTER TABLE ").append(table).append(" ADD CONSTRAINT ").append(constraintName).append(" UNIQUE (");
    buf.append(StringHelper.listToString(", ", attributes));
    buf.append(");\n");
  }

  public boolean renameTableAttribute(final String table, final String oldName, final String newName) {
    final String alterStatement = getDatabaseSupport().renameAttribute(table, oldName, newName);
    execute(alterStatement);
    return true;
  }

  public boolean addUniqueConstraint(final String table, final String constraintName, final String... attributes) {
    accessCheck(true);
    final StringBuffer buf = new StringBuffer();
    buildAddUniqueConstraintStatement(buf, table, constraintName, attributes);
    execute(buf.toString());
    return true;
  }

  public boolean dropAndRecreateAllUniqueConstraints(final Class<?> entity) {
    accessCheck(true);
    final Table table = new Table(entity).autoAddAttributes();
    final String[] uniqueConstraintNames = getAllUniqueConstraintNames(table.getName());
    if (uniqueConstraintNames != null) {
      for (final String uniqueConstraintName : uniqueConstraintNames) {
        execute("ALTER TABLE " + table.getName() + " DROP CONSTRAINT " + uniqueConstraintName);
      }
    } else {
      log.info("No unique constraints found for table '" + table.getName() + "'.");
    }
    final UniqueConstraint[] uniqueConstraints = table.getUniqueConstraints();
    final List<String> existingConstraintNames = new LinkedList<>();
    if (uniqueConstraints != null && uniqueConstraints.length > 0) {
      for (final UniqueConstraint uniqueConstraint : uniqueConstraints) {
        final String[] columnNames = uniqueConstraint.columnNames();
        if (columnNames.length > 0) {
          final String constraintName = createUniqueConstraintName(table.getName(), columnNames,
                  existingConstraintNames.toArray(new String[0]));
          addUniqueConstraint(table.getName(), constraintName, columnNames);
          existingConstraintNames.add(constraintName);
        }
      }
    }
    for (final TableAttribute attr : table.getAttributes()) {
      if (!attr.isUnique()) {
        continue;
      }
      final String[] columnNames = new String[1];
      columnNames[0] = attr.getName();
      final String constraintName = createUniqueConstraintName(table.getName(), columnNames,
              existingConstraintNames.toArray(new String[0]));
      addUniqueConstraint(table.getName(), constraintName, columnNames);
      existingConstraintNames.add(constraintName);
    }
    return true;
  }

  /**
   * Max length is 30 (may-be for Oracle compatibility).
   *
   * @param table
   * @param columnNames
   * @param existingConstraintNames
   * @return The generated constraint name different to the given names.
   */
  public String createUniqueConstraintName(final String table, final String[] columnNames,
                                           final String[] existingConstraintNames) {
    final StringBuilder sb = new StringBuilder();
    sb.append(StringUtils.left(table, 15)).append("_uq_").append(StringUtils.left(columnNames[0], 8));
    final String prefix = sb.toString().toLowerCase();
    for (int i = 1; i < 1000; i++) {
      final String name = prefix + i;
      if (existingConstraintNames == null || existingConstraintNames.length == 0) {
        return name;
      }
      boolean exists = false;
      for (final String existingName : existingConstraintNames) {
        if (existingName != null && existingName.equals(name)) {
          exists = true;
          break;
        }
      }
      if (!exists) {
        return name;
      }
    }
    final String message = "Oups, can't find any free constraint name! This must be a bug or a database out of control! Trying to find a name '"
            + prefix
            + "[0-999]' for table '"
            + table
            + "'.";
    log.error(message);
    throw new UnsupportedOperationException(message);
  }

  public String[] getAllUniqueConstraintNames(final String table) {
    final String uniqueConstraintNamesSql = getDatabaseSupport().getQueryForAllUniqueConstraintNames();
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    final List<DatabaseResultRow> result = jdbc.query(uniqueConstraintNamesSql, table.toLowerCase());
    if (result == null || result.size() == 0) {
      return null;
    }
    final String[] names = new String[result.size()];
    int i = 0;
    for (final DatabaseResultRow row : result) {
      final List<DatabaseResultRowEntry> entries = row.getEntries();
      final int numberOfEntries = entries != null ? entries.size() : 0;
      if (numberOfEntries != 1) {
        log.error("Error while getting unique constraint name for table '"
                + table
                + "'. Each result entry of the query should be one but is: "
                + numberOfEntries);
      }
      if (numberOfEntries > 0) {
        final DatabaseResultRowEntry entry = entries.get(0);
        names[i++] = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
      } else {
        names[i++] = null;
      }
    }
    return names;
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

  public void insertInto(final String table, final String[] columns, final Object[] values) {
    final StringBuffer buf = new StringBuffer();
    buf.append("insert into ").append(table).append(" (").append(StringHelper.listToString(",", columns))
            .append(") values (");
    boolean first = true;
    for (Object value : values) {
      first = StringHelper.append(buf, first, "?", ",");
    }
    buf.append(")");
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    final String sql = buf.toString();
    log.info(sql + "; values = " + StringHelper.listToString(", ", values));
    jdbc.update(sql, values);
  }

  /**
   * @param regionId
   * @param version
   * @return true, if any entry for the given regionId and version is found in the database table t_database_update.
   */
  public boolean isVersionUpdated(final String regionId, final String version) {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    final int result = jdbc.queryForInt("select count(*) from t_database_update where region_id=? and version=?",
            regionId, version);
    return result > 0;
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
   * @param name
   * @return true, if the index was dropped, false if an error has occured or the index does not exist.
   */
  public boolean dropIndex(final String name) {
    accessCheck(true);
    try {
      execute("DROP INDEX " + name);
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

  public int update(final String sql, final Object... args) {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    log.info(sql);
    return jdbc.update(sql, args);
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

  public int getDatabaseTableColumnLenght(final Class<?> entityClass, final String attributeNames) {
    String jdbcQuery = "select character_maximum_length from information_schema.columns where LOWER(table_name) = '"
            + new Table(entityClass).getName().toLowerCase() + "' And LOWER(column_name) = '" + attributeNames.toLowerCase() + "'";
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    log.info(jdbcQuery);
    return jdbc.queryForInt(jdbcQuery);
  }

  public static PFUserDO __internalGetSystemAdminPseudoUser() {
    return SYSTEM_ADMIN_PSEUDO_USER;
  }

  public SystemUpdater getSystemUpdater() {
    if (systemUpdater == null) {
      systemUpdater = new SystemUpdater();
    }
    return systemUpdater;
  }

  /**
   *
   */
  public List<DatabaseUpdateDO> getUpdateHistory() {
    accessCheck(false);
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    final List<Map<String, Object>> dbResult = jdbc
            .queryForList("SELECT * FROM t_database_update ORDER BY update_date DESC");
    final List<DatabaseUpdateDO> result = new ArrayList<>();
    for (final Map<String, Object> map : dbResult) {
      final DatabaseUpdateDO entry = new DatabaseUpdateDO();
      entry.setUpdateDate((Date) map.get("update_date"));
      entry.setRegionId((String) map.get("region_id"));
      entry.setVersionString((String) map.get("version"));
      entry.setExecutionResult((String) map.get("execution_result"));
      final PFUserDO executedByUser = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache()
              .getUser((Integer) map.get("executed_by_user_fk"));
      entry.setExecutedBy(executedByUser);
      entry.setDescription((String) map.get("description"));
      result.add(entry);
    }
    return result;
  }

  public boolean databaseTablesExists() {
    try {
      final Table userTable = new Table(PFUserDO.class);
      return !internalDoesTableExist(userTable.getName());
    } catch (final Exception ex) {
      log.error("Error while checking existing of user table.", ex);
    }
    return false;
  }

  public boolean databaseTablesWithEntriesExists() {
    try {
      final Table userTable = new Table(PFUserDO.class);
      return internalDoesTableExist(userTable.getName()) && !internalIsTableEmpty(userTable.getName());
    } catch (final Exception ex) {
      log.error("Error while checking existing of user table with entries.", ex);
    }
    return false;
  }

  public boolean doesGroupExists(ProjectForgeGroup group) {
    return em.createQuery( "SELECT count(*) FROM GroupDO g WHERE g.name = :name", Integer.class)
            .setParameter("name",group.getName())
            .getSingleResult() > 0;
  }

  public boolean doesTableRowExists(final Class<?> entity, final String columnName, final String cellValue, final boolean useQuotationMarks) {
    return doesTableRowExists(new Table(entity).getName(), columnName, cellValue, useQuotationMarks);
  }

  public boolean doesTableRowExists(String tablename, String columnname, String columnvalue, boolean useQuotationMarks) {
    String quotationMark = useQuotationMarks ? "'" : "";
    String sql = "SELECT * FROM " + tablename + " WHERE " + columnname + " = " + quotationMark + columnvalue
            + quotationMark;
    try {
      List<DatabaseResultRow> query = query(sql);
      if (query != null && query.size() > 0) {
        return true;
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }

  public int countTimeableAttrGroupEntries(final Class<? extends TimeableAttrRow<?>> entityClass, final String groupName) {
    accessCheck(false);
    final Table table = new Table(entityClass);
    final TableAttribute attr = TableAttribute.createTableAttribute(table.getEntityClass(), "groupName");

    final DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      return jdbc.queryForInt("SELECT COUNT(*) FROM " + table.getName() + " WHERE " + attr.getName() + "=?", groupName);
    } catch (final Exception ex) {
      return -1;
    }
  }

  public int replaceTableCellStrings(final Class<?> entity, final String columnName, final String oldCellValue, final String newCellValue) {
    final String tableName = new Table(entity).getName();
    return update("UPDATE " + tableName + " SET " + columnName + " = ? WHERE status = ?", newCellValue, oldCellValue);
  }

  public boolean doesUniqueConstraintExists(final String tableName, final String uniqueConstraintName) {
    final String[] allUniqueConstraintNames = getAllUniqueConstraintNames(tableName);
    return (allUniqueConstraintNames != null) &&
            Arrays.asList(allUniqueConstraintNames).contains(uniqueConstraintName);
  }

  public boolean doesUniqueConstraintExists(final String tableName, final String... fields) {
    return this.getUniqueConstraintName(tableName, fields) != null;
  }

  public String getUniqueConstraintName(final String tableName, final String... fields) {
    String queryString = String.format("select tc.constraint_name, cc.Column_Name from INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc "
            + "inner join information_schema.constraint_column_usage cc on tc.Constraint_Name = cc.Constraint_Name "
            + "where tc.CONSTRAINT_TYPE='UNIQUE' and LOWER(tc.table_name)='%s'", tableName.toLowerCase());

    List<DatabaseResultRow> resultSet = this.query(queryString);

    if (resultSet == null || resultSet.isEmpty()) {
      return null;
    }

    Map<String, List<String>> constraints = new HashMap<>();
    for (DatabaseResultRow row : resultSet) {
      String constraint = row.getEntry(0) != null && row.getEntry(0).getValue() != null ? row.getEntry(0).getValue().toString() : null;
      String column = row.getEntry(1) != null && row.getEntry(1).getValue() != null ? row.getEntry(1).getValue().toString() : null;

      if (constraint == null || column == null) {
        continue;
      }

      if (!constraints.containsKey(constraint)) {
        constraints.put(constraint, new ArrayList<>());
      }
      constraints.get(constraint).add(column.toLowerCase());
    }

    for (final String constraint : constraints.keySet()) {
      final List columns = constraints.get(constraint);
      boolean ok = true;

      if (columns.size() != fields.length) {
        continue;
      }

      for (final String field : fields) {
        if (columns.contains(field.toLowerCase())) {
          continue;
        }

        ok = false;
        break;
      }

      if (ok) {
        return constraint;
      }

    }

    return null;
  }

  public Optional<Boolean> isColumnNullable(final String tableName, final String columnName) {
    try (final Connection connection = dataSource.getConnection()) {

      final ResultSet columns;

      if (DatabaseSupport.getInstance().getDialect() == DatabaseDialect.HSQL) {
        columns = connection.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase());
      } else { // postgres needs lower case
        columns = connection.getMetaData().getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase());
      }
      Validate.isTrue(columns.next());

      // for columnIndex see https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getColumns-java.lang.String-java.lang.String-java.lang.String-java.lang.String-
      Validate.isTrue(columns.getString(4).equalsIgnoreCase(columnName));
      final boolean isNullable = columns.getInt(11) == ResultSetMetaData.columnNullable;
      return Optional.of(isNullable);
    } catch (SQLException e) {
      log.error(e.toString());
      return Optional.empty();
    }
  }

  public int dropForeignKeys() {
    final DatabaseExecutor jdbc = this.getDatabaseExecutor();
    int countDroppedKeys = 0;
    int countCurrent = 0;
    String tableNameLast = null;

    final List<DatabaseResultRow> queryResult = jdbc
            .query("SELECT TABLE_NAME, CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_TYPE='FOREIGN KEY' ORDER BY TABLE_NAME;");
    for (DatabaseResultRow row : queryResult) {
      final String tableName = (String) row.getEntry(0).getValue();
      final String foreignKey = (String) row.getEntry(1).getValue();

      if (!tableName.equals(tableNameLast)) {
        log.info(String.format("Dropped '%s' foreign keys for table '%s'", countCurrent, tableNameLast));
        log.info(String.format("Check foreign key constraints of table '%s'", tableName));

        tableNameLast = tableName;
        countCurrent = 0;
      }

      jdbc.execute(String.format("ALTER TABLE %s DROP CONSTRAINT %s;", tableName, foreignKey), true);

      ++countDroppedKeys;
      ++countCurrent;
    }

    log.info(String.format("Dropped '%s' foreign keys for table '%s'", tableNameLast, countCurrent));

    return countDroppedKeys;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
