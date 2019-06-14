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

package org.projectforge.plugins.core;

import java.util.List;

/**
 * For administration of plugins.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface PluginAdminService
{
  List<AbstractPlugin> getActivePlugin();

  /**
   * All installed plugin services.
   *
   * @return the plugin services
   */
  List<AvailablePlugin> getAvailablePlugins();

  /**
   * Store a plugin as activated.
   * 
   * @param id
   * @param activate
   * @return
   */
  boolean storePluginToBeActivated(String id, boolean activate);

  /**
   * Will be active plugins
   */
  void initializeActivePlugins();

  void initializeAllPluginsForUnittest();

  public static interface PluginCallback
  {
    void call(AbstractPlugin plugin);
  }

  void addExecuteAfterActivePluginCreated(PluginCallback run);
}
