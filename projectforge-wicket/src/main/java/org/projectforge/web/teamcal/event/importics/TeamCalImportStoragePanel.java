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

package org.projectforge.web.teamcal.event.importics;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.core.importstorage.AbstractImportStoragePanel;
import org.projectforge.web.core.importstorage.ImportFilter;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class TeamCalImportStoragePanel extends AbstractImportStoragePanel<TeamCalImportPage>
{
  private static final long serialVersionUID = -2102550089275871727L;

  /**
   * @param id
   */
  public TeamCalImportStoragePanel(final String id, final TeamCalImportPage parentPage, final ImportFilter filter)
  {
    super(id, parentPage, filter);
  }

  /**
   * @see org.projectforge.web.core.importstorage.AbstractImportStoragePanel#addHeadColumns(org.apache.wicket.markup.repeater.RepeatingView)
   */
  @Override
  protected void addHeadColumns(final RepeatingView headColRepeater)
  {
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("plugins.teamcal.event.subject")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("plugins.teamcal.event.location")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("plugins.teamcal.event.allDay")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("plugins.teamcal.event.beginDate")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("plugins.teamcal.event.endDate")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("plugins.teamcal.event.note")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("plugins.teamcal.event.recurrence")));
    headColRepeater.add(new Label(headColRepeater.newChildId(), getString("plugins.teamcal.event.recurrence.until")));
  }

  /**
   * @see org.projectforge.web.core.importstorage.AbstractImportStoragePanel#addColumns(org.apache.wicket.markup.repeater.RepeatingView,
   *      org.projectforge.framework.persistence.utils.ImportedElement)
   */
  @Override
  protected void addColumns(final RepeatingView cellRepeater, final ImportedElement<?> element, final String style)
  {
    final TeamEventDO event = (TeamEventDO) element.getValue();
    addCell(cellRepeater, event.getSubject(), style);
    addCell(cellRepeater, event.getLocation(), style);
    addCell(cellRepeater, String.valueOf(event.getAllDay()), style);
    addCell(cellRepeater, DateTimeFormatter.instance().getFormattedDateTime(event.getStartDate()), style);
    addCell(cellRepeater, DateTimeFormatter.instance().getFormattedDateTime(event.getEndDate()), style);
    addCell(cellRepeater, event.getNote(), style);
    addCell(cellRepeater, event.getRecurrenceRule(), style);
    addCell(cellRepeater, DateTimeFormatter.instance().getFormattedDate(event.getRecurrenceUntil()), style);
  }
}
