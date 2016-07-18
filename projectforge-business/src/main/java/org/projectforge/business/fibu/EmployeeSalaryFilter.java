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

package org.projectforge.business.fibu;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class EmployeeSalaryFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -5950749513241408479L;

  protected int year;

  protected int month;

  public EmployeeSalaryFilter()
  {
  }

  public EmployeeSalaryFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  /**
   * Year of salaries to filter. "<= 0" means showing all years.
   */
  public int getYear()
  {
    return year;
  }

  public void setYear(int year)
  {
    this.year = year;
  }

  /**
   * Month of salaries to filter. "<=0" (for month or year) means showing all months.
   */
  public int getMonth()
  {
    return month;
  }

  public void setMonth(int month)
  {
    this.month = month;
  }
}
