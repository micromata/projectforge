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

import org.slf4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class UserPrefListForm extends AbstractListForm<UserPrefListFilter, UserPrefListPage>
{
  private static final long serialVersionUID = 3750000537686305181L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserPrefListForm.class);

  public UserPrefListForm(final UserPrefListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel, org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    // DropDownChoice area
    final DropDownChoice<UserPrefArea> areaDropDownChoice = UserPrefEditForm.createAreaDropdownChoice(this, optionsFieldsetPanel.getDropDownChoiceId(),
        getSearchFilter(), "area", true);
    optionsFieldsetPanel.add(areaDropDownChoice);
  }

  @Override
  protected UserPrefListFilter newSearchFilterInstance()
  {
    return new UserPrefListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#showOptionsPanel()
   */
  @Override
  protected boolean showOptionsPanel()
  {
    return true;
  }
}
