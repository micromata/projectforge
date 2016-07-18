package org.projectforge.launcher;

import org.projectforge.launcher.config.PfLocalSettingsConfigModel;

import de.micromata.mgc.javafx.launcher.MgcLauncher;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PfiLauncherMain extends MgcLauncher<PfLocalSettingsConfigModel>
{
  public static void main(String[] args)
  {
    PfiLauncherMain el = new PfiLauncherMain();
    el.launch(args);
  }

  public PfiLauncherMain()
  {
    super(new PfSpringLauncherApplication(), (Class) PfMainWindow.class);
  }

}
