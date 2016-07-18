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
import org.projectforge.business.user.GroupFilter;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class GroupListForm extends AbstractListForm<GroupFilter, GroupListPage>
{
  private static final long serialVersionUID = -1577132974803866434L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroupListForm.class);

  public GroupListForm(final GroupListPage parentPage)
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
    if (Login.getInstance().hasExternalUsermanagementSystem() == true) {
      // DropDownChoice localGroup
      final LabelValueChoiceRenderer<Boolean> localGroupRenderer = new LabelValueChoiceRenderer<Boolean>();
      localGroupRenderer.addValue(false, getString("group.localGroup.not"));
      localGroupRenderer.addValue(true, getString("group.localGroup"));
      final DropDownChoice<Boolean> localGroupChoice = new DropDownChoice<Boolean>(
          optionsFieldsetPanel.getDropDownChoiceId(),
          new PropertyModel<Boolean>(getSearchFilter(), "localGroup"), localGroupRenderer.getValues(),
          localGroupRenderer);
      localGroupChoice.setNullValid(true);
      optionsFieldsetPanel.add(localGroupChoice, true).setTooltip(getString("group.localGroup"));
    }
  }

  @Override
  protected GroupFilter newSearchFilterInstance()
  {
    return new GroupFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
