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

package org.projectforge.web.user;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.login.Login;
import org.projectforge.business.user.PFUserFilter;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class UserListForm extends AbstractListForm<PFUserFilter, UserListPage>
{
  private static final long serialVersionUID = 7625173316784007696L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserListForm.class);

  public UserListForm(final UserListPage parentPage)
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
    {
      // DropDownChoice deactivated
      final LabelValueChoiceRenderer<Boolean> deactivatedRenderer = new LabelValueChoiceRenderer<Boolean>();
      deactivatedRenderer.addValue(false, getString("user.activated"));
      deactivatedRenderer.addValue(true, getString("user.deactivated"));
      final DropDownChoice<Boolean> deactivatedChoice = new DropDownChoice<Boolean>(
          optionsFieldsetPanel.getDropDownChoiceId(), new PropertyModel<Boolean>(
              getSearchFilter(), "deactivatedUser"),
          deactivatedRenderer.getValues(), deactivatedRenderer);
      deactivatedChoice.setNullValid(true);
      optionsFieldsetPanel.add(deactivatedChoice, true).setTooltip(getString("user.activated"));
    }
    {
      // DropDownChoice admin-user
      final LabelValueChoiceRenderer<Boolean> isAdminRenderer = new LabelValueChoiceRenderer<Boolean>();
      isAdminRenderer.addValue(true, getString("user.adminUsers"));
      isAdminRenderer.addValue(false, getString("user.adminUsers.none"));
      final DropDownChoice<Boolean> isAdminChoice = new DropDownChoice<Boolean>(
          optionsFieldsetPanel.getDropDownChoiceId(), new PropertyModel<Boolean>(
              getSearchFilter(), "isAdminUser"),
          isAdminRenderer.getValues(), isAdminRenderer);
      isAdminChoice.setNullValid(true);
      optionsFieldsetPanel.add(isAdminChoice, true).setTooltip(getString("user.adminUsers"));
    }
    if (Login.getInstance().hasExternalUsermanagementSystem() == true) {
      {
        // DropDownChoice restricted
        final LabelValueChoiceRenderer<Boolean> restrictedRenderer = new LabelValueChoiceRenderer<Boolean>();
        restrictedRenderer.addValue(false, getString("user.restricted.not"));
        restrictedRenderer.addValue(true, getString("user.restricted"));
        final DropDownChoice<Boolean> restrictedChoice = new DropDownChoice<Boolean>(
            optionsFieldsetPanel.getDropDownChoiceId(),
            new PropertyModel<Boolean>(getSearchFilter(), "restrictedUser"), restrictedRenderer.getValues(),
            restrictedRenderer);
        restrictedChoice.setNullValid(true);
        optionsFieldsetPanel.add(restrictedChoice, true).setTooltip(getString("user.restrictedUser"));
      }
      {
        // DropDownChoice localUser
        final LabelValueChoiceRenderer<Boolean> localUserRenderer = new LabelValueChoiceRenderer<Boolean>();
        localUserRenderer.addValue(false, getString("user.localUser.not"));
        localUserRenderer.addValue(true, getString("user.localUser"));
        final DropDownChoice<Boolean> localUserChoice = new DropDownChoice<Boolean>(
            optionsFieldsetPanel.getDropDownChoiceId(),
            new PropertyModel<Boolean>(getSearchFilter(), "localUser"), localUserRenderer.getValues(),
            localUserRenderer);
        localUserChoice.setNullValid(true);
        optionsFieldsetPanel.add(localUserChoice, true).setTooltip(getString("user.localUser"));
      }
    }
    {
      // DropDownChoice hrPlanning
      final LabelValueChoiceRenderer<Boolean> hrPlanningRenderer = new LabelValueChoiceRenderer<Boolean>();
      hrPlanningRenderer.addValue(false, getString("user.hrPlanningEnabled.not"));
      hrPlanningRenderer.addValue(true, getString("user.hrPlanningEnabled"));
      final DropDownChoice<Boolean> hrPlanningChoice = new DropDownChoice<Boolean>(
          optionsFieldsetPanel.getDropDownChoiceId(), new PropertyModel<Boolean>(
              getSearchFilter(), "hrPlanning"),
          hrPlanningRenderer.getValues(), hrPlanningRenderer);
      hrPlanningChoice.setNullValid(true);
      optionsFieldsetPanel.add(hrPlanningChoice, true).setTooltip(getString("user.hrPlanningEnabled"));
    }
  }

  @Override
  protected PFUserFilter newSearchFilterInstance()
  {
    return new PFUserFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
