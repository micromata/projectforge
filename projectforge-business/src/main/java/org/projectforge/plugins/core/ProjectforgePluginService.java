package org.projectforge.plugins.core;

/**
 * A service, which is registered via JDK ServiceLoader.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface ProjectforgePluginService
{
  /**
   * A short id of this plugin.
   * 
   * @return
   */
  String getPluginId();

  /**
   * Name of the plugin
   * 
   * @return
   */
  String getPluginName();

  /**
   * Short explanation of the plugin.
   * 
   * @return
   */
  String getPluginDescription();

  /**
   * Creates the plugin instance.
   *
   * @return the abstract plugin
   */
  AbstractPlugin createPluginInstance();

  default boolean isBuildIn()
  {
    return false;
  }
}
