package org.projectforge.plugins.plugintemplate;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * @author Florian Blumenstein
 */
public class PluginTemplateProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {
    return PluginTemplatePlugin.ID;
  }

  @Override
  public String getPluginName()
  {
    return "PluginTemplate";
  }

  @Override
  public String getPluginDescription()
  {
    return "Template for PF PlugIn";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new PluginTemplatePlugin();
  }

}
