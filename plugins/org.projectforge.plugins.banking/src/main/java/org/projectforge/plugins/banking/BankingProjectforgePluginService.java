package org.projectforge.plugins.banking;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * Banking plugin.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class BankingProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {
    return "banking";
  }

  @Override
  public String getPluginName()
  {
    return getPluginId();
  }

  @Override
  public String getPluginDescription()
  {
    return getPluginName();
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new BankingPlugin();
  }

}
