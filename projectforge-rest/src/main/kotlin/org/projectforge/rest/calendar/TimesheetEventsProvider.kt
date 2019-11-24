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

package org.projectforge.rest.calendar

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.teamcal.common.CalendarHelper
import org.projectforge.business.timesheet.OrderDirection
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.TimePeriod
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Month
import java.time.ZonedDateTime

@Component
class TimesheetEventsProvider() {
    private val log = org.slf4j.LoggerFactory.getLogger(TimesheetEventsProvider::class.java)

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    fun addTimesheetEvents(start: PFDateTime,
                           end: PFDateTime,
                           userId: Int?,
                           events: MutableList<BigCalendarEvent>,
                           showBreaks: Boolean = false,
                           showStatistics: Boolean = true) {
        if (userId == null || userId < 0) {
            return
        }
        val ctx = Context()
        val tsFilter = TimesheetFilter()
        tsFilter.startTime = start.utilDate
        tsFilter.stopTime = end.utilDate
        tsFilter.orderType = OrderDirection.ASC
        if (timesheetDao.showTimesheetsOfOtherUsers()) {
            tsFilter.userId = userId
        } else {
            tsFilter.userId = ThreadLocalUserContext.getUserId()
        }
        val timesheets = timesheetDao.getList(tsFilter)

        ctx.days = start.daysBetween(end)
        if (ctx.days < 10) {
            // Week or day view:
            ctx.longFormat = true
        } else {
            // Month view:
            val dayInCurrentMonth = start.dateTime.plusDays(10) // Now we're definitely in the right month
            ctx.month = dayInCurrentMonth.month
            ctx.firstDayOfMonth = dayInCurrentMonth.withDayOfMonth(1)
        }

        //var lastStopTime: LocalDateTime? = null
        for (timesheet in timesheets) {
            val startTime = PFDateTime.from(timesheet.startTime, true)
            val stopTime = PFDateTime.from(timesheet.stopTime, true)
            if (stopTime!!.isBefore(start) || startTime!!.isAfter(end) == true) {
                // Time sheet doesn't match time period start - end.
                continue
            }
            if (showBreaks) {
/*                    if (lastStopTime != null
                            && DateHelper.isSameDay(stopTime, lastStopTime) == true
                            && startTime.millis - lastStopTime.millis > 60000) {
                        // Show breaks between time sheets of one day (> 60s).
                        val breakEvent = Event()
                        breakEvent.setEditable(false)
                        val breakId = (++breaksCounter).toString()
                        breakEvent.setClassName(Const.BREAK_EVENT_CLASS_NAME).setId(breakId).setStart(lastStopTime)
                                .setEnd(startTime)
                                .setTitle(getString("timesheet.break"))
                        breakEvent.setTextColor("#666666").setBackgroundColor("#F9F9F9").setColor("#F9F9F9")
                        events.put(breakId, breakEvent)
                        val breakTimesheet = TimesheetDO().setStartDate(lastStopTime.toDate())
                                .setStopDate(startTime.millis)
                        breaksMap.put(breakId, breakTimesheet)
                    }
                    lastStopTime = stopTime*/
            }
            var title: String = CalendarHelper.getTitle(timesheet)
            var tooltip: String? = null
            var formattedDuration: String? = formatDuration(timesheet.getDuration(), false)
            var description: String? = null//getToolTip(timesheet)
            var outOfRange: Boolean? = null
            //if (ctx.longFormat) {
            // }
            if (ctx.month != null && startTime.month != ctx.month && stopTime.month != ctx.month) {
                outOfRange = true
            }
            //val link = "timesheet/edit/${timesheet.id}"
            events.add(BigCalendarEvent(title, timesheet.startTime!!, timesheet.stopTime!!, null,
                    location = timesheet.location, desc = description, tooltip = tooltip, formattedDuration = formattedDuration, outOfRange = outOfRange,
                    cssClass = "timesheet", category = "timesheet", dbId = timesheet.id))

            val duration = timesheet.getDuration()
            if (ctx.month == null || ctx.month == startTime.month) {
                ctx.totalDuration += duration
                ctx.addDurationOfDay(startTime.dayOfMonth, duration)
            }
            val dayOfYear = startTime.dayOfYear
            ctx.addDurationOfDayOfYear(dayOfYear, duration)
            //event.setTooltip(
            //        getString("timesheet"),
            //        arrayOf(arrayOf(title), arrayOf(timesheet.location, getString("timesheet.location")), arrayOf(KostFormatter.formatLong(timesheet.kost2), getString("fibu.kost2")), arrayOf(WicketTaskFormatter.getTaskPath(timesheet.taskId, true, OutputType.PLAIN), getString("task")), arrayOf(timesheet.description, getString("description"))))

        }
        if (showStatistics) { // Show statistics: duration of every day is shown as all day event.
            var day = start
            val numberOfDaysInYear = day.numberOfDaysInYear
            var paranoiaCounter = 0
            do {
                if (++paranoiaCounter > 1000) {
                    log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of buildEvents.")
                    break
                }
                val dayOfYear = day.dayOfYear
                val duration: Long = ctx.getDurationOfDayOfYear(dayOfYear)
                val firstDayOfWeek = (day.dayOfWeek == ThreadLocalUserContext.getFirstDayOfWeek())
                if (!firstDayOfWeek && duration == 0L) {
                    day = day.plusDays(1)
                    continue
                }
                val durationString = formatDuration(duration, false)
                val title = if (firstDayOfWeek) { // Show week of year at top of first day of week.
                    var weekDuration: Long = 0
                    for (i in 0..6) {
                        var d = dayOfYear + i
                        if (d > numberOfDaysInYear) {
                            d -= numberOfDaysInYear
                        }
                        weekDuration += ctx.getDurationOfDayOfYear(d)
                    }
                    val buf = StringBuffer()
                    buf.append(translate("calendar.weekOfYearShortLabel")).append(day.weekOfYear)
                    if (ctx.days > 1 && weekDuration > 0) { // Show total sum of durations over all time sheets of current week (only in week and month view).
                        buf.append(": ").append(formatDuration(weekDuration, false))
                    }
                    if (duration > 0) {
                        buf.append(", ").append(durationString)
                    }
                    buf.toString()
                } else {
                    durationString
                }
                val event = BigCalendarEvent(title, start = day.utilDate, end = day.utilDate, allDay = true, category = "ts-stats", cssClass = "timesheet-stats", readOnly = true)
                events.add(event)
                day = day.plusDays(1)
            } while (!day.isAfter(end))
        }
    }

