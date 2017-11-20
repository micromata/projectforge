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

package org.projectforge.framework.calendar;

import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class Holiday
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Holiday.class);

  private boolean workingDay;

  private String i18nKey;

  private String label;

  private BigDecimal workFraction;

  /**
   * @param i18nKey Should be given xor label should be given.
   * @param label Not localized label.
   * @param workingDay
   * @param workingFraction 0.5 means a half working day.
   */
  public Holiday(final String i18nKey, final String label, boolean workingDay, final BigDecimal workingFraction)
  {
    this.i18nKey = i18nKey;
    this.label = label;
    this.workingDay = workingDay;
    this.workFraction = workingFraction;
    if (workingDay == false && workFraction != null) {
      log.warn("Non-working days should not have a work fraction: " + this);
    }
  }

  public String getI18nKey()
  {
    return i18nKey;
  }

  /**
   * @return The label (if given). Label xor i18nKey must be given.
   */
  public String getLabel()
  {
    return this.label;
  }

  public boolean isWorkingDay()
  {
    return workingDay;
  }

  public BigDecimal getWorkFraction()
  {
    return workFraction;
  }

  @Override
  public String toString()
  {
    final ReflectionToStringBuilder builder = new ReflectionToStringBuilder(this);
    return builder.toString();
  }
}
