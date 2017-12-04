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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.task.TaskFilter;
import org.projectforge.web.wicket.AbstractSecuredForm;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldSetIconPosition;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;

public class TaskTreeForm extends AbstractSecuredForm<TaskFilter, TaskTreePage>
{
  private static final long serialVersionUID = -203572415793301622L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskTreeForm.class);

  private TaskFilter searchFilter;

  private MyComponentsRepeater<Component> actionButtons;

  private SingleButtonPanel cancelButtonPanel;

  private SingleButtonPanel resetButtonPanel;

  private SingleButtonPanel listViewButtonPanel;

  private SingleButtonPanel searchButtonPanel;

  protected GridBuilder gridBuilder;

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    add(createFeedbackPanel());
    gridBuilder = newGridBuilder(this, "flowform");
    {
      gridBuilder.newSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("searchFilter"));
      final TextField<String> searchField = new TextField<String>(InputPanel.WICKET_ID,
          new PropertyModel<String>(getSearchFilter(),
              "searchString"));
      searchField.add(WicketUtils.setFocus());
      fs.add(new InputPanel(fs.newChildId(), searchField));
      fs.add(new IconPanel(fs.newIconChildId(), IconType.HELP, getString("tooltip.lucene.link")).setOnClickLocation(
          getRequestCycle(),
          WebConstants.DOC_LINK_HANDBUCH_LUCENE, true), FieldSetIconPosition.TOP_RIGHT);
    }
    {
      gridBuilder.newSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.options")).suppressLabelForWarning();
      final DivPanel checkBoxPanel = fs.addNewCheckBoxButtonDiv();
      checkBoxPanel.add(
          new MyCheckBoxPanel(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(), "notOpened"),
              getString("task.status.notOpened")));
      checkBoxPanel
          .add(new MyCheckBoxPanel(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(), "opened"),
              getString("task.status.opened")));
      checkBoxPanel
          .add(new MyCheckBoxPanel(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(), "closed"),
              getString("task.status.closed")));
      checkBoxPanel
          .add(new MyCheckBoxPanel(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(), "deleted"),
              getString("deleted")));
    }

    actionButtons = new MyComponentsRepeater<Component>("actionButtons");
    add(actionButtons.getRepeatingView());
    {
      final Button cancelButton = new Button("button", new Model<String>("cancel"))
      {
        @Override
        public final void onSubmit()
        {
          getParentPage().onCancelSubmit();
        }
      };
      cancelButton.setDefaultFormProcessing(false);
      cancelButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), cancelButton, getString("cancel"),
          SingleButtonPanel.CANCEL);
      actionButtons.add(cancelButtonPanel);
    }
    {
      final Button resetButton = new Button("button", new Model<String>("reset"))
      {
        @Override
        public final void onSubmit()
        {
          getParentPage().onResetSubmit();
        }
      };
      resetButton.setDefaultFormProcessing(false);
      resetButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), resetButton, getString("reset"),
          SingleButtonPanel.RESET);
      actionButtons.add(resetButtonPanel);
    }
    {
      final Button listViewButton = new Button("button", new Model<String>("listView"))
      {
        @Override
        public final void onSubmit()
        {
          getParentPage().onListViewSubmit();
        }
      };

      listViewButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), listViewButton, getString("listView"),
          SingleButtonPanel.NORMAL);
      actionButtons.add(listViewButtonPanel);
    }
    {
      final Button searchButton = new Button("button", new Model<String>("search"))
      {
        @Override
        public final void onSubmit()
        {
          getParentPage().onSearchSubmit();
        }
      };
      searchButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), searchButton, getString("search"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(searchButtonPanel);
      setDefaultButton(searchButton);
    }
    setComponentsVisibility();
  }

  public TaskTreeForm(final TaskTreePage parentPage)
  {
    super(parentPage);
  }

  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    actionButtons.render();
  }

  protected void setComponentsVisibility()
  {
    if (parentPage.isSelectMode() == false) {
      // Show cancel button only in select mode.
      cancelButtonPanel.setVisible(false);
    }
    searchButtonPanel.setVisible(true);
    resetButtonPanel.setVisible(true);
  }

  public TaskFilter getSearchFilter()
  {
    if (this.searchFilter == null) {
      final Object filter = getParentPage().getUserPrefEntry(TaskListForm.class.getName() + ":Filter");
      if (filter != null) {
        try {
          this.searchFilter = (TaskFilter) filter;
        } catch (final ClassCastException ex) {
          // Probably a new software release results in an incompability of old and new filter format.
          log.info("Could not restore filter from user prefs: (old) filter type "
              + filter.getClass().getName()
              + " is not assignable to (new) filter type TaskFilter (OK, probably new software release).");
        }
      }
    }
    if (this.searchFilter == null) {
      this.searchFilter = new TaskFilter();
      getParentPage().putUserPrefEntry(TaskListForm.class.getName() + ":Filter", this.searchFilter, true);
    }
    return this.searchFilter;
  }

  @Override
  protected void onSubmit()
  {
    super.onSubmit();
    parentPage.refresh();
  }

  @SuppressWarnings("serial")
  private class MyCheckBoxPanel extends CheckBoxButton
  {
    public MyCheckBoxPanel(final String id, final IModel<Boolean> model, final String labelString)
    {
      super(id, model, labelString);
    }

    @Override
    protected boolean wantOnSelectionChangedNotifications()
    {
      return true;
    }

    @Override
    protected void onSelectionChanged(final Boolean newSelection)
    {
      parentPage.refresh();
    }
  }
}
