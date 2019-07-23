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

package org.projectforge.business.configuration;

import java.util.Calendar;
import java.util.Locale;

/**
 * For accessing ConfigurationService without Spring context.
 */
public class ConfigurationServiceAccessor {
  private static ConfigurationService configurationService;

  public static void internalInitJunitTestMode() {
    ConfigurationServiceImpl cfg = new ConfigurationServiceImpl();
    cfg.setDefaultLocale(Locale.ENGLISH);
    cfg.setDefaultFirstDayOfWeek(Calendar.MONDAY);
    cfg.setCurrencySymbol("€");
    configurationService = cfg;
  }

  public static ConfigurationService get() {
    return configurationService;
  }
  static void setConfigurationService(ConfigurationService configurationService) {
    ConfigurationServiceAccessor.configurationService = configurationService;
  }
}
