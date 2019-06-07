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

import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

/**
 * Used in config.xml for (re-)definition of holidays.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "holiday")
public class ConfigureHoliday
{
  @XmlField(asAttribute = true)
  private String label;

  private HolidayDefinition id;

  private boolean ignore;

  private Integer year;

  private Integer month;

  private Integer dayOfMonth;

  private boolean workingDay;

  private BigDecimal workFraction;

  public ConfigureHoliday()
  {
  }

  /**
   * @return Label to display in calendar.
   */
  public String getLabel()
  {
    return label;
  }

  /**
   * @return Id of pre-defined holidays.
   * @see HolidayDefinition
   */
  public HolidayDefinition getId()
  {
    return id;
  }

  /**
   * If not set then this holiday is repeated every year.
   */
  public Integer getYear()
  {
    return year;
  }

  public void setYear(Integer year)
  {
    this.year = year;
  }

  /**
   * @return Month of year (January = 0, December = 11)
   */
  public Integer getMonth()
  {
    return month;
  }

  /**
   * @return Day of month (1..31)
   */
  public Integer getDayOfMonth()
  {
    return dayOfMonth;
  }

  public boolean isWorkingDay()
  {
    return workingDay;
  }

  /**
   * @return null or fraction of working hours if the day is not full a working day (e. g. 0.5 for Xmas Eve or Sylvester for an half working
   *         day).
   */
  public BigDecimal getWorkFraction()
  {
    return workFraction;
  }

  /**
   * Ignore this holiday (pre-defined holiday can therefore be disabled).
   */
  public boolean isIgnore()
  {
    return ignore;
  }

  @Override
  public String toString()
  {
    ReflectionToStringBuilder builder = new ReflectionToStringBuilder(this);
    return builder.toString();
  }
}
