package org.projectforge.launcher.config;

import java.util.ArrayList;
import java.util.List;

import de.micromata.genome.util.runtime.config.CastableLocalSettingsConfigModel;
import de.micromata.mgc.javafx.launcher.gui.TabConfig;
import de.micromata.mgc.javafx.launcher.gui.generic.ConfigurationTabLoaderService;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PfConfigurationTabLoaderService implements ConfigurationTabLoaderService
{

  @Override
  public List<TabConfig> getTabsByConfiguration(CastableLocalSettingsConfigModel configModel)
  {
    List<TabConfig> ret = new ArrayList<>();
    PfBasicLocalSettingsConfigModel pfBaseConfig = configModel
        .castToForConfigDialog(PfBasicLocalSettingsConfigModel.class);
    if (pfBaseConfig != null) {
      ret.add(new TabConfig(PfBaseSettingsController.class, pfBaseConfig));
    }
    return ret;
  }

}
