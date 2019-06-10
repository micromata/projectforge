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

import org.projectforge.business.address.AddressDao
import org.projectforge.business.calendar.CalendarStyleMap
import org.projectforge.common.DateFormatType
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.PFDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.*

object BirthdaysProvider {
    private val log = org.slf4j.LoggerFactory.getLogger(BirthdaysProvider::class.java)
    private val holidays = Holidays.getInstance()

    fun addEvents(addressDao: AddressDao,
                  start: PFDateTime,
                  end: PFDateTime,
                  events: MutableList<BigCalendarEvent>,
                  styleMap: CalendarStyleMap,
                  showFavoritesBirthdays: Boolean = false,
                  showAllBirthdays: Boolean = false,
                  dataProtection: Boolean = true) {
        var from = start
        if (start.month == Month.MARCH && start.dayOfMonth == 1) {
            from = start.minusDays(1)
        }
        val set = addressDao.getBirthdays(from.utilDate, end.utilDate, true)
        val favoritesStyle = styleMap.birthdaysFavoritesStyle
        val allStyle = styleMap.birthdaysAllStyle

        for (birthdayAddress in set) {
            if (!showAllBirthdays && !birthdayAddress.isFavorite)
                continue // Ignore non-favorites
            val address = birthdayAddress.getAddress()
            val month = birthdayAddress.getMonth() + 1
            val dayOfMonth = birthdayAddress.getDayOfMonth()
            var date = getDate(from, end, month, dayOfMonth)
            // February, 29th fix:
            if (date == null && month == Calendar.FEBRUARY + 1 && dayOfMonth == 29) {
                date = getDate(from, end, month + 1, 1)
            }
            if (date == null) {
                log.info("Date ${birthdayAddress.getDayOfMonth()} / ${birthdayAddress.getMonth() + 1} not found between $from and $end")
                continue
            } else {
                if (dataProtection == false) {
                    birthdayAddress.setAge(date.utilDate)
                }
            }
            val name = "${birthdayAddress.address?.firstName ?: ""} ${birthdayAddress.address?.name ?: ""}"
            val title = if (!dataProtection && birthdayAddress.age > 0) {
                val birthday = org.projectforge.framework.time.DateTimeFormatter.instance().getFormattedDate(address.birthday, DateFormats.getFormatString(DateFormatType.DATE_SHORT))
                "$birthday $name (${birthdayAddress.age} ${ThreadLocalUserContext.getLocalizedString("address.age.short")})"
            } else {
                name
            }

            val bgColor: String
            val fgColor: String
            if (showFavoritesBirthdays && birthdayAddress.isFavorite) {
                bgColor = favoritesStyle.bgColor
                fgColor = favoritesStyle.fgColor
            } else {
                bgColor = allStyle.bgColor // favorites are not selected or entry is not a favorite
                fgColor = allStyle.fgColor // favorites are not selected or entry is not a favorite
            }

            events.add(BigCalendarEvent(
                    title = title,
                    start = date.beginOfDay.utilDate,
                    end = date.endOfDay.utilDate,
                    allDay = true,
                    category = "address",
                    bgColor = bgColor,
                    fgColor = fgColor,
                    dbId = birthdayAddress.address?.id))
        }
    }

    private fun getDate(start: PFDateTime, end: PFDateTime, month: Int, dayOfMonth: Int): PFDateTime? {
        var day = start
        var paranoiaCounter = 0
        do {
            if (day.monthValue == month && day.dayOfMonth == dayOfMonth) {
                return day
            }
            day = day.plusDays(1)
            if (++paranoiaCounter > 1000) {
                log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of getDate.")
                break
            }
        } while (!day.isAfter(end))
        return null
    }

    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
}
