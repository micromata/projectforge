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

package org.projectforge.web.access;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.web.wicket.flowlayout.ButtonGroupPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;

/**
 * Rows of access rights (without header).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AccessEditTablePanel extends Panel
{
  private static final long serialVersionUID = -7347864057331989812L;

  private final GroupTaskAccessDO data;

  public AccessEditTablePanel(final String id, final GroupTaskAccessDO data)
  {
    super(id);
    this.data = data;
  }

  public AccessEditTablePanel init()
  {
    final RepeatingView rowRepeater = new RepeatingView("accessRows");
    add(rowRepeater);
    addAccessRow(rowRepeater, data.ensureAndGetAccessEntry(AccessType.TASK_ACCESS_MANAGEMENT));
    addAccessRow(rowRepeater, data.ensureAndGetAccessEntry(AccessType.TASKS));
    addAccessRow(rowRepeater, data.ensureAndGetAccessEntry(AccessType.TIMESHEETS));
    addAccessRow(rowRepeater, data.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS));
    return this;
  }

  private void addAccessRow(final RepeatingView rowRepeater, final AccessEntryDO accessEntry)
  {
    final WebMarkupContainer row = new WebMarkupContainer(rowRepeater.newChildId());
    rowRepeater.add(row);
    row.add(new Label("area", getString(accessEntry.getAccessType().getI18nKey())));
    final ButtonGroupPanel groupPanel = new ButtonGroupPanel("checkboxes").setToggleButtons();
    row.add(groupPanel);
    groupPanel.addButton(new CheckBoxButton(groupPanel.newChildId(), new PropertyModel<Boolean>(accessEntry, "accessSelect"), getString("access.type.select")));
    groupPanel.addButton(new CheckBoxButton(groupPanel.newChildId(), new PropertyModel<Boolean>(accessEntry, "accessInsert"), getString("access.type.insert")));
    groupPanel.addButton(new CheckBoxButton(groupPanel.newChildId(), new PropertyModel<Boolean>(accessEntry, "accessUpdate"), getString("access.type.update")));
    groupPanel.addButton(new CheckBoxButton(groupPanel.newChildId(), new PropertyModel<Boolean>(accessEntry, "accessDelete"), getString("access.type.delete")));
  }
}
