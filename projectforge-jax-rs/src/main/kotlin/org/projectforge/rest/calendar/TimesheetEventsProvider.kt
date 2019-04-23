package org.projectforge.rest.calendar

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.teamcal.common.CalendarHelper
import org.projectforge.business.timesheet.OrderDirection
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.common.StringHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.TimePeriod
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.Month
import java.time.ZonedDateTime

@Component
class TimesheetEventsProvider() {

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    fun addTimesheetEvents(start: PFDateTime,
                           end: PFDateTime,
                           userId: Int,
                           events: MutableList<BigCalendarEvent>,
                           showBreaks: Boolean = false,
                           showStatistics: Boolean = true) {
        val ctx = Context()
        val tsFilter = TimesheetFilter()
        tsFilter.userId = userId
        tsFilter.startTime = start.asUtilDate()
        tsFilter.stopTime = end.asUtilDate()
        tsFilter.orderType = OrderDirection.ASC
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

        var lastStopTime: LocalDateTime? = null
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
            if (ctx.month != null && startTime.month() != ctx.month && stopTime.month() != ctx.month) {
                outOfRange = true
            }
            val link = "timesheet/edit/${timesheet.id}"
            events.add(BigCalendarEvent(title, timesheet.startTime!!, timesheet .stopTime!!, null,
                    location =  timesheet.location, desc = description, tooltip=tooltip, formattedDuration = formattedDuration, outOfRange = outOfRange,
                    cssClass = "timesheet", link = link))

            /*  if (ctx.month == startTime.month()) {
                  ctx.totalDuration += timesheet.duration
                  addDurationOfDay(startTime.dayOfMonth, duration)
              }
              val dayOfYear = startTime.dayOfYear
              addDurationOfDayOfYear(dayOfYear, duration)
              event.setTooltip(
                      getString("timesheet"),
                      arrayOf(arrayOf(title), arrayOf(timesheet.location, getString("timesheet.location")), arrayOf(KostFormatter.formatLong(timesheet.kost2), getString("fibu.kost2")), arrayOf(WicketTaskFormatter.getTaskPath(timesheet.taskId, true, OutputType.PLAIN), getString("task")), arrayOf(timesheet.description, getString("description"))))
        */
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
    }
}
