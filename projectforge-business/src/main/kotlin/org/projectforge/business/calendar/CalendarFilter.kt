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

package org.projectforge.business.calendar

import org.projectforge.business.teamcal.filter.TemplateEntry
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext

/**
 * Persist the settings of one named filter entry. The user may configure a list of filters and my switch the active
 * calendar filter.
 *
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class CalendarFilter(
  name: String? = null,
  id: Int? = null,
  /**
   * New items created in the calendar will be assumed as entries of this calendar. If null, then the creation
   * page for new time sheets is instantiated.
   */
  var defaultCalendarId: Int? = null,

  /**
   * Grid size of the calendar to display in minutes (60 should be dividable by step).
   */
  var gridSize: Int = 15,

  /**
   * The first hour of day to show in time grids (week and day view).
   */
  var firstHour: Int = 8,

  var otherTimesheetUsersEnabled: Boolean = false,
  /**
   * Display the time sheets of the user with this id. If null, no time sheets are displayed.
   */
  var timesheetUserId: Int? = null,

  /**
   * Check box for enabling and disabling vacation entries.
   */
  // var showVacations: Boolean = false,

  /**
   * All vacations of any employee assigned to at least one of this
   * vacationGroups (group ids) will be displayed.
   * Null items should only occur on (de)serialization issues.
   */
  var vacationGroupIds: Set<Int?>? = null,

  /**
   * All vacations of the given employees (by id) will be displayed.
   * Null items should only occur on (de)serialization issues.
   */
  var vacationUserIds: Set<Int?>? = null,

  /**
   * If true, breaks between time sheets of a day will be displayed. If the user clicks on a break, a time sheet
   * with the start and stop time of the break could easily be created.
   */
  var showBreaks: Boolean? = null,

  /**
   * Not yet supported.
   */
  var showPlanning: Boolean? = null
) : AbstractFavorite(name, id) {

  /**
   * All calendars of this filter (visible and invisible ones).
   */
  var calendarIds = mutableSetOf<Int?>()

  /**
   * Some calendarIds aren't visible (if they are listed here).
   */
  var invisibleCalendars = mutableSetOf<Int?>()

  /**
   * Makes a deep copy of all values.
   * @return this for chaining.
   */
  fun copyFrom(src: CalendarFilter): CalendarFilter {
    this.name = src.name
    this.id = src.id
    this.defaultCalendarId = src.defaultCalendarId
    this.timesheetUserId = src.timesheetUserId
    //this.showVacations = src.showVacations
    this.vacationGroupIds = copySet(src.vacationGroupIds)
    this.vacationUserIds = copySet(src.vacationUserIds)
    this.gridSize = src.gridSize
    this.firstHour = src.firstHour
    this.showBreaks = src.showBreaks
    this.showPlanning = src.showPlanning
    this.calendarIds = mutableSetOf()
    this.calendarIds.addAll(src.calendarIds)
    this.invisibleCalendars = mutableSetOf()
    this.invisibleCalendars.addAll(src.invisibleCalendars)
    return this
  }

  fun addCalendarId(calendarId: Int) {
    calendarIds.add(calendarId)
    invisibleCalendars.remove(calendarId) // New added calendars should be visible.
  }

  fun removeCalendarId(calendarId: Int) {
    calendarIds.remove(calendarId)
    invisibleCalendars.remove(calendarId)
  }

  /**
   * @param calendarId Null should only occur on (de)serialization issues.
   */
  fun setVisibility(calendarId: Int?, visible: Boolean) {
    calendarId ?: return // May occur during (de-)serialization
    if (visible) {
      invisibleCalendars.remove(calendarId)
    } else {
      invisibleCalendars.add(calendarId)
    }
    tidyUp()
  }

  /**
   * @param calendarId Null should only occur on (de)serialization issues.
   */
  fun isInvisible(calendarId: Int?): Boolean {
    return calendarId == null || invisibleCalendars.contains(calendarId)
  }

  /**
   * This method tidies up the list of invisible calendars by
   * removing invisible calendars not contained in the main calendar set.
   */
  @Suppress("SENSELESS_COMPARISON")
  fun tidyUp() {
    invisibleCalendars.removeIf { !calendarIds.contains(it) } // Tidy up: remove invisible ids if not in main list.
  }

  /**
   * The sets [calendarIds] and [invisibleCalendars] may contain a null value after deserialization. This will be removed by calling this
   * function. [tidyUp] will also be called.
   */
  @Suppress("SENSELESS_COMPARISON")
  fun afterDeserialization() {
    val nullValue: Int? = null
    if (calendarIds == null) {
      calendarIds = mutableSetOf() // Might be null after deserialization.
    } else {
      calendarIds.remove(nullValue) // Might occur after deserialization.
    }
    if (invisibleCalendars == null) {
      invisibleCalendars = mutableSetOf() // Might be null after deserialization.
    } else {
      invisibleCalendars.remove(nullValue) // Might occur after deserialization.
    }
    tidyUp()
  }

  fun isModified(other: CalendarFilter): Boolean {
    return this.name != other.name ||
        this.id != other.id ||
        this.defaultCalendarId != other.defaultCalendarId ||
        this.timesheetUserId != other.timesheetUserId ||
        // this.showVacations != other.showVacations ||
        isModified(this.vacationGroupIds, other.vacationGroupIds) ||
        isModified(this.vacationUserIds, other.vacationUserIds) ||
        this.gridSize != other.gridSize ||
        this.firstHour != other.firstHour ||
        this.showBreaks != other.showBreaks ||
        this.showPlanning != other.showPlanning ||
        isModified(this.calendarIds, other.calendarIds) ||
        isModified(this.invisibleCalendars, other.invisibleCalendars)
  }

  private fun copySet(srcSet: Set<Int?>?): Set<Int>? {
    if (srcSet == null) {
      return null
    }
    val list = mutableSetOf<Int>()
    list.addAll(srcSet.filterNotNull())
    return list
  }

  private fun isModified(col1: Collection<Int?>?, col2: Collection<Int?>?): Boolean {
    if (col1 == null || col2 == null) {
      return col1 != col2
    }
    col1.forEach {
      if (it != null && !col2.contains(it)) {
        return true
      }
    }
    col2.forEach {
      if (it != null && !col1.contains(it)) {
        return true
      }
    }
    return false
  }

  companion object {
    // LEGACY STUFF:

    /**
     * For re-using legacy filters (from ProjectForge version up to 6, Wicket-Calendar).
     */
    internal fun copyFrom(templateEntry: TemplateEntry?): CalendarFilter {
      val filter = CalendarFilter()
      if (templateEntry != null) {
        filter.defaultCalendarId = templateEntry.defaultCalendarId
        filter.name = templateEntry.name
        filter.showBreaks = templateEntry.isShowBreaks
        filter.showPlanning = templateEntry.isShowPlanning
        filter.timesheetUserId = templateEntry.timesheetUserId
        if (templateEntry.isShowTimesheets)
          filter.timesheetUserId = ThreadLocalUserContext.getUserId()
        templateEntry.calendarProperties?.forEach {
          filter.addCalendarId(it.calId)
        }
        filter.calendarIds.forEach {
          filter.setVisibility(it, templateEntry.isVisible(it))
        }
      }
      return filter
    }
  }
}
