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

package org.projectforge.business.fibu

import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang3.Validate
import org.apache.commons.text.StringEscapeUtils
import org.projectforge.business.PfCaches
import org.projectforge.business.common.OutputType
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskFormatter.Companion.getTaskPath
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.common.StringHelper
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.calendar.MonthHolder
import org.projectforge.framework.i18n.I18nHelper.getLocalizedMessage
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.projectforge.framework.utils.NumberHelper.formatFraction2
import org.projectforge.web.WicketSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Repräsentiert einen Monatsbericht eines Mitarbeiters.
 *
 * @param year
 * @param month 1-based: 1 - January, ..., 12 - December
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MonthlyEmployeeReport(user: PFUserDO, year: Int, month: Int) : Serializable {
    class Kost2Row(@JvmField val kost2: Kost2DO?) : Serializable {
        val projektname: String?
            /**
             * XML-escaped or null if not exists.
             */
            get() {
                if (kost2?.projekt == null) {
                    return null
                }
                return StringEscapeUtils.escapeXml11(kost2.projekt!!.name)
            }

        val kundename: String?
            /**
             * XML-escaped or null if not exists.
             */
            get() {
                if (kost2?.projekt == null || kost2.projekt!!.kunde == null) {
                    return null
                }
                return StringEscapeUtils.escapeXml11(kost2.projekt!!.kunde!!.name)
            }

        val kost2ArtName: String?
            /**
             * XML-escaped or null if not exists.
             */
            get() {
                if (kost2?.kost2Art == null) {
                    return null
                }
                return StringEscapeUtils.escapeXml11(kost2.kost2Art!!.name)
            }

        val kost2Description: String?
            /**
             * XML-escaped or null if not exists.
             */
            get() {
                if (kost2 == null) {
                    return null
                }
                return StringEscapeUtils.escapeXml11(kost2.description)
            }

        companion object {
            private const val serialVersionUID = -5379735557333691194L
        }
    }

    private val fromDateTime = withDate(year, month, 1)

    private val toDateTime = fromDateTime.endOfMonth

    var numberOfWorkingDays: BigDecimal? = null
        private set

    /**
     * Use only as fallback, if employee is not available.
     *
     * @param user
     */
    /**
     * Employee can be null, if not found. As fall back, store user.
     */
    @JvmField
    var user: PFUserDO?

    var employee: EmployeeDO? = null
        private set

    /**
     * @return Total duration in ms.
     */
    var totalGrossDuration: Long = 0
        private set

    /**
     * The net duration may differ from total duration (e. g. for travelling times if a fraction is defined for used cost
     * 2 types).
     *
     * @return the netDuration in ms.
     */
    var totalNetDuration: Long = 0
        private set

    private var vacationCount: BigDecimal? = BigDecimal.ZERO

    private var vacationPlannedCount: BigDecimal? = BigDecimal.ZERO

    var kost1Id: Long? = null
        private set

    val weeks = mutableListOf<MonthlyEmployeeReportWeek>()

    /**
     * Days with time sheets.
     */
    private val bookedDays: MutableSet<Int> = HashSet()

    private val unbookedDays: MutableList<Int> = ArrayList()

    /**
     * Key is kost2.id.
     */
    val kost2Durations = mutableMapOf<Long, MonthlyEmployeeReportEntry>()

    /**
     * Key is task.id.
     */
    val taskDurations = mutableMapOf<Long, MonthlyEmployeeReportEntry>()

    /**
     * String is formatted Kost2-String for sorting.
     */
    val kost2Rows = mutableMapOf<String?, Kost2Row>()

    /**
     * String is formatted Task path string for sorting.
     */
    val taskEntries = mutableMapOf<String?, TaskDO?>()

    /**
     * Don't forget to initialize: setFormatter and setUser or setEmployee.
     */
    init {
        this.user = user
        setEmployee(WicketSupport.get(EmployeeCache::class.java).getEmployeeByUserId(user.id))
    }

    /**
     * User will be set automatically from given employee.
     *
     * @param employee
     */
    fun setEmployee(employee: EmployeeDO?) {
        this.employee = employee
        if (employee != null) {
            this.user = employee.user
            val kost1 = employee.kost1
            if (kost1 != null) {
                this.kost1Id = kost1.id
            }
        }
    }

    fun init() {
        if (this.user != null) {
            this.employee = WicketSupport.get(EmployeeCache::class.java).getEmployeeByUserId(
                user!!.id
            )
        }
        // Create the weeks:
        var paranoiaCounter = 0
        var date = fromDateTime
        do {
            val week = MonthlyEmployeeReportWeek(date)
            weeks.add(week)
            date = date.plusWeeks(1).beginOfWeek
            if (paranoiaCounter++ > 10) {
                throw RuntimeException("Endless loop protection: Please contact developer!")
            }
        } while (date.isBefore(toDateTime))
    }

    fun addTimesheet(sheet: TimesheetDO, hasSelectAccess: Boolean) {
        val day = from(sheet.startTime!!) // not null
        bookedDays.add(day.dayOfMonth)
        for (week in weeks!!) {
            if (week.matchWeek(sheet)) {
                week.addEntry(sheet, hasSelectAccess)
                return
            }
        }
        log.info(
            ("Ignoring time sheet which isn't inside current month: "
                    + year
                    + "-"
                    + StringHelper.format2DigitNumber(month)
                    + ": "
                    + sheet)
        )
    }

    fun calculate() {
        Validate.notEmpty<List<MonthlyEmployeeReportWeek>?>(weeks)
        for (week in weeks) {
            if (MapUtils.isNotEmpty(week.kost2Entries)) {
                for (entry in week.kost2Entries.values) {
                    val kost2 = PfCaches.instance.getKost2IfNotInitialized(entry.kost2)
                    Objects.requireNonNull(kost2)
                    kost2Rows[kost2!!.displayName] = Kost2Row(kost2)
                    var kost2Total = kost2Durations.get(entry.kost2!!.id)
                    if (kost2Total == null) {
                        kost2Total = MonthlyEmployeeReportEntry(entry.kost2)
                        kost2Total.addMillis(entry.workFractionMillis)
                        entry.kost2?.id?.let { id ->
                            kost2Durations[id] = kost2Total
                        }
                    } else {
                        kost2Total.addMillis(entry.workFractionMillis)
                    }
                    // Travelling times etc. (see cost 2 type factor):
                    totalGrossDuration += entry.millis
                    totalNetDuration += entry.workFractionMillis
                }
            }
            if (MapUtils.isNotEmpty(week.taskEntries)) {
                for (entry in week.taskEntries.values) {
                    Objects.requireNonNull(entry.task)
                    val taskId = entry.task!!.id!!
                    if (isPseudoTask(taskId)) {
                        // Pseudo task (see MonthlyEmployeeReportWeek for timesheet the current user has no select access.
                        val pseudoTask = createPseudoTask()
                        taskEntries[pseudoTask.title] = pseudoTask
                    } else {
                        taskEntries[getTaskPath(
                            taskId, true,
                            OutputType.XML
                        )] = entry.task
                    }
                    var taskTotal = taskDurations.get(entry.task!!.id)
                    if (taskTotal == null) {
                        taskTotal = MonthlyEmployeeReportEntry(entry.task)
                        taskTotal.addMillis(entry.millis)
                        entry.task?.id?.let { id ->
                            taskDurations[id] = taskTotal
                        }
                    } else {
                        taskTotal.addMillis(entry.millis)
                    }
                    totalGrossDuration += entry.millis
                    totalNetDuration += entry.workFractionMillis
                }
            }
        }
        val monthHolder = MonthHolder(this.fromDateTime)
        this.numberOfWorkingDays = monthHolder.numberOfWorkingDays
        val holidays = Holidays.instance
        for (week in monthHolder.weeks) {
            for (day in week.days) {
                if (day.month == fromDateTime.month && holidays.isWorkingDay(day)
                    && !bookedDays.contains(day.dayOfMonth)
                ) {
                    unbookedDays.add(day.dayOfMonth)
                }
            }
        }
        val vacationService = WicketSupport.get(VacationService::class.java)
        if (vacationService != null && this.employee != null && employee!!.user != null) {
            if (vacationService.hasAccessToVacationService(employee!!.user, false)) {
                val stats = vacationService.getVacationStats(employee!!)
                this.vacationCount =
                    stats.vacationDaysLeftInYear // was vacationService.getAvailableVacationDaysForYearAtDate(this.employee, this.toDate.getLocalDate());
                this.vacationPlannedCount =
                    stats.vacationDaysInProgress // was vacationService.getPlandVacationDaysForYearAtDate(this.employee, this.toDate.getLocalDate());
            }
        }
    }

    /**
     * Gets the list of unbooked working days. These are working days without time sheets of the actual user.
     */
    fun getUnbookedDays(): List<Int> {
        return unbookedDays
    }

    val formattedUnbookedDays: String?
        /**
         * @return Days of month without time sheets: 03.11., 08.11., ... or null if no entries exists.
         */
        get() {
            val buf = StringBuilder()
            var first = true
            for (dayOfMonth in unbookedDays) {
                if (first) {
                    first = false
                } else {
                    buf.append(", ")
                }
                buf.append(StringHelper.format2DigitNumber(dayOfMonth)).append(".")
                    .append(StringHelper.format2DigitNumber(month)).append(".")
            }
            if (first) {
                return null
            }
            return buf.toString()
        }

    val year: Int
        get() = fromDateTime.year

    val month: Int
        /**
         * @return 1-January, ..., 12-December.
         */
        get() = fromDateTime.monthValue

    val formmattedMonth: String
        get() = StringHelper.format2DigitNumber(month)

    val fromDate: Date
        get() = fromDateTime.utilDate

    val toDate: Date
        get() = toDateTime.utilDate

    val formattedTotalGrossDuration: String
        get() = getFormattedDuration(totalGrossDuration)

    val formattedVacationCount: String
        get() = vacationCount.toString() + " " + getLocalizedMessage("day")

    val formattedVacationPlandCount: String
        get() = vacationPlannedCount.toString() + " " + getLocalizedMessage("day")

    val formattedTotalNetDuration: String
        get() = getFormattedDuration(totalNetDuration)

    companion object {
        private const val serialVersionUID = -4636357379552246075L

        private val log: Logger = LoggerFactory.getLogger(MonthlyEmployeeReport::class.java)

        /**
         * ID of pseudo task, see below.
         */
        const val MAGIC_PSEUDO_TASK_ID: Long = -42L

        /**
         * Checks if the given taskId is the Pseudo task, see below.
         * @return true, if the given task id matches the magic pseudo task id.
         */
        fun isPseudoTask(taskId: Long): Boolean {
            return taskId == MAGIC_PSEUDO_TASK_ID
        }

        /**
         * Pseudo task are used for team leaders for showing time sheet hours of foreign users without detailed information,
         * if the team leader has no select access.
         * @return Pseudo task with magic task id (-42) and title '******'.
         */
        @JvmStatic
        fun createPseudoTask(): TaskDO {
            val pseudoTask = TaskDO()
            pseudoTask.id = MAGIC_PSEUDO_TASK_ID
            pseudoTask.title = "******"
            return pseudoTask
        }


        @JvmStatic
        fun getFormattedDuration(duration: Long): String {
            if (duration == 0L) {
                return ""
            }
            val hours = BigDecimal(duration).divide(
                BigDecimal(1000 * 60 * 60), 2,
                RoundingMode.HALF_UP
            )
            return formatFraction2(hours)
        }
    }
}
