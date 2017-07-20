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

import java.util.Collection;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookRight;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.GroupsWicketProvider;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.user.UsersProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.JodaDatePanel;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import com.vaynberg.wicket.select2.Select2MultiChoice;

/**
 * Creates a top form-panel to add filter functions or other options.
 *
 * @author Florian Blumenstein
 */
public class AddressbookEditForm extends AbstractEditForm<AddressbookDO, AddressbookEditPage>
{
  private static final Logger log = Logger.getLogger(AddressbookEditForm.class);

  private static final long serialVersionUID = 137912345678844519L;

  @SpringBean
  protected AccessChecker accessChecker;

  @SpringBean
  GroupDao groupDao;

  @SpringBean
  UserDao userDao;

  @SpringBean
  GroupService groupService;

  private boolean access = false;

  private JodaDatePanel datePanel;

  MultiChoiceListHelper<PFUserDO> fullAccessUsersListHelper, readonlyAccessUsersListHelper;

  MultiChoiceListHelper<GroupDO> fullAccessGroupsListHelper, readonlyAccessGroupsListHelper;

  /**
   * @param parentPage
   * @param data
   */
  public AddressbookEditForm(final AddressbookEditPage parentPage, final AddressbookDO data)
  {
    super(parentPage, data);
  }

  /**
   * @see AbstractEditForm#init()
   */
  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();

    gridBuilder.newSplitPanel(GridSize.COL50);

    // checking visibility rights
    final AddressbookRight right = new AddressbookRight(accessChecker);
    if (isNew() == true || right.hasUpdateAccess(getUser(), data, data) == true) {
      access = true;
    }

