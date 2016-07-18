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

package org.projectforge.web.task;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.task.TaskFilter;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class TaskListForm extends AbstractListForm<TaskFilter, TaskListPage>
{
  private static final long serialVersionUID = 153015604624697061L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TaskListForm.class);

  @Override
  protected void init()
  {
    super.init();
    // Task tree view
    @SuppressWarnings("serial")
    final Button taskTreeButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("listView"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.onTreeViewSubmit();
      }
    };
    actionButtons.add(2,
        new SingleButtonPanel(actionButtons.newChildId(), taskTreeButton, getString("task.tree.perspective"),
            SingleButtonPanel.NORMAL));
  }

  public TaskListForm(final TaskListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<Boolean>(getSearchFilter(), "notOpened"), getString("task.status.notOpened")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<Boolean>(getSearchFilter(), "opened"),
        getString("task.status.opened")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<Boolean>(getSearchFilter(), "closed"),
        getString("task.status.closed")));
  }

  @Override
  protected TaskFilter newSearchFilterInstance()
  {
    return new TaskFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
