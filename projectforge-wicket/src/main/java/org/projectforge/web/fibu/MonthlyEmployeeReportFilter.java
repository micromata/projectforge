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

import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;

public class MonthlyEmployeeReportFilter implements Serializable
{
  private static final long serialVersionUID = -3700442530651487472L;

  private int year;

  private int month;

  private PFUserDO user;

  public void reset()
  {
    if (year <= 0 || month < 0) {
      DateHolder date = new DateHolder();
      year = date.getYear();
      month = date.getMonth();
    }
    if (user == null) {
      user = ThreadLocalUserContext.getUser();
    }
  }

  public PFUserDO getUser()
  {
    return user;
  }

  public void setUser(PFUserDO user)
  {
    this.user = user;
  }

  public Integer getUserId()
  {
    return user != null ? user.getId() : null;
  }

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

  public String getFormattedMonth()
  {
    return month >= 0 ? StringHelper.format2DigitNumber(month + 1) : "";
  }
}
