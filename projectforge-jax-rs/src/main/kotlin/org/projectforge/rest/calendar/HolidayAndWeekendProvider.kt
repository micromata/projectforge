package org.projectforge.rest.calendar

import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import java.time.DayOfWeek

object HolidayAndWeekendProvider {
    private val log = org.slf4j.LoggerFactory.getLogger(HolidayAndWeekendProvider::class.java)
    private val holidays = Holidays.getInstance()

    class SpecialDayInfo(val weekend: Boolean = false, val holiday: Boolean = false, val holidayTitle: String? = null, val workingDay: Boolean = false)

    fun getSpecialDayInfos(start: PFDateTime, end: PFDateTime): List<SpecialDayInfo> {
        val result = mutableListOf<SpecialDayInfo>()
        var day = start.getBeginOfDay()
        do {
            var idCounter = 0
            var paranoiaCounter = 0
            val dateTime = day.dateTime
            if (++paranoiaCounter > 4000) {
                log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of build.")
                break
            }
            val holiday = holidays.isHoliday(dateTime.year, dateTime.dayOfYear)
            val weekend = dateTime.dayOfWeek == DayOfWeek.SATURDAY || dateTime.dayOfWeek == DayOfWeek.SUNDAY
            if (holiday || weekend) {
                val workingDay = holidays.isWorkingDay(dateTime)
                var holidayInfo = holidays.getHolidayInfo(dateTime.year, dateTime.dayOfYear)
                if (holidayInfo != null && holidayInfo.startsWith("calendar.holiday.") == true) {
                    holidayInfo = translate(holidayInfo)
                }
                val dayInfo = SpecialDayInfo(weekend, holiday, holidayInfo, workingDay)
                result.add(dayInfo)
            }
            day = day.plusDays(1)
        } while (!day.isAfter(end))
        return result
    }
}
