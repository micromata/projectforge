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
