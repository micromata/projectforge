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

package org.projectforge.web.teamcal.admin;

import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalFilter;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivType;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.RadioGroupPanel;
import org.slf4j.Logger;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class TeamCalListForm extends AbstractListForm<TeamCalFilter, TeamCalListPage>
{
  private static final long serialVersionUID = 3659495003810851072L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamCalListForm.class);

  public TeamCalListForm(final TeamCalListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#newSearchFilterInstance()
   */
  @Override
  protected TeamCalFilter newSearchFilterInstance()
  {
    return new TeamCalFilter();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   * org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @SuppressWarnings("serial")
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    {
      final DivPanel radioGroupPanel = optionsFieldsetPanel.addNewRadioBoxButtonDiv();
      final RadioGroupPanel<TeamCalFilter.OwnerType> radioGroup = new RadioGroupPanel<TeamCalFilter.OwnerType>(
          radioGroupPanel.newChildId(), "ownerType", new PropertyModel<>(getSearchFilter(), "ownerType"),
          new FormComponentUpdatingBehavior()
          {
            @Override
            public void onUpdate()
            {
              parentPage.refresh();
            }
          }
      )
      {
        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible()
        {
          return getSearchFilter().getDeleted() == false;
        }
      };
      radioGroupPanel.add(radioGroup);
      radioGroup.add(new Model<TeamCalFilter.OwnerType>(TeamCalFilter.OwnerType.ALL), getString("filter.all"));
      radioGroup.add(new Model<TeamCalFilter.OwnerType>(TeamCalFilter.OwnerType.OWN), getString("plugins.teamcal.own"));
      radioGroup.add(new Model<TeamCalFilter.OwnerType>(TeamCalFilter.OwnerType.OTHERS), getString("plugins.teamcal.others"));
      if (WicketSupport.getAccessChecker().isLoggedInUserMemberOfAdminGroup() == true) {
        radioGroup.add(new Model<TeamCalFilter.OwnerType>(TeamCalFilter.OwnerType.ADMIN), getString("plugins.teamcal.adminAccess"));
      }
    }
    final DivPanel checkBoxesPanel = new DivPanel(optionsFieldsetPanel.newChildId(), DivType.BTN_GROUP)
    {
      @Override
      public boolean isVisible()
      {

        // Show check box panel only if user selects others calendar.
        return getSearchFilter().getDeleted() == false && (getSearchFilter().isAll() == true || getSearchFilter().isOthers() == true);
      }
    };
    optionsFieldsetPanel.add(checkBoxesPanel);
    checkBoxesPanel.add(createAutoRefreshCheckBoxButton(checkBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "fullAccess"), getString("plugins.teamcal.fullAccess")));
    checkBoxesPanel.add(createAutoRefreshCheckBoxButton(checkBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "readonlyAccess"), getString("plugins.teamcal.readonlyAccess")));
    checkBoxesPanel.add(createAutoRefreshCheckBoxButton(checkBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "minimalAccess"), getString("plugins.teamcal.minimalAccess")));
    optionsFieldsetPanel.add(checkBoxesPanel);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @return the filter
   */
  public TeamCalFilter getFilter()
  {
    return getSearchFilter();
  }
}
