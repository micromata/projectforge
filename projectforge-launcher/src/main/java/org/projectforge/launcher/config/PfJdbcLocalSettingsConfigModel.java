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
   *
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
        return "spring.datasource.dbcp.default-catalog";

      case "validationQuery":
        return "spring.datasource.dbcp.validation-query";

      case "validationQueryTimeout":
        return "spring.datasource.dbcp.validation-query-timeout";

      case "maxActive":
        return "spring.datasource.dbcp.max-active";

      case "maxIdle":
        return "spring.datasource.dbcp.max-idle";

      case "minIdle":
        return "spring.datasource.dbcp.min-idle";

      case "maxWait":
        return "spring.datasource.dbcp.max-wait";

      case "intialSize": // "intial" -> typo in JdbcLocalSettingsConfigModel
        return "spring.datasource.dbcp.initial-size";

      case "defaultAutoCommit":
        return "spring.datasource.dbcp.default-auto-commit";

      default:
        // only for keys that are the same in spring (except the prefix)
        return "spring.datasource." + key;
    }
  }
}
