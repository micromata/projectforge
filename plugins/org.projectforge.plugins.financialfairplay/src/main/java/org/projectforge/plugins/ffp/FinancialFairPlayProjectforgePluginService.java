package org.projectforge.plugins.ffp;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * 
 * @author Florian Blumenstein
 *
 */
public class FinancialFairPlayProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {
    return "financialfairplay";
  }

  @Override
  public String getPluginName()
  {
    return "FinancialFairPlay";
  }

  @Override
  public String getPluginDescription()
  {
    return "PlugIn for organize financial fairplay";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new FinancialFairPlayPlugin();
  }

}
