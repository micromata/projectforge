package org.projectforge.plugins.eed;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.fibu.EmployeeTimedAttrDataDO;
import org.projectforge.business.fibu.EmployeeTimedAttrWithDataDO;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.plugins.eed.model.EmployeeConfigurationAttrDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationAttrDataDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedAttrDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedAttrWithDataDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;

public class ExtendedEmployeeDataPluginUpdates
{
  static DatabaseService databaseService;

  @SuppressWarnings("serial")
  public static List<UpdateEntry> getUpdateEntries()
  {
    final List<UpdateEntry> list = new ArrayList<UpdateEntry>();
    list.add(new UpdateEntryImpl(ExtendEmployeeDataPlugin.ID, "6.5", "2016-11-15",
        "Adds attribute tables for employee configuration page")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (databaseService
            .doTablesExist(EmployeeConfigurationAttrDO.class, EmployeeConfigurationAttrDataDO.class, EmployeeConfigurationTimedAttrWithDataDO.class) == true) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (databaseService
            .doTablesExist(EmployeeConfigurationAttrDO.class, EmployeeConfigurationAttrDataDO.class, EmployeeConfigurationTimedAttrWithDataDO.class) == false) {
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
