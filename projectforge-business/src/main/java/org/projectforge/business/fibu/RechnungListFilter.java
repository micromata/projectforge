/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.projectforge.framework.persistence.api.BaseSearchFilter;

import java.time.LocalDate;

@XStreamAlias("RechnungFilter")
public class RechnungListFilter extends RechnungFilter implements SearchFilterWithPeriodOfPerformance
{
  private static final long serialVersionUID = -874619598640299510L;

  private LocalDate periodOfPerformanceStartDate;

  private LocalDate periodOfPerformanceEndDate;

  public RechnungListFilter()
  {
  }

  public RechnungListFilter(final BaseSearchFilter filter)
  {
    super(filter);

    if (filter instanceof RechnungListFilter) {
      this.periodOfPerformanceStartDate = ((RechnungListFilter) filter).getPeriodOfPerformanceStartDate();
      this.periodOfPerformanceEndDate = ((RechnungListFilter) filter).getPeriodOfPerformanceEndDate();
      setShowKostZuweisungStatus(((RechnungListFilter) filter).isShowKostZuweisungStatus());
    }
  }

  @Override
  public LocalDate getPeriodOfPerformanceStartDate()
  {
    return periodOfPerformanceStartDate;
  }

  public void setPeriodOfPerformanceStartDate(final LocalDate periodOfPerformanceStartDate)
  {
    this.periodOfPerformanceStartDate = periodOfPerformanceStartDate;
  }

  @Override
  public LocalDate getPeriodOfPerformanceEndDate()
  {
    return periodOfPerformanceEndDate;
  }

  public void setPeriodOfPerformanceEndDate(final LocalDate periodOfPerformanceEndDate)
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
