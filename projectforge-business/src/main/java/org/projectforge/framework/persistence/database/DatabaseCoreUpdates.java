/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.framework.persistence.database;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.projectforge.business.address.AddressDO;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.PaymentScheduleDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.user.UserXmlPreferencesDO;
import org.projectforge.continuousdb.DatabaseResultRow;
import org.projectforge.continuousdb.SchemaGenerator;
import org.projectforge.continuousdb.Table;
import org.projectforge.continuousdb.TableAttribute;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.time.DateHelper;
import org.springframework.context.ApplicationContext;

import de.micromata.genome.db.jpa.tabattr.api.TimeableRow;
import de.micromata.genome.jpa.CriteriaUpdate;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DatabaseCoreUpdates
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DatabaseCoreUpdates.class);

  public static final String CORE_REGION_ID = DatabaseCoreInitial.CORE_REGION_ID;

  private static final String VERSION_5_0 = "5.0";

  static ApplicationContext applicationContext;

  @SuppressWarnings("serial")
  public static List<UpdateEntry> getUpdateEntries()
  {
    final List<UpdateEntry> list = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////
    // 6.3.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.3.0", "2016-08-31",
        "Add column to attendee data table. Alter table column for ssh-key.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.3.0");
        final MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT_ATTENDEE", "address_id") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        } else if (databaseUpdateDao.getDatabaseTableColumnLenght(PFUserDO.class, "ssh_public_key") < 4096) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        } else {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        final InitDatabaseDao initDatabaseDao = applicationContext.getBean(InitDatabaseDao.class);
        final MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT_ATTENDEE", "address_id") == false) {
          //Updating the schema
          initDatabaseDao.updateSchema();
        }
        if (databaseUpdateDao.getDatabaseTableColumnLenght(PFUserDO.class, "ssh_public_key") < 4096) {
          final Table userTable = new Table(PFUserDO.class);
          databaseUpdateDao.alterTableColumnVarCharLength(userTable.getName(), "ssh_public_key", 4096);
        }
        return UpdateRunningStatus.DONE;
      }

    });

    ////////////////////////////////////////////////////////////////////
    // 6.1.1
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.1.1", "2016-07-27",
        "Changed timezone of starttime of the configurable attributes.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.1.1");
        final MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doEntitiesExist(EmployeeTimedDO.class) == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        if (databaseUpdateDao.isTableEmpty(EmployeeTimedDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        }

        final PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);
        final boolean timeFieldsOfAllEmployeeTimedDOsStartTimeAreZero = emf
            .runWoTrans(emgr -> emgr.selectAllAttached(EmployeeTimedDO.class)
                .stream()
                .map(EmployeeTimedDO::getStartTime)
                .map(DateHelper::convertDateToLocalDateTimeInUTC)
                .map(localDateTime -> localDateTime.get(ChronoField.SECOND_OF_DAY))
                .allMatch(seconds -> seconds == 0));

        return timeFieldsOfAllEmployeeTimedDOsStartTimeAreZero ? UpdatePreCheckStatus.ALREADY_UPDATED
            : UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        final PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);
        return emf.runInTrans(emgr -> {
          emgr.selectAllAttached(EmployeeTimedDO.class)
              .forEach(this::normalizeStartTime);

          return UpdateRunningStatus.DONE;
        });
      }

      private void normalizeStartTime(TimeableRow entity)
      {
        final Date oldStartTime = entity.getStartTime();
        LocalDateTime ldt = DateHelper.convertDateToLocalDateTimeInUTC(oldStartTime);
        /*
         * In UTC+x the UTC hour value of 00:00:00 is 24-x hours and minus 1 day if x > 0 examples: 00:00:00 in UTC+1 is
         * 23:00:00 minus 1 day in UTC 00:00:00 in UTC+12 is 12:00:00 minus 1 day in UTC 00:00:00 in UTC-1 is 01:00:00
         * in UTC 00:00:00 in UTC-11 is 11:00:00 in UTC therefore, to calculate the zoned time back to local time, we
         * have to add one day if hour >= 12
         */
        final int daysToAdd = (ldt.getHour() >= 12) ? 1 : 0;
        ldt = ldt.toLocalDate().plusDays(daysToAdd).atStartOfDay();
        final Date newStartTime = DateHelper.convertLocalDateTimeToDateInUTC(ldt);
        entity.setStartTime(newStartTime);
      }
    });

    ////////////////////////////////////////////////////////////////////
    // 6.1.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.1.0", "2016-07-14",
        "Adds several columns to employee table.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.1.0");
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(EmployeeDO.class, "staffNumber") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        InitDatabaseDao initDatabaseDao = applicationContext.getBean(InitDatabaseDao.class);
        //Updating the schema
        initDatabaseDao.updateSchema();
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 6.0.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.0.0", "2016-04-01",
        "Adds tenant table, tenant_id to all entities for multi-tenancy. Adds new history tables. Adds attr table for address. Adds t_configuration.is_global, t_pf_user.super_admin.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.0.0");
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doEntitiesExist(TenantDO.class) == false
            || databaseUpdateDao.internalIsTableEmpty("t_tenant") == true ||
            databaseUpdateDao.doTableAttributesExist(ConfigurationDO.class, "global") == false ||
            databaseUpdateDao.doTableAttributesExist(PFUserDO.class, "superAdmin") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public UpdateRunningStatus runUpdate()
      {
        InitDatabaseDao initDatabaseDao = applicationContext.getBean(InitDatabaseDao.class);

        //Generating the schema
        initDatabaseDao.updateSchema();

        //Init default tenant
        TenantDO defaultTenant = initDatabaseDao.insertDefaultTenant();

        //Insert default tenant on every entity
        log.info("Start adding default tenant to entities.");
        PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);
        List<EntityMetadata> entities = emf.getMetadataRepository().getTableEntities();
        Collections.reverse(entities);
        for (EntityMetadata entityClass : entities) {
          if (AbstractBaseDO.class.isAssignableFrom(entityClass.getJavaType())) {
            try {
              log.info("Set tenant id for entities of type: " + entityClass.getJavaType());
              emf.tx().go(emgr -> {
                Class<? extends AbstractBaseDO> entity = (Class<? extends AbstractBaseDO>) entityClass.getJavaType();
                CriteriaUpdate<? extends AbstractBaseDO> cu = CriteriaUpdate.createUpdate(entity);
                cu.set("tenant", defaultTenant);
                emgr.update(cu);
                return null;
              });
            } catch (Exception e) {
              log.error("Failed to update default tenant for entities of type: " + entityClass.getJavaType());
            }
          }
          if (UserXmlPreferencesDO.class.isAssignableFrom(entityClass.getJavaType())) {
            try {
              emf
                  .runInTrans(emgr -> {
                    log.info("Set tenant id for entities of type: " + UserXmlPreferencesDO.class.getClass());
                    CriteriaUpdate<UserXmlPreferencesDO> cu = CriteriaUpdate.createUpdate(UserXmlPreferencesDO.class);
                    cu.set("tenant", defaultTenant);
                    emgr.update(cu);
                    return null;
                  });
            } catch (Exception e) {
              log.error("Failed to update default tenant for user xml prefs.");
            }
          }
        }
        log.info("Finished adding default tenant to entities.");

        //User default tenant zuweisen
        log.info("Start assigning users to default tenant.");
        try {
          emf.tx().go(emgr -> {
            TenantDO attachedDefaultTenant = emgr.selectByPkAttached(TenantDO.class, defaultTenant.getId());
            List<PFUserDO> users = emgr.selectAttached(PFUserDO.class, "select u from PFUserDO u");
            for (PFUserDO user : users) {
              log.info("Assign user with id: " + user.getId() + " to default tenant.");
              attachedDefaultTenant.getAssignedUsers().add(user);
              emgr.update(attachedDefaultTenant);
            }
            return null;
          });
        } catch (Exception e) {
          log.error("Failed to assign users to default tenant.");
        }
        log.info("Finished assigning users to default tenant.");

        //History migration
        log.info("Start migrating history data.");
        HistoryMigrateService ms = applicationContext.getBean(HistoryMigrateService.class);
        long start = System.currentTimeMillis();
        try {
          ms.migrate();
        } catch (Exception ex) {
          log.error("Error while migrating history data", ex);
        }
        log.info("History Migration took: " + (System.currentTimeMillis() - start) / 1000 + " sec");
        log.info("Finished migrating history data.");

        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////
    // 5.5
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(
        CORE_REGION_ID,
        "5.5",
        "2014-08-11",
        "Adds t_group.ldap_values, t_fibu_auftrag_position.period_of_performance_type, t_fibu_auftrag_position.mode_of_payment_type, t_fibu_payment_schedule, t_fibu_auftrag.period_of_performance_{begin|end}, length of t_address.public_key increased.")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(EmployeeDO.class, "weeklyWorkingHours") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        if (databaseUpdateDao.doTableAttributesExist(GroupDO.class, "ldapValues") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        if (databaseUpdateDao.doTableAttributesExist(AuftragsPositionDO.class, "periodOfPerformanceType",
            "modeOfPaymentType") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        if (databaseUpdateDao.doTableAttributesExist(AuftragDO.class, "periodOfPerformanceBegin",
            "periodOfPerformanceEnd") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        if (databaseUpdateDao.doEntitiesExist(PaymentScheduleDO.class) == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(EmployeeDO.class, "weeklyWorkingHours") == false) {
          // No length check available so assume enlargement if ldapValues doesn't yet exist:
          final Table addressTable = new Table(AddressDO.class);
          databaseUpdateDao.alterTableColumnVarCharLength(addressTable.getName(), "public_key", 20000);

          // TODO HIBERNATE5 no longer supported
          //          final Table propertyDeltaTable = new Table(PropertyDelta.class);
          //          dao.alterTableColumnVarCharLength(propertyDeltaTable.getName(), "old_value", 20000);
          //          dao.alterTableColumnVarCharLength(propertyDeltaTable.getName(), "new_value", 20000);

          final Table employeeTable = new Table(EmployeeDO.class);
          databaseUpdateDao.renameTableAttribute(employeeTable.getName(), "wochenstunden", "old_weekly_working_hours");
          databaseUpdateDao.addTableAttributes(EmployeeDO.class, "weeklyWorkingHours");
          final List<DatabaseResultRow> rows = databaseUpdateDao
              .query("select pk, old_weekly_working_hours from t_fibu_employee");
          if (rows != null) {
            for (final DatabaseResultRow row : rows) {
              final Integer pk = (Integer) row.getEntry("pk").getValue();
              final Integer oldWeeklyWorkingHours = (Integer) row.getEntry("old_weekly_working_hours").getValue();
              if (oldWeeklyWorkingHours == null) {
                continue;
              }
              databaseUpdateDao.update("update t_fibu_employee set weekly_working_hours=? where pk=?",
                  new BigDecimal(oldWeeklyWorkingHours), pk);
            }
          }
        }
        if (databaseUpdateDao.doTableAttributesExist(GroupDO.class, "ldapValues") == false) {
          databaseUpdateDao.addTableAttributes(GroupDO.class, "ldapValues");
        }
        if (databaseUpdateDao.doTableAttributesExist(AuftragsPositionDO.class, "periodOfPerformanceType",
            "modeOfPaymentType") == false) {
          databaseUpdateDao.addTableAttributes(AuftragsPositionDO.class, "periodOfPerformanceType",
              "modeOfPaymentType");
        }
        if (databaseUpdateDao.doTableAttributesExist(AuftragDO.class, "periodOfPerformanceBegin",
            "periodOfPerformanceEnd") == false) {
          databaseUpdateDao.addTableAttributes(AuftragDO.class, "periodOfPerformanceBegin", "periodOfPerformanceEnd");
        }
        if (databaseUpdateDao.doEntitiesExist(PaymentScheduleDO.class) == false) {
          new SchemaGenerator(databaseUpdateDao).add(PaymentScheduleDO.class).createSchema();
          databaseUpdateDao.createMissingIndices();
        }
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 5.3
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "5.3", "2013-11-24",
        "Adds t_pf_user.last_password_change, t_pf_user.password_salt.")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(PFUserDO.class, "lastPasswordChange", "passwordSalt") == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(PFUserDO.class, "lastPasswordChange", "passwordSalt") == false) {
          databaseUpdateDao.addTableAttributes(PFUserDO.class, "lastPasswordChange", "passwordSalt");
        }
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 5.2
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(
        CORE_REGION_ID,
        "5.2",
        "2013-05-13",
        "Adds t_fibu_auftrag_position.time_of_performance_{start|end}, t_script.file{_name} and changes type of t_script.script{_backup} to byte[].")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(ScriptDO.class, "file", "filename") == true
            && databaseUpdateDao.doTableAttributesExist(AuftragsPositionDO.class, "periodOfPerformanceBegin",
                "periodOfPerformanceEnd") == true) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        }
        return UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(ScriptDO.class, "file", "filename") == false) {
          databaseUpdateDao.addTableAttributes(ScriptDO.class, "file", "filename");
          final Table scriptTable = new Table(ScriptDO.class);
          databaseUpdateDao.renameTableAttribute(scriptTable.getName(), "script", "old_script");
          databaseUpdateDao.renameTableAttribute(scriptTable.getName(), "scriptbackup", "old_script_backup");
          databaseUpdateDao.addTableAttributes(ScriptDO.class, "script", "scriptBackup");
          final List<DatabaseResultRow> rows = databaseUpdateDao
              .query("select pk, old_script, old_script_backup from t_script");
          if (rows != null) {
            for (final DatabaseResultRow row : rows) {
              final Integer pk = (Integer) row.getEntry("pk").getValue();
              final String oldScript = (String) row.getEntry("old_script").getValue();
              final String oldScriptBackup = (String) row.getEntry("old_script_backup").getValue();
              final ScriptDO script = new ScriptDO();
              script.setScriptAsString(oldScript);
              script.setScriptBackupAsString(oldScriptBackup);
              databaseUpdateDao.update("update t_script set script=?, script_backup=? where pk=?", script.getScript(),
                  script.getScriptBackup(), pk);
            }
          }
        }
        if (databaseUpdateDao.doTableAttributesExist(AuftragsPositionDO.class, "periodOfPerformanceBegin",
            "periodOfPerformanceEnd") == false) {
          databaseUpdateDao.addTableAttributes(AuftragsPositionDO.class, "periodOfPerformanceBegin",
              "periodOfPerformanceEnd");
        }
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 5.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, VERSION_5_0, "2013-02-15",
        "Adds t_fibu_rechnung.konto, t_pf_user.ssh_public_key, fixes contract.IN_PROGRES -> contract.IN_PROGRESS")
    {
      final Table rechnungTable = new Table(RechnungDO.class);

      final Table userTable = new Table(PFUserDO.class);

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        int entriesToMigrate = 0;
        if (databaseUpdateDao.isVersionUpdated(CORE_REGION_ID, VERSION_5_0) == false) {
          entriesToMigrate = databaseUpdateDao.queryForInt("select count(*) from t_contract where status='IN_PROGRES'");
        }
        return databaseUpdateDao.doTableAttributesExist(rechnungTable, "konto") == true //
            && databaseUpdateDao.doTableAttributesExist(userTable, "sshPublicKey") //
            && entriesToMigrate == 0 //
                ? UpdatePreCheckStatus.ALREADY_UPDATED : UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(rechnungTable, "konto") == false) {
          databaseUpdateDao.addTableAttributes(rechnungTable, new TableAttribute(RechnungDO.class, "konto"));
        }
        if (databaseUpdateDao.doTableAttributesExist(userTable, "sshPublicKey") == false) {
          databaseUpdateDao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "sshPublicKey"));
        }
        final int entriesToMigrate = databaseUpdateDao
            .queryForInt("select count(*) from t_contract where status='IN_PROGRES'");
        if (entriesToMigrate > 0) {
          databaseUpdateDao.execute("update t_contract set status='IN_PROGRESS' where status='IN_PROGRES'", true);
        }
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 4.3.1
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "4.3.1", "2013-01-29", "Adds t_fibu_projekt.konto")
    {
      final Table projektTable = new Table(ProjektDO.class);

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        return databaseUpdateDao.doTableAttributesExist(projektTable, "konto") == true //
            ? UpdatePreCheckStatus.ALREADY_UPDATED
            : UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(projektTable, "konto") == false) {
          databaseUpdateDao.addTableAttributes(projektTable, new TableAttribute(ProjektDO.class, "konto"));
        }
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 4.2
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(
        CORE_REGION_ID,
        "4.2",
        "2012-08-09",
        "Adds t_pf_user.authenticationToken|local_user|restricted_user|deactivated|ldap_values, t_group.local_group, t_fibu_rechnung|eingangsrechnung|auftrag(=incoming and outgoing invoice|order).ui_status_as_xml")
    {
      final Table userTable = new Table(PFUserDO.class);

      final Table groupTable = new Table(GroupDO.class);

      final Table outgoingInvoiceTable = new Table(RechnungDO.class);

      final Table incomingInvoiceTable = new Table(EingangsrechnungDO.class);

      final Table orderTable = new Table(AuftragDO.class);

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        return databaseUpdateDao.doTableAttributesExist(userTable, "authenticationToken", "localUser", "restrictedUser",
            "deactivated", "ldapValues") == true //
            && databaseUpdateDao.doTableAttributesExist(groupTable, "localGroup") == true
        // , "nestedGroupsAllowed", "nestedGroupIds") == true //
            && databaseUpdateDao.doTableAttributesExist(outgoingInvoiceTable, "uiStatusAsXml") == true //
            && databaseUpdateDao.doTableAttributesExist(incomingInvoiceTable, "uiStatusAsXml") == true //
            && databaseUpdateDao.doTableAttributesExist(orderTable, "uiStatusAsXml") == true //
                ? UpdatePreCheckStatus.ALREADY_UPDATED : UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(userTable, "authenticationToken") == false) {
          databaseUpdateDao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "authenticationToken"));
        }
        if (databaseUpdateDao.doTableAttributesExist(userTable, "localUser") == false) {
          databaseUpdateDao.addTableAttributes(userTable,
              new TableAttribute(PFUserDO.class, "localUser").setDefaultValue("false"));
        }
        if (databaseUpdateDao.doTableAttributesExist(userTable, "restrictedUser") == false) {
          databaseUpdateDao.addTableAttributes(userTable,
              new TableAttribute(PFUserDO.class, "restrictedUser").setDefaultValue("false"));
        }
        if (databaseUpdateDao.doTableAttributesExist(userTable, "deactivated") == false) {
          databaseUpdateDao.addTableAttributes(userTable,
              new TableAttribute(PFUserDO.class, "deactivated").setDefaultValue("false"));
        }
        if (databaseUpdateDao.doTableAttributesExist(userTable, "ldapValues") == false) {
          databaseUpdateDao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "ldapValues"));
        }
        if (databaseUpdateDao.doTableAttributesExist(groupTable, "localGroup") == false) {
          databaseUpdateDao.addTableAttributes(groupTable,
              new TableAttribute(GroupDO.class, "localGroup").setDefaultValue("false"));
        }
        // if (dao.doesTableAttributesExist(groupTable, "nestedGroupsAllowed") == false) {
        // dao.addTableAttributes(groupTable, new TableAttribute(GroupDO.class, "nestedGroupsAllowed").setDefaultValue("true"));
        // }
        // if (dao.doesTableAttributesExist(groupTable, "nestedGroupIds") == false) {
        // dao.addTableAttributes(groupTable, new TableAttribute(GroupDO.class, "nestedGroupIds"));
        // }
        if (databaseUpdateDao.doTableAttributesExist(outgoingInvoiceTable, "uiStatusAsXml") == false) {
          databaseUpdateDao.addTableAttributes(outgoingInvoiceTable,
              new TableAttribute(RechnungDO.class, "uiStatusAsXml"));
        }
        if (databaseUpdateDao.doTableAttributesExist(incomingInvoiceTable, "uiStatusAsXml") == false) {
          databaseUpdateDao.addTableAttributes(incomingInvoiceTable,
              new TableAttribute(EingangsrechnungDO.class, "uiStatusAsXml"));
        }
        if (databaseUpdateDao.doTableAttributesExist(orderTable, "uiStatusAsXml") == false) {
          databaseUpdateDao.addTableAttributes(orderTable, new TableAttribute(AuftragDO.class, "uiStatusAsXml"));
        }
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 4.1
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "4.1", "2012-04-21",
        "Adds t_pf_user.first_day_of_week and t_pf_user.hr_planning.")
    {
      final Table userTable = new Table(PFUserDO.class);

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        return databaseUpdateDao.doTableAttributesExist(userTable, "firstDayOfWeek", "hrPlanning") == true //
            ? UpdatePreCheckStatus.ALREADY_UPDATED
            : UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(userTable, "firstDayOfWeek") == false) {
          databaseUpdateDao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "firstDayOfWeek"));
        }
        if (databaseUpdateDao.doTableAttributesExist(userTable, "hrPlanning") == false) {
          databaseUpdateDao.addTableAttributes(userTable,
              new TableAttribute(PFUserDO.class, "hrPlanning").setDefaultValue("true"));
        }
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 4.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "4.0", "2012-04-18",
        "Adds 6th parameter to t_script and payment_type to t_fibu_eingangsrechnung.")
    {
      final Table scriptTable = new Table(ScriptDO.class);

      final Table eingangsrechnungTable = new Table(EingangsrechnungDO.class);

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        return databaseUpdateDao.doTableAttributesExist(scriptTable, "parameter6Name", "parameter6Type") == true //
            && databaseUpdateDao.doTableAttributesExist(eingangsrechnungTable, "paymentType") == true //
                ? UpdatePreCheckStatus.ALREADY_UPDATED : UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        if (databaseUpdateDao.doTableAttributesExist(scriptTable, "parameter6Name") == false) {
          databaseUpdateDao.addTableAttributes(scriptTable, new TableAttribute(ScriptDO.class, "parameter6Name"));
        }
        if (databaseUpdateDao.doTableAttributesExist(scriptTable, "parameter6Type") == false) {
          databaseUpdateDao.addTableAttributes(scriptTable, new TableAttribute(ScriptDO.class, "parameter6Type"));
        }
        if (databaseUpdateDao.doTableAttributesExist(eingangsrechnungTable, "paymentType") == false) {
          databaseUpdateDao.addTableAttributes(eingangsrechnungTable,
              new TableAttribute(EingangsrechnungDO.class, "paymentType"));
        }
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 3.6.2
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(
        CORE_REGION_ID,
        "3.6.1.3",
        "2011-12-05",
        "Adds columns t_kunde.konto_id, t_fibu_eingangsrechnung.konto_id, t_konto.status, t_task.protection_of_privacy and t_address.communication_language.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        final Table kundeTable = new Table(KundeDO.class);
        final Table eingangsrechnungTable = new Table(EingangsrechnungDO.class);
        final Table kontoTable = new Table(KontoDO.class);
        final Table taskTable = new Table(TaskDO.class);
        final Table addressTable = new Table(AddressDO.class);
        return databaseUpdateDao.doTableAttributesExist(kundeTable, "konto") == true //
            && databaseUpdateDao.doTableAttributesExist(eingangsrechnungTable, "konto") == true //
            && databaseUpdateDao.doTableAttributesExist(kontoTable, "status") == true //
            && databaseUpdateDao.doTableAttributesExist(addressTable, "communicationLanguage") == true //
            && databaseUpdateDao.doTableAttributesExist(taskTable, "protectionOfPrivacy") //
                ? UpdatePreCheckStatus.ALREADY_UPDATED : UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        final Table kundeTable = new Table(KundeDO.class);
        if (databaseUpdateDao.doTableAttributesExist(kundeTable, "konto") == false) {
          databaseUpdateDao.addTableAttributes(kundeTable, new TableAttribute(KundeDO.class, "konto"));
        }
        final Table eingangsrechnungTable = new Table(EingangsrechnungDO.class);
        if (databaseUpdateDao.doTableAttributesExist(eingangsrechnungTable, "konto") == false) {
          databaseUpdateDao.addTableAttributes(eingangsrechnungTable,
              new TableAttribute(EingangsrechnungDO.class, "konto"));
        }
        final Table kontoTable = new Table(KontoDO.class);
        if (databaseUpdateDao.doTableAttributesExist(kontoTable, "status") == false) {
          databaseUpdateDao.addTableAttributes(kontoTable, new TableAttribute(KontoDO.class, "status"));
        }
        final Table taskTable = new Table(TaskDO.class);
        if (databaseUpdateDao.doTableAttributesExist(taskTable, "protectionOfPrivacy") == false) {
          databaseUpdateDao.addTableAttributes(taskTable,
              new TableAttribute(TaskDO.class, "protectionOfPrivacy").setDefaultValue("false"));
        }
        final Table addressTable = new Table(AddressDO.class);
        if (databaseUpdateDao.doTableAttributesExist(addressTable, "communicationLanguage") == false) {
          databaseUpdateDao.addTableAttributes(addressTable,
              new TableAttribute(AddressDO.class, "communicationLanguage"));
        }
        databaseUpdateDao.createMissingIndices();
        return UpdateRunningStatus.DONE;
      }
    });

    // /////////////////////////////////////////////////////////////////
    // 3.5.4
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "3.5.4", "2011-02-24",
        "Adds table t_database_update. Adds attribute (excel_)date_format, hour_format_24 to table t_pf_user.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        final Table dbUpdateTable = new Table(DatabaseUpdateDO.class);
        final Table userTable = new Table(PFUserDO.class);
        return databaseUpdateDao.doExist(dbUpdateTable) == true
            && databaseUpdateDao.doTableAttributesExist(userTable, "dateFormat", "excelDateFormat",
                "timeNotation") == true //
                    ? UpdatePreCheckStatus.ALREADY_UPDATED : UpdatePreCheckStatus.READY_FOR_UPDATE;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        MyDatabaseUpdateService databaseUpdateDao = applicationContext.getBean(MyDatabaseUpdateService.class);
        final Table dbUpdateTable = new Table(DatabaseUpdateDO.class);
        final Table userTable = new Table(PFUserDO.class);
        dbUpdateTable.addAttributes("updateDate", "regionId", "versionString", "executionResult", "executedBy",
            "description");
        databaseUpdateDao.createTable(dbUpdateTable);
        databaseUpdateDao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "dateFormat"));
        databaseUpdateDao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "excelDateFormat"));
        databaseUpdateDao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "timeNotation"));
        databaseUpdateDao.createMissingIndices();
        TenantRegistryMap.getInstance().setAllUserGroupCachesAsExpired();
        //TODO: Lösung finden!!!
        //Registry.instance().getUserCache().setExpired();
        return UpdateRunningStatus.DONE;
      }
    });
    return list;
  }
}
