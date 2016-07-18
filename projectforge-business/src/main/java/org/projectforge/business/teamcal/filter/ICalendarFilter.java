/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.io.Serializable;

import org.joda.time.DateMidnight;

/**
 * Some settings for the SelectDateAction stored in the flow scope (configurable by the caller).
 */
public interface ICalendarFilter extends Serializable
{
  /**
   * @return the startDate
   */
  public DateMidnight getStartDate();

  /**
   * @param startDate the startDate to set
   * @return this for chaining.
   */
  public ICalendarFilter setStartDate(final DateMidnight startDate);

  /**
   * If true then the slot is 30 minutes otherwise 15 minutes.
   * 
   * @return the slot30
   */
  public boolean isSlot30();

  /**
   * @param slot30 the slot30 to set
   * @return this for chaining.
   */
  public ICalendarFilter setSlot30(final boolean slot30);

  /**
   * @return the viewType
   */
  public ViewType getViewType();

  /**
   * @param viewType the viewType to set
   * @return this for chaining.
   */
  public ICalendarFilter setViewType(final ViewType viewType);

  /**
   * @return the firstHour to display in week mode of calendar.
   */
  public Integer getFirstHour();

  /**
   * @param firstHour the firstHour to set
   * @return this for chaining.
   */
  public ICalendarFilter setFirstHour(final Integer firstHour);

  public boolean isShowBirthdays();

  public ICalendarFilter setShowBirthdays(final boolean showBirthdays);

  /**
   * @return the showStatistics
   */
  public boolean isShowStatistics();

  /**
   * @param showStatistics the showStatistics to set
   * @return this for chaining.
   */
  public ICalendarFilter setShowStatistics(final boolean showStatistics);

  /**
   * @return the showPlanning
   */
  public boolean isShowPlanning();

  /**
   * @param showPlanning the showPlanning to set
   * @return this for chaining.
   */
  public ICalendarFilter setShowPlanning(final boolean showPlanning);

  /**
   * The user id of the user for showing his time sheets.
   * 
   * @return this for chaining.
   */
  public Integer getTimesheetUserId();

  public ICalendarFilter setTimesheetUserId(final Integer timesheetUserId);

  /**
   * Show timesheets?
   * 
   * @return true if the time-sheet user id is given.
   */
  public boolean isShowTimesheets();

  /**
   * If true then the current logged-in user is set as time-sheet user.
   * 
   * @param showTimesheets
   * @return this for chaining.
   */
  public ICalendarFilter setShowTimesheets(boolean showTimesheets);

  /**
   * If the time sheets of an user are displayed and this option is set, then also all breaks between time-sheets of
   * ones day will be displayed.
   * 
   * @return the showBreaks
   */
  public boolean isShowBreaks();

  /**
   * @param showBreaks the showBreaks to set
   * @return this for chaining.
   */
  public ICalendarFilter setShowBreaks(final boolean showBreaks);

  /**
   * @return the selectedCalendar
   */
  public String getSelectedCalendar();

  /**
   * @param selectedCalendar the selectedCalendar to set
   * @return this for chaining.
   */
  public ICalendarFilter setSelectedCalendar(final String selectedCalendar);
}
