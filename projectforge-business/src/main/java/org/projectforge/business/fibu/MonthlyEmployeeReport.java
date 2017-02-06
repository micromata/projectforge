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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.formatter.TaskFormatter;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.calendar.MonthHolder;
import org.projectforge.framework.calendar.WeekHolder;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.NumberHelper;

/**
 * Repräsentiert einen Monatsbericht eines Mitarbeiters.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MonthlyEmployeeReport implements Serializable
{
  private static final long serialVersionUID = -4636357379552246075L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MonthlyEmployeeReport.class);

  public class Kost2Row implements Serializable
  {
    private static final long serialVersionUID = -5379735557333691194L;

    public Kost2Row(final Kost2DO kost2)
    {
      this.kost2 = kost2;
    }

    /**
     * XML-escaped or null if not exists.
     */
    public String getProjektname()
    {
      if (kost2 == null || kost2.getProjekt() == null) {
        return null;
      }
      return StringEscapeUtils.escapeXml(kost2.getProjekt().getName());
    }

    /**
     * XML-escaped or null if not exists.
     */
    public String getKundename()
    {
      if (kost2 == null || kost2.getProjekt() == null || kost2.getProjekt().getKunde() == null) {
        return null;
      }
      return StringEscapeUtils.escapeXml(kost2.getProjekt().getKunde().getName());
    }

    /**
     * XML-escaped or null if not exists.
     */
    public String getKost2ArtName()
    {
      if (kost2 == null || kost2.getKost2Art() == null) {
        return null;
      }
      return StringEscapeUtils.escapeXml(kost2.getKost2Art().getName());
    }

    /**
     * XML-escaped or null if not exists.
     */
    public String getKost2Description()
    {
      if (kost2 == null) {
        return null;
      }
      return StringEscapeUtils.escapeXml(kost2.getDescription());
    }

    public Kost2DO getKost2()
    {
      return kost2;
    }

    private final Kost2DO kost2;
  }

  private final int year;

  private final int month;

  private Date fromDate;

  private Date toDate;

  private BigDecimal numberOfWorkingDays;

  /**
   * Employee can be null, if not found. As fall back, store user.
   */
  private PFUserDO user;

  private EmployeeDO employee;

  private long totalGrossDuration = 0, totalNetDuration = 0;

  private BigDecimal vacationCount = BigDecimal.ZERO;

  private Integer kost1Id;

  private List<MonthlyEmployeeReportWeek> weeks;

  /**
   * Days with time sheets.
   */
  private final Set<Integer> bookedDays = new HashSet<Integer>();

  private final List<Integer> unbookedDays = new ArrayList<Integer>();

  /**
   * Key is kost2.id.
   */
  private Map<Integer, MonthlyEmployeeReportEntry> kost2Durations;

  /**
   * Key is task.id.
   */
  private Map<Integer, MonthlyEmployeeReportEntry> taskDurations;

  /**
   * String is formatted Kost2-String for sorting.
   */
  private Map<String, Kost2Row> kost2Rows;

  /**
   * String is formatted Task path string for sorting.
   */
  private Map<String, TaskDO> taskEntries;

  private VacationService vacationService;

  private EmployeeService employeeService;

  public static final String getFormattedDuration(final long duration)
  {
    if (duration == 0) {
      return "";
    }
    final BigDecimal hours = new BigDecimal(duration).divide(new BigDecimal(1000 * 60 * 60), 2,
        BigDecimal.ROUND_HALF_UP);
    return NumberHelper.formatFraction2(hours);
  }

  /**
   * Dont't forget to initialize: setFormatter and setUser or setEmployee.
   *
   * @param year
   * @param month
   */
  public MonthlyEmployeeReport(final EmployeeService employeeService, final VacationService vacationService, final PFUserDO user, final int year,
      final int month)
  {
    this.year = year;
    this.month = month;
    this.user = user;
    this.employeeService = employeeService;
    this.vacationService = vacationService;
  }

  /**
   * Use only as fallback, if employee is not available.
   *
   * @param user
   */
  public void setUser(final PFUserDO user)
  {
    this.user = user;
  }

  /**
   * User will be set automatically from given employee.
   *
   * @param employee
   */
  public void setEmployee(final EmployeeDO employee)
  {
    this.employee = employee;
    if (employee != null) {
      this.user = employee.getUser();
      this.kost1Id = employee.getKost1Id();
    }
  }

  public void init()
  {
    if (this.user != null) {
      this.employee = employeeService.getEmployeeByUserId(this.user.getId());
    }
    // Create the weeks:
    this.weeks = new ArrayList<MonthlyEmployeeReportWeek>();
    final DateHolder dh = new DateHolder();
    dh.setDate(year, month, 1, 0, 0, 0);
    fromDate = dh.getDate();
    final DateHolder dh2 = new DateHolder(dh.getDate());
    dh2.setEndOfMonth();
    toDate = dh2.getDate();
    int i = 0;
    do {
      final MonthlyEmployeeReportWeek week = new MonthlyEmployeeReportWeek(dh.getDate());
      weeks.add(week);
      dh.setEndOfWeek();
      dh.add(Calendar.DAY_OF_WEEK, +1);
      dh.setBeginOfWeek();
      if (i++ > 10) {
        throw new RuntimeException("Endless loop protection: Please contact developer!");
      }
    } while (dh.getDate().before(toDate));
  }

  public void addTimesheet(final TimesheetDO sheet)
  {
    final DayHolder day = new DayHolder(sheet.getStartTime());
    bookedDays.add(day.getDayOfMonth());
    for (final MonthlyEmployeeReportWeek week : weeks) {
      if (week.matchWeek(sheet) == true) {
        week.addEntry(sheet);
        return;
      }
    }
    log.info("Ignoring time sheet which isn't inside current month: "
        + year
        + "-"
        + StringHelper.format2DigitNumber(month + 1)
        + ": "
        + sheet);

  }

  public void calculate()
  {
    Validate.notEmpty(weeks);
    kost2Rows = new TreeMap<String, Kost2Row>();
    taskEntries = new TreeMap<String, TaskDO>();
    kost2Durations = new HashMap<Integer, MonthlyEmployeeReportEntry>();
    taskDurations = new HashMap<Integer, MonthlyEmployeeReportEntry>();
    for (final MonthlyEmployeeReportWeek week : weeks) {
      if (MapUtils.isNotEmpty(week.getKost2Entries()) == true) {
        for (final MonthlyEmployeeReportEntry entry : week.getKost2Entries().values()) {
          Validate.notNull(entry.getKost2());
          kost2Rows.put(entry.getKost2().getShortDisplayName(), new Kost2Row(entry.getKost2()));
          MonthlyEmployeeReportEntry kost2Total = kost2Durations.get(entry.getKost2().getId());
          if (kost2Total == null) {
            kost2Total = new MonthlyEmployeeReportEntry(entry.getKost2());
            kost2Total.addMillis(entry.getWorkFractionMillis());
            kost2Durations.put(entry.getKost2().getId(), kost2Total);
          } else {
            kost2Total.addMillis(entry.getWorkFractionMillis());
          }
          // Travelling times etc. (see cost 2 type factor):
          totalGrossDuration += entry.getMillis();
          totalNetDuration += entry.getWorkFractionMillis();
        }
      }
      if (MapUtils.isNotEmpty(week.getTaskEntries()) == true) {
        for (final MonthlyEmployeeReportEntry entry : week.getTaskEntries().values()) {
          Validate.notNull(entry.getTask());
          taskEntries.put(TaskFormatter.getTaskPath(entry.getTask().getId(), true, OutputType.XML),
              entry.getTask());
          MonthlyEmployeeReportEntry taskTotal = taskDurations.get(entry.getTask().getId());
          if (taskTotal == null) {
            taskTotal = new MonthlyEmployeeReportEntry(entry.getTask());
            taskTotal.addMillis(entry.getMillis());
            taskDurations.put(entry.getTask().getId(), taskTotal);
          } else {
            taskTotal.addMillis(entry.getMillis());
          }
          totalGrossDuration += entry.getMillis();
          totalNetDuration += entry.getMillis();
        }
      }
    }
    final MonthHolder monthHolder = new MonthHolder(this.fromDate);
    this.numberOfWorkingDays = monthHolder.getNumberOfWorkingDays();
    for (final WeekHolder week : monthHolder.getWeeks()) {
      for (final DayHolder day : week.getDays()) {
        if (day.getMonth() == this.month && day.isWorkingDay() == true
            && bookedDays.contains(day.getDayOfMonth()) == false) {
          unbookedDays.add(day.getDayOfMonth());
        }
      }
    }
    if (vacationService != null && this.employee != null && this.employee.getUser() != null) {
      if (vacationService.couldUserUseVacationService(this.employee.getUser(), false)) {
        this.vacationCount = vacationService.getAvailableVacationdaysForYear(this.employee, this.year, false);
      }
    }
  }

  /**
   * Gets the list of unbooked working days. These are working days without time sheets of the actual user.
   */
  public List<Integer> getUnbookedDays()
  {
    return unbookedDays;
  }

  /**
   * @return Days of month without time sheets: 03.11., 08.11., ... or null if no entries exists.
   */
  public String getFormattedUnbookedDays()
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final Integer dayOfMonth : unbookedDays) {
      if (first == true) {
        first = false;
      } else {
        buf.append(", ");
      }
      buf.append(StringHelper.format2DigitNumber(dayOfMonth)).append(".")
          .append(StringHelper.format2DigitNumber(month + 1)).append(".");
    }
    if (first == true) {
      return null;
    }
    return buf.toString();

  }

  /**
   * Key is the shortDisplayName of Kost2DO. The Map is a TreeMap sorted by the keys.
   */
  public Map<String, Kost2Row> getKost2Rows()
  {
    return kost2Rows;
  }

  /**
   * Key is the kost2 id.
   */
  public Map<Integer, MonthlyEmployeeReportEntry> getKost2Durations()
  {
    return kost2Durations;
  }

  /**
   * Key is the task path string of TaskDO. The Map is a TreeMap sorted by the keys.
   */
  public Map<String, TaskDO> getTaskEntries()
  {
    return taskEntries;
  }

  /**
   * Key is the task id.
   */
  public Map<Integer, MonthlyEmployeeReportEntry> getTaskDurations()
  {
    return taskDurations;
  }

  public int getYear()
  {
    return year;
  }

  public int getMonth()
  {
    return month;
  }

  public List<MonthlyEmployeeReportWeek> getWeeks()
  {
    return weeks;
  }

  public String getFormmattedMonth()
  {
    return StringHelper.format2DigitNumber(month + 1);
  }

  public Date getFromDate()
  {
    return fromDate;
  }

  public Date getToDate()
  {
    return toDate;
  }

  /**
   * Can be null, if not set (not available).
   */
  public EmployeeDO getEmployee()
  {
    return employee;
  }

  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @return Total duration in ms.
   */
  public long getTotalGrossDuration()
  {
    return totalGrossDuration;
  }

  /**
   * The net duration may differ from total duration (e. g. for travelling times if a fraction is defined for used cost
   * 2 types).
   *
   * @return the netDuration in ms.
   */
  public long getTotalNetDuration()
  {
    return totalNetDuration;
  }

  public String getFormattedTotalGrossDuration()
  {
    return MonthlyEmployeeReport.getFormattedDuration(totalGrossDuration);
  }

  public String getFormattedVacationCount()
  {
    return vacationCount + " " + I18nHelper.getLocalizedMessage("day");
  }

  public String getFormattedTotalNetDuration()
  {
    return MonthlyEmployeeReport.getFormattedDuration(totalNetDuration);
  }

  public Integer getKost1Id()
  {
    return kost1Id;
  }

  public BigDecimal getNumberOfWorkingDays()
  {
    return numberOfWorkingDays;
  }
}
