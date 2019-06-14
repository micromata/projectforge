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

/**
 * Status of a plugin
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class AvailablePlugin
{
  private ProjectforgePluginService projectForgePluginService;

  private boolean activated;

  private boolean buildIn;

  public AvailablePlugin()
  {

  }

  public AvailablePlugin(ProjectforgePluginService projectForgePluginService, boolean activated, boolean buildIn)
  {

    this.projectForgePluginService = projectForgePluginService;
    this.activated = activated;
    this.buildIn = buildIn;
  }

  public ProjectforgePluginService getProjectForgePluginService()
  {
    return projectForgePluginService;
  }

  public void setProjectForgePluginService(ProjectforgePluginService projectForgePluginService)
  {
    this.projectForgePluginService = projectForgePluginService;
  }

  public boolean isActivated()
  {
    return activated;
  }

  public void setActivated(boolean activated)
  {
    this.activated = activated;
  }

  public boolean isBuildIn()
  {
    return buildIn;
  }

  public void setBuildIn(boolean buildIn)
  {
    this.buildIn = buildIn;
  }

}
