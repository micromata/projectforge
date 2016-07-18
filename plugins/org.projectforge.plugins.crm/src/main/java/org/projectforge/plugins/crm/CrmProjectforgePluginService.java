package org.projectforge.plugins.crm;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class CrmProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {

    return "crm";
  }

  @Override
  public String getPluginName()
  {
    return "CustomRelationManagement";
  }

  @Override
  public String getPluginDescription()
  {
    return "blah";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new CrmPlugin();
  }

}
