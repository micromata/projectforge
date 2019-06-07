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

import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

object HolidayAndWeekendProvider {
    private val log = org.slf4j.LoggerFactory.getLogger(HolidayAndWeekendProvider::class.java)
    private val holidays = Holidays.getInstance()

    class SpecialDayInfo(val weekend: Boolean = false, val holiday: Boolean = false, val holidayTitle: String? = null, val workingDay: Boolean = false)

    fun getSpecialDayInfos(start: PFDateTime, end: PFDateTime): Map<String, SpecialDayInfo> {
        val result = mutableMapOf<String, SpecialDayInfo>()
        var day = start.getBeginOfDay()
        do {
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
                if (holidayInfo.isNullOrBlank()) {
                    holidayInfo = null
                } else
                    if (holidayInfo.startsWith("calendar.holiday.")) {
                        holidayInfo = translate(holidayInfo)
                    }
                val dayInfo = SpecialDayInfo(weekend, holiday, holidayInfo, workingDay)
                val localDate = day.asLocalDate()
                result.put(isoDateFormatter.format(localDate), dayInfo)
            }
            day = day.plusDays(1)
        } while (!day.isAfter(end))
        return result
    }

    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
}
