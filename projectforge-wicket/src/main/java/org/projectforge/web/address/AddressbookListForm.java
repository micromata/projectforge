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

package org.projectforge.web.address;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressbookFilter;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivType;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.RadioGroupPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class AddressbookListForm extends AbstractListForm<AddressbookFilter, AddressbookListPage>
{
  private static final long serialVersionUID = 3659495003810851072L;

  private static final Logger log = LoggerFactory.getLogger(AddressbookListForm.class);

  @SpringBean
  private AccessChecker accessChecker;

  public AddressbookListForm(final AddressbookListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see AbstractListForm#newSearchFilterInstance()
   */
  @Override
  protected AddressbookFilter newSearchFilterInstance()
  {
    return new AddressbookFilter();
  }

  /**
   * @see AbstractListForm#onOptionsPanelCreate(FieldsetPanel,
   * DivPanel)
   */
  @SuppressWarnings("serial")
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    {
      final DivPanel radioGroupPanel = optionsFieldsetPanel.addNewRadioBoxButtonDiv();
      final RadioGroupPanel<AddressbookFilter.OwnerType> radioGroup = new RadioGroupPanel<AddressbookFilter.OwnerType>(
          radioGroupPanel.newChildId(), "ownerType", new PropertyModel<AddressbookFilter.OwnerType>(getSearchFilter(), "ownerType"))
      {
        /**
         * @see RadioGroupPanel#wantOnSelectionChangedNotifications()
         */
        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }

        /**
         * @see RadioGroupPanel#onSelectionChanged(Object)
         */
        @Override
        protected void onSelectionChanged(final Object newSelection)
        {
          parentPage.refresh();
        }

        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible()
        {
          return !getSearchFilter().isDeleted();
        }
      };
      radioGroupPanel.add(radioGroup);
      radioGroup.add(new Model<>(AddressbookFilter.OwnerType.ALL), getString("filter.all"));
      radioGroup.add(new Model<>(AddressbookFilter.OwnerType.OWN), getString("addressbook.own"));
      radioGroup.add(new Model<>(AddressbookFilter.OwnerType.OTHERS), getString("addressbook.others"));
      if (accessChecker.isLoggedInUserMemberOfAdminGroup()) {
        radioGroup.add(new Model<>(AddressbookFilter.OwnerType.ADMIN), getString("addressbook.adminAccess"));
      }
    }
    final DivPanel checkBoxesPanel = new DivPanel(optionsFieldsetPanel.newChildId(), DivType.BTN_GROUP)
    {
      @Override
      public boolean isVisible()
      {

        // Show check box panel only if user selects others addressbooks.
        return !getSearchFilter().isDeleted() && (getSearchFilter().isAll() == true || getSearchFilter().isOthers() == true);
      }
    };
    optionsFieldsetPanel.add(checkBoxesPanel);
    checkBoxesPanel.add(createAutoRefreshCheckBoxButton(checkBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "fullAccess"), getString("addressbook.fullAccess")));
    checkBoxesPanel.add(createAutoRefreshCheckBoxButton(checkBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "readonlyAccess"), getString("addressbook.readonlyAccess")));
    optionsFieldsetPanel.add(checkBoxesPanel);
  }

  /**
   * @see AbstractListForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @return the filter
   */
  public AddressbookFilter getFilter()
  {
    return getSearchFilter();
  }
}
