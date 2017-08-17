package org.projectforge.plugins.plugintemplate;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.persistence.database.DatabaseUpdateService;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.plugins.plugintemplate.model.PluginTemplateDO;
import org.springframework.context.ApplicationContext;

public class PluginTemplatePluginUpdates
{
  static ApplicationContext applicationContext;

  public static List<UpdateEntry> getUpdateEntries()
  {
    final DatabaseUpdateService databaseUpdateService = applicationContext.getBean(DatabaseUpdateService.class);
    final PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);
    final InitDatabaseDao initDatabaseDao = applicationContext.getBean(InitDatabaseDao.class);

    final List<UpdateEntry> list = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////
    // 6.X.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(PluginTemplatePlugin.ID, "6.X.0", "20XX-XX-XX",
        "Some updates -> NOT INITIAL.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        if (false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (false) {
          //DO SOME UPDATES!!!!
          //ATTENTION BY USING THIS METHOD
          initDatabaseDao.updateSchema();
        }
        return UpdateRunningStatus.DONE;
      }
    });

    return list;
  }

  public static UpdateEntry getInitializationUpdateEntry()
  {
    final DatabaseUpdateService databaseUpdateService = applicationContext.getBean(DatabaseUpdateService.class);
    final InitDatabaseDao initDatabaseDao = applicationContext.getBean(InitDatabaseDao.class);

    return new UpdateEntryImpl(PluginTemplatePlugin.ID, "2017-08-01",
        "Adds T_PLUGIN_PLUGINTEMPLATE* Tables")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (databaseUpdateService.doTablesExist(PluginTemplateDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        initDatabaseDao.updateSchema();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
