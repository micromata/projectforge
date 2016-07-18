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

package org.projectforge.web.fibu;

import java.io.Serializable;

import org.projectforge.business.fibu.EmployeeSalaryType;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Stores the last added salary entry. Year and month will be persisted for pre-filling on adding next entry.
 */
@XStreamAlias("EmployeeSalaryEditRecentEntry")
public class EmployeeSalaryEditRecentEntry implements Serializable
{
  private static final long serialVersionUID = 5811156553895802033L;

  private int year;

  private int month;
  
  private EmployeeSalaryType type;

  public int getYear()
  {
    return year;
  }

  public void setYear(int year)
  {
    this.year = year;
  }

  public int getMonth()
  {
    return month;
  }

  public void setMonth(int month)
  {
    this.month = month;
  }
  
  public EmployeeSalaryType getType()
  {
    return type;
  }
  
  public void setType(EmployeeSalaryType type)
  {
    this.type = type;
  }
}
