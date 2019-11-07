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

package org.projectforge.plugins.licensemanagement;

import org.projectforge.continuousdb.*;
import org.projectforge.framework.persistence.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the initial data-base set-up script and later all update scripts if any data-base schema updates are
 * required by any later release of this to-do plugin.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LicenseManagementPluginUpdates
{
  static DatabaseService dao;

  @SuppressWarnings("serial")
  public static List<UpdateEntry> getUpdateEntries()
  {
    final List<UpdateEntry> list = new ArrayList<>();
    // /////////////////////////////////////////////////////////////////
    // 5.1
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(LicenseManagementPlugin.ID, "5.2", "2013-05-19",
        "Adds T_PLUGIN_LM_LICENSE.file{name}{1,2}.")
    {
      String[] newAttributes = { "file1", "filename1", "file2", "filename2" };

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (dao.doTableAttributesExist(LicenseDO.class, newAttributes)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!dao.doTableAttributesExist(LicenseDO.class, newAttributes)) {
          dao.addTableAttributes(LicenseDO.class, newAttributes);
        }
        return UpdateRunningStatus.DONE;
      }
    });
    return list;
  }

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(LicenseManagementPlugin.ID, "2012-10-23", "Adds table T_PLUGIN_LM_LICENSE.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        // Check only the oldest table.
        if (dao.doTablesExist(LicenseDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          // The oldest table doesn't exist, therefore the plugin has to initialized completely.
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        // Create initial data-base table:
        final SchemaGenerator schemaGenerator = new SchemaGenerator(dao);
        schemaGenerator.add(LicenseDO.class);
        schemaGenerator.createSchema();
        dao.createMissingIndices();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
