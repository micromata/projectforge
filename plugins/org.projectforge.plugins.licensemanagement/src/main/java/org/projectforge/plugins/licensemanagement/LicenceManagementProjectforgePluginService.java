package org.projectforge.plugins.licensemanagement;

import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.ProjectforgePluginService;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class LicenceManagementProjectforgePluginService implements ProjectforgePluginService
{

  @Override
  public String getPluginId()
  {

    return "licenseManagementPlugin";
  }

  @Override
  public String getPluginName()
  {
    return "LicenseManagementPlugin";
  }

  @Override
  public String getPluginDescription()
  {
    return "license management plugin";
  }

  @Override
  public AbstractPlugin createPluginInstance()
  {
    return new LicenseManagementPlugin();
  }

}
