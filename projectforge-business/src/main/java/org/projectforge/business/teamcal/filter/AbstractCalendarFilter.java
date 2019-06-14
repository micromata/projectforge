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

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Will be removed when Wicket filter is replaced by React filter in ProjectForge version 7.
 */
@Deprecated
public abstract class AbstractCalendarFilter implements ICalendarFilter
{
  private static final long serialVersionUID = -2054541010143924205L;

  @XStreamAsAttribute
  private DateMidnight startDate;

  @XStreamAsAttribute
  private Integer firstHour = 8;

  @XStreamAsAttribute
  private Boolean slot30;

  @XStreamAsAttribute
  private ViewType viewType;

  public AbstractCalendarFilter()
  {
    startDate = new DateMidnight();
  }

  /**
   * @return the startDate
   */
  public DateMidnight getStartDate()
  {
    return startDate;
  }

  /**
   * @param startDate the startDate to set
   * @return this for chaining.
   */
  public AbstractCalendarFilter setStartDate(final DateMidnight startDate)
  {
    if (startDate != null) {
      this.startDate = startDate;
    } else {
      this.startDate = new DateMidnight();
    }
    return this;
  }

  /**
   * If true then the slot is 30 minutes otherwise 15 minutes.
   * 
   * @return the slot30
   */
  public boolean isSlot30()
  {
    return slot30 == Boolean.TRUE;
  }

  /**
   * @param slot30 the slot30 to set
   * @return this for chaining.
   */
  public AbstractCalendarFilter setSlot30(final boolean slot30)
  {
    this.slot30 = slot30;
    return this;
  }

  /**
   * @return the viewType
   */
  public ViewType getViewType()
  {
    return viewType != null ? viewType : ViewType.AGENDA_WEEK;
  }

  /**
   * @param viewType the viewType to set
   * @return this for chaining.
   */
  public AbstractCalendarFilter setViewType(final ViewType viewType)
  {
    this.viewType = viewType;
    return this;
  }

  /**
   * @return the firstHour to display in week mode of calendar.
   */
  public Integer getFirstHour()
  {
    return (firstHour != null && firstHour < 24) ? firstHour : 8;
  }

  /**
   * @param firstHour the firstHour to set
   * @return this for chaining.
   */
  public AbstractCalendarFilter setFirstHour(final Integer firstHour)
  {
    this.firstHour = firstHour;
    return this;
  }
}
