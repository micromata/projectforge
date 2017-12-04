package org.projectforge.flyway.dbmigration;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

public class V6_18_0_1234_1__PROJECTFORGE_1234 implements SpringJdbcMigration
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(V6_18_0_1234_1__PROJECTFORGE_1234.class);

  @Override
  public void migrate(final JdbcTemplate jdbcTemplate) throws Exception
  {
    log.info("Running migration method for new version!");
    jdbcTemplate.execute("SELECT * FROM T_PLUGIN_PLUGINTEMPLATE");
  }
}
