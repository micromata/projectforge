/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.model.LambdaModel;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.RechnungListFilter;
import org.projectforge.business.fibu.RechnungsStatistik;
import org.slf4j.Logger;

import java.io.Serial;

public class RechnungListForm extends AbstractRechnungListForm<RechnungListFilter, RechnungListPage>
{
  @Serial
  private static final long serialVersionUID = 1657084619520768905L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RechnungListForm.class);

  @Override
  protected void init()
  {
    final RechnungDao rechnungDao = getParentPage().getBaseDao();
    this.years = rechnungDao.getYears();
    super.init();
  }

  @Override
  protected void onBeforeAddStatistics()
  {
    // time period for period of performance
    final RechnungListFilter filter = getSearchFilter();
    addTimePeriodPanel("fibu.periodOfPerformance",
        LambdaModel.of(filter::getPeriodOfPerformanceStartDate, filter::setPeriodOfPerformanceStartDate),
        LambdaModel.of(filter::getPeriodOfPerformanceEndDate, filter::setPeriodOfPerformanceEndDate)
    );
  }

  @Override
  protected RechnungsStatistik getStats()
  {
    return parentPage.getRechnungsStatistik();
  }

  public RechnungListForm(final RechnungListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected RechnungListFilter newSearchFilterInstance()
  {
    return new RechnungListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
