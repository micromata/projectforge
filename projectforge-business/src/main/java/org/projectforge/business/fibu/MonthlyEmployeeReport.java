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

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.StringEscapeUtils;
import org.projectforge.business.PfCaches;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskFormatter;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.business.vacation.service.VacationStats;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.calendar.Holidays;
import org.projectforge.framework.calendar.MonthHolder;
import org.projectforge.framework.calendar.WeekHolder;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Repräsentiert einen Monatsbericht eines Mitarbeiters.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MonthlyEmployeeReport implements Serializable {
    private static final long serialVersionUID = -4636357379552246075L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MonthlyEmployeeReport.class);

    /**
     * ID of pseudo task, see below.
     */
    static final long MAGIC_PSEUDO_TASK_ID = -42L;

    /**
     * Checks if the given taskId is the Pseudo task, see below.
     * @return true, if the given task id matches the magic pseudo task id.
     */
    public static boolean isPseudoTask(Long taskId) {
        return taskId == MAGIC_PSEUDO_TASK_ID;
    }

    /**
     * Pseudo task are used for team leaders for showing time sheet hours of foreign users without detailed information,
     * if the team leader has no select access.
     * @return Pseudo task with magic task id (-42) and title '******'.
     */
    public static TaskDO createPseudoTask() {
        TaskDO pseudoTask = new TaskDO();
        pseudoTask.setId(MAGIC_PSEUDO_TASK_ID);
        pseudoTask.setTitle("******");
        return pseudoTask;
    }


    public static class Kost2Row implements Serializable {
        private static final long serialVersionUID = -5379735557333691194L;

        public Kost2Row(final Kost2DO kost2) {
            this.kost2 = kost2;
        }

        /**
         * XML-escaped or null if not exists.
         */
        public String getProjektname() {
            if (kost2 == null || kost2.getProjekt() == null) {
                return null;
            }
            return StringEscapeUtils.escapeXml11(kost2.getProjekt().getName());
        }

        /**
         * XML-escaped or null if not exists.
         */
        public String getKundename() {
            if (kost2 == null || kost2.getProjekt() == null || kost2.getProjekt().getKunde() == null) {
                return null;
            }
            return StringEscapeUtils.escapeXml11(kost2.getProjekt().getKunde().getName());
        }

        /**
         * XML-escaped or null if not exists.
         */
        public String getKost2ArtName() {
            if (kost2 == null || kost2.getKost2Art() == null) {
                return null;
            }
            return StringEscapeUtils.escapeXml11(kost2.getKost2Art().getName());
        }

        /**
         * XML-escaped or null if not exists.
         */
        public String getKost2Description() {
            if (kost2 == null) {
                return null;
            }
            return StringEscapeUtils.escapeXml11(kost2.getDescription());
        }

        public Kost2DO getKost2() {
            return kost2;
        }

        private final Kost2DO kost2;
    }

    private PFDateTime fromDate;

    private PFDateTime toDate;

    private BigDecimal numberOfWorkingDays;

    /**
     * Employee can be null, if not found. As fall back, store user.
     */
    private PFUserDO user;

    private EmployeeDO employee;

    private long totalGrossDuration = 0, totalNetDuration = 0;

    private BigDecimal vacationCount = BigDecimal.ZERO;

    private BigDecimal vacationPlannedCount = BigDecimal.ZERO;

    private Long kost1Id;

    private List<MonthlyEmployeeReportWeek> weeks;

    /**
     * Days with time sheets.
     */
    private final Set<Integer> bookedDays = new HashSet<>();

    private final List<Integer> unbookedDays = new ArrayList<>();

    /**
     * Key is kost2.id.
     */
    private Map<Long, MonthlyEmployeeReportEntry> kost2Durations;

    /**
     * Key is task.id.
     */
    private Map<Long, MonthlyEmployeeReportEntry> taskDurations;

    /**
     * String is formatted Kost2-String for sorting.
     */
    private Map<String, Kost2Row> kost2Rows;

    /**
     * String is formatted Task path string for sorting.
     */
    private Map<String, TaskDO> taskEntries;

    public static String getFormattedDuration(final long duration) {
        if (duration == 0) {
            return "";
        }
        final BigDecimal hours = new BigDecimal(duration).divide(new BigDecimal(1000 * 60 * 60), 2,
                RoundingMode.HALF_UP);
        return NumberHelper.formatFraction2(hours);
    }

    /**
     * Don't forget to initialize: setFormatter and setUser or setEmployee.
     *
     * @param year
     * @param month 1-based: 1 - January, ..., 12 - December
     */
    public MonthlyEmployeeReport(final PFUserDO user, final int year, final Integer month) {
        this.fromDate = PFDateTime.withDate(year, month, 1);
        this.toDate = this.fromDate.getEndOfMonth();
        this.user = user;
        setEmployee(WicketSupport.get(EmployeeCache.class).getEmployeeByUserId(user.getId()));
    }

    /**
     * Use only as fallback, if employee is not available.
     *
     * @param user
     */
    public void setUser(final PFUserDO user) {
        this.user = user;
    }

    /**
     * User will be set automatically from given employee.
     *
     * @param employee
     */
    public void setEmployee(final EmployeeDO employee) {
        this.employee = employee;
        if (employee != null) {
            this.user = employee.getUser();
            Kost1DO kost1 = employee.getKost1();
            if (kost1 != null) {
                this.kost1Id = kost1.getId();
            }
        }
    }

    public void init() {
        if (this.user != null) {
            this.employee = WicketSupport.get(EmployeeCache.class).getEmployeeByUserId(this.user.getId());
        }
        // Create the weeks:
        this.weeks = new ArrayList<>();

        int paranoiaCounter = 0;
        PFDateTime date = fromDate;
        do {
            final MonthlyEmployeeReportWeek week = new MonthlyEmployeeReportWeek(date);
            weeks.add(week);
            date = date.plusWeeks(1).getBeginOfWeek();
            if (paranoiaCounter++ > 10) {
                throw new RuntimeException("Endless loop protection: Please contact developer!");
            }
        } while (date.isBefore(toDate));
    }

    public void addTimesheet(final TimesheetDO sheet, final boolean hasSelectAccess) {
        final PFDateTime day = PFDateTime.from(sheet.getStartTime()); // not null
        bookedDays.add(day.getDayOfMonth());
        for (final MonthlyEmployeeReportWeek week : weeks) {
            if (week.matchWeek(sheet)) {
                week.addEntry(sheet, hasSelectAccess);
                return;
            }
        }
        log.info("Ignoring time sheet which isn't inside current month: "
                + getYear()
                + "-"
                + StringHelper.format2DigitNumber(getMonth())
                + ": "
                + sheet);

    }

    public void calculate() {
        Validate.notEmpty(weeks);
        kost2Rows = new TreeMap<>();
        taskEntries = new TreeMap<>();
        kost2Durations = new HashMap<>();
        taskDurations = new HashMap<>();
        for (final MonthlyEmployeeReportWeek week : weeks) {
            if (MapUtils.isNotEmpty(week.getKost2Entries())) {
                for (final MonthlyEmployeeReportEntry entry : week.getKost2Entries().values()) {
                    Kost2DO kost2 = PfCaches.getInstance().getKost2IfNotInitialized(entry.getKost2());
                    Objects.requireNonNull(kost2);
                    kost2Rows.put(kost2.getDisplayName(), new Kost2Row(kost2));
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
            if (MapUtils.isNotEmpty(week.getTaskEntries())) {
                for (final MonthlyEmployeeReportEntry entry : week.getTaskEntries().values()) {
                    Objects.requireNonNull(entry.getTask());
                    long taskId = entry.getTask().getId();
                    if (isPseudoTask(taskId)) {
                        // Pseudo task (see MonthlyEmployeeReportWeek for timesheet the current user has no select access.
                        TaskDO pseudoTask = createPseudoTask();
                        taskEntries.put(pseudoTask.getTitle(), pseudoTask);
                    } else {
                        taskEntries.put(TaskFormatter.getTaskPath(taskId, true,
                                        OutputType.XML),
                                entry.getTask());
                    }
                    MonthlyEmployeeReportEntry taskTotal = taskDurations.get(entry.getTask().getId());
                    if (taskTotal == null) {
                        taskTotal = new MonthlyEmployeeReportEntry(entry.getTask());
                        taskTotal.addMillis(entry.getMillis());
                        taskDurations.put(entry.getTask().getId(), taskTotal);
                    } else {
                        taskTotal.addMillis(entry.getMillis());
                    }
                    totalGrossDuration += entry.getMillis();
                    totalNetDuration += entry.getWorkFractionMillis();
                }
            }
        }
        final MonthHolder monthHolder = new MonthHolder(this.fromDate);
        this.numberOfWorkingDays = monthHolder.getNumberOfWorkingDays();
        final Holidays holidays = Holidays.getInstance();
        for (final WeekHolder week : monthHolder.getWeeks()) {

            for (final PFDay day : week.getDays()) {
                if (day.getMonth() == this.fromDate.getMonth() && holidays.isWorkingDay(day)
                        && !bookedDays.contains(day.getDayOfMonth())) {
                    unbookedDays.add(day.getDayOfMonth());
                }
            }
        }
        final VacationService vacationService = WicketSupport.get(VacationService.class);
        if (vacationService != null && this.employee != null && this.employee.getUser() != null) {
            if (vacationService.hasAccessToVacationService(this.employee.getUser(), false)) {
                VacationStats stats = vacationService.getVacationStats(employee);
                this.vacationCount = stats.getVacationDaysLeftInYear(); // was vacationService.getAvailableVacationDaysForYearAtDate(this.employee, this.toDate.getLocalDate());
                this.vacationPlannedCount = stats.getVacationDaysInProgress(); // was vacationService.getPlandVacationDaysForYearAtDate(this.employee, this.toDate.getLocalDate());
            }
        }
    }

    /**
     * Gets the list of unbooked working days. These are working days without time sheets of the actual user.
     */
    public List<Integer> getUnbookedDays() {
        return unbookedDays;
    }

    /**
     * @return Days of month without time sheets: 03.11., 08.11., ... or null if no entries exists.
     */
    public String getFormattedUnbookedDays() {
        final StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (final Integer dayOfMonth : unbookedDays) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }
            buf.append(StringHelper.format2DigitNumber(dayOfMonth)).append(".")
                    .append(StringHelper.format2DigitNumber(getMonth())).append(".");
        }
        if (first) {
            return null;
        }
        return buf.toString();

    }

    /**
     * Key is the displayName of Kost2DO. The Map is a TreeMap sorted by the keys.
     */
    public Map<String, Kost2Row> getKost2Rows() {
        return kost2Rows;
    }

    /**
     * Key is the kost2 id.
     */
    public Map<Long, MonthlyEmployeeReportEntry> getKost2Durations() {
        return kost2Durations;
    }

    /**
     * Key is the task path string of TaskDO. The Map is a TreeMap sorted by the keys.
     */
    public Map<String, TaskDO> getTaskEntries() {
        return taskEntries;
    }

    /**
     * Key is the task id.
     */
    public Map<Long, MonthlyEmployeeReportEntry> getTaskDurations() {
        return taskDurations;
    }

    public int getYear() {
        return fromDate.getYear();
    }

    /**
     * @return 1-January, ..., 12-December.
     */
    public Integer getMonth() {
        return fromDate.getMonthValue();
    }

    public List<MonthlyEmployeeReportWeek> getWeeks() {
        return weeks;
    }

    public String getFormmattedMonth() {
        return StringHelper.format2DigitNumber(getMonth());
    }

    public Date getFromDate() {
        return fromDate.getUtilDate();
    }

    public Date getToDate() {
        return toDate.getUtilDate();
    }

    /**
     * Can be null, if not set (not available).
     */
    public EmployeeDO getEmployee() {
        return employee;
    }

    public PFUserDO getUser() {
        return user;
    }

    /**
     * @return Total duration in ms.
     */
    public long getTotalGrossDuration() {
        return totalGrossDuration;
    }

    /**
     * The net duration may differ from total duration (e. g. for travelling times if a fraction is defined for used cost
     * 2 types).
     *
     * @return the netDuration in ms.
     */
    public long getTotalNetDuration() {
        return totalNetDuration;
    }

    public String getFormattedTotalGrossDuration() {
        return MonthlyEmployeeReport.getFormattedDuration(totalGrossDuration);
    }

    public String getFormattedVacationCount() {
        return vacationCount + " " + I18nHelper.getLocalizedMessage("day");
    }

    public String getFormattedVacationPlandCount() {
        return vacationPlannedCount + " " + I18nHelper.getLocalizedMessage("day");
    }

    public String getFormattedTotalNetDuration() {
        return MonthlyEmployeeReport.getFormattedDuration(totalNetDuration);
    }

    public Long getKost1Id() {
        return kost1Id;
    }

    public BigDecimal getNumberOfWorkingDays() {
        return numberOfWorkingDays;
    }
}
