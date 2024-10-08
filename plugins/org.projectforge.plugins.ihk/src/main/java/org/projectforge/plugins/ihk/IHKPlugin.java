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

package org.projectforge.plugins.ihk;

import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.common.logging.LogEventLoggerNameMatcher;
import org.projectforge.common.logging.LogSubscription;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.ihk.service.IHKService;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by mnuhn on 05.12.2019
 */
public class IHKPlugin extends AbstractPlugin {

  public static final String RESOURCE_BUNDLE_NAME = "IHKI18nResources";

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  @Autowired
  private IHKService ihkService;

  @Autowired
  private TimesheetDao ihkDao;

  public IHKPlugin() {
    super("ihk", "IHK", "Plugin zur Generierung von Ausbildungsnachweise für Ausbildungsberufe der IHK.");
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize() {

    // Register it:
    register(getId(), TimesheetDao.class, ihkDao, "plugins.ihk");

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(getId());

    // Register the menu entry as sub menu entry of the misc menu:
    pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.MISC, MenuItemDef.create(getId(), "plugins.ihk.menu"),
            IHKPage.class);

    // Define the access management:
    registerRight(new IHKRight());

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);

  }

  static LogSubscription ensureUserLogSubscription() {
    String username = ThreadLocalUserContext.getLoggedInUser().getUsername();
    if (username == null) {
      return null;
    }
    return LogSubscription.ensureSubscription(
         "IHK-Plugin",
         username,
        (title, user) ->
            new LogSubscription(
                title,
                user,
                new LogEventLoggerNameMatcher("org.projectforge.plugins.ihk")
            )
        );
  }


}
