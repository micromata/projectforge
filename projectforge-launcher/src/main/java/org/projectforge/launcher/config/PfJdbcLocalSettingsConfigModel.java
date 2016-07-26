package org.projectforge.launcher.config;

import de.micromata.genome.util.runtime.config.JdbcLocalSettingsConfigModel;

public class PfJdbcLocalSettingsConfigModel extends JdbcLocalSettingsConfigModel
{
  public PfJdbcLocalSettingsConfigModel()
  {
    super("projectForgeDs", "Standard JDBC for Genome");
  }

  /**
   * this method overrides the parent method of JdbcLocalSettingsConfigModel to remap keys for spring boot
   * @param key
   * @return the new key
   */
  @Override
  public String buildKey(String key)
  {
    switch (key) {
      // these two keys could not be remapped and are instead pointing to their old destinations
      case "jdbcConntextionTypeId":
      case "extendedSettings":
        return "db.ds." + getName() + "." + key;
      // in spring concatenation or camel case are not used for properties. That's why some names are remapped to use '-'
      case "drivername":
        return "spring.datasource.driver-class-name";
      case "defaultCatalog":
        return "spring.datasource.default-catalog";
      case "validationQuery":
        return "spring.datasource.validation-query";
      case "validationQueryTimeout":
        return "spring.datasource.validation-query-timeout";
      default:
        // only for keys that are the same in spring (except the prefix)
        return "spring.datasource." + key;
    }
  }
}
