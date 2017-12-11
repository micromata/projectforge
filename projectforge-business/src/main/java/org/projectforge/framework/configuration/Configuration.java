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

import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.xstream.XmlObject;

/**
 * This class also provides the configuration of the parameters which are stored via ConfigurationDao. Those parameters
 * are cached. <br/>
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "config")
public class Configuration extends AbstractConfiguration
{
  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(Configuration.class);

  private final TenantDO tenant;

  /**
   * @return The instance of the current tenant or if no tenant does exist the default instance.
   */
  public static Configuration getInstance()
  {
    final TenantRegistryMap tennatRegistryMap = TenantRegistryMap.getInstance();
    final TenantRegistry tenantRegistry = tennatRegistryMap.getTenantRegistry();
    Validate.notNull(tenantRegistry);
    return tenantRegistry.getConfiguration();
  }

  public Configuration(ConfigurationService configurationService, final TenantDO tenant)
  {
    super(false);
    this.tenant = tenant;
    this.configurationService = configurationService;
  }

  public boolean isMebConfigured()
  {
    if (StringUtils.isNotEmpty(getStringValue(ConfigurationParam.MEB_SMS_RECEIVING_PHONE_NUMBER)) == true
        || configurationService.isMebMailAccountConfigured() == true) {
      return true;
    }
    return false;
  }

  public String getCalendarDomain()
  {
    final String calendarDomain = (String) getValue(ConfigurationParam.CALENDAR_DOMAIN);
    return calendarDomain;
  }

  public boolean isCalendarDomainValid()
  {
    final String calendarDomain = getCalendarDomain();
    return isDomainValid(calendarDomain);
  }

  /**
   * Validates the domain.
   */
  public static boolean isDomainValid(final String domain)
  {
    return StringUtils.isNotBlank(domain) == true
        && domain.matches("^[a-zA-Z]+[a-zA-Z0-9\\.\\-]*[a-zA-Z0-9]+$") == true;
  }

  /**
   * Return the configured time zone. If not found the default time zone of the system is returned.
   * 
   * @param parameter
   * @see ConfigurationDao#getTimeZoneValue(ConfigurationParam)
   */
  public TimeZone getDefaultTimeZone()
  {
    final TimeZone timeZone = (TimeZone) getValue(ConfigurationParam.DEFAULT_TIMEZONE);
    if (timeZone != null) {
      return timeZone;
    } else {
      return TimeZone.getDefault();
    }
  }

  /**
   * Available date formats (configurable as parameter, see web dialogue with system parameters).
   * 
   * @return
   */
  public String[] getDateFormats()
  {
    final String str = getStringValue(ConfigurationParam.DATE_FORMATS);
    final String[] sa = StringUtils.split(str, " \t\r\n,;");
    return sa;
  }

  /**
   * @return The first entry of {@link #getDateFormats()} if exists, otherwise yyyy-MM-dd (ISO date format).
   */
  public String getDefaultDateFormat()
  {
    final String[] sa = getDateFormats();
    if (sa != null && sa.length > 0) {
      return sa[0];
    } else {
      return DateFormats.ISO_DATE;
    }
  }

  /**
   * Available excel date formats (configurable as parameter, see web dialogue with system parameters).
   * 
   * @return
   */
  public String[] getExcelDateFormats()
  {
    final String str = getStringValue(ConfigurationParam.EXCEL_DATE_FORMATS);
    final String[] sa = StringUtils.split(str, " \t\r\n,;");
    return sa;
  }

  /**
   * @return The first entry of {@link #getExcelDateFormats()} if exists, otherwise YYYY-MM-DD (ISO date format).
   */
  public String getDefaultExcelDateFormat()
  {
    final String[] sa = getExcelDateFormats();
    if (sa != null && sa.length > 0) {
      return sa[0];
    } else {
      return DateFormats.EXCEL_ISO_DATE;
    }
  }

  /**
   * @see org.projectforge.framework.configuration.AbstractConfiguration#getValue(org.projectforge.framework.configuration.ConfigurationParam)
   */
  @Override
  protected Object getValue(final IConfigurationParam parameter)
  {
    final Object obj = super.getValue(parameter);
    if (parameter.getType() == ConfigurationType.TASK) {
      final TaskTree taskTree = TaskTreeHelper.getTaskTree();
      final Integer taskId = (Integer) obj;
      if (taskId != null) {
        return taskTree.getTaskById(taskId);
      } else {
        return null;
      }
    }
    return obj;
  }

  /**
   * @see org.projectforge.framework.configuration.AbstractConfiguration#getIdentifier4LogMessage()
   */
  @Override
  protected String getIdentifier4LogMessage()
  {
    return "Configuration[tenant=" + (tenant != null ? tenant.getId() : null) + "]";
  }

  @Override
  protected List<ConfigurationDO> loadParameters()
  {
    return configurationService.daoInternalLoadAll(tenant);
  }

}
