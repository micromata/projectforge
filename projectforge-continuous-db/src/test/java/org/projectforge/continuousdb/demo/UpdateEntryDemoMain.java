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

package org.projectforge.continuousdb.demo;

import org.apache.commons.dbcp.BasicDataSource;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.continuousdb.DatabaseUpdateService;
import org.projectforge.continuousdb.SchemaGenerator;
import org.projectforge.continuousdb.Table;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.continuousdb.UpdaterConfiguration;
import org.projectforge.continuousdb.demo.entities.AccessEntryDO;
import org.projectforge.continuousdb.demo.entities.Address1DO;
import org.projectforge.continuousdb.demo.entities.Address2DO;
import org.projectforge.continuousdb.demo.entities.GroupDO;
import org.projectforge.continuousdb.demo.entities.GroupTaskAccessDO;
import org.projectforge.continuousdb.demo.entities.TaskDO;
import org.projectforge.continuousdb.demo.entities.UserDO;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UpdateEntryDemoMain
{
  public static void main(final String[] args)
  {
    UpdateEntryDemoMain main = null;
    try {
      main = new UpdateEntryDemoMain();
      // Create tables:
      main.createInitialSchema();
      // Create and update single table:
      main.createAndUpdateAddressDO();
    } finally {
      if (main != null) {
        main.shutdown();
      }
    }
  }

  private final UpdaterConfiguration configuration;

  private final DatabaseUpdateService databaseUpdateDao;

  private UpdateEntryDemoMain()
  {
    final BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
    dataSource.setUsername("sa");
    // dataSource.setPassword("password");
    dataSource.setUrl("jdbc:hsqldb:testdatabase");

    configuration = new UpdaterConfiguration().setDialect(DatabaseDialect.HSQL).setDataSource(dataSource);
    databaseUpdateDao = configuration.getDatabaseUpdateDao();
    // TableAttribute.register(new TableAttributeHookImpl());

    // final SortedSet<UpdateEntry> updateEntries = new TreeSet<UpdateEntry>();
    // updateEntries.addAll(DatabaseCoreUpdates.getUpdateEntries(this));
    // getSystemUpdater().setUpdateEntries(updateEntries);
  }

  private void shutdown()
  {
    databaseUpdateDao.shutdownDatabase();
  }

  private void createAndUpdateAddressDO()
  {
    if (databaseUpdateDao.doEntitiesExist(Address1DO.class) == false) {
      // Initial creation of t_address because database table doesn't yet exist:
      configuration.createSchemaGenerator().add(Address1DO.class).createSchema();
    }
    if (databaseUpdateDao.doEntitiesExist(Address1DO.class) == false) {
      throw new RuntimeException("What the hell? The table '" + Address1DO.class + "' wasn't created as expected!");
    }
    // Table t_address does now exist.
    if (databaseUpdateDao.doTableAttributesExist(Address2DO.class, "birthday", "address") == false) {
      // One or both attributes don't yet exist, alter table to add the missing columns now:
      databaseUpdateDao.addTableAttributes(Address2DO.class, "birthday", "address"); // Works also, if one of both attributes does already
      // exist.
    }
    if (databaseUpdateDao.doTableAttributesExist(Address2DO.class, "birthday", "address") == false) {
      throw new RuntimeException("What the hell? The missing columns 'birthday' and 'address' weren't created as expected!");
    }
  }

  private void createInitialSchema()
  {
    final UpdateEntry initialUpdateEntry = getInitialUpdateEntry();
    if (initialUpdateEntry.runPreCheck() == UpdatePreCheckStatus.READY_FOR_UPDATE) {
      initialUpdateEntry.runUpdate();
    }
  }

  private UpdateEntry getInitialUpdateEntry()
  {
    final Class< ? >[] doClasses = new Class< ? >[] { //
        // Please note, the order of the entities is the order of their creation!
        UserDO.class, //
        TaskDO.class, GroupDO.class, TaskDO.class, GroupTaskAccessDO.class, //
        AccessEntryDO.class, //
    };

    @SuppressWarnings("serial")
    final UpdateEntryImpl entry = new UpdateEntryImpl("core", "2013-05-10", "Adds all core tables T_*.") {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the database tables already exist?
        if (databaseUpdateDao.doEntitiesExist(doClasses) == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (databaseUpdateDao.doExist(new Table(UserDO.class)) == false && configuration.getDialect() == DatabaseDialect.PostgreSQL) {
          // User table doesn't exist, therefore schema should be empty. PostgreSQL needs sequence for primary keys:
          databaseUpdateDao.createSequence("hibernate_sequence", true);
        }
        final SchemaGenerator schemaGenerator = configuration.createSchemaGenerator().add(doClasses);
        // You may add also add table manually, e. g. of third party libraries or such which aren't yet supported:
        // final Table propertyDeltaTable = schemaGenerator.getTable(PropertyDelta.class);
        // propertyDeltaTable.addAttribute(new TableAttribute("clazz", TableAttributeType.VARCHAR, 31).setNullable(false));
        // final Table historyEntryTable = schemaGenerator.getTable(HistoryEntry.class);
        // final TableAttribute typeAttr = historyEntryTable.getAttributeByName("type");
        // typeAttr.setType(TableAttributeType.INT);
        schemaGenerator.createSchema();
        databaseUpdateDao.createMissingIndices(); // Create missing indices.

        return UpdateRunningStatus.DONE;
      }
    };
    return entry;
  }
}
