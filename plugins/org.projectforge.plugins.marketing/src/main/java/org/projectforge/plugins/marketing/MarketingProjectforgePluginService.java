package org.projectforge.plugins.marketing;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class MarketingProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {

    return "marketing";
  }

  @Override
  public String getPluginName()
  {
    return "Marketing";
  }

  @Override
  public String getPluginDescription()
  {
    return "Marketing plugin";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new MarketingPlugin();
  }

}
