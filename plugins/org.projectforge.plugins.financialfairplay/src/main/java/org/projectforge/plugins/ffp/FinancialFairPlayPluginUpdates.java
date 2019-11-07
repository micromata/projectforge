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

package org.projectforge.plugins.ffp;

import org.projectforge.business.user.PFUserFilter;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.continuousdb.*;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class FinancialFairPlayPluginUpdates
{
  static ApplicationContext applicationContext;

  public static List<UpdateEntry> getUpdateEntries()
  {
    final DatabaseService databaseService = applicationContext.getBean(DatabaseService.class);
    final PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);

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
        if (!checkAlreadyChangedToUser()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!checkAlreadyChangedToUser()) {
          updateEmployeeToUser();
          databaseService.updateSchema();
        }
        return UpdateRunningStatus.DONE;
      }

      private void updateEmployeeToUser()
      {
        if (!databaseService.doesTableAttributeExist("t_plugin_financialfairplay_event", "organizer_user_id")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_event ADD organizer_user_id integer");
        }
        List<DatabaseResultRow> eventQueryResult = databaseService.query("SELECT organizer_id FROM t_plugin_financialfairplay_event");
        for (DatabaseResultRow resultRow : eventQueryResult) {
          DatabaseResultRowEntry entry = resultRow.getEntry(0);
          Integer organizerId = (Integer) entry.getValue();
          Integer userId = getUserIdForEmployeeId(organizerId);
          databaseService.update("UPDATE t_plugin_financialfairplay_event SET organizer_user_id = " + userId + " WHERE organizer_id = " + organizerId);
        }
        if (databaseService.doesTableAttributeExist("t_plugin_financialfairplay_event", "organizer_id")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_event DROP COLUMN organizer_id");
        }

        if (!databaseService.doesTableAttributeExist("t_plugin_financialfairplay_event_attendee", "attendee_user_pk")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_event_attendee ADD attendee_user_pk integer");
        }
        List<DatabaseResultRow> attendeeQueryResult = databaseService.query("SELECT attendee_pk FROM t_plugin_financialfairplay_event_attendee");
        for (DatabaseResultRow resultRow : attendeeQueryResult) {
          DatabaseResultRowEntry entry = resultRow.getEntry(0);
          Integer attendeeId = (Integer) entry.getValue();
          Integer userId = getUserIdForEmployeeId(attendeeId);
          databaseService
              .update("UPDATE t_plugin_financialfairplay_event_attendee SET attendee_user_pk = " + userId + " WHERE attendee_pk = " + attendeeId);
        }
        if (databaseService.doesTableAttributeExist("t_plugin_financialfairplay_event_attendee", "attendee_pk")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_event_attendee DROP COLUMN attendee_pk");
        }

        if (!databaseService.doesTableAttributeExist("t_plugin_financialfairplay_debt", "attendee_user_id_from")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_debt ADD attendee_user_id_from integer");
        }
        if (!databaseService.doesTableAttributeExist("t_plugin_financialfairplay_debt", "attendee_user_id_to")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_debt ADD attendee_user_id_to integer");
        }
        List<DatabaseResultRow> attendeeFromToQueryResult = databaseService
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
          databaseService.update(
              "UPDATE t_plugin_financialfairplay_debt SET attendee_user_id_from = " + userFromId + ", attendee_user_id_to = " + userToId
                  + " WHERE attendee_id_from = " + attendeeFromId + " AND attendee_id_to = " + attendeeToId);
        }
        if (databaseService.doesTableAttributeExist("t_plugin_financialfairplay_debt", "attendee_id_from")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_debt DROP COLUMN attendee_id_from");
        }
        if (databaseService.doesTableAttributeExist("t_plugin_financialfairplay_debt", "attendee_id_to")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_debt DROP COLUMN attendee_id_to");
        }

        if (!databaseService.doesTableAttributeExist("t_plugin_financialfairplay_accounting", "attendee_user_id")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_accounting ADD attendee_user_id integer");
        }
        List<DatabaseResultRow> attendeeAccountingQueryResult = databaseService.query("SELECT attendee_id FROM t_plugin_financialfairplay_accounting");
        for (
            DatabaseResultRow resultRow : attendeeAccountingQueryResult)

        {
          DatabaseResultRowEntry entry = resultRow.getEntry(0);
          Integer attendeeId = (Integer) entry.getValue();
          Integer userId = getUserIdForEmployeeId(attendeeId);
          databaseService
              .update("UPDATE t_plugin_financialfairplay_accounting SET attendee_user_id = " + userId + " WHERE attendee_id = " + attendeeId);
        }
        if (databaseService.doesTableAttributeExist("t_plugin_financialfairplay_accounting", "attendee_id")) {
          databaseService.update("ALTER TABLE t_plugin_financialfairplay_accounting DROP COLUMN attendee_id");
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
        List<DatabaseResultRow> userIdQueryResult = databaseService.query("SELECT user_id FROM t_fibu_employee where pk = ?", employeeId);
        if (userIdQueryResult != null && userIdQueryResult.size() > 0) {
          DatabaseResultRowEntry userIdEntry = userIdQueryResult.get(0).getEntry(0);
          return (Integer) userIdEntry.getValue();
        }
        return null;
      }

      private boolean checkAlreadyChangedToUser()
      {
        if (!databaseService.doesTableAttributeExist("t_plugin_financialfairplay_event", "organizer_id")) {
          return true;
        }
        if (!databaseService.doesTableAttributeExist("t_plugin_financialfairplay_event", "organizer_user_id")) {
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
        if (!databaseService.doesTableAttributeExist("T_PLUGIN_FINANCIALFAIRPLAY_ACCOUNTING", "comment") ||
            !databaseService.doesTableAttributeExist("T_PLUGIN_FINANCIALFAIRPLAY_EVENT", "commonDebtValue")) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!databaseService.doesTableAttributeExist("T_PLUGIN_FINANCIALFAIRPLAY_ACCOUNTING", "comment") ||
            !databaseService.doesTableAttributeExist("T_PLUGIN_FINANCIALFAIRPLAY_EVENT", "commonDebtValue")) {
          //Updating the schema
          databaseService.updateSchema();
        }
        return UpdateRunningStatus.DONE;
      }

    });

    return list;
  }

  public static UpdateEntry getInitializationUpdateEntry()
  {
    final DatabaseService databaseService = applicationContext.getBean(DatabaseService.class);

    return new UpdateEntryImpl(FinancialFairPlayPlugin.ID, "2016-09-06",
        "Adds T_PLUGIN_FINANCIAL_FAIR_PLAY* Tables")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (databaseService.doTablesExist(FFPEventDO.class, FFPAccountingDO.class, FFPDebtDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        DatabaseService databaseService = ApplicationContextProvider.getApplicationContext()
            .getBean(DatabaseService.class);
        // Updating the schema
        databaseService.updateSchema();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
