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

import org.projectforge.framework.time.PFDateTime
import java.time.LocalDate
import java.util.*

class FullCalendarEvent(
  /** Unique identifier. */
  var id: String? = null,
  category: Category? = null,
  var title: String? = null,
  var allDay: Boolean? = null,
  var textColor: String? = null,
  var backgroundColor: String? = null,
  var classNames: String? = null,
  var editable: Boolean = false,
) {
  enum class Category { BIRTHDAY, TIMESHEET, TIMESHEET_STATS, HOLIDAY, VACATION, CAL_EVENT, TEAM_CAL_EVENT }

  class EventDate(
    var date: Date? = null,
    var day: LocalDate? = null,
  )

  class ExtendedProps(
    val category: Category? = null,
    var location: String? = null,
    var duration: String? = null,
    /**
     * For subscribed events.
     */
    var uid: String? = null,
    /**
     * The db id of the object (team event, address (birthday) etc.)
     */
    var dbId: Int? = null
  ) {
    /**
     * Additional params to display in Popover: Kost2, description for time sheets and attendees, recurrence, ... for events.
     */
    var params: MutableMap<String, String>? = null
    fun addParam(key: String, value: String?) {
      value ?: return
      if (params == null) {
        params = mutableMapOf()
      }
      params!![key] = value
    }
  }

  var start: EventDate? = null

  var end: EventDate? = null

  /**
   * Extended props of fullcalendar events available in frontend
   */
  var extendedProps: ExtendedProps? = null

  var overlap: Boolean? = null

  var display: String? = null

  init {
    if (category != null) {
      extendedProps = ExtendedProps(category)
    }
  }

  fun ensureExtendedProps(): ExtendedProps {
    if (extendedProps == null) {
      extendedProps = ExtendedProps()
    }
    return extendedProps!!
  }

  /**
   * @return this for chaining.
   */
  fun addParam(key: String, value: String?): FullCalendarEvent {
    value ?: return this
    ensureExtendedProps().addParam(key, value)
    return this
  }

  companion object {
    fun samePeriod(event: FullCalendarEvent, start: LocalDate?, end: LocalDate?): Boolean {
      start ?: return false
      end ?: return false
      return event.start?.day == start && event.end?.day == end
    }

    fun createEvent(
      id: Any?,
      category: Category,
      title: String?,
      start: Date,
      end: Date,
      allDay: Boolean? = false,
      location: String? = null,
      textColor: String? = null,
      backgroundColor: String? = null,
      classNames: String? = null,
      dbId: Int? = null,
      uid: String? = null,
      editable: Boolean = false,
      formattedDuration: String? = null,
    ): FullCalendarEvent {
      val event = FullCalendarEvent(
        id = "$category-${id?.toString() ?: "-1"}",
        category = category,
        title = title,
        textColor = textColor,
        backgroundColor = backgroundColor,
        classNames = classNames,
        editable = editable,
      )
      if (allDay == true) {
        event.allDay = true
        event.start = EventDate(day = PFDateTime.from(start).localDate)
        event.end = EventDate(day = PFDateTime.from(end).localDate)
      } else {
        event.start = EventDate(date = start)
        event.end = EventDate(date = end)
      }
      event.ensureExtendedProps().let { props ->
        props.dbId = dbId
        props.uid = uid
        props.location = location
        props.duration = formattedDuration
      }
      return event
    }

    fun createAllDayEvent(
      id: Any?,
      category: Category,
      title: String?,
      start: LocalDate,
      end: LocalDate = start,
      location: String? = null,
      textColor: String? = null,
      backgroundColor: String? = null,
      classNames: String? = null,
      dbId: Int? = null,
      uid: String? = null,
      editable: Boolean = false,
    ): FullCalendarEvent {
      val event = FullCalendarEvent(
        id = "$category-${id?.toString() ?: "-1"}",
        category = category,
        allDay = true,
        title = title,
        textColor = textColor,
        backgroundColor = backgroundColor,
        classNames = classNames,
        editable = editable,
      )
      event.start = EventDate(day = start)
      event.end = EventDate(day = end)
      event.ensureExtendedProps().let { props ->
        props.dbId = dbId
        props.uid = uid
        props.location = location
      }
      return event
    }

    /**
     * For rendering weekends and holiday.
     * @return this for chaining.
     */
    fun createBackgroundEvent(
      start: LocalDate,
      end: LocalDate? = null,
      classNames: String? = null,
      title: String? = null,
    ): FullCalendarEvent {
      val event = FullCalendarEvent()
      event.start = EventDate(day = start)
      event.end = EventDate(day = end ?: start)
      event.overlap = false
      event.display = "background"
      event.classNames = classNames
      event.title = title
      return event
    }
  }
}
