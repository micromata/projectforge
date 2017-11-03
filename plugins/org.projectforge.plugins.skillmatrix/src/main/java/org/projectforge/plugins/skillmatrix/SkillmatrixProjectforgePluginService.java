package org.projectforge.plugins.skillmatrix;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class SkillmatrixProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {

    return "skillmatrix";
  }

  @Override
  public String getPluginName()
  {
    return "Skillmatrix";
  }

  @Override
  public String getPluginDescription()
  {
    return "Plugin to manage user skills";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new SkillMatrixPlugin();
  }

}
