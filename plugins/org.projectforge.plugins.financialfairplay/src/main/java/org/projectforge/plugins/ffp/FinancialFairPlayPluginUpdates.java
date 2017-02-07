package org.projectforge.plugins.ffp;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.database.DatabaseUpdateService;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;

public class FinancialFairPlayPluginUpdates
{
  static DatabaseUpdateService databaseUpdateService;
  static InitDatabaseDao initDatabaseDao;

  public static List<UpdateEntry> getUpdateEntries()
  {
    final List<UpdateEntry> list = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////
    // 6.8.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(FinancialFairPlayPlugin.ID, "6.8.0", "2017-02-08",
        "Add comment coulumn to accounting. Add commonDebtValue to event.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        if (databaseUpdateService.doesTableAttributeExist("T_PLUGIN_FINANCIALFAIRPLAY_ACCOUNTING", "comment") == false ||
            databaseUpdateService.doesTableAttributeExist("T_PLUGIN_FINANCIALFAIRPLAY_EVENT", "commonDebtValue") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (databaseUpdateService.doesTableAttributeExist("T_PLUGIN_FINANCIALFAIRPLAY_ACCOUNTING", "comment") == false ||
            databaseUpdateService.doesTableAttributeExist("T_PLUGIN_FINANCIALFAIRPLAY_EVENT", "commonDebtValue") == false) {
          //Updating the schema
          initDatabaseDao.updateSchema();
        }
        return UpdateRunningStatus.DONE;
      }

    });

    return list;
  }

  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(FinancialFairPlayPlugin.ID, "2016-09-06",
        "Adds T_PLUGIN_FINANCIAL_FAIR_PLAY* Tables")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (databaseUpdateService.doTablesExist(FFPEventDO.class, FFPAccountingDO.class, FFPDebtDO.class)) {
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