    fun getToolTip(timesheet: TimesheetDO): String {
        val location = timesheet.location
        val description = timesheet.getShortDescription()
        val task = timesheet.task
        val buf = StringBuffer()
        if (StringUtils.isNotBlank(location) == true) {
            buf.append(location)
            if (StringUtils.isNotBlank(description) == true) {
                buf.append(": ")
            }
        }
        buf.append(description)
        if (timesheet.kost2 == null) {
            buf.append("; \n").append(task?.title)
        }
        return buf.toString()
    }

    private fun formatDuration(millis: Long, ctx: Context): String {
        return formatDuration(millis, ctx.firstDayOfMonth != null)
    }

    private fun formatDuration(millis: Long, showTimePeriod: Boolean): String {
        val fields = TimePeriod.getDurationFields(millis, 8, 200)
        val buf = StringBuffer()
        if (fields[0] > 0) {
            buf.append(fields[0]).append(ThreadLocalUserContext.getLocalizedString("calendar.unit.day")).append(" ")
        }
        buf.append(fields[1]).append(":").append(StringHelper.format2DigitNumber(fields[2]))
                .append(ThreadLocalUserContext.getLocalizedString("calendar.unit.hour"))
        if (showTimePeriod == true) {
            buf.append(" (").append(ThreadLocalUserContext.getLocalizedString("calendar.month")).append(")")
        }
        return buf.toString()
    }


    private class Context() {
        var firstDayOfMonth: ZonedDateTime? = null
        var longFormat = false
        var totalDuration: Long = 0
        var month: Month? = null
        var days: Long = 0
        val durationsPerDayOfMonth = LongArray(32)
        val durationsPerDayOfYear = LongArray(380)
        var breaksMap = mutableMapOf<String, TimesheetDO>()
        var breaksCounter = 0

        fun addDurationOfDay(dayOfMonth: Int, duration: Long) {
            durationsPerDayOfMonth[dayOfMonth] += duration
        }

        fun addDurationOfDayOfYear(dayOfYear: Int, duration: Long) {
            durationsPerDayOfYear[dayOfYear] += duration
        }

        fun getDurationOfDay(dayOfMonth: Int): Long {
            return durationsPerDayOfMonth[dayOfMonth]
        }

        fun getDurationOfDayOfYear(dayOfYear: Int): Long {
            return durationsPerDayOfYear[dayOfYear]
        }
    }
}
