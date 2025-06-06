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

package org.projectforge.web.gantt;

import org.projectforge.web.wicket.AbstractListForm;
import org.slf4j.Logger;

public class GanttChartListForm extends AbstractListForm<GanttChartListFilter, GanttChartListPage>
{
  private static final long serialVersionUID = 80462620378178721L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GanttChartListForm.class);

  public GanttChartListForm(final GanttChartListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected GanttChartListFilter newSearchFilterInstance()
  {
    return new GanttChartListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
