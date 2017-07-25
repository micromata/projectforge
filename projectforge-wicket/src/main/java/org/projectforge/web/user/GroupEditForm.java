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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.ldap.GroupDOConverter;
import org.projectforge.business.ldap.LdapGroupValues;
import org.projectforge.business.ldap.LdapPosixGroupsUtils;
import org.projectforge.business.ldap.LdapUserDao;
import org.projectforge.business.login.Login;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.wicketstuff.select2.Select2MultiChoice;

public class GroupEditForm extends AbstractEditForm<GroupDO, GroupEditPage>
{
  private static final long serialVersionUID = 3044732844606748738L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroupEditForm.class);

  @SpringBean
  private AccessChecker accessChecker;

  @SpringBean
  private GroupDao groupDao;

  @SpringBean
  UserDao userDao;

  @SpringBean
  GroupDOConverter groupDOConverter;

  @SpringBean
  LdapPosixGroupsUtils ldapPosixGroupsUtils;

  @SpringBean
  LdapUserDao ldapUserDao;

  MultiChoiceListHelper<PFUserDO> assignUsersListHelper;

  // MultiChoiceListHelper<GroupDO> nestedGroupsListHelper;

  LdapGroupValues ldapGroupValues;

  private MinMaxNumberField<Integer> gidNumberField;

  public GroupEditForm(final GroupEditPage parentPage, final GroupDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("name"));
      final RequiredMaxLengthTextField name = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(getData(),
              "name"));
      name.setMarkupId("name").setOutputMarkupId(true);
      name.add((IValidator<String>) validatable -> {
        final String groupname = validatable.getValue();
        getData().setName(groupname);
        if (groupDao.doesGroupnameAlreadyExist(getData()) == true) {
          validatable.error(new ValidationError().addKey("fibu.kost.error.invalidKost"));
        }
      });
      fs.add(name);
      WicketUtils.setStrong(name);
    }
    {
      // Organization
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("organization"));
      MaxLengthTextField organization = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(getData(), "organization"));
      organization.setMarkupId("organization").setOutputMarkupId(true);
      fs.add(organization);
    }
    {
      // Description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      MaxLengthTextArea description = new MaxLengthTextArea(TextAreaPanel.WICKET_ID,
          new PropertyModel<String>(getData(), "description"));
      description.setMarkupId("description").setOutputMarkupId(true);
      fs.add(description);
    }
    if (Login.getInstance().hasExternalUsermanagementSystem() == true) {
      gridBuilder.newFieldset(getString("group.localGroup"))
          .addCheckBox(new PropertyModel<Boolean>(data, "localGroup"), null)
          .setTooltip(getString("group.localGroup.tooltip"));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Assigned users
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("group.assignedUsers")).setLabelSide(false);
      final Set<PFUserDO> assignedUsers = getData().getAssignedUsers();
      final UsersProvider usersProvider = new UsersProvider(userDao);
      assignUsersListHelper = new MultiChoiceListHelper<PFUserDO>().setComparator(new UsersComparator()).setFullList(
          usersProvider.getSortedUsers());
      if (assignedUsers != null) {
        for (final PFUserDO user : assignedUsers) {
          assignUsersListHelper.addOriginalAssignedItem(user).assignItem(user);
        }
      }
      final Select2MultiChoice<PFUserDO> users = new Select2MultiChoice<PFUserDO>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<PFUserDO>>(this.assignUsersListHelper, "assignedItems"), usersProvider);
      users.setMarkupId("users").setOutputMarkupId(true);
      fs.add(users);
    }
    final boolean adminAccess = accessChecker.isLoggedInUserMemberOfAdminGroup();
    if (adminAccess == true && Login.getInstance().hasExternalUsermanagementSystem() == true) {
      ldapGroupValues = groupDOConverter.readLdapGroupValues(data.getLdapValues());
      if (ldapGroupValues == null) {
        ldapGroupValues = new LdapGroupValues();
      }
      addLdapStuff();
    }

    // {
    // WicketUtils.addYesNoRadioFieldset(gridBuilder, getString("group.nestedGroupsAllowed"), "nestedGroupsAllowed", new
    // PropertyModel<Boolean>(data,
    // "nestedGroupsAllowed"), getString("group.nestedGroupsAllowed.tooltip"));
    // }
    // {
    // // Nested groups
    // final FieldsetPanel fs = gridBuilder.newFieldset(getString("group.nestedGroups"), true).setLabelSide(false);
    // final GroupsProvider groupsProvider = new GroupsProvider();
    // final Collection<GroupDO> nestedGroups = groupDao.getSortedNestedGroups(getData());
    // nestedGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator()).setFullList(
    // groupsProvider.getSortedGroups());
    // if (nestedGroups != null) {
    // for (final GroupDO group : nestedGroups) {
    // nestedGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
    // }
    // }
    // final Select2MultiChoice<GroupDO> users = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
    // new PropertyModel<Collection<GroupDO>>(this.nestedGroupsListHelper, "assignedItems"), groupsProvider);
    // fs.add(users);
    // }
  }

  @SuppressWarnings("serial")
  private void addLdapStuff()
  {
    gridBuilder.newGridPanel();
    gridBuilder.newFormHeading(getString("ldap"));
    gridBuilder.newSplitPanel(GridSize.COL50);
    final boolean posixConfigured = ldapUserDao.isPosixAccountsConfigured();
    if (posixConfigured == false) {
      return;
    }
    final List<FormComponent<?>> dependentLdapPosixFormComponentsList = new LinkedList<FormComponent<?>>();
    if (posixConfigured == true) {
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("ldap.gidNumber"), getString("ldap.posixAccount"));
        gidNumberField = new MinMaxNumberField<Integer>(fs.getTextFieldId(),
            new PropertyModel<Integer>(ldapGroupValues, "gidNumber"), 1,
            65535);
        WicketUtils.setSize(gidNumberField, 6);
        fs.add(gidNumberField);
        fs.addHelpIcon(gridBuilder.getString("ldap.gidNumber.tooltip"));
        dependentLdapPosixFormComponentsList.add(gidNumberField);
        if (ldapGroupValues.isPosixValuesEmpty() == true) {
          final Button createButton = newCreateButton(dependentLdapPosixFormComponentsList);
          fs.add(new SingleButtonPanel(fs.newChildId(), createButton, gridBuilder.getString("create"),
              SingleButtonPanel.NORMAL));
          WicketUtils.addTooltip(createButton, gridBuilder.getString("ldap.gidNumber.createDefault.tooltip"));
        }
      }
    }
    if (posixConfigured == true) {
      add(new IFormValidator()
      {
        @Override
        public FormComponent<?>[] getDependentFormComponents()
        {
          return dependentLdapPosixFormComponentsList.toArray(new FormComponent[0]);
        }

        @Override
        public void validate(final Form<?> form)
        {
          final LdapGroupValues values = new LdapGroupValues();
          values.setGidNumber(gidNumberField.getConvertedInput());
          if (StringUtils.isBlank(data.getLdapValues()) == true && values.isPosixValuesEmpty() == true) {
            // Nothing to validate: all fields are zero and posix account wasn't set for this group before.
            return;
          }
          if (values.getGidNumber() == null) {
            gidNumberField
                .error(getLocalizedMessage(WebConstants.I18N_KEY_FIELD_REQUIRED, getString("ldap.gidNumber")));
          } else {
            if (ldapPosixGroupsUtils.isGivenNumberFree(data, values.getGidNumber()) == false) {
              gidNumberField.error(
                  getLocalizedMessage("ldap.gidNumber.alreadyInUse", ldapPosixGroupsUtils.getNextFreeGidNumber()));
            }
          }
        }
      });
    }
  }

  @SuppressWarnings("serial")
  private Button newCreateButton(final List<FormComponent<?>> dependentPosixLdapFormComponentsList)
  {
    final AjaxButton createButton = new AjaxButton(SingleButtonPanel.WICKET_ID, this)
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        ldapPosixGroupsUtils.setDefaultValues(ldapGroupValues);
        for (final FormComponent<?> component : dependentPosixLdapFormComponentsList) {
          component.modelChanged();
          component.setEnabled(true);
        }
        this.setVisible(false);
        for (final FormComponent<?> comp : dependentPosixLdapFormComponentsList) {
          target.add(comp);
        }
        target.add(this, GroupEditForm.this.feedbackPanel);
        target.appendJavaScript("hideAllTooltips();"); // Otherwise a tooltip is left as zombie.
      }

      @Override
      protected void onError(final AjaxRequestTarget target)
      {
        target.add(GroupEditForm.this.feedbackPanel);
      }
    };
    createButton.setDefaultFormProcessing(false);
    return createButton;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
