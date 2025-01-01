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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.externalsubscription.SubscriptionUpdateInterval;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.teamcal.dialog.TeamCalICSExportDialog;
import org.projectforge.web.user.GroupsWicketProvider;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.user.UsersProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.JodaDatePanel;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;
import org.wicketstuff.select2.Select2MultiChoice;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Creates a top form-panel to add filter functions or other options.
 *
 * @author Maximilian Lauterbach (m.lauterbach@micromata.de)
 */
public class TeamCalEditForm extends AbstractEditForm<TeamCalDO, TeamCalEditPage>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamCalEditForm.class);

  private static final long serialVersionUID = 1379614008604844519L;

  private boolean access = false;

  private JodaDatePanel datePanel;

  MultiChoiceListHelper<PFUserDO> fullAccessUsersListHelper, readonlyAccessUsersListHelper,
      minimalAccessUsersListHelper;

  MultiChoiceListHelper<GroupDO> fullAccessGroupsListHelper, readonlyAccessGroupsListHelper,
      minimalAccessGroupsListHelper;

  private TeamCalICSExportDialog icsExportDialog;

  private FieldsetPanel fsExternalSubscriptionUrl;

  private FieldsetPanel fsExternalSubscriptionInterval;

  /**
   * @param parentPage
   * @param data
   */
  public TeamCalEditForm(final TeamCalEditPage parentPage, final TeamCalDO data)
  {
    super(parentPage, data);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#init()
   */
  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    var accessChecker = WicketSupport.getAccessChecker();
    var userDao = WicketSupport.getUserDao();
    var groupService = WicketSupport.get(GroupService.class);
    gridBuilder.newSplitPanel(GridSize.COL50);

    // checking visibility rights
    final TeamCalRight right = new TeamCalRight();
    if (isNew() == true || right.hasUpdateAccess(getUser(), data, data) == true) {
      access = true;
    }

    // set title
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.title"));
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
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.description"));
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
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.id"));
        fs.add(new Label(fs.newChildId(), data.getId()));
      }
    }
    // set owner
    {
      if (data.getOwner() == null) {
        data.setOwner(getUser());
      }
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.owner")).suppressLabelForWarning();
      if (accessChecker.isLoggedInUserMemberOfAdminGroup() == true
          || Objects.equals(data.getOwnerId(), getUserId()) == true) {
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
    }

    if (accessChecker.isRestrictedUser() == false && isNew() == false) {
      icsExportDialog = new TeamCalICSExportDialog(parentPage.newModalDialogId());
      parentPage.add(icsExportDialog);
      icsExportDialog.init();
      icsExportDialog.redraw(getData());
      final FieldsetPanel fsSubscribe = gridBuilder.newFieldset(getString("plugins.teamcal.subscription"))
          .suppressLabelForWarning();
      fsSubscribe.add(new AjaxIconLinkPanel(fsSubscribe.newChildId(), IconType.SUBSCRIPTION, new ResourceModel(
          "plugins.teamcal.subscription.tooltip"))
      {
        @Override
        public void onClick(final AjaxRequestTarget target)
        {
          icsExportDialog.setCalendarTitle(target, data.getTitle());
          icsExportDialog.open(target);
        }

      });
    }
    if (access == true) {
      // external subscription
      final FieldsetPanel fsSubscription = gridBuilder
          .newFieldset(getString("plugins.teamcal.externalsubscription.label"));
      final CheckBoxPanel checkboxPanel = new CheckBoxPanel(fsSubscription.newChildId(),
          new PropertyModel<Boolean>(data,
              "externalSubscription"),
          null);
      // ajax stuff
      checkboxPanel.getCheckBox().setMarkupId("externalSubscription").setOutputMarkupId(true);
      checkboxPanel.getCheckBox().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          // update visibility
          fsExternalSubscriptionUrl.getFieldset().setVisible(data.getExternalSubscription() == true);
          fsExternalSubscriptionInterval.getFieldset().setVisible(data.getExternalSubscription() == true);
          // update components through ajax
          target.add(fsExternalSubscriptionUrl.getFieldset());
          target.add(fsExternalSubscriptionInterval.getFieldset());
        }
      });
      checkboxPanel.setTooltip(getString("plugins.teamcal.externalsubscription.label.tooltip"));

      fsSubscription.add(checkboxPanel);
      fsExternalSubscriptionUrl = gridBuilder.newFieldset(getString("plugins.teamcal.externalsubscription.url"));
      fsExternalSubscriptionUrl.getFieldset().setOutputMarkupId(true);
      fsExternalSubscriptionUrl.getFieldset().setOutputMarkupPlaceholderTag(true);
      fsExternalSubscriptionUrl.getFieldset().setVisible(data.getExternalSubscription() == true);
      fsExternalSubscriptionUrl.addHelpIcon(new ResourceModel("plugins.teamcal.externalsubscription.label.tooltip"),
          new ResourceModel(
              "plugins.teamcal.externalsubscription.url.tooltip"));

      final TextField<String> urlField = new TextField<String>(fsExternalSubscriptionUrl.getTextFieldId(),
          new PropertyModel<String>(data,
              "externalSubscriptionUrl"));
      urlField.setRequired(true);
      fsExternalSubscriptionUrl.add(urlField);

      fsExternalSubscriptionInterval = gridBuilder
          .newFieldset(getString("plugins.teamcal.externalsubscription.updateInterval"));
      fsExternalSubscriptionInterval.getFieldset().setOutputMarkupId(true);
      fsExternalSubscriptionInterval.getFieldset().setOutputMarkupPlaceholderTag(true);
      fsExternalSubscriptionInterval.getFieldset().setVisible(data.getExternalSubscription() == true);

      final IChoiceRenderer<Integer> intervalRenderer = new IChoiceRenderer<Integer>()
      {
        @Override
        public Object getDisplayValue(final Integer object)
        {
          return getString(SubscriptionUpdateInterval.getI18nKeyForInterval(object));
        }

        @Override
        public String getIdValue(final Integer object, final int index)
        {
          return object.toString();
        }

        @Override
        public Integer getObject(final String s, final IModel<? extends List<? extends Integer>> iModel)
        {
          if (s == null) {
            return null;
          }

          for (Integer instance : iModel.getObject()) {
            // TODO sn migration
            if (s.equals(instance.toString())) {
              return instance;
            }
          }

          return null;
        }
      };
      final DropDownChoicePanel<Integer> intervalField = new DropDownChoicePanel<Integer>(
          fsExternalSubscriptionUrl.getDropDownChoiceId(),
          new PropertyModel<Integer>(data, "externalSubscriptionUpdateInterval"),
          SubscriptionUpdateInterval.getIntervals(),
          intervalRenderer);
      intervalField.setRequired(true);
      fsExternalSubscriptionInterval.add(intervalField);
    }
    if (access == true) {
      gridBuilder.newSplitPanel(GridSize.COL50);
      // set access users
      {
        // Full access users
        final FieldsetPanel fs = gridBuilder
            .newFieldset(getString("plugins.teamcal.fullAccess"), getString("plugins.teamcal.access.users"));
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
        fs.add(users);
      }
      {
        // Read-only access users
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.readonlyAccess"),
            getString("plugins.teamcal.access.users"));
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
        fs.add(users);
      }
      {
        // Minimal access users
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.minimalAccess"),
            getString("plugins.teamcal.access.users"));
        final UsersProvider usersProvider = new UsersProvider(userDao);
        final Collection<PFUserDO> minimalAccessUsers = new UsersProvider(userDao)
            .getSortedUsers(getData().getMinimalAccessUserIds());
        minimalAccessUsersListHelper = new MultiChoiceListHelper<PFUserDO>().setComparator(new UsersComparator())
            .setFullList(
                usersProvider.getSortedUsers());
        if (minimalAccessUsers != null) {
          for (final PFUserDO user : minimalAccessUsers) {
            minimalAccessUsersListHelper.addOriginalAssignedItem(user).assignItem(user);
          }
        }
        final Select2MultiChoice<PFUserDO> users = new Select2MultiChoice<PFUserDO>(fs.getSelect2MultiChoiceId(),
            new PropertyModel<Collection<PFUserDO>>(this.minimalAccessUsersListHelper, "assignedItems"), usersProvider);
        users.setMarkupId("minimalAccessUsers").setOutputMarkupId(true);
        fs.addHelpIcon(getString("plugins.teamcal.minimalAccess.users.hint"));
        fs.add(users);
      }

      gridBuilder.newSplitPanel(GridSize.COL50);
      // set access groups
      {
        // Full access groups
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.fullAccess"),
            getString("plugins.teamcal.access.groups"));

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
        fs.add(groups);
      }
      {
        // Read-only access groups
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.readonlyAccess"),
            getString("plugins.teamcal.access.groups"));
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
        fs.add(groups);
      }
      {
        // Minimal access groups
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.minimalAccess"),
            getString("plugins.teamcal.access.groups"));
        final Collection<GroupDO> minimalAccessGroups = groupService
            .getSortedGroups(getData().getMinimalAccessGroupIds());
        minimalAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator())
            .setFullList(
                groupService.getSortedGroups());
        if (minimalAccessGroups != null) {
          for (final GroupDO group : minimalAccessGroups) {
            minimalAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
          }
        }
        final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
            new PropertyModel<Collection<GroupDO>>(this.minimalAccessGroupsListHelper, "assignedItems"),
            new GroupsWicketProvider(groupService));
        groups.setMarkupId("minimalAccessGroups").setOutputMarkupId(true);
        fs.addHelpIcon(getString("plugins.teamcal.minimalAccess.groups.hint"));
        fs.add(groups);
      }
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
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
}
