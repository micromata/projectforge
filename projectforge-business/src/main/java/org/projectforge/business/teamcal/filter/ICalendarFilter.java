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

package org.projectforge.business.teamcal.filter;

import org.joda.time.DateMidnight;

import java.io.Serializable;

/**
 * Some settings for the SelectDateAction stored in the flow scope (configurable by the caller).
 * Will be removed when Wicket filter is replaced by React filter in ProjectForge version 7.
 */
@Deprecated
public interface ICalendarFilter extends Serializable
{
  /**
   * @return the startDate
   */
  DateMidnight getStartDate();

  /**
   * @param startDate the startDate to set
   * @return this for chaining.
   */
  ICalendarFilter setStartDate(final DateMidnight startDate);

  /**
   * If true then the slot is 30 minutes otherwise 15 minutes.
   * 
   * @return the slot30
   */
  boolean isSlot30();

  /**
   * @param slot30 the slot30 to set
   * @return this for chaining.
   */
  ICalendarFilter setSlot30(final boolean slot30);

  /**
   * @return the viewType
   */
  ViewType getViewType();

  /**
   * @param viewType the viewType to set
   * @return this for chaining.
   */
  ICalendarFilter setViewType(final ViewType viewType);

  /**
   * @return the firstHour to display in week mode of calendar.
   */
  Integer getFirstHour();

  /**
   * @param firstHour the firstHour to set
   * @return this for chaining.
   */
  ICalendarFilter setFirstHour(final Integer firstHour);

  boolean isShowBirthdays();

  ICalendarFilter setShowBirthdays(final boolean showBirthdays);

  /**
   * @return the showStatistics
   */
  boolean isShowStatistics();

  /**
   * @param showStatistics the showStatistics to set
   * @return this for chaining.
   */
  ICalendarFilter setShowStatistics(final boolean showStatistics);

  /**
   * @return the showPlanning
   */
  boolean isShowPlanning();

  /**
   * @param showPlanning the showPlanning to set
   * @return this for chaining.
   */
  ICalendarFilter setShowPlanning(final boolean showPlanning);

  /**
   * The user id of the user for showing his time sheets.
   * 
   * @return this for chaining.
   */
  Integer getTimesheetUserId();

  ICalendarFilter setTimesheetUserId(final Integer timesheetUserId);

  /**
   * Show timesheets?
   * 
   * @return true if the time-sheet user id is given.
   */
  boolean isShowTimesheets();

  /**
   * If true then the current logged-in user is set as time-sheet user.
   * 
   * @param showTimesheets
   * @return this for chaining.
   */
  ICalendarFilter setShowTimesheets(boolean showTimesheets);

  /**
   * If the time sheets of an user are displayed and this option is set, then also all breaks between time-sheets of
   * ones day will be displayed.
   * 
   * @return the showBreaks
   */
  boolean isShowBreaks();

  /**
   * @param showBreaks the showBreaks to set
   * @return this for chaining.
   */
  ICalendarFilter setShowBreaks(final boolean showBreaks);

  /**
   * @return the selectedCalendar
   */
  String getSelectedCalendar();

  /**
   * @param selectedCalendar the selectedCalendar to set
   * @return this for chaining.
   */
  ICalendarFilter setSelectedCalendar(final String selectedCalendar);
}
