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

import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.TimePeriod
import org.projectforge.framework.utils.NumberFormatter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class FullCalendarEvent(
  /** Unique identifier. */
  var id: String? = null,
  category: Category? = null,
  var title: String? = null,
  var description: String? = null,
  var allDay: Boolean? = null,
  var textColor: String? = null,
  var backgroundColor: String? = null,
  var classNames: String? = null,
  var editable: Boolean = false,
) {
  enum class Category(val string: String) {
    BIRTHDAY("address"),
    TIMESHEET("timesheet"),
    TIMESHEET_STATS("timesheet-stats"),
    VACATION("vacation"),
    CAL_EVENT("calEvent"),
    TEAM_CAL_EVENT("teamEvent"),
    HOLIDAY("holiday"),
  }

  class EventDate(
    var date: Date? = null,
    var day: LocalDate? = null,
  )

  class ExtendedProps(
    category: Category? = null,
    var duration: String? = null,
    /**
     * For subscribed events.
     */
    var uid: String? = null,
    /**
     * The db id of the object (team event, address (birthday) etc.)
     */
    var dbId: Int? = null,
    /**
     * If given, a tooltip will be displayed on mouse-over. Don't forget to add the content of the tooltip.
     */
    var tooltip: Tooltip? = null,
  ) {
    var category = category?.string
  }

  class Tooltip(var title: String?, var text: String)

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

  fun setTooltip(
    title: String? = null,
    tooltipBuilder: TooltipBuilder,
  ): Tooltip {
    return setTooltip(title, tooltipBuilder.toString())
  }

  fun setTooltip(
    title: String? = null,
    text: String,
  ): Tooltip {
    ensureExtendedProps().let { extendedProps ->
      Tooltip(title, text).let { tooltip ->
        extendedProps.tooltip = tooltip
        return tooltip
      }
    }
  }

  fun setDuration(duration: Long) {
    ensureExtendedProps().let { props ->
      props.duration = formatDuration(duration)
    }
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
      description: String? = null,
      allDay: Boolean? = false,
      textColor: String? = null,
      backgroundColor: String? = null,
      classNames: String? = null,
      dbId: Int? = null,
      uid: String? = null,
      editable: Boolean = false,
      formattedDuration: String? = null,
      duration: Long? = null,
    ): FullCalendarEvent {
      val event = FullCalendarEvent(
        id = "$category-${id?.toString() ?: "-1"}",
        category = category,
        title = title,
        description = description,
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
        if (formattedDuration != null) {
          props.duration = formattedDuration
        } else if (duration != null) {
          event.setDuration(duration)
        } else {
          event.setDuration(end.time - start.time)
        }
      }
      return event
    }

    /**
     * @param end LocalDate inclusive (plus one day is used for export for Fullcalendar which uses enddate as
     * exclusive for allDay events.
     */
    fun createAllDayEvent(
      title: String?,
      start: LocalDate,
      end: LocalDate = start,
      id: Any? = null,
      description: String? = null,
      category: Category? = null,
      textColor: String? = null,
      backgroundColor: String? = null,
      classNames: String? = null,
      dbId: Int? = null,
      uid: String? = null,
      editable: Boolean = false,
      formattedDuration: String? = null,
      durationDays: BigDecimal? = null,
    ): FullCalendarEvent {
      val event = FullCalendarEvent(
        id = "$category-${id?.toString() ?: "-1"}",
        category = category,
        allDay = true,
        title = title,
        description = description,
        textColor = textColor,
        backgroundColor = backgroundColor,
        classNames = classNames,
        editable = editable,
      )
      event.start = EventDate(day = start)
      event.end = EventDate(day = end.plusDays(1))
      event.ensureExtendedProps().let { props ->
        props.dbId = dbId
        props.uid = uid
        if (formattedDuration != null) {
          props.duration = formattedDuration
        } else {
          val days = if (durationDays != null) {
            NumberFormatter.format(durationDays)
          } else {
            (ChronoUnit.DAYS.between(start, end) + 1).toString()
          }
          val unit = if (days == "1") "calendar.day" else "days"
          props.duration = "$days ${translate(unit)}"
        }
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
      description: String? = null,
      category: Category? = null,
    ): FullCalendarEvent {
      val event = FullCalendarEvent(
        category = category,
        title = title,
        description = description,
        classNames = classNames,
      )
      event.start = EventDate(day = start)
      event.end = EventDate(day = end ?: start)
      event.display = "background"
      return event
    }

    /**
     * @see TimePeriod.getDurationFields
     */
    fun formatDuration(millis: Long, hoursPerDay: Int = 24, minHours4DaySeparation: Int = 24): String {
      val fields = TimePeriod.getDurationFields(millis, hoursPerDay, minHours4DaySeparation)
      val buf = StringBuffer()
      if (fields[0] > 0) {
        buf.append(fields[0]).append(translate("calendar.unit.day")).append(" ")
      }
      buf.append(fields[1]).append(":").append(StringHelper.format2DigitNumber(fields[2]))
        .append(translate("calendar.unit.hour"))
      return buf.toString()
    }
  }
}
