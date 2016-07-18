package org.projectforge.plugins.liquidityplanning;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class LiquidPlanningProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {

    return "liquidplanning";
  }

  @Override
  public String getPluginName()
  {
    return "Liquidplanning";
  }

  @Override
  public String getPluginDescription()
  {
    return "blah";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new LiquidityPlanningPlugin();
  }

}
