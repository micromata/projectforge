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

package org.projectforge.framework.configuration;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang.Validate;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration values persistet in the data base. Please access the configuration parameters via
 * {@link AbstractConfiguration}.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class ConfigurationDao extends BaseDao<ConfigurationDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ConfigurationDao.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private UserDao userDao;

  @Autowired
  private PfEmgrFactory emf;

  /**
   * Force reload of the Configuration cache.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(ExtendedBaseDO)
   * @see AbstractConfiguration#setExpired()
   */
  @Override
  protected void afterSaveOrModify(final ConfigurationDO obj)
  {
    if (obj.getConfigurationType() == ConfigurationParam.MULTI_TENANCY_ENABLED.getType()
        && obj.getBooleanValue() == true) {
      // Enable current logged in user as super admin user.
      final Integer adminUserId = ThreadLocalUserContext.getUserId();
      final PFUserDO adminUser = userDao.getById(adminUserId);
      log.info("Enabling current user as super admin (for administer tenants) because he has enabled multi-tenancy: "
          + adminUser.getDisplayUsername());
      adminUser.setSuperAdmin(true);
      userDao.update(adminUser);
    }
    if (obj.getGlobal() == true) {
      GlobalConfiguration.getInstance().setExpired();
    } else {
      final TenantDO tenant = obj.getTenant();
      final TenantRegistry tenantRegistry = TenantRegistryMap.getInstance().getTenantRegistry(tenant);
      tenantRegistry.getConfiguration().setExpired();
    }
  }

  /**
   * Checks and creates missing data base entries. Updates also out-dated descriptions.
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void checkAndUpdateDatabaseEntries()
  {
    final List<ConfigurationDO> list = internalLoadAll();
    final Set<String> params = new HashSet<String>();
    for (final ConfigurationParam param : ConfigurationParam.values()) {
      checkAndUpdateDatabaseEntry(param, list, params);
    }
    for (final ConfigurationDO entry : list) {
      if (params.contains(entry.getParameter()) == false) {
        log.error("Unknown configuration entry. Mark as deleted: " + entry.getParameter());
        internalMarkAsDeleted(entry);
      }
    }
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public ConfigurationDO getEntry(final IConfigurationParam param)
  {
    Validate.notNull(param);
    @SuppressWarnings("unchecked")
    final List<ConfigurationDO> list = (List<ConfigurationDO>) getHibernateTemplate()
        .find("from ConfigurationDO c where c.parameter = ?", param.getKey());
    if (list == null || list.isEmpty() == true || list.get(0) == null) {
      return null;
    }
    return list.get(0);
  }

  public Object getValue(final IConfigurationParam parameter)
  {
    return getValue(parameter, getEntry(parameter));
  }

  public Object getValue(final IConfigurationParam parameter, final ConfigurationDO configurationDO)
  {
    if (parameter.getType().isIn(ConfigurationType.STRING, ConfigurationType.TEXT) == true) {
      if (configurationDO == null) {
        return parameter.getDefaultStringValue();
      }
      final String result = configurationDO.getStringValue();
      if (result != null) {
        return result;
      } else {
        return parameter.getDefaultStringValue();
      }
    } else if (parameter.getType().isIn(ConfigurationType.FLOAT, ConfigurationType.PERCENT) == true) {
      if (configurationDO == null) {
        return BigDecimal.ZERO;
      }
      return configurationDO.getFloatValue();
    } else if (parameter.getType() == ConfigurationType.INTEGER) {
      if (configurationDO == null) {
        return 0;
      }
      return configurationDO.getIntValue();
    } else if (parameter.getType() == ConfigurationType.BOOLEAN) {
      if (configurationDO == null) {
        return null;
      }
      return configurationDO.getBooleanValue();
    } else if (parameter.getType() == ConfigurationType.TASK) {
      if (configurationDO == null) {
        return null;
      }
      final Integer taskId = configurationDO.getTaskId();
      return taskId;
    } else if (parameter.getType() == ConfigurationType.CALENDAR) {
      if (configurationDO == null) {
        return null;
      }
      final Integer calendarId = configurationDO.getCalendarId();
      return calendarId;
    } else if (parameter.getType() == ConfigurationType.TIME_ZONE) {
      String timezoneId = configurationDO != null ? configurationDO.getTimeZoneId() : null;
      if (timezoneId == null) {
        timezoneId = parameter.getDefaultStringValue();
      }
      if (timezoneId != null) {
        return TimeZone.getTimeZone(timezoneId);
      }
      return null;
    }
    throw new UnsupportedOperationException("Type unsupported: " + parameter.getType());
  }

  public ConfigurationDao()
  {
    super(ConfigurationDO.class);
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final ConfigurationDO obj, final ConfigurationDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  @Override
  public ConfigurationDO newInstance()
  {
    throw new UnsupportedOperationException();
  }

  private void checkAndUpdateDatabaseEntry(final IConfigurationParam param, final List<ConfigurationDO> list,
      final Set<String> params)
  {
    params.add(param.getKey());
    for (final ConfigurationDO configuration : list) {
      if (param.getKey().equals(configuration.getParameter()) == true) {
        boolean modified = false;
        if (configuration.getConfigurationType() != param.getType()) {
          log.info("Updating configuration type of configuration entry: " + param);
          configuration.internalSetConfigurationType(param.getType());
          modified = true;
        }
        if (configuration.getGlobal() != param.isGlobal()) {
          log.info("Updating configuration flag 'global' of configuration entry: " + param);
          configuration.setGlobal(param.isGlobal());
          modified = true;
        }
        if (configuration.isDeleted() == true) {
          log.info("Restore deleted configuration entry: " + param);
          configuration.setDeleted(false);
          modified = true;
        }
        if (modified == true) {
          internalUpdate(configuration);
        }
        return;
      }
    }
    // Entry does not exist: Create entry:
    log.info("Entry does not exist. Creating parameter '" + param.getKey() + "'.");
    final ConfigurationDO configuration = new ConfigurationDO();
    configuration.setParameter(param.getKey());
    configuration.setConfigurationType(param.getType());
    configuration.setGlobal(param.isGlobal());
    if (param.getType().isIn(ConfigurationType.STRING, ConfigurationType.TEXT) == true) {
      configuration.setValue(param.getDefaultStringValue());
    }
    internalSave(configuration);
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<ConfigurationDO> internalLoadAll()
  {
    TenantDao tenantDao = applicationContext.getBean(TenantDao.class);
    if (tenantDao.tenantTableExists()) {
      return super.internalLoadAll();
    } else {
      return emf.runInTrans((emgr) -> {
        return emgr.select(ConfigurationDO.class,
            "SELECT new org.projectforge.framework.configuration.entities.ConfigurationDO(c.id, c.created, c.deleted, c.lastUpdate, c.configurationType, c.floatValue, c.intValue, c.parameter, c.stringValue) FROM ConfigurationDO c");
      });
    }
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<ConfigurationDO> internalLoadAll(TenantDO tenant)
  {
    TenantDao tenantDao = applicationContext.getBean(TenantDao.class);
    if (tenantDao.tenantTableExists()) {
      return super.internalLoadAll(tenant);
    } else {
      return internalLoadAll();
    }
  }

}
