package org.projectforge.plugins.ffp;

import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.database.MyDatabaseUpdateService;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;

public class FinancialFairPlayPluginUpdates
{
  static MyDatabaseUpdateService dao;

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(FinancialFairPlayPlugin.ID, "6.3", "2016-09-06",
        "Adds T_PLUGIN_FINANCIAL_FAIR_PLAY* Tables")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (dao.doEntitiesExist(FFPEventDO.class, FFPAccountingDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        InitDatabaseDao initDatabaseDao = ApplicationContextProvider.getApplicationContext()
            .getBean(InitDatabaseDao.class);
        // Updating the schema
        initDatabaseDao.updateSchema();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
