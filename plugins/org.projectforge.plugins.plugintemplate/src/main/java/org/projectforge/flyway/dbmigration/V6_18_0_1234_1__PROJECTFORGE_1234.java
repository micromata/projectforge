/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

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
