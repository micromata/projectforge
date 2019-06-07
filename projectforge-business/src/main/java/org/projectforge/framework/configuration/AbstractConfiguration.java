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

package org.projectforge.framework.configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.xstream.XmlObject;

/**
 * This class also provides the configuration of the parameters which are stored via ConfigurationDao. Those parameters
 * are cached. <br/>
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * <p>
 * <p>
 * TODO DESIGNBUG derived
 */
@XmlObject(alias = "config")
public abstract class AbstractConfiguration extends AbstractCache
{
  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(AbstractConfiguration.class);

  protected ConfigurationService configurationService;

  protected Map<ConfigurationParam, Object> configurationParamMap;

  private final boolean global;

  public void putParameterManual(final ConfigurationParam param, final Object value)
  {
    checkRefresh();
    this.configurationParamMap.put(param, value);
  }

  public AbstractConfiguration(final boolean global)
  {
    super(TICKS_PER_HOUR);
    this.global = global;
  }

  /**
   * @return The string value of the given parameter stored as ConfigurationDO in the data base.
   * @throws ClassCastException if configuration parameter is from the wrong type.
   */
  public String getStringValue(final IConfigurationParam parameter)
  {
    return (String) getValue(parameter);
  }

  /**
   * @return The boolean value of the given parameter stored as ConfigurationDO in the data base.
   * @throws ClassCastException if configuration parameter is from the wrong type.
   */
  public boolean getBooleanValue(final IConfigurationParam parameter)
  {
    final Object obj = getValue(parameter);
    if (obj != null && Boolean.TRUE.equals(obj)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * @return The BigDecimal value of the given parameter stored as ConfigurationDO in the data base.
   * @throws ClassCastException if configuration parameter is from the wrong type.
   */
  public BigDecimal getPercentValue(final IConfigurationParam parameter)
  {
    return (BigDecimal) getValue(parameter);
  }

  /**
   * @return The Integer value of the given parameter stored as ConfigurationDO in the data base.
   * @throws ClassCastException if configuration parameter is from the wrong type.
   */
  public Integer getTaskIdValue(final IConfigurationParam parameter)
  {
    final TaskDO task = (TaskDO) getValue(parameter);
    if (task != null) {
      return task.getId();
    }
    return null;
  }

  public boolean isCostConfigured()
  {
    return getBooleanValue(ConfigurationParam.COST_CONFIGURED);
  }

  protected Object getValue(final IConfigurationParam parameter)
  {
    checkRefresh();
    return this.configurationParamMap.get(parameter);
  }

  protected abstract List<ConfigurationDO> loadParameters();

  protected abstract String getIdentifier4LogMessage();

  @Override
  protected void refresh()
  {
    final String identifier = getIdentifier4LogMessage();
    final Map<ConfigurationParam, Object> newMap = new HashMap<ConfigurationParam, Object>();
    log.info("Initializing " + identifier + " (ConfigurationDO parameters) ...");
    if (configurationService == null) {
      // Do nothing.
      log.info("Do nothing (configuration dao not available)...");
      return;
    }
    List<ConfigurationDO> list;
    try {
      list = loadParameters();
    } catch (final Exception ex) {
      log.error(
          "******* Exception while getting configuration parameters from data-base (only OK for migration from older versions): "
              + ex.getMessage(),
          ex);
      list = new ArrayList<ConfigurationDO>();
    }
    for (final ConfigurationParam param : ConfigurationParam.values()) {
      if (param.isGlobal() != global) {
        continue;
      }
      ConfigurationDO configuration = null;
      for (final ConfigurationDO entry : list) {
        if (StringUtils.equals(param.getKey(), entry.getParameter()) == true) {
          configuration = entry;
          break;
        }
      }
      newMap.put(param, configurationService.getDaoValue(param, configuration));
    }
    if (this.configurationParamMap == null) {
      for (final Map.Entry<ConfigurationParam, Object> entry : newMap.entrySet()) {
        final Object value = entry.getValue();
        if (value == null) {
          continue;
        }
        log.info(identifier + ": " + entry.getKey().getKey() + "=" + value);
      }
    }
    this.configurationParamMap = newMap;
  }
}
