package org.projectforge.plugins.eed;

import org.projectforge.business.fibu.EmployeeTimedAttrDataDO;
import org.projectforge.business.fibu.EmployeeTimedAttrWithDataDO;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.database.MyDatabaseUpdateService;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedAttrDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;

public class ExtendedEmployeeDataPluginUpdates
{
  static MyDatabaseUpdateService dao;

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(ExtendEmployeeDataPlugin.ID, "6.3", "2016-08-16", "Adds T_PLUGIN_EMPLOYEE_CONFIGURATION* Tables")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (dao.doEntitiesExist(EmployeeConfigurationDO.class, EmployeeConfigurationTimedAttrDO.class,
            EmployeeConfigurationTimedDO.class, EmployeeTimedAttrWithDataDO.class, EmployeeTimedAttrDataDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        InitDatabaseDao initDatabaseDao = ApplicationContextProvider.getApplicationContext().getBean(InitDatabaseDao.class);
        // Updating the schema
        initDatabaseDao.updateSchema();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
