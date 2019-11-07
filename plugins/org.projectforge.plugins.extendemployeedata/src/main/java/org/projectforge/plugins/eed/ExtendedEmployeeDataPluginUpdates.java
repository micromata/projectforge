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

package org.projectforge.plugins.eed;

import org.projectforge.business.fibu.EmployeeTimedAttrDataDO;
import org.projectforge.business.fibu.EmployeeTimedAttrWithDataDO;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.plugins.eed.model.*;

import java.util.ArrayList;
import java.util.List;

public class ExtendedEmployeeDataPluginUpdates
{
  static DatabaseService databaseService;

  @SuppressWarnings("serial")
  public static List<UpdateEntry> getUpdateEntries()
  {
    final List<UpdateEntry> list = new ArrayList<>();
    list.add(new UpdateEntryImpl(ExtendEmployeeDataPlugin.ID, "6.5", "2016-11-15",
        "Adds attribute tables for employee configuration page")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (databaseService
            .doTablesExist(EmployeeConfigurationAttrDO.class, EmployeeConfigurationAttrDataDO.class, EmployeeConfigurationTimedAttrWithDataDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!databaseService
            .doTablesExist(EmployeeConfigurationAttrDO.class, EmployeeConfigurationAttrDataDO.class, EmployeeConfigurationTimedAttrWithDataDO.class)) {
          databaseService.updateSchema();
        }
        return UpdateRunningStatus.DONE;
      }
    });
    return list;
  }

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(ExtendEmployeeDataPlugin.ID, "2016-08-16",
        "Adds T_PLUGIN_EMPLOYEE_CONFIGURATION* Tables")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (databaseService.doTablesExist(EmployeeConfigurationDO.class, EmployeeConfigurationTimedAttrDO.class,
            EmployeeConfigurationTimedDO.class, EmployeeTimedAttrWithDataDO.class, EmployeeTimedAttrDataDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        // Updating the schema
        databaseService.updateSchema();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
