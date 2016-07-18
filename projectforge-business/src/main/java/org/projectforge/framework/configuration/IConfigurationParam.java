package org.projectforge.framework.configuration;

/**
 * Configuration names.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface IConfigurationParam
{
  /**
   * key used in DB, etc.
   * 
   * @return
   */
  String getKey();

  ConfigurationType getType();

  /**
   * @return The full i18n key including the i18n prefix "administration.configuration.param.".
   */
  String getI18nKey();

  /**
   * @return The full i18n key including the i18n prefix "administration.configuration.param." and the suffix
   *         ".description".
   */
  String getDescriptionI18nKey();

  boolean isGlobal();

  String getDefaultStringValue();
}
