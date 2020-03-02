/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.time.PFDayUtils;

import java.io.Serializable;

public class MonthlyEmployeeReportFilter implements Serializable
{
  private static final long serialVersionUID = -3700442530651487472L;

  private Integer year;

  /**
   * 1-January, ..., 12-December.
   */
  private Integer month;

  private PFUserDO user;

  public void reset()
  {
    if (year <= 0 || month == null) {
      PFDay day = PFDay.now();
      year = day.getYear();
      month = day.getMonthValue();
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

  public Integer getYear()
  {
    return year;
  }

  public void setYear(Integer year)
  {
    this.year = year != null && year > 0 ? year : null;
  }

  /**
   * 1-January, ..., 12-December.
   */
  public Integer getMonth()
  {
    if (month == null || month < 1) {
      // May occur, if deserialized from old 0-based format.
      month = 1;
    }
    return month;
  }

  /**
   * 1-January, ..., 12-December.
   */
  public void setMonth(Integer month)
  {
    this.month = PFDayUtils.validateMonthValue(month);
  }

  public String getFormattedMonth()
  {
    return month != null ? StringHelper.format2DigitNumber(month) : "";
  }
}
