/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.genome.jpa.metainf.EntityMetadata;
import de.micromata.genome.jpa.metainf.JpaMetadataEntityNotFoundException;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.RRule;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.image.ImageService;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.continuousdb.*;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.attr.impl.InternalAttrSchemaConstants;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.*;
import org.springframework.context.ApplicationContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @deprecated Since version 6.18.0 please use flyway db migration.
 */
@Deprecated
public class DatabaseCoreUpdates
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseCoreUpdates.class);

  public static final String CORE_REGION_ID = "ProjectForge";

  private static final String VERSION_5_0 = "5.0";

  private static ApplicationContext applicationContext;
  private static String RESTART_RQUIRED = "no";

  static void setApplicationContext(final ApplicationContext applicationContext)
  {
    DatabaseCoreUpdates.applicationContext = applicationContext;
  }

  @SuppressWarnings("serial")
  public static List<UpdateEntry> getUpdateEntries()
  {
    final DatabaseService databaseService = applicationContext.getBean(DatabaseService.class);
    final PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);
    final TenantDao tenantDao = applicationContext.getBean(TenantDao.class);

    final List<UpdateEntry> list = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////
    // 6.17.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.17.0", "2017-08-16",
        "Add uid to addresses.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.16.0");
        if (!addressHasUid()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!addressHasUid()) {
          databaseService.updateSchema();
          List<DatabaseResultRow> resultList = databaseService.query("select pk from t_address where uid is null");
          for (DatabaseResultRow row : resultList) {
            Integer id = (Integer) row.getEntry(0).getValue();
            UUID uid = UUID.randomUUID();
            String sql = "UPDATE t_address SET uid = '" + uid.toString() + "' WHERE pk = " + id;
            databaseService.execute(sql);
          }
        }
        return UpdateRunningStatus.DONE;
      }

      private boolean addressHasUid()
      {
        if (!databaseService.doesTableAttributeExist("t_address", "uid")) {
          return false;
        }

        if (databaseService.query("select * from t_address LIMIT 1").size() == 0) {
          return true;
        }

        return databaseService.query("select * from t_address where uid is not null LIMIT 1").size() > 0;
      }
    });

    ////////////////////////////////////////////////////////////////////
    // 6.16.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.16.0", "2017-08-01",
        "Remove unique constraints from EmployeeTimedAttrDO and EmployeeConfigurationTimedAttrDO. Add thumbnail for address images. Add addressbooks, remove tasks from addresses.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.16.0");
        if (oldUniqueConstraint() || isImageDataPreviewMissing() || !checkForAddresses()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        // update unique constraint
        if (oldUniqueConstraint()) {
          String uniqueConstraint1 = databaseService.getUniqueConstraintName("t_fibu_employee_timedattr", "parent", "propertyName");
          String uniqueConstraint2 = databaseService.getUniqueConstraintName("T_PLUGIN_EMPLOYEE_CONFIGURATION_TIMEDATTR", "parent", "propertyName");

          if (uniqueConstraint1 != null) {
            databaseService.execute("ALTER TABLE t_fibu_employee_timedattr DROP CONSTRAINT " + uniqueConstraint1);
          }

          if (uniqueConstraint2 != null) {
            databaseService.execute("ALTER TABLE T_PLUGIN_EMPLOYEE_CONFIGURATION_TIMEDATTR DROP CONSTRAINT " + uniqueConstraint2);
          }
        }
        if (isImageDataPreviewMissing()) {
          final ImageService imageService = applicationContext.getBean(ImageService.class);
          databaseService.updateSchema();
          List<DatabaseResultRow> resultList = databaseService.query("select pk, imagedata from t_address where imagedata is not null");
          log.info("Found: " + resultList.size() + " event entries to update imagedata.");

          String sql = "UPDATE t_address SET image_data_preview = ?1 WHERE pk = ?2";
          PreparedStatement ps = null;
          try {
            ps = databaseService.getDataSource().getConnection().prepareStatement(sql);

            for (DatabaseResultRow row : resultList) {
              Integer id = (Integer) row.getEntry(0).getValue();
              byte[] imageDataPreview = imageService.resizeImage((byte[]) row.getEntry(1).getValue());
              try {
                ps.setInt(2, id);
                ps.setBytes(1, imageDataPreview);
                ps.executeUpdate();
              } catch (Exception e) {
                log.error(String.format("Error while updating event with id '%s' and new imageData. Ignoring it.", id, imageDataPreview));
              }
            }
            ps.close();
          } catch (SQLException e) {
            log.error("Error while updating imageDataPreview in Database : " + e.getMessage());
          }
        }

        //Add addressbook, remove task from addresses
        if (!checkForAddresses()) {
          databaseService.updateSchema();
          String taskUniqueConstraint = databaseService.getUniqueConstraintName("t_address", "task_id");
          if (!StringUtils.isBlank(taskUniqueConstraint)) {
            databaseService.execute("ALTER TABLE t_address DROP CONSTRAINT " + taskUniqueConstraint);
          }
          databaseService.execute("ALTER TABLE t_address DROP COLUMN task_id");
          databaseService.insertGlobalAddressbook();
          List<DatabaseResultRow> addressIds = databaseService.query("SELECT pk FROM t_address");
          addressIds.forEach(addressId -> {
            databaseService
                .execute("INSERT INTO t_addressbook_address (address_id, addressbook_id) VALUES (" + addressId.getEntry(0).getValue() + ", "
                    + AddressbookDao.GLOBAL_ADDRESSBOOK_ID + ")");
          });
          databaseService.execute("DELETE FROM t_configuration WHERE parameter = 'defaultTask4Addresses'");
        }

        return UpdateRunningStatus.DONE;
      }

      private boolean isImageDataPreviewMissing()
      {
        return !databaseService.doesTableAttributeExist("t_address", "image_data_preview") ||
            ((Long) databaseService.query("select count(*) from t_address where imagedata is not null AND imagedata != ''").get(0).getEntry(0).getValue())
                > 0L
                && databaseService.query("select pk from t_address where imagedata is not null AND image_data_preview is not null LIMIT 1").size() < 1;
      }

      private boolean oldUniqueConstraint()
      {
        return databaseService.doesUniqueConstraintExists("t_fibu_employee_timedattr", "parent", "propertyName")
            || databaseService.doesUniqueConstraintExists("T_PLUGIN_EMPLOYEE_CONFIGURATION_TIMEDATTR", "parent", "propertyName");
      }

      private boolean checkForAddresses()
      {
        return databaseService.doesTableExist("T_ADDRESSBOOK") && databaseService.query("select * from t_addressbook where pk = 1").size() > 0;
      }
    });

    ////////////////////////////////////////////////////////////////////
    // 6.15.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.15.0", "2017-07-19",
        "Add fields to event and event attendee table. Change unique constraint in event table. Refactoring invoice template.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.15.0");
        if (!hasRefactoredInvoiceFields()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        if (this.missingFields() || oldUniqueConstraint() || noOwnership() || dtStampMissing()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!hasRefactoredInvoiceFields()) {
          databaseService.updateSchema();
          migrateCustomerRef();
        }

        // update unique constraint
        if (oldUniqueConstraint()) {
          databaseService.execute("ALTER TABLE T_PLUGIN_CALENDAR_EVENT DROP CONSTRAINT unique_t_plugin_calendar_event_uid");
          databaseService.updateSchema();
        }

        // add missing fields
        if (missingFields()) {
          databaseService.updateSchema();
        }

        // check ownership
        if (noOwnership()) {
          List<DatabaseResultRow> resultList = databaseService.query("select e.pk, e.organizer from t_plugin_calendar_event e");
          log.info("Found: " + resultList.size() + " event entries to update ownership.");

          for (DatabaseResultRow row : resultList) {
            Integer id = (Integer) row.getEntry(0).getValue();
            String organizer = (String) row.getEntry(1).getValue();
            Boolean ownership = Boolean.TRUE;

            if (organizer != null && !organizer.equals("mailto:null")) {
              ownership = Boolean.FALSE;
            }

            try {
              databaseService.execute(String.format("UPDATE t_plugin_calendar_event SET ownership = '%s' WHERE pk = %s", ownership, id));
            } catch (Exception e) {
              log.error(String.format("Error while updating event with id '%s' and new ownership. Ignoring it.", id, ownership));
            }

            log.info(String.format("Updated event with id '%s' set ownership to '%s'", id, ownership));
          }
          log.info("Ownership computation DONE.");
        }

        // update DT_STAMP
        if (dtStampMissing()) {
          try {
            databaseService.execute("UPDATE t_plugin_calendar_event SET dt_stamp = last_update");
            log.info("Creating DT_STAMP values successful");
          } catch (Exception e) {
            log.error("Error while creating DT_STAMP values");
          }
        }

        return UpdateRunningStatus.DONE;
      }

      private boolean missingFields()
      {
        return !databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT_ATTENDEE", "COMMON_NAME")
            || !databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT_ATTENDEE", "CU_TYPE")
            || !databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT_ATTENDEE", "RSVP")
            || !databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT_ATTENDEE", "ROLE")
            || !databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT_ATTENDEE", "ADDITIONAL_PARAMS")
            || !databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT", "OWNERSHIP")
            || !databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT", "ORGANIZER_ADDITIONAL_PARAMS")
            || !databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT", "DT_STAMP");
      }

      private boolean oldUniqueConstraint()
      {
        return databaseService.doesUniqueConstraintExists("T_PLUGIN_CALENDAR_EVENT", "unique_t_plugin_calendar_event_uid");
      }

      private boolean noOwnership()
      {
        List<DatabaseResultRow> resultAll = databaseService.query("select pk from t_plugin_calendar_event LIMIT 1");

        if (resultAll.size() == 0) {
          return false;
        }

        List<DatabaseResultRow> result = databaseService.query("select pk from t_plugin_calendar_event where ownership is not null LIMIT 1");
        return result.size() == 0;
      }

      private boolean dtStampMissing()
      {
        List<DatabaseResultRow> resultAll = databaseService.query("select pk from t_plugin_calendar_event LIMIT 1");

        if (resultAll.size() == 0) {
          return false;
        }

        List<DatabaseResultRow> result = databaseService.query("select pk from t_plugin_calendar_event where dt_stamp is not null LIMIT 1");
        return result.size() == 0;
      }

      private void migrateCustomerRef()
      {
        //Migrate customer ref 1 & 2
        List<DatabaseResultRow> resultSet = databaseService.query("SELECT pk, customerref1, customerref2 FROM t_fibu_rechnung");

        for (DatabaseResultRow row : resultSet) {
          String pk = row.getEntry(0) != null && row.getEntry(0).getValue() != null ? row.getEntry(0).getValue().toString() : null;
          if (pk != null) {
            String cr1 = row.getEntry(1) != null && row.getEntry(1).getValue() != null ? row.getEntry(1).getValue().toString() : "";
            String cr2 = row.getEntry(2) != null && row.getEntry(2).getValue() != null ? row.getEntry(2).getValue().toString() : "";
            String newCr = "";
            if (!StringUtils.isEmpty(cr1) && !StringUtils.isEmpty(cr2)) {
              newCr = cr1 + "\r\n" + cr2;
            } else if (!StringUtils.isEmpty(cr1) && StringUtils.isEmpty(cr2)) {
              newCr = cr1;
            } else if (StringUtils.isEmpty(cr1) && !StringUtils.isEmpty(cr2)) {
              newCr = cr2;
            }
            databaseService.execute("UPDATE t_fibu_rechnung SET customerref1 = '" + newCr + "' WHERE pk = " + pk);
          }
        }
        databaseService.execute("ALTER TABLE t_fibu_rechnung DROP COLUMN customerref2");
      }

      private boolean hasRefactoredInvoiceFields()
      {
        return databaseService.doesTableAttributeExist("t_fibu_rechnung", "attachment")
            && !databaseService.doesTableAttributeExist("t_fibu_rechnung", "customerref2");
      }
    });

    ////////////////////////////////////////////////////////////////////
    // 6.13.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.13.0", "2017-06-21",
        "Correct error in until date of recurring events. Add fields to invoice DO.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.13.0");
        if (!hasNewInvoiceFields() || hasBadUntilDate()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!hasNewInvoiceFields()) {
          databaseService.updateSchema();
        }

        if (hasBadUntilDate()) {
          Calendar calUntil = new GregorianCalendar(DateHelper.UTC);
          Calendar calStart = new GregorianCalendar(DateHelper.UTC);

          List<DatabaseResultRow> resultList = databaseService
              .query(
                  "select e.pk, e.start_date, e.recurrence_rule, e.recurrence_until, u.time_zone from t_plugin_calendar_event e, t_pf_user u where e.team_event_fk_creator = u.pk and e.recurrence_until is not null and all_day = false and to_char(recurrence_until, 'hh24:mi:ss') = '00:00:00'");
          log.info("Found: " + resultList.size() + " event entries to update until date.");

          for (DatabaseResultRow row : resultList) {
            Integer id = (Integer) row.getEntry(0).getValue();
            Date startDate = (Date) row.getEntry(1).getValue();
            String rruleStr = (String) row.getEntry(2).getValue();
            Date untilDate = (Date) row.getEntry(3).getValue();
            String timeZoneString = (String) row.getEntry(4).getValue();

            if (startDate == null || rruleStr == null || untilDate == null) {

              log.warn(String
                  .format("Processing event with id '%s', start date '%s', RRule '%s', and until date '%s' failed. Invalid data.",
                      id, startDate, rruleStr, untilDate));
              continue;
            }

            log.debug(String.format("Processing event with id '%s', start date '%s', RRule '%s', until date '%s', and timezone '%s'",
                id, startDate, rruleStr, untilDate, timeZoneString));

            TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
            if (timeZone == null) {
              timeZone = DateHelper.UTC;
            }

            calUntil.clear();
            calStart.clear();
            calUntil.setTimeZone(timeZone);
            calStart.setTimeZone(timeZone);

            // start processing
            calUntil.setTime(untilDate);
            calStart.setTime(startDate);

            // update date of start date to until date
            calStart.set(Calendar.YEAR, calUntil.get(Calendar.YEAR));
            calStart.set(Calendar.DAY_OF_YEAR, calUntil.get(Calendar.DAY_OF_YEAR));

            // add 23:59:59 to event start (next possible event time is +24h, 1 day)
            calStart.set(Calendar.HOUR_OF_DAY, 23);
            calStart.set(Calendar.MINUTE, 59);
            calStart.set(Calendar.SECOND, 59);
            calStart.set(Calendar.MILLISECOND, 0);

            // update recur until
            DateTime untilICal4J = new DateTime(calStart.getTime());
            untilICal4J.setUtc(true);
            RRule rRule = ICal4JUtils.calculateRRule(rruleStr);
            rRule.getRecur().setUntil(untilICal4J);

            try {
              databaseService
                  .execute(String.format("UPDATE t_plugin_calendar_event SET recurrence_rule = '%s', recurrence_until = '%s' WHERE pk = %s",
                      rRule.getValue(), DateHelper.formatIsoTimestamp(calStart.getTime()), id));
            } catch (Exception e) {
              log.error(String.format("Error while updating event with id '%s' and new recurrence_rule '%s', recurrence_until '%s'. Ignoring it.",
                  id, rRule.getValue(), DateHelper.formatIsoTimestamp(calStart.getTime())));
            }

            log.info(String.format("Updated event with id '%s' from '%s' to '%s'",
                id, DateHelper.formatIsoTimestamp(untilDate), DateHelper.formatIsoTimestamp(calStart.getTime())));
          }
          log.info("Until date migration is DONE.");
        }
        return UpdateRunningStatus.DONE;
      }

      private boolean hasNewInvoiceFields()
      {
        return databaseService.doesTableAttributeExist("t_fibu_rechnung", "customerref1") &&
            databaseService.doesTableAttributeExist("t_fibu_rechnung", "customeraddress");
      }

      private boolean hasBadUntilDate()
      {
        List<DatabaseResultRow> result = databaseService.query(
            "select pk from t_plugin_calendar_event where recurrence_until is not null and to_char(recurrence_until, 'hh24:mi:ss') = '00:00:00' and all_day = false LIMIT 1");
        return result.size() > 0;
      }
    });

    ////////////////////////////////////////////////////////////////////
    // 6.12.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.12.0", "2017-05-22",
        "Correct calendar exdates. Change address image data to AddressDO.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.12.0");
        if (hasISODates() || hasOldImageData()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (hasOldImageData()) {
          databaseService.updateSchema();
          migrateImageData();
          deleteImageHistoryData();
          deleteImageAddressAttrData();
          log.info("Address image data migration DONE.");
        }

        if (hasISODates()) {
          SimpleDateFormat iCalFormatterWithTime = new SimpleDateFormat(DateFormats.ICAL_DATETIME_FORMAT);
          SimpleDateFormat iCalFormatterAllDay = new SimpleDateFormat(DateFormats.COMPACT_DATE);
          List<SimpleDateFormat> formatterPatterns = Arrays
              .asList(new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_SECONDS), new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES),
                  new SimpleDateFormat(DateFormats.ISO_DATE), iCalFormatterWithTime, iCalFormatterAllDay);
          List<DatabaseResultRow> resultList = databaseService
              .query(
                  "SELECT pk, recurrence_ex_date, all_day FROM t_plugin_calendar_event te WHERE te.recurrence_ex_date IS NOT NULL AND te.recurrence_ex_date <> ''");
          log.info("Found: " + resultList.size() + " event entries to update.");
          for (DatabaseResultRow row : resultList) {
            Integer id = (Integer) row.getEntry(0).getValue();
            String exDateList = (String) row.getEntry(1).getValue();
            Boolean allDay = (Boolean) row.getEntry(2).getValue();
            log.debug("Event with id: " + id + " has exdate value: " + exDateList);
            String[] exDateArray = exDateList.split(",");
            List<String> finalExDates = new ArrayList<>();
            for (String exDateOld : exDateArray) {
              Date oldDate = null;
              for (SimpleDateFormat sdf : formatterPatterns) {
                try {
                  oldDate = sdf.parse(exDateOld);
                  break;
                } catch (ParseException e) {
                  if (log.isDebugEnabled()) {
                    log.debug("Date not parsable. Try another parser.");
                  }
                }
              }
              if (oldDate == null) {
                log.error("Date not parsable. Ignoring it: " + exDateOld);
                continue;
              }
              if (allDay != null && allDay) {
                finalExDates.add(iCalFormatterAllDay.format(oldDate));
              } else {
                finalExDates.add(iCalFormatterWithTime.format(oldDate));
              }
            }
            String newExDateValue = String.join(",", finalExDates);
            try {
              databaseService.execute("UPDATE t_plugin_calendar_event SET recurrence_ex_date = '" + newExDateValue + "' WHERE pk = " + id);
            } catch (Exception e) {
              log.error("Error while updating event with id: " + id + " and new exdatevalue: " + newExDateValue + " . Ignoring it.");
            }
          }
          log.info("Exdate migration DONE.");
        }
        return UpdateRunningStatus.DONE;
      }

      private boolean hasOldImageData()
      {
        return !databaseService.doesTableAttributeExist("T_ADDRESS", "imagedata")
            || databaseService.query("SELECT pk FROM t_address_attr WHERE propertyname = 'profileImageData' limit 1").size() > 0
            || databaseService.query("SELECT pk FROM t_pf_history_attr WHERE propertyname LIKE '%attrs.profileImageData%' limit 1").size() > 0;
      }

      private boolean hasISODates()
      {
        List<DatabaseResultRow> result = databaseService.query("SELECT * FROM T_PLUGIN_CALENDAR_EVENT WHERE recurrence_ex_date LIKE '%-%' LIMIT 1");
        return result.size() > 0;
      }

      private void deleteImageAddressAttrData()
      {
        List<DatabaseResultRow> attrResultList = databaseService.query("SELECT pk FROM t_address_attr WHERE propertyname = 'profileImageData'");
        for (DatabaseResultRow attrRow : attrResultList) {
          Integer attrId = (Integer) attrRow.getEntry(0).getValue();
          databaseService.execute("DELETE FROM t_address_attrdata WHERE parent_id = " + attrId);
          databaseService.execute("DELETE FROM t_address_attr WHERE pk = " + attrId);
        }
      }

      private void deleteImageHistoryData()
      {
        List<DatabaseResultRow> histAttrResultList = databaseService
            .query("SELECT pk FROM t_pf_history_attr WHERE propertyname LIKE '%attrs.profileImageData%'");
        for (DatabaseResultRow histAttrRow : histAttrResultList) {
          Long histAttrId = (Long) histAttrRow.getEntry(0).getValue();
          databaseService.execute("DELETE FROM t_pf_history_attr_data WHERE parent_pk = " + histAttrId);
          databaseService.execute("DELETE FROM t_pf_history_attr WHERE pk = " + histAttrId);
        }
      }

      private void migrateImageData()
      {
        AddressDao addressDao = applicationContext.getBean(AddressDao.class);
        List<AddressDO> allAddresses = addressDao.internalLoadAll();
        for (AddressDO ad : allAddresses) {
          byte[] imageData = ad.getAttribute("profileImageData", byte[].class);
          if (imageData != null && imageData.length > 0) {
            final PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);
            emf.runInTrans(emgr -> {
              AddressDO addressDO = emgr.selectByPkAttached(AddressDO.class, ad.getId());
              addressDO.setImageData(imageData);
              emgr.update(addressDO);
              return null;
            });
          }
        }
      }
    });

    ////////////////////////////////////////////////////////////////////
    // 6.11.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.11.0", "2017-05-03",
        "Add discounts and konto informations. Add period of performance to invoices.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.11.0");
        if (isSchemaUpdateNecessary()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (isSchemaUpdateNecessary()) {
          databaseService.updateSchema();
        }
        return UpdateRunningStatus.DONE;
      }

      private boolean isSchemaUpdateNecessary()
      {
        return !databaseService.doesTableAttributeExist("t_fibu_eingangsrechnung", "discountmaturity")
            || !databaseService.doesTableAttributeExist("t_fibu_rechnung", "discountmaturity")
            || !databaseService.doesTableAttributeExist("t_fibu_eingangsrechnung", "customernr")
            || !databaseService.doTableAttributesExist(RechnungDO.class, "periodOfPerformanceBegin", "periodOfPerformanceEnd")
            || !databaseService.doTableAttributesExist(RechnungsPositionDO.class, "periodOfPerformanceType", "periodOfPerformanceBegin",
            "periodOfPerformanceEnd");
      }
    });

    ////////////////////////////////////////////////////////////////////
    // 6.10.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.10.0", "2017-04-11",
        "Add column position_number to table T_FIBU_PAYMENT_SCHEDULE.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.10.0");
        if (!databaseService.doesTableAttributeExist("T_FIBU_PAYMENT_SCHEDULE", "position_number")) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!databaseService.doesTableAttributeExist("T_FIBU_PAYMENT_SCHEDULE", "position_number")) {
          databaseService.updateSchema();
        }
        return UpdateRunningStatus.DONE;
      }

    });

    ////////////////////////////////////////////////////////////////////
    // 6.9.0
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "6.9.0", "2017-03-15",
        "Allow multiple substitutions on application for leave.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        log.info("Running pre-check for ProjectForge version 6.9.0");
        if (!databaseService.doesTableExist("t_employee_vacation_substitution") ||
            databaseService.doesTableAttributeExist("t_employee_vacation", "substitution_id") ||
            uniqueConstraintMissing()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        final Optional<Boolean> isColumnNullable = databaseService.isColumnNullable("T_PLUGIN_CALENDAR_EVENT", "UID");
        if (!isColumnNullable.isPresent() || isColumnNullable.get()) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }

        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!databaseService.doesTableExist("t_employee_vacation_substitution") || uniqueConstraintMissing()) {
          if (doesDuplicateUidsExists()) {
            handleDuplicateUids();
          }
          // Updating the schema
          databaseService.updateSchema();
        }

        final Optional<Boolean> isColumnNullable = databaseService.isColumnNullable("T_PLUGIN_CALENDAR_EVENT", "UID");
        if (!isColumnNullable.isPresent() || isColumnNullable.get()) {
          databaseService.execute("ALTER TABLE t_plugin_calendar_event ALTER COLUMN uid SET NOT NULL;");
        }

        if (databaseService.doesTableAttributeExist("t_employee_vacation", "substitution_id")) {
          migrateSubstitutions();
          // drop old substitution column
          databaseService.dropTableAttribute("t_employee_vacation", "substitution_id");
        }

        return UpdateRunningStatus.DONE;
      }

      private void handleDuplicateUids()
      {
        final PfEmgrFactory emf = applicationContext.getBean(PfEmgrFactory.class);
        emf.runInTrans(emgr -> {
          List<DatabaseResultRow> resultSet = databaseService
              .query("SELECT uid, COUNT(*) FROM t_plugin_calendar_event GROUP BY uid HAVING COUNT(*) > 1");
          for (DatabaseResultRow resultLine : resultSet) {
            List<TeamEventDO> teList = emgr
                .selectAttached(TeamEventDO.class, "SELECT t FROM TeamEventDO t WHERE t.uid = :uid", "uid", resultLine.getEntry(0).getValue());
            for (TeamEventDO te : teList) {
              te.setUid(TeamCalConfig.get().createEventUid());
              emgr.update(te);
            }
          }
          return null;
        });
      }

      private boolean doesDuplicateUidsExists()
      {
        List<DatabaseResultRow> resultSet = databaseService.query("SELECT uid, COUNT(*) FROM t_plugin_calendar_event GROUP BY uid HAVING COUNT(*) > 1");
        return resultSet != null && resultSet.size() > 0;
      }

      // migrate from old substitution column to new t_employee_vacation_substitution table
      private void migrateSubstitutions()
      {
        final VacationDao vacationDao = applicationContext.getBean(VacationDao.class);
        final EmployeeDao employeeDao = applicationContext.getBean(EmployeeDao.class);

        final List<DatabaseResultRow> resultRows = databaseService
            .query("SELECT pk, substitution_id FROM t_employee_vacation WHERE substitution_id IS NOT NULL;");

        for (final DatabaseResultRow row : resultRows) {
          final int vacationId = (int) row.getEntry("pk").getValue();
          final int substitutionId = (int) row.getEntry("substitution_id").getValue();
          final VacationDO vacation = vacationDao.internalGetById(vacationId);
          final EmployeeDO substitution = employeeDao.internalGetById(substitutionId);
          throw new UnsupportedOperationException("Migration from 6.9.0 isn't supported since version 7.0. Try to update from 6.10+ first.");
          //vacation.getSubstitutions().add(substitution); // Substitutions are not available anymore!
          //vacationDao.internalUpdate(vacation);
        }
      }

      private boolean uniqueConstraintMissing()
      {
        return !(databaseService.doesUniqueConstraintExists("T_PLUGIN_CALENDAR_EVENT", "unique_t_plugin_calendar_event_uid")
            || databaseService.doesUniqueConstraintExists("T_PLUGIN_CALENDAR_EVENT", "unique_t_plugin_calendar_event_uid_calendar_fk"));
      }
    });
    return list;
  }

  private static void uniqueConstraintWorkaround(final DatabaseService databaseService, final PfEmgrFactory emf)
  {
    EntityMetadata pce;

    try {
      pce = emf.getMetadataRepository().getEntityMetaDataBySimpleClassName("TeamEventDO");
    } catch (JpaMetadataEntityNotFoundException e) {
      log.error("No JPA class found for TeamEventDO");
      pce = null;
    }

    if (!databaseService.doesTableAttributeExist("T_PLUGIN_CALENDAR_EVENT", "uid") && pce != null) {
      // required workaround, because null values are not accepted
      final String type = databaseService.getAttribute(pce.getJavaType(), "uid");
      final String command1 = String.format("ALTER TABLE T_PLUGIN_CALENDAR_EVENT ADD COLUMN UID %s DEFAULT 'default value'", type);

      databaseService.execute(command1);
      databaseService.execute("ALTER TABLE T_PLUGIN_CALENDAR_EVENT ALTER COLUMN UID SET NOT NULL");
      databaseService.execute("ALTER TABLE T_PLUGIN_CALENDAR_EVENT ALTER COLUMN UID DROP DEFAULT");
    }
  }

  public static void migrateEmployeeStatusToAttr()
  {
    final EmployeeService employeeService = applicationContext.getBean(EmployeeService.class);
    final EmployeeDao employeeDao = applicationContext.getBean(EmployeeDao.class);

    final List<EmployeeDO> employees = employeeDao.internalLoadAll();
    employees.forEach(employee -> {
      final EmployeeStatus status = employee.getStatus();
      if (status != null) {
        final EmployeeTimedDO newAttrRow = employeeService.addNewTimeAttributeRow(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME);
        newAttrRow.setStartTime(getDateForStatus(employee));
        newAttrRow.putAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME, status.getI18nKey());
        employeeDao.internalUpdate(employee);
      }
    });
  }

  private static Date getDateForStatus(final EmployeeDO employee)
  {
    // At first try to find the last change of the employee status in the history ...
    final Optional<Date> lastChange = findLastChangeOfEmployeeStatusInHistory(employee);
    if (lastChange.isPresent()) {
      // convert date from UTC to current zone date
      final TimeZone utc = TimeZone.getTimeZone("UTC");
      final TimeZone currentTimeZone = Configuration.getInstance().getDefaultTimeZone();
      final Date dateInCurrentTimezone = convertDateIntoOtherTimezone(lastChange.get(), utc, currentTimeZone);
      return PFDateTimeUtils.getUTCBeginOfDay(dateInCurrentTimezone);
    }

    // ... if there is nothing in the history, then use the entrittsdatum ...
    final LocalDate eintrittsDatum = employee.getEintrittsDatum();
    if (eintrittsDatum != null) {
      return PFDay.from(eintrittsDatum).getSqlDate();
    }

    // ... if there is no eintrittsdatum, use the current date.
    return PFDateTime.now().getBeginOfDay().getUtilDate();
  }

  private static Date convertDateIntoOtherTimezone(final Date date, final TimeZone from, final TimeZone to) {
    final Instant instant = date.toInstant();
    final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, to.toZoneId());
    final Instant instant2 = localDateTime.toInstant(from.toZoneId().getRules().getOffset(instant));
    return Date.from(instant2);
  }

  private static Optional<Date> findLastChangeOfEmployeeStatusInHistory(final EmployeeDO employee)
  {
    final Predicate<HistoryEntry> hasStatusChangeHistoryEntries = historyEntry ->
        ((HistoryEntry<?>) historyEntry)
            .getDiffEntries()
            .stream()
            .anyMatch(
                diffEntry -> diffEntry.getPropertyName().startsWith("status")
            );

    return HistoryBaseDaoAdapter
        .getHistoryEntries(employee)
        .stream()
        .filter(hasStatusChangeHistoryEntries)
        .map(HistoryEntry::getModifiedAt)
        .findFirst(); // the history entries are already sorted by date
  }
}
