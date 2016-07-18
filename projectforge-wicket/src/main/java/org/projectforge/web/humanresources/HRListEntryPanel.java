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

package org.projectforge.web.humanresources;

import java.math.BigDecimal;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.projectforge.business.humanresources.HRFilter;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HRListEntryPanel extends Panel
{

  private static final long serialVersionUID = -718881597957595460L;

  public HRListEntryPanel(final String id, final HRFilter filter, final BigDecimal plannedDays, final BigDecimal actualDays,
      final Link< ? > link)
  {
    super(id);
    final BigDecimal planned = filter.isShowPlanning() == true ? plannedDays : null;
    final BigDecimal actual = filter.isShowBookedTimesheets() == true ? actualDays : null;
    final Label plannedDaysLabel = new Label("plannedDays", NumberFormatter.format(planned, 2));
    add(plannedDaysLabel.setRenderBodyOnly(true));
    if (NumberHelper.isNotZero(plannedDays) == false) {
      plannedDaysLabel.setVisible(false);
    }
    add(link);
    final Label actualDaysLabel = new Label("actualDays", "(" + NumberFormatter.format(actual, 2) + ")");
    link.add(actualDaysLabel.setRenderBodyOnly(true));
    if (NumberHelper.isNotZero(actualDays) == false) {
      link.setVisible(false);
    }
  }
}
