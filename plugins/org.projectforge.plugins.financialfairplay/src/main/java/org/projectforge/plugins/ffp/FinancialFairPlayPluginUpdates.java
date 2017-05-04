package org.projectforge.plugins.ffp;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.user.PFUserFilter;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.continuousdb.DatabaseResultRow;
import org.projectforge.continuousdb.DatabaseResultRowEntry;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.database.DatabaseUpdateService;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.springframework.context.ApplicationContext;

public class FinancialFairPlayPluginUpdates
{
  static ApplicationContext applicationContext;

  public static List<UpdateEntry> getUpdateEntries()
  {
    final DatabaseUpdateService databaseUpdateService = applicationContext.getBean(DatabaseUpdateService.class);
    final PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);
    final InitDatabaseDao initDatabaseDao = applicationContext.getBean(InitDatabaseDao.class);

    final List<UpdateEntry> list = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////
    // 6.11.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(FinancialFairPlayPlugin.ID, "6.11.0", "2017-05-02",
        "Change organizer and attendee to user.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        if (checkAlreadyChangedToUser() == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (checkAlreadyChangedToUser() == false) {
          updateEmployeeToUser();
          initDatabaseDao.updateSchema();
        }
        return UpdateRunningStatus.DONE;
      }

      private void updateEmployeeToUser()
      {
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_event", "organizer_user_id") == false) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_event ADD organizer_user_id integer");
        }
        List<DatabaseResultRow> eventQueryResult = databaseUpdateService.query("SELECT organizer_id FROM t_plugin_financialfairplay_event");
        for (DatabaseResultRow resultRow : eventQueryResult) {
          DatabaseResultRowEntry entry = resultRow.getEntry(0);
          Integer organizerId = (Integer) entry.getValue();
          Integer userId = getUserIdForEmployeeId(organizerId);
          databaseUpdateService.update("UPDATE t_plugin_financialfairplay_event SET organizer_user_id = " + userId + " WHERE organizer_id = " + organizerId);
        }
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_event", "organizer_id")) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_event DROP COLUMN organizer_id");
        }

        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_event_attendee", "attendee_user_pk") == false) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_event_attendee ADD attendee_user_pk integer");
        }
        List<DatabaseResultRow> attendeeQueryResult = databaseUpdateService.query("SELECT attendee_pk FROM t_plugin_financialfairplay_event_attendee");
        for (DatabaseResultRow resultRow : attendeeQueryResult) {
          DatabaseResultRowEntry entry = resultRow.getEntry(0);
          Integer attendeeId = (Integer) entry.getValue();
          Integer userId = getUserIdForEmployeeId(attendeeId);
          databaseUpdateService
              .update("UPDATE t_plugin_financialfairplay_event_attendee SET attendee_user_pk = " + userId + " WHERE attendee_pk = " + attendeeId);
        }
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_event_attendee", "attendee_pk")) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_event_attendee DROP COLUMN attendee_pk");
        }

        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_debt", "attendee_user_id_from") == false) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_debt ADD attendee_user_id_from integer");
        }
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_debt", "attendee_user_id_to") == false) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_debt ADD attendee_user_id_to integer");
        }
        List<DatabaseResultRow> attendeeFromToQueryResult = databaseUpdateService
            .query("SELECT attendee_id_from, attendee_id_to FROM t_plugin_financialfairplay_debt");
        for (
            DatabaseResultRow resultRow : attendeeFromToQueryResult)

        {
          DatabaseResultRowEntry entryFrom = resultRow.getEntry(0);
          DatabaseResultRowEntry entryTo = resultRow.getEntry(1);
          Integer attendeeFromId = (Integer) entryFrom.getValue();
          Integer attendeeToId = (Integer) entryTo.getValue();
          Integer userFromId = getUserIdForEmployeeId(attendeeFromId);
          Integer userToId = getUserIdForEmployeeId(attendeeToId);
          databaseUpdateService.update(
              "UPDATE t_plugin_financialfairplay_debt SET attendee_user_id_from = " + userFromId + ", attendee_user_id_to = " + userToId
                  + " WHERE attendee_id_from = " + attendeeFromId + " AND attendee_id_to = " + attendeeToId);
        }
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_debt", "attendee_id_from")) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_debt DROP COLUMN attendee_id_from");
        }
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_debt", "attendee_id_to")) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_debt DROP COLUMN attendee_id_to");
        }

        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_accounting", "attendee_user_id") == false) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_accounting ADD attendee_user_id integer");
        }
        List<DatabaseResultRow> attendeeAccountingQueryResult = databaseUpdateService.query("SELECT attendee_id FROM t_plugin_financialfairplay_accounting");
        for (
            DatabaseResultRow resultRow : attendeeAccountingQueryResult)

        {
          DatabaseResultRowEntry entry = resultRow.getEntry(0);
          Integer attendeeId = (Integer) entry.getValue();
          Integer userId = getUserIdForEmployeeId(attendeeId);
          databaseUpdateService
              .update("UPDATE t_plugin_financialfairplay_accounting SET attendee_user_id = " + userId + " WHERE attendee_id = " + attendeeId);
        }
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_accounting", "attendee_id")) {
          databaseUpdateService.update("ALTER TABLE t_plugin_financialfairplay_accounting DROP COLUMN attendee_id");
        }

        UserDao userDao = applicationContext.getBean(UserDao.class);
        UserXmlPreferencesCache cache = applicationContext.getBean(UserXmlPreferencesCache.class);
        List<PFUserDO> userList = userDao.getList(new PFUserFilter());
        for (
            PFUserDO user : userList)

        {
          cache.removeEntry(user.getId(), "org.projectforge.plugins.ffp.wicket.FFPDebtListForm:Filter");
        }
      }

      private Integer getUserIdForEmployeeId(Integer employeeId)
      {
        List<DatabaseResultRow> userIdQueryResult = databaseUpdateService.query("SELECT user_id FROM t_fibu_employee where pk = ?", employeeId);
        if (userIdQueryResult != null && userIdQueryResult.size() > 0) {
          DatabaseResultRowEntry userIdEntry = userIdQueryResult.get(0).getEntry(0);
          return (Integer) userIdEntry.getValue();
        }
        return null;
      }

      private boolean checkAlreadyChangedToUser()
      {
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_event", "organizer_id") == false) {
          return true;
        }
        if (databaseUpdateService.doesTableAttributeExist("t_plugin_financialfairplay_event", "organizer_user_id") == false) {
          return false;
        }
        return false;
      }

    });

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
    final DatabaseUpdateService databaseUpdateService = applicationContext.getBean(DatabaseUpdateService.class);

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
