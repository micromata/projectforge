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

package org.projectforge.framework.calendar;

import java.io.Serializable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents a date (day of month, month and year). It contains three integer values and has no time zone information.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XStreamAlias("day")
public class DayMonthYearHolder implements Serializable
{
  private static final long serialVersionUID = 8775389501399585485L;

  @XStreamAsAttribute
  private short dayOfMonth = 1;

  @XStreamAsAttribute
  private short month = 1;

  @XStreamAsAttribute
  private int year = 1970;

  public DayMonthYearHolder()
  {
  }

  public DayMonthYearHolder(final short dayOfMonth, final short month, final int year)
  {
    this.dayOfMonth = dayOfMonth;
    this.month = month;
    this.year = year;
  }

  /**
   * 1 - 31.
   */
  public short getDayOfMonth()
  {
    return dayOfMonth;
  }

  /**
   * No sanity check will be done.
   * @param dayOfMonth
   * @return this for chaining.
   */
  public DayMonthYearHolder setDayOfMonth(final short dayOfMonth)
  {
    this.dayOfMonth = dayOfMonth;
    return this;
  }

  /**
   * 1 (January) - 12 (December).
   */
  public short getMonth()
  {
    return month;
  }

  /**
   * /** No sanity check will be done.
   * @param month
   * @return this for chaining.
   */
  public DayMonthYearHolder setMonth(final short month)
  {
    this.month = month;
    return this;
  }

  public int getYear()
  {
    return year;
  }

  public DayMonthYearHolder setYear(final int year)
  {
    this.year = year;
    return this;
  }

}
