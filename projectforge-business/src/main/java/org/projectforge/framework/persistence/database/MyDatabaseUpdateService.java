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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.projectforge.business.login.Login;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.continuousdb.DatabaseUpdateService;
import org.projectforge.continuousdb.SystemUpdater;
import org.projectforge.continuousdb.Table;
import org.projectforge.continuousdb.TableAttribute;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdaterConfiguration;
import org.projectforge.continuousdb.hibernate.TableAttributeHookImpl;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessCheckerImpl;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * For manipulating the database (patching data etc.)
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@Service
public class MyDatabaseUpdateService extends DatabaseUpdateService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MyDatabaseUpdateService.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private PluginAdminService pluginAdminService;

  private UpdaterConfiguration configuration;

  @Autowired
  private DataSource dataSource;

  private static PFUserDO SYSTEM_ADMIN_PSEUDO_USER = new PFUserDO()
      .setUsername("System admin user only for internal usage");

  public static PFUserDO __internalGetSystemAdminPseudoUser()
  {
    return SYSTEM_ADMIN_PSEUDO_USER;
  }

  @PostConstruct
  public void initialize()
  {
    if (configuration != null) {
      return;
    }
    configuration = new UpdaterConfiguration();
    configuration.setDialect(HibernateUtils.getDialect()).setDataSource(dataSource);
    this.setConfiguration(configuration);
    configuration.setDatabaseUpdateDao(this);
    TableAttribute.register(new TableAttributeHookImpl());

    final SortedSet<UpdateEntry> updateEntries = new TreeSet<UpdateEntry>();
    DatabaseCoreUpdates.applicationContext = applicationContext;
    updateEntries.addAll(DatabaseCoreUpdates.getUpdateEntries());
    getSystemUpdater().setUpdateEntries(updateEntries);
  }

  public SystemUpdater getSystemUpdater()
  {
    initialize();
    return configuration.getSystemUpdater();
  }

  public MyDatabaseUpdateService getDatabaseUpdateService()
  {
    initialize();
    return (MyDatabaseUpdateService) configuration.getDatabaseUpdateDao();
  }

  @Override
  protected void accessCheck(final boolean writeaccess)
  {
    if (ThreadLocalUserContext.getUser() == SYSTEM_ADMIN_PSEUDO_USER) {
      // No access check for the system admin pseudo user.
      return;
    }
    if (Login.getInstance().isAdminUser(ThreadLocalUserContext.getUser()) == false) {
      throw new AccessException(AccessCheckerImpl.I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF,
          ProjectForgeGroup.ADMIN_GROUP.getKey());
    }
    accessChecker.checkRestrictedOrDemoUser();
  }

  /**
   */
  public List<DatabaseUpdateDO> getUpdateHistory()
  {
    accessCheck(false);
    final JdbcTemplate jdbc = new JdbcTemplate(getDataSource());
    final List<Map<String, Object>> dbResult = jdbc
        .queryForList("select * from t_database_update order by update_date desc");
    final List<DatabaseUpdateDO> result = new ArrayList<DatabaseUpdateDO>();
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

  /**
   * @see org.projectforge.continuousdb.DatabaseUpdateService#createMissingIndices()
   */
  @Override
  public int createMissingIndices()
  {
    // TODO FB (RK) spring is not initialized.
    if (true) {
      return 0;
    }
    int result = super.createMissingIndices();
    if (createIndex("idx_timesheet_user_time", "t_timesheet", "user_id, start_time") == true) {
      ++result;
    }
    for (final AbstractPlugin plugin : pluginAdminService.getActivePlugin()) {
      if (plugin.isInitialized() == false) {
        // Plug-in not (yet) initialized, skip. this is normal on first start-up phase.
        continue;
      }
      final UpdateEntry updateEntry = plugin.getInitializationUpdateEntry();
      if (updateEntry != null) {
        result += updateEntry.createMissingIndices();
      }
    }
    return result;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public boolean databaseTablesExists()
  {
    try {
      final Table userTable = new Table(PFUserDO.class);
      return internalDoesTableExist(userTable.getName()) == false;
    } catch (final Exception ex) {
      log.error("Error while checking existing of user table.", ex);
    }
    return false;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public boolean databaseTablesWithEntriesExists()
  {
    try {
      final Table userTable = new Table(PFUserDO.class);
      return internalDoesTableExist(userTable.getName()) == false
          || internalIsTableEmpty(userTable.getName()) == true;
    } catch (final Exception ex) {
      log.error("Error while checking existing of user table with entries.", ex);
    }
    return false;
  }

}
