package org.projectforge.launcher;

import java.util.TimeZone;

import org.projectforge.framework.time.DateHelper;
import org.projectforge.launcher.config.PfLocalSettingsConfigModel;

import de.micromata.genome.util.i18n.ChainedResourceBundleTranslationResolver;
import de.micromata.genome.util.i18n.DefaultWarnI18NTranslationProvider;
import de.micromata.genome.util.i18n.I18NTranslationProvider;
import de.micromata.genome.util.i18n.I18NTranslationProviderImpl;
import de.micromata.genome.util.i18n.I18NTranslations;
import de.micromata.genome.util.i18n.PlaceholderTranslationProvider;
import de.micromata.genome.util.runtime.InitWithCopyFromCpLocalSettingsClassLoader;
import de.micromata.genome.util.runtime.LocalSettings;
import de.micromata.genome.util.runtime.config.ExtLocalSettingsLoader;
import de.micromata.mgc.application.MgcApplicationStartStopStatus;
import de.micromata.mgc.springbootapp.SpringBootApplication;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PfSpringLauncherApplication extends SpringBootApplication<PfLocalSettingsConfigModel>
{

  public PfSpringLauncherApplication()
  {
    LocalSettings.localSettingsLoaderFactory = new InitWithCopyFromCpLocalSettingsClassLoader(
        () -> {
          ExtLocalSettingsLoader ret = new ExtLocalSettingsLoader();
          ret.setLocalSettingsPrefixName("projectforge");
          return ret;
        });

    I18NTranslationProvider provider = new DefaultWarnI18NTranslationProvider(new PlaceholderTranslationProvider(
        new I18NTranslationProviderImpl(I18NTranslations.systemDefaultLocaleProvider(),
            new ChainedResourceBundleTranslationResolver("pflauncher", "mgclauncher", "mgcapp"))));

    setTranslateService(provider);
  }

  @Override
  protected Class<?> getSpringBootApplicationClass()
  {
    return ProjectForgeLauncher.class;
  }

  @Override
  public MgcApplicationStartStopStatus startImpl(String[] args)
  {
    System.setProperty("user.timezone", "UTC");
    TimeZone.setDefault(DateHelper.UTC);
    return super.startImpl(args);

  }

  @Override
  protected PfLocalSettingsConfigModel newModel()
  {
    return new PfLocalSettingsConfigModel();
  }

}
