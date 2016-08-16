package org.projectforge.plugins.eed;

import org.projectforge.continuousdb.SchemaGenerator;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.persistence.database.MyDatabaseUpdateService;

import java.util.ArrayList;
import java.util.List;

public class ExtendedEmployeeDataPluginUpdates
{
  static MyDatabaseUpdateService dao;

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(ExtendEmployeeDataPlugin.ID, "6.3", "2016-08-16", "Adds T_PLUGIN_EMPLOYEE_GENERAL_VALUE") {

    @Override
    public UpdatePreCheckStatus runPreCheck()
    {
      // Does the data-base table already exist?
      if (dao.doEntitiesExist(EmployeeGeneralValueDO.class)) {
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      } else {
        return UpdatePreCheckStatus.READY_FOR_UPDATE;
      }
    }

    @Override
    public UpdateRunningStatus runUpdate()
    {
      new SchemaGenerator(dao).add(EmployeeGeneralValueDO.class).createSchema();
      dao.createMissingIndices();
      return UpdateRunningStatus.DONE;
    }
  };
  }
}
