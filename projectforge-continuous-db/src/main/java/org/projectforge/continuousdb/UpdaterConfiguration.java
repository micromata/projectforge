/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.continuousdb;

import javax.sql.DataSource;

import org.projectforge.common.DatabaseDialect;
import org.projectforge.continuousdb.jdbc.DatabaseExecutorImpl;

/**
 * Main class for configuration of this module.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UpdaterConfiguration
{
  private DatabaseExecutor databaseExecutor;

  private DatabaseSupport databaseSupport;

  private DatabaseDialect dialect;

  private SystemUpdater systemUpdater;

  private DatabaseUpdateService databaseUpdateDao;

  private DataSource dataSource;

  public void setDatabaseExecutor(DatabaseExecutor databaseExecutor)
  {
    this.databaseExecutor = databaseExecutor;
  }

  public void setDatabaseUpdateDao(DatabaseUpdateService databaseUpdateDao)
  {
    this.databaseUpdateDao = databaseUpdateDao;
  }

  public DatabaseUpdateService getDatabaseUpdateDao()
  {
    return databaseUpdateDao;
  }

  /**
   * @param dialect
   * @return this for chaining.
   */
  public UpdaterConfiguration setDialect(DatabaseDialect dialect)
  {
    this.dialect = dialect;
    this.databaseSupport = null;
    return this;
  }

  /**
   * @param dataSource
   * @return this for chaining.
   */
  public UpdaterConfiguration setDataSource(DataSource dataSource)
  {
    this.dataSource = dataSource;
    if (databaseExecutor != null) {
      this.databaseExecutor.setDataSource(dataSource);
    }
    return this;
  }

  public DatabaseExecutor getDatabaseExecutor()
  {
    if (databaseExecutor == null) {
      databaseExecutor = new DatabaseExecutorImpl();
      databaseExecutor.setDataSource(dataSource);
    }
    return databaseExecutor;
  }

  public DatabaseSupport getDatabaseSupport()
  {
    if (databaseSupport == null) {
      databaseSupport = new DatabaseSupport(dialect);
    }
    return databaseSupport;
  }

  public DatabaseDialect getDialect()
  {
    return getDatabaseSupport().getDialect();
  }

  public SystemUpdater getSystemUpdater()
  {
    if (systemUpdater == null) {
      systemUpdater = new SystemUpdater(this);
    }
    return systemUpdater;
  }

  public SchemaGenerator createSchemaGenerator()
  {
    SchemaGenerator schemaGenerator = new SchemaGenerator(getDatabaseUpdateDao());
    return schemaGenerator;
  }
}
