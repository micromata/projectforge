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

package org.projectforge.business.fibu;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.time.DateHolder;


/**
 * Repräsentiert einen Wochenbericht eines Mitarbeiters. Diese Wochenberichte sind dem MonthlyEmployeeReport zugeordnet.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MonthlyEmployeeReportWeek implements Serializable
{
  private static final long serialVersionUID = 6075755848054540114L;

  private Date fromDate;

  private int fromDayOfMonth;

  private Date toDate;

  private int toDayOfMonth;

  private int weekOfYear;

  private long totalDuration = 0;

  /**
   * Key is kost2 id.
   */
  private Map<Integer, MonthlyEmployeeReportEntry> kost2Entries = new HashMap<Integer, MonthlyEmployeeReportEntry>();

  /**
   * Key is task id.
   */
  private Map<Integer, MonthlyEmployeeReportEntry> taskEntries = new HashMap<Integer, MonthlyEmployeeReportEntry>();

  /**
   * ToDate will be set to end of week but not after the last day of month.
   * @param fromDate
   */
  public MonthlyEmployeeReportWeek(Date fromDate)
  {
    Validate.notNull(fromDate);
    this.fromDate = fromDate;
    DateHolder d1 = new DateHolder(fromDate);
    this.fromDayOfMonth = d1.getDayOfMonth();
    this.weekOfYear = d1.getWeekOfYear();
    d1.setEndOfMonth();
    DateHolder d2 = new DateHolder(fromDate);
    d2.setEndOfWeek();
    if (d1.getDate().before(d2.getDate()) == true) {
      this.toDate = d1.getDate();
      this.toDayOfMonth = d1.getDayOfMonth();
    } else {
      this.toDate = d2.getDate();
      this.toDayOfMonth = d2.getDayOfMonth();
    }
  }

  /**
   * Start time of sheet must be fromDate or later and before toDate.
   * @param sheet
   */
  public boolean matchWeek(TimesheetDO sheet)
  {
    return sheet.getStartTime().before(fromDate) == false && sheet.getStartTime().before(toDate) == true;
  }

  void addEntry(TimesheetDO sheet)
  {
    if (matchWeek(sheet) == false) {
      throw new RuntimeException("Oups, given time sheet is not inside the week represented by this week object.");
    }
    MonthlyEmployeeReportEntry entry;
    if (sheet.getKost2Id() != null) {
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
    totalDuration += duration;
  }

  public Date getFromDate()
  {
    return fromDate;
  }

  public Date getToDate()
  {
    return toDate;
  }

  public int getWeekOfYear()
  {
    return weekOfYear;
  }

  public int getFromDayOfMonth()
  {
    return fromDayOfMonth;
  }

  /**
   * @see StringHelper#format2DigitNumber(int)
   */
  public String getFormattedFromDayOfMonth()
  {
    return StringHelper.format2DigitNumber(fromDayOfMonth);
  }

  public int getToDayOfMonth()
  {
    return toDayOfMonth;
  }

  /**
   * @see StringHelper#format2DigitNumber(int)
   */
  public String getFormattedToDayOfMonth()
  {
    return StringHelper.format2DigitNumber(toDayOfMonth);
  }

  /** Summe aller Stunden der Woche in Millis. */
  public long getTotalDuration()
  {
    return totalDuration;
  }

  public String getFormattedTotalDuration()
  {
    return MonthlyEmployeeReport.getFormattedDuration(totalDuration);
  }

  /**
   * Return the hours assigned to the different Kost2's. The key of the map is the kost2 id.
   * @return
   */
  public Map<Integer, MonthlyEmployeeReportEntry> getKost2Entries()
  {
    return kost2Entries;
  }

  /**
   * Return the hours assigned to the different tasks which do not have a kost2-id. The key of the map is the task id.
   * @return
   */
  public Map<Integer, MonthlyEmployeeReportEntry> getTaskEntries()
  {
    return taskEntries;
  }
}
