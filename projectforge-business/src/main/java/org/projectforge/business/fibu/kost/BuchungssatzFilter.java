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

package org.projectforge.business.fibu.kost;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class BuchungssatzFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -7413778899432789456L;

  private int fromYear;

  private int toYear;

  private int fromMonth;

  private int toMonth;

  public BuchungssatzFilter()
  {
  }

  public BuchungssatzFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  @Override
  public BuchungssatzFilter reset()
  {
    super.reset();
    toMonth = fromMonth;
    toYear = fromYear;
    return this;
  }

  /**
   * @param year
   * @param month 0-11
   */
  public void setFrom(final int year, final int month)
  {
    this.fromYear = year;
    this.fromMonth = month;
  }

  /**
   * @param year
   * @param month 0-11
   */
  public void setTo(final int year, final int month)
  {
    this.toYear = year;
    this.toMonth = month;
  }

  public int getFromYear()
  {
    return fromYear;
  }

  public void setFromYear(final int fromYear)
  {
    this.fromYear = fromYear;
  }

  /**
   * @return month (0-11)
   */
  public int getFromMonth()
  {
    return fromMonth;
  }

  public void setFromMonth(final int fromMonth)
  {
    this.fromMonth = fromMonth;
  }

  public int getToYear()
  {
    return toYear;
  }

  public void setToYear(final int toYear)
  {
    this.toYear = toYear;
  }

  /**
   * @return month (0-11)
   */
  public int getToMonth()
  {
    return toMonth;
  }

  public void setToMonth(final int toMonth)
  {
    this.toMonth = toMonth;
  }
}
