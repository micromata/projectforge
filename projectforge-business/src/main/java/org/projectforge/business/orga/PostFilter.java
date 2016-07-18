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

package org.projectforge.business.orga;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;


/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class PostFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = 3285658836430413602L;

  protected int year = -1;

  protected int month = -1;

  public PostFilter()
  {
  }

  public PostFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }


  /**
   * Year of invoices to filter. "<= 0" means showing all years.
   * @return
   */
  public int getYear()
  {
    return year;
  }

  public void setYear(final int year)
  {
    this.year = year;
  }

  /**
   * Month of invoices to filter. "<=0" (for month or year) means showing all months.
   * @return
   */
  public int getMonth()
  {
    return month;
  }

  public void setMonth(final int month)
  {
    this.month = month;
  }
}