    // set title
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("addressbook.title"));
      final RequiredMaxLengthTextField title = new RequiredMaxLengthTextField(fs.getTextFieldId(),
          new PropertyModel<String>(data, "title"));
      title.setMarkupId("title").setOutputMarkupId(true);
      if (isNew() == true) {
        title.add(WicketUtils.setFocus());
      }
      WicketUtils.setStrong(title);
      fs.add(title);
      if (access == false) {
        title.setEnabled(false);
      }
    }

    // set description
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("addressbook.description"));
      final MaxLengthTextArea descr = new MaxLengthTextArea(fs.getTextAreaId(),
          new PropertyModel<String>(data, "description"));
      descr.setMarkupId("description").setOutputMarkupId(true);
      fs.add(descr).setAutogrow();
      if (access == false) {
        descr.setEnabled(false);
      }
    }

    gridBuilder.newSplitPanel(GridSize.COL50);
    // ID
    {
      if (isNew() == false && accessChecker.isLoggedInUserMemberOfAdminGroup() == true) {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("addressbook.id"));
        fs.add(new Label(fs.newChildId(), data.getId()));
      }
    }
    // set owner
    {
      if (data.getOwner() == null) {
        data.setOwner(getUser());
      }
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("addressbook.owner")).suppressLabelForWarning();
      if (accessChecker.isLoggedInUserMemberOfAdminGroup() == true
          || ObjectUtils.equals(data.getOwnerId(), getUserId()) == true) {
        final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
            new PropertyModel<PFUserDO>(data, "owner"), parentPage,
            "ownerId");
        userSelectPanel.getFormComponent().setMarkupId("owner").setOutputMarkupId(true);
        userSelectPanel.setRequired(true);
        fs.add(userSelectPanel);
        userSelectPanel.init();
      } else {
        fs.add(new Label(fs.newChildId(), data.getOwner().getUsername() + ""));
      }
      fs.setEnabled(isGlobalAddressbook(data) == false);
    }

    if (access == true) {
      gridBuilder.newSplitPanel(GridSize.COL50);
      // set access users
      {
        // Full access users
        final FieldsetPanel fs = gridBuilder
            .newFieldset(getString("addressbook.fullAccess"), getString("access.users"));
        final UsersProvider usersProvider = new UsersProvider(userDao);
        final Collection<PFUserDO> fullAccessUsers = new UsersProvider(userDao)
            .getSortedUsers(getData().getFullAccessUserIds());
        fullAccessUsersListHelper = new MultiChoiceListHelper<PFUserDO>().setComparator(new UsersComparator())
            .setFullList(
                usersProvider.getSortedUsers());
        if (fullAccessUsers != null) {
          for (final PFUserDO user : fullAccessUsers) {
            fullAccessUsersListHelper.addOriginalAssignedItem(user).assignItem(user);
          }
        }
        final Select2MultiChoice<PFUserDO> users = new Select2MultiChoice<PFUserDO>(fs.getSelect2MultiChoiceId(),
            new PropertyModel<Collection<PFUserDO>>(this.fullAccessUsersListHelper, "assignedItems"), usersProvider);
        users.setMarkupId("fullAccessUsers").setOutputMarkupId(true);
        fs.setEnabled(isGlobalAddressbook(data) == false);
        fs.add(users);
      }
      {
        // Read-only access users
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("addressbook.readonlyAccess"),
            getString("access.users"));
        final UsersProvider usersProvider = new UsersProvider(userDao);
        final Collection<PFUserDO> readOnlyAccessUsers = new UsersProvider(userDao)
            .getSortedUsers(getData().getReadonlyAccessUserIds());
        readonlyAccessUsersListHelper = new MultiChoiceListHelper<PFUserDO>().setComparator(new UsersComparator())
            .setFullList(
                usersProvider.getSortedUsers());
        if (readOnlyAccessUsers != null) {
          for (final PFUserDO user : readOnlyAccessUsers) {
            readonlyAccessUsersListHelper.addOriginalAssignedItem(user).assignItem(user);
          }
        }
        final Select2MultiChoice<PFUserDO> users = new Select2MultiChoice<PFUserDO>(fs.getSelect2MultiChoiceId(),
            new PropertyModel<Collection<PFUserDO>>(this.readonlyAccessUsersListHelper, "assignedItems"),
            usersProvider);
        users.setMarkupId("readOnlyAccessUsers").setOutputMarkupId(true);
        fs.setEnabled(isGlobalAddressbook(data) == false);
        fs.add(users);
      }

      gridBuilder.newSplitPanel(GridSize.COL50);
      // set access groups
      {
        // Full access groups
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("addressbook.fullAccess"),
            getString("access.groups"));

        final Collection<GroupDO> fullAccessGroups = groupService
            .getSortedGroups(getData().getFullAccessGroupIds());
        fullAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator())
            .setFullList(
                groupService.getSortedGroups());
        if (fullAccessGroups != null) {
          for (final GroupDO group : fullAccessGroups) {
            fullAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
          }
        }
        final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
            new PropertyModel<Collection<GroupDO>>(this.fullAccessGroupsListHelper, "assignedItems"),
            new GroupsWicketProvider(groupService));
        groups.setMarkupId("fullAccessGroups").setOutputMarkupId(true);
        fs.setEnabled(isGlobalAddressbook(data) == false);
        fs.add(groups);
      }
      {
        // Read-only access groups
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("addressbook.readonlyAccess"),
            getString("access.groups"));
        final Collection<GroupDO> readOnlyAccessGroups = groupService
            .getSortedGroups(getData().getReadonlyAccessGroupIds());
        readonlyAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator())
            .setFullList(
                groupService.getSortedGroups());
        if (readOnlyAccessGroups != null) {
          for (final GroupDO group : readOnlyAccessGroups) {
            readonlyAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
          }
        }
        final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
            new PropertyModel<Collection<GroupDO>>(this.readonlyAccessGroupsListHelper, "assignedItems"),
            new GroupsWicketProvider(groupService));
        groups.setMarkupId("readOnlyAccessGroups").setOutputMarkupId(true);
        fs.setEnabled(isGlobalAddressbook(data) == false);
        fs.add(groups);
      }
    }
  }

  /**
   * @see AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @return the datePanel
   */
  public JodaDatePanel getDatePanel()
  {
    return datePanel;
  }

  /**
   * @param datePanel the datePanel to set
   * @return this for chaining.
   */
  public void setDatePanel(final JodaDatePanel datePanel)
  {
    this.datePanel = datePanel;
  }

  @Override
  protected void updateButtonVisibility()
  {
    super.updateButtonVisibility();
    //Disable undelete button for global addressbook
    if (isGlobalAddressbook(data)) {
      markAsDeletedButtonPanel.setVisible(false);
    }
  }

  private boolean isGlobalAddressbook(AddressbookDO data)
  {
    return data != null && data.getId() != null && data.getId().equals(1);
  }
}
