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

package org.projectforge.plugins.plugintemplate;

import java.util.List;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.plugintemplate.repository.PluginTemplateDao;
import org.projectforge.plugins.plugintemplate.service.PluginTemplateService;
import org.projectforge.plugins.plugintemplate.wicket.PluginTemplateListPage;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florian Blumenstein
 */
public class PluginTemplatePlugin extends AbstractPlugin {
  public static final String ID = "plugintemplate";

  public static final String RESOURCE_BUNDLE_NAME = "PluginTemplateI18nResources";

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  @Autowired
  private PluginTemplateService pluginTemplateService;

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize() {
    PluginTemplatePluginUpdates.applicationContext = applicationContext;

    // Register it:
    register(ID, PluginTemplateDao.class, pluginTemplateService.getPluginTemplateDao(), "plugins.plugintemplate");

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ID);

    pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.MISC,
            MenuItemDef.create("plugintemplate", "plugins.plugintemplate.submenu.plugintemplate.list"),
            PluginTemplateListPage.class);

    // Define the access management:
    registerRight(new PluginTemplateRight(accessChecker));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getInitializationUpdateEntry()
   */
  @Override
  public UpdateEntry getInitializationUpdateEntry() {
    return PluginTemplatePluginUpdates.getInitializationUpdateEntry();
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getUpdateEntries()
   */
  @Override
  public List<UpdateEntry> getUpdateEntries() {
    return PluginTemplatePluginUpdates.getUpdateEntries();
  }

}
