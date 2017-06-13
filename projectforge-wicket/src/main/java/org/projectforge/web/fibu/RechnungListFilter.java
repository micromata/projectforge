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

import java.util.Date;

import org.projectforge.business.fibu.RechnungFilter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("RechnungFilter")
public class RechnungListFilter extends RechnungFilter
{
  private static final long serialVersionUID = -874619598640299510L;

  private Date periodOfPerformanceStartDate;

  private Date periodOfPerformanceEndDate;

  public Date getPeriodOfPerformanceStartDate()
  {
    return periodOfPerformanceStartDate;
  }

  public void setPeriodOfPerformanceStartDate(final Date periodOfPerformanceStartDate)
  {
    this.periodOfPerformanceStartDate = periodOfPerformanceStartDate;
  }

  public Date getPeriodOfPerformanceEndDate()
  {
    return periodOfPerformanceEndDate;
  }

  public void setPeriodOfPerformanceEndDate(final Date periodOfPerformanceEndDate)
  {
    this.periodOfPerformanceEndDate = periodOfPerformanceEndDate;
  }

  @Override
  public RechnungFilter reset()
  {
    periodOfPerformanceStartDate = null;
    periodOfPerformanceEndDate = null;
    return super.reset();
  }
}
