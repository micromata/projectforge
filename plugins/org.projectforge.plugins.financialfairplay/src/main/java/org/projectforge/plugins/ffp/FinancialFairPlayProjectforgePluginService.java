package org.projectforge.plugins.ffp;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class FinancialFairPlayProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {
    return "extendemployeedata";
  }

  @Override
  public String getPluginName()
  {
    return "ExtendEmployeeData";
  }

  @Override
  public String getPluginDescription()
  {
    return "PlugIn for extended employee data";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new FinancialFairPlayPlugin();
  }

}
