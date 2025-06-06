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

package org.projectforge.rest.calendar

import mu.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.business.calendar.CalendarStyleMap
import org.projectforge.common.DateFormatType
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.PFDateTime
import java.time.Month
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

object BirthdaysProvider {
  private val holidays = Holidays.instance

  fun addEvents(
    addressDao: AddressDao,
    start: PFDateTime,
    end: PFDateTime,
    events: MutableList<FullCalendarEvent>,
    styleMap: CalendarStyleMap,
    calendarSettings: CalendarSettings,
    showFavoritesBirthdays: Boolean = false,
    showAllBirthdays: Boolean = false,
    dataProtection: Boolean = true
  ) {
    var from = start
    if (start.month == Month.MARCH && start.dayOfMonth == 1) {
      from = start.minusDays(1)
    }
    val set = addressDao.getBirthdays(from.utilDate, end.utilDate, true)
    val favoritesStyle = styleMap.birthdaysFavoritesStyle
    val allStyle = styleMap.birthdaysAllStyle

    for (birthdayAddress in set) {
      if (!showAllBirthdays && !birthdayAddress.isFavorite) continue // Ignore non-favorites
      val address = birthdayAddress.getAddress()
      val month = birthdayAddress.getMonth()
      val dayOfMonth = birthdayAddress.getDayOfMonth()
      var date = getDate(from, end, month, dayOfMonth)
      // February, 29th fix:
      if (date == null && month == Month.FEBRUARY && dayOfMonth == 29) {
        date = getDate(from, end, Month.MARCH, 1)
      }
      if (date == null) {
        log.info("Date ${birthdayAddress.getDayOfMonth()} / ${birthdayAddress.month} not found between $from and $end")
        continue
      } else {
        if (dataProtection == false) {
          birthdayAddress.setAge(date.utilDate)
        }
      }
      val name = "${birthdayAddress.address?.firstName ?: ""} ${birthdayAddress.address?.name ?: ""}"
      val title = if (!dataProtection && birthdayAddress.age > 0) {
        val birthday = org.projectforge.framework.time.DateTimeFormatter.instance()
          .getFormattedDate(address.birthday, DateFormats.getFormatString(DateFormatType.DATE_SHORT))
        "$birthday $name (${birthdayAddress.age} ${ThreadLocalUserContext.getLocalizedString("address.age.short")})"
      } else {
        name
      }

      val style = if (showFavoritesBirthdays && birthdayAddress.isFavorite) {
        favoritesStyle
      } else {
        allStyle // favorites are not selected or entry is not a favorite
      }

      events.add(
        FullCalendarEvent.createAllDayEvent(
          id = birthdayAddress.address?.id,
          category = FullCalendarEvent.Category.BIRTHDAY,
          title = title,
          calendarSettings = calendarSettings,
          start = date.beginOfDay.localDate,
          style = style,
          dbId = birthdayAddress.address?.id,
          editable = true,
        )
      )
    }
  }

  private fun getDate(start: PFDateTime, end: PFDateTime, month: Month, dayOfMonth: Int): PFDateTime? {
    var day = start
    var paranoiaCounter = 0
    do {
      if (day.month == month && day.dayOfMonth == dayOfMonth) {
        return day
      }
      day = day.plusDays(1)
      if (++paranoiaCounter > 1000) {
        log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of getUtilDate.")
        break
      }
    } while (!day.isAfter(end))
    return null
  }

  private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
}
