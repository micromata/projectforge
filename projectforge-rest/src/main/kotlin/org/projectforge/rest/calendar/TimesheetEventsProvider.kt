/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.common.OutputType
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.task.TaskFormatter
import org.projectforge.business.teamcal.common.CalendarHelper
import org.projectforge.business.timesheet.OrderDirection
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Month
import java.time.ZonedDateTime

@Component
class TimesheetEventsProvider {
  private val log = org.slf4j.LoggerFactory.getLogger(TimesheetEventsProvider::class.java)

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  fun addTimesheetEvents(
    start: PFDateTime,
    end: PFDateTime,
    userId: Int?,
    events: MutableList<FullCalendarEvent>,
    showBreaks: Boolean = false,
    showStatistics: Boolean = true
  ) {
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
    val timesheetUser = UserGroupCache.getInstance().getUser(userId)
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

    if (timesheets != null) {
      //var lastStopTime: LocalDateTime? = null
      for (timesheet in timesheets) {
        val startTime = PFDateTime.fromOrNow(timesheet.startTime)
        val stopTime = PFDateTime.fromOrNow(timesheet.stopTime)
        if (stopTime.isBefore(start) || startTime.isAfter(end)) {
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
        val title: String = CalendarHelper.getTitle(timesheet)
        val formattedDuration = FullCalendarEvent.formatDuration(timesheet.getDuration())
        /*var outOfRange: Boolean? = null
        //if (ctx.longFormat) {
        // }
        if (ctx.month != null && startTime.month != ctx.month && stopTime.month != ctx.month) {
          outOfRange = true
        }*/
        //val link = "timesheet/edit/${timesheet.id}"
        FullCalendarEvent.createEvent(
          id = timesheet.id,
          category = FullCalendarEvent.Category.TIMESHEET,
          title = title,
          start = timesheet.startTime!!,
          end = timesheet.stopTime!!,
          editable = true,
          formattedDuration = formattedDuration,
          classNames = "timesheet",
          dbId = timesheet.id,
        ).let { event ->
          events.add(event)
          val tooltipBuilder = TooltipBuilder()
          timesheet.kost2?.let { kost2 ->
            tooltipBuilder.addPropRow(translate("fibu.kost2"), KostFormatter.formatLong(kost2))
          }
          tooltipBuilder
            .addPropRow(
              translate("task"),
              TaskFormatter.getTaskPath(timesheet.taskId, true, OutputType.HTML, abreviationLength = 50),
              escapeHtml = false,
            )
            .addPropRow(translate("timesheet.location"), timesheet.location, abbreviate = true)
            .addPropRow(translate("description"), timesheet.description, pre = true, abbreviate = true)
          event.setTooltip("${translate("timesheet")}: ${timesheetUser?.displayName}", tooltipBuilder)
        }

        val duration = timesheet.getDuration()
        if (ctx.month == null || ctx.month == startTime.month) {
          ctx.totalDuration += duration
          ctx.addDurationOfDay(startTime.dayOfMonth, duration)
        }
        val dayOfYear = startTime.dayOfYear
        ctx.addDurationOfDayOfYear(dayOfYear, duration)
      }
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
        val durationString = FullCalendarEvent.formatDuration(duration)
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
            buf.append(": ").append(FullCalendarEvent.formatDuration(weekDuration))
          }
          if (duration > 0) {
            buf.append(", ").append(durationString)
          }
          buf.toString()
        } else {
          durationString
        }
        val event = FullCalendarEvent.createAllDayEvent(
          id = paranoiaCounter,
          category = FullCalendarEvent.Category.TIMESHEET_STATS,
          title = title,
          start = day.localDate,
          classNames = "timesheet-stats",
        )
        events.add(event)
        day = day.plusDays(1)
      } while (!day.isAfter(end))
    }
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
