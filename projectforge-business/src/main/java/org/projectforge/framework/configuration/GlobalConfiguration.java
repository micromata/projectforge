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

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.xstream.XmlObject;

/**
 * This class also provides the configuration of the parameters which are stored via ConfigurationDao. Those parameters
 * are cached. <br/>
 * These parameters are global (valid for all tenants).
 * 
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "config")
public class GlobalConfiguration extends AbstractConfiguration
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GlobalConfiguration.class);

  private static GlobalConfiguration instance;

  private Boolean multitenancyMode;

  public static GlobalConfiguration getInstance()
  {
    if (instance == null) {
      throw new IllegalStateException("GlobalConfiguration is not yet configured");
    }
    return instance;
  }

  public static boolean isInitialized()
  {
    return instance != null;
  }

  static public void createConfiguration(final ConfigurationService configurationService)
  {
    if (instance != null) {
      log.warn("GlobalConfiguration is already instantiated.");
      return;
    }
    instance = new GlobalConfiguration();
    instance.configurationService = configurationService;
  }

  private GlobalConfiguration()
  {
    super(true);
  }

  /**
   * @see org.projectforge.framework.cache.AbstractCache#setExpired()
   */
  @Override
  public void setExpired()
  {
    super.setExpired();
    this.multitenancyMode = null;
  }

  public boolean isMultiTenancyConfigured()
  {
    if (multitenancyMode == null) {
      multitenancyMode = getBooleanValue(ConfigurationParam.MULTI_TENANCY_ENABLED);
    }
    return multitenancyMode;
  }

  /**
   * @see org.projectforge.framework.configuration.AbstractConfiguration#getIdentifier4LogMessage()
   */
  @Override
  protected String getIdentifier4LogMessage()
  {
    return "GlobalConfiguration";
  }

  @Override
  protected List<ConfigurationDO> loadParameters()
  {
    return configurationService.daoInternalLoadAll();
  }
}
