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

import java.util.Date;

import org.projectforge.Const;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Some settings for the SelectDateAction stored in the flow scope (configurable by the caller).
 * Will be removed when Wicket filter is replaced by React filter in ProjectForge version 7.
 */
@Deprecated
@XStreamAlias("dateFilter")
public class CalendarFilter extends AbstractCalendarFilter
{
  private static final long serialVersionUID = -4154764049316136395L;

  @SuppressWarnings("unused")
  @Deprecated
  @XStreamAsAttribute
  private transient Date current;

  @SuppressWarnings("unused")
  @Deprecated
  @XStreamAsAttribute
  private transient Integer userId;

  @XStreamAsAttribute
  private String selectedCalendar;

  @XStreamAsAttribute
  private Boolean showBirthdays;

  @XStreamAsAttribute
  private Boolean showStatistics;

  @XStreamAsAttribute
  private Integer timesheetUserId;

  @XStreamAsAttribute
  private Boolean showBreaks = true;

  @XStreamAsAttribute
  private Boolean showPlanning;

  public CalendarFilter()
  {
    super();
    timesheetUserId = ThreadLocalUserContext.getUserId();
    selectedCalendar = Const.EVENT_CLASS_NAME;
  }

  public boolean isShowBirthdays()
  {
    return showBirthdays == Boolean.TRUE;
  }

  public CalendarFilter setShowBirthdays(final boolean showBirthdays)
  {
    this.showBirthdays = showBirthdays;
    return this;
  }

  /**
   * @return the showStatistics
   */
  public boolean isShowStatistics()
  {
    return showStatistics == Boolean.TRUE;
  }

  /**
   * @param showStatistics the showStatistics to set
   * @return this for chaining.
   */
  public CalendarFilter setShowStatistics(final boolean showStatistics)
  {
    this.showStatistics = showStatistics;
    return this;
  }

  /**
   * @return the showPlanning
   */
  public boolean isShowPlanning()
  {
    return showPlanning == Boolean.TRUE;
  }

  /**
   * @param showPlanning the showPlanning to set
   * @return this for chaining.
   */
  public CalendarFilter setShowPlanning(final boolean showPlanning)
  {
    this.showPlanning = showPlanning;
    return this;
  }

  public Integer getTimesheetUserId()
  {
    return timesheetUserId;
  }

  public CalendarFilter setTimesheetUserId(final Integer timesheetUserId)
  {
    this.timesheetUserId = timesheetUserId;
    return this;
  }

  /**
   * @see org.projectforge.business.teamcal.filter.ICalendarFilter#isShowTimesheets()
   */
  @Override
  public boolean isShowTimesheets()
  {
    return this.timesheetUserId != null;
  }

  /**
   * @see org.projectforge.business.teamcal.filter.ICalendarFilter#setShowTimesheets(boolean)
   */
  @Override
  public CalendarFilter setShowTimesheets(final boolean showTimesheets)
  {
    if (showTimesheets == true) {
      this.timesheetUserId = ThreadLocalUserContext.getUserId();
    } else {
      this.timesheetUserId = null;
    }
    return this;
  }

  /**
   * If the time sheets of an user are displayed and this option is set, then also all breaks between time-sheets of
   * ones day will be displayed.
   * 
   * @return the showBreaks
   */
  public boolean isShowBreaks()
  {
    return showBreaks == Boolean.TRUE;
  }

  /**
   * @param showBreaks the showBreaks to set
   * @return this for chaining.
   */
  public CalendarFilter setShowBreaks(final boolean showBreaks)
  {
    this.showBreaks = showBreaks;
    return this;
  }

  /**
   * @return the selectedCalendar
   */
  public String getSelectedCalendar()
  {
    return selectedCalendar;
  }

  /**
   * @param selectedCalendar the selectedCalendar to set
   * @return this for chaining.
   */
  public CalendarFilter setSelectedCalendar(final String selectedCalendar)
  {
    this.selectedCalendar = selectedCalendar;
    return this;
  }
}
