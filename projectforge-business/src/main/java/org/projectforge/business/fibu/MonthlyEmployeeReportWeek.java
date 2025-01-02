/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu;

import org.apache.commons.lang3.Validate;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.time.PFDateTime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * Repräsentiert einen Wochenbericht eines Mitarbeiters. Diese Wochenberichte sind dem MonthlyEmployeeReport zugeordnet.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MonthlyEmployeeReportWeek implements Serializable {
  private static final long serialVersionUID = 6075755848054540114L;

  private PFDateTime fromDate;

  private PFDateTime toDate;

  private long totalDuration = 0;

  private long totalGrossDuration = 0;

  /**
   * Key is kost2 id.
   */
  private Map<Long, MonthlyEmployeeReportEntry> kost2Entries = new HashMap<>();

  /**
   * Key is task id.
   */
  private Map<Long, MonthlyEmployeeReportEntry> taskEntries = new HashMap<>();

  /**
   * FromDate will be set to the begin of week but not before first day of month.
   * ToDate will be set to end of week but not after the last day of month.
   *
   * @param date
   */
  public MonthlyEmployeeReportWeek(PFDateTime date) {
    Validate.notNull(date);
    this.fromDate = date.getBeginOfWeek();
    if (this.fromDate.getMonth() != date.getMonth()) {
      this.fromDate = date.getBeginOfMonth();
    }
    this.toDate = fromDate.getEndOfWeek();
    if (this.toDate.getMonth() != this.fromDate.getMonth()) {
      this.toDate = this.fromDate.getEndOfMonth();
    }
  }

  /**
   * Start time of sheet must be fromDate or later and before toDate.
   *
   * @param sheet
   */
  public boolean matchWeek(TimesheetDO sheet) {
    return !sheet.getStartTime().before(fromDate.getUtilDate()) && sheet.getStartTime().before(toDate.getUtilDate());
  }

  void addEntry(TimesheetDO sheet, final boolean hasSelectAccess) {
    if (!matchWeek(sheet)) {
      throw new RuntimeException("Oups, given time sheet is not inside the week represented by this week object.");
    }
    MonthlyEmployeeReportEntry entry;
    if (!hasSelectAccess) {
      entry = taskEntries.get(MonthlyEmployeeReport.MAGIC_PSEUDO_TASK_ID); // -42 represents timesheets without access.
      if (entry == null) {
        entry = new MonthlyEmployeeReportEntry(MonthlyEmployeeReport.createPseudoTask());
        taskEntries.put(MonthlyEmployeeReport.MAGIC_PSEUDO_TASK_ID, entry);
      }
    } else if (sheet.getKost2Id() != null) {
      entry = kost2Entries.get(sheet.getKost2Id());
      if (entry == null) {
        entry = new MonthlyEmployeeReportEntry(sheet.getKost2());
        kost2Entries.put(sheet.getKost2Id(), entry);
      }
    } else {
      entry = taskEntries.get(sheet.getTaskId());
      if (entry == null) {
        entry = new MonthlyEmployeeReportEntry(sheet.getTask());
        taskEntries.put(sheet.getTaskId(), entry);
      }
    }
    long duration = sheet.getDuration();
    entry.addMillis(duration);
    totalDuration += entry.getWorkFractionMillis();
    totalGrossDuration += duration;
  }

  public String getFormattedFromDayOfMonth() {
    return StringHelper.format2DigitNumber(fromDate.getDayOfMonth());
  }

  public String getFormattedToDayOfMonth() {
    return StringHelper.format2DigitNumber(toDate.getDayOfMonth());
  }

  /**
   * Summe aller Stunden der Woche in Millis.
   */
  public long getTotalDuration() {
    return totalDuration;
  }

  public String getFormattedTotalDuration() {
    return MonthlyEmployeeReport.getFormattedDuration(totalDuration);
  }

  public String getFormattedGrossDuration() {
    return MonthlyEmployeeReport.getFormattedDuration(totalGrossDuration);
  }

  /**
   * Return the hours assigned to the different Kost2's. The key of the map is the kost2 id.
   *
   * @return
   */
  public Map<Long, MonthlyEmployeeReportEntry> getKost2Entries() {
    return kost2Entries;
  }

  /**
   * Return the hours assigned to the different tasks which do not have a kost2-id. The key of the map is the task id.
   *
   * @return
   */
  public Map<Long, MonthlyEmployeeReportEntry> getTaskEntries() {
    return taskEntries;
  }
}
