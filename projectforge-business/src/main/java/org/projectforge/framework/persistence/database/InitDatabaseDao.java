/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.persistence.Persistence;
import javax.sql.DataSource;

import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.history.HibernateSearchReindexer;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * For initialization of a new ProjectForge system.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Repository
@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
public class InitDatabaseDao
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(InitDatabaseDao.class);

  public static final String DEFAULT_ADMIN_USER = "admin";

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private HibernateSearchReindexer hibernateSearchReindexer;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TenantService tenantService;

  @Value("${hibernate.search.default.indexBase}")
  private String hibernateIndexDir;

  @Autowired
  private DataSource dataSource;

  /**
   * If the database is empty (user list is empty) then a admin user and ProjectForge root task will be created.
   * 
   * @param adminUser The admin user with the desired username and the salted password (salt string included). All other
   *          attributes and groups of the user are set by this method.
   */
  public void initializeDefaultData(final PFUserDO adminUser, final TimeZone adminUserTimezone)
  {
    log.info("Init admin user and root task.");
    DatabaseUpdateService ser = applicationContext.getBean(DatabaseUpdateService.class);
    if (ser.databaseTablesWithEntriesExists() == false) {
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
    final Serializable id = hibernateTemplate.save(task);
    log.info("New object added (" + id + "): " + task.toString());
    // Use of taskDao does not work with maven test case: Could not synchronize database state with session?

    // Create Admin user
    adminUser.setLocalUser(true);
    adminUser.setLastname("Administrator");
    adminUser.setDescription("ProjectForge administrator");
    adminUser.setTimeZone(adminUserTimezone);
    adminUser.setTenant(defaultTenant);
    adminUser.setSuperAdmin(true);
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
    userDao.internalSave(adminUser);

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

  public TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  public UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }

  public TenantDO insertDefaultTenant()
  {
    log.info("Adding default tenant.");
    String insertDefaultTenant = "INSERT INTO t_tenant VALUES (1,'2016-03-17 14:00:00',FALSE,'2016-03-17 14:00:00',TRUE,'Default tenant','Default tenant','defaultTenant',1)";
    jdbcTemplate.execute(insertDefaultTenant);
    log.info("Adding default tenant finished.");
    tenantService.resetTenantTableStatus();
    return tenantService.getDefaultTenant();
  }

  /**
   * Only for internal usage by {@link TenantDao} or {@link InitDatabaseDao}!
   * 
   * @param tenant
   * @param adminUser
   */
  public void internalCreateProjectForgeGroups(final TenantDO tenant, final PFUserDO adminUser)
  {
    final Set<PFUserDO> adminUsers = new HashSet<PFUserDO>();
    adminUsers.add(adminUser);
    Set<PFUserDO> adminUsersForNewTenants = null;
    if (tenant.isDefault() == true) {
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
    addGroup(ProjectForgeGroup.PROJECT_MANAGER,
        "Project managers have access to assigned orders and resource planning.", tenant, null);
    addGroup(ProjectForgeGroup.PROJECT_ASSISTANT, "Project assistants have access to assigned orders.", tenant, null);
  }

  private void addGroup(final ProjectForgeGroup projectForgeGroup, final String description, final TenantDO tenant,
      final Set<PFUserDO> users)
  {
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
   * @param adminUser The admin user with the desired username and the salted password (salt string included).
   * @param adminUserTimezone
   */
  public PFUserDO updateAdminUser(PFUserDO user, final TimeZone adminUserTimezone)
  {
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

  public void afterCreatedTestDb(boolean blocking)
  {
    Thread rebuildThread = new Thread()
    {
      @Override
      public void run()
      {
        hibernateSearchReindexer.rebuildDatabaseSearchIndices();
      }
    };
    rebuildThread.start();
    if (blocking == true) {
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

  private void databaseNotEmpty()
  {
    final String msg = "Database seems to be not empty. Initialization of database aborted.";
    log.error(msg);
    throw new AccessException(msg);
  }

  public void updateSchema()
  {
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

}
