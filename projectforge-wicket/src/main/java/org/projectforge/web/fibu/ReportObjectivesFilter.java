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
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.framework.time.DateHolder;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("ReportObjectivesFilter")
public class ReportObjectivesFilter implements Serializable
{
  private static final long serialVersionUID = -5213357252774369959L;

  private Date from, to;

  /**
   * Sets the begin of the month.
   * @param from
   */
  public void setFromDate(final Date from)
  {
    if (from == null) {
      this.from = null;
      return;
    }
    final DateHolder day = new DateHolder(from);
    day.setBeginOfMonth();
    this.from = day.getDate();
  }

  public Date getFromDate()
  {
    return from;
  }

  /**
   * Sets the end of the month.
   * @param to
   */
  public void setToDate(final Date to)
  {
    if (to == null) {
      this.to = null;
      return;
    }
    final DateHolder day = new DateHolder(to);
    day.setEndOfMonth();
    this.to = day.getDate();
  }

  public Date getToDate()
  {
    return to;
  }

  @Override
  public String toString()
  {
    return new ToStringBuilder(this).append("from", from).append("to", to).toString();
  }
}
