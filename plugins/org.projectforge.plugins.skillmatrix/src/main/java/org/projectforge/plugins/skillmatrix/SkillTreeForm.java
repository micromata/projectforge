/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.skillmatrix;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.AbstractSecuredForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.*;

/**
 * @author Billy Duong (b.duong@micromata.de)
 */
public class SkillTreeForm extends AbstractSecuredForm<SkillFilter, SkillTreePage>
{

  private static final long serialVersionUID = 1227686732149287124L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SkillTreeForm.class);

  private MyComponentsRepeater<Component> actionButtons;

  protected GridBuilder gridBuilder;

  private SkillFilter searchFilter;

  private SingleButtonPanel cancelButtonPanel;

  private SingleButtonPanel searchButtonPanel;

  private SingleButtonPanel resetButtonPanel;

  /**
   * @param parentPage
   */
  public SkillTreeForm(final SkillTreePage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractForm#init()
   */
  @Override
  protected void init()
  {
    super.init();

    gridBuilder = newGridBuilder(this, "flowform");
    {
      gridBuilder.newSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("searchFilter"));
      final TextField<String> searchField = new TextField<>(InputPanel.WICKET_ID,
          new PropertyModel<>(getSearchFilter(),
              "searchString"));
      searchField.add(WicketUtils.setFocus());
      fs.add(new InputPanel(fs.newChildId(), searchField));
      // fs.add(new IconPanel(fs.newIconChildId(), IconType.HELP, getString("tooltip.lucene.link")).setOnClickLocation(getRequestCycle(),
      // WebConstants.DOC_LINK_HANDBUCH_LUCENE, true), FieldSetIconPosition.TOP_RIGHT);
    }
    {
      gridBuilder.newSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.options")).suppressLabelForWarning();
      final DivPanel checkBoxPanel = fs.addNewCheckBoxButtonDiv();
      checkBoxPanel.add(
          new MyCheckBoxButton(checkBoxPanel.newChildId(), new PropertyModel<>(getSearchFilter(), "deleted"),
              getString("deleted")).setWarning());
    }

    actionButtons = new MyComponentsRepeater<>("actionButtons");

    add(actionButtons.getRepeatingView());
    {
      @SuppressWarnings("serial")
      final Button cancelButton = new Button("button", new Model<>("cancel"))
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
      @SuppressWarnings("serial")
      final Button resetButton = new Button("button", new Model<>("reset"))
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
      @SuppressWarnings("serial")
      final Button skillListButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("listView"))
      {

        @Override
        public void onSubmit()
        {
          getParentPage().onListViewSubmit();
        }
      };
      final SingleButtonPanel skillListButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), skillListButton,
          "List View",
          SingleButtonPanel.NORMAL);
      actionButtons.add(skillListButtonPanel);
    }
    {
      @SuppressWarnings("serial")
      final Button searchButton = new Button("button", new Model<>("search"))
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

  protected void setComponentsVisibility()
  {
    if (!parentPage.isSelectMode()) {
      // Show cancel button only in select mode.
      cancelButtonPanel.setVisible(false);
    }
    searchButtonPanel.setVisible(true);
    resetButtonPanel.setVisible(true);
  }

  public SkillFilter getSearchFilter()
  {
    if (this.searchFilter == null) {
      final Object filter = getParentPage().getUserPrefEntry(SkillListForm.class.getName() + ":Filter");
      if (filter != null) {
        try {
          this.searchFilter = (SkillFilter) filter;
        } catch (final ClassCastException ex) {
          // Probably a new software release results in an incompability of old and new filter format.
          log.info("Could not restore filter from user prefs: (old) filter type "
              + filter.getClass().getName()
              + " is not assignable to (new) filter type SkillFilter (OK, probably new software release).");
        }
      }
    }
    if (this.searchFilter == null) {
      this.searchFilter = new SkillFilter();
      getParentPage().putUserPrefEntry(SkillListForm.class.getName() + ":Filter", this.searchFilter, true);
    }
    return this.searchFilter;
  }

  @Override
  protected void onSubmit()
  {
    super.onSubmit();
    parentPage.refresh();
  }

  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    actionButtons.render();
  }

  @SuppressWarnings("serial")
  private class MyCheckBoxButton extends CheckBoxButton
  {
    public MyCheckBoxButton(final String id, final IModel<Boolean> model, final String labelString)
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
