package org.projectforge.plugins.core;

import java.util.List;

/**
 * For administration of plugins.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface PluginAdminService
{
  List<AbstractPlugin> getActivePlugin();

  /**
   * All installed plugin services.
   *
   * @return the plugin services
   */
  List<AvailablePlugin> getAvailablePlugins();

  /**
   * Store a plugin as activated.
   * 
   * @param id
   * @param activate
   * @return
   */
  boolean storePluginToBeActivated(String id, boolean activate);

  /**
   * Will be active plugins
   */
  void initializeActivePlugins();

  void initializeAllPluginsForUnittest();

  public static interface PluginCallback
  {
    void call(AbstractPlugin plugin);
  }

  void addExecuteAfterActivePluginCreated(PluginCallback run);
}
