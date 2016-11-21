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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.projectforge.Const;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.ldap.LdapPosixAccountsUtils;
import org.projectforge.business.ldap.LdapSambaAccountsConfig;
import org.projectforge.business.ldap.LdapSambaAccountsUtils;
import org.projectforge.business.ldap.LdapService;
import org.projectforge.business.ldap.LdapUserDao;
import org.projectforge.business.ldap.LdapUserValues;
import org.projectforge.business.ldap.PFUserDOConverter;
import org.projectforge.business.login.Login;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.multitenancy.TenantsComparator;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRight;
import org.projectforge.business.user.UserRightDao;
import org.projectforge.business.user.UserRightVO;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.business.user.service.UserService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.TimeNotation;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.multitenancy.TenantsProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.components.TimeZoneField;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import com.vaynberg.wicket.select2.Select2MultiChoice;

public class UserEditForm extends AbstractEditForm<PFUserDO, UserEditPage>
{
  private static final long serialVersionUID = 7872294377838461659L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserEditForm.class);

  @SpringBean
  private AccessChecker accessChecker;

  @SpringBean
  private UserRightDao userRightDao;

  @SpringBean
  private GroupDao groupDao;

  @SpringBean
  private TenantDao tenantDao;

  @SpringBean
  private LdapPosixAccountsUtils ldapPosixAccountsUtils;

  @SpringBean
  private LdapSambaAccountsUtils ldapSambaAccountsUtils;

  @SpringBean
  private LdapUserDao ldapUserDao;

  @SpringBean
  private LdapService ldapService;

  @SpringBean
  private GroupService groupService;

  @SpringBean
  private TenantService tenantService;

  @SpringBean
  private UserService userService;

  protected UserRightsEditData rightsData;

  private String password;

  @SuppressWarnings("unused")
  private String passwordRepeat;

  private String wlanPassword;

  private String wlanPasswordRepeat;

  private PFUserDO passwordUser;

  private boolean wlanPasswordValid = false;

  boolean invalidateAllStayLoggedInSessions;

  protected MultiChoiceListHelper<GroupDO> assignGroupsListHelper;

  protected MultiChoiceListHelper<TenantDO> assignTenantsListHelper;

  protected LdapUserValues ldapUserValues;

  private TextField<?> usernameTextField;

  private MinMaxNumberField<Integer> uidNumberField;

  private MinMaxNumberField<Integer> gidNumberField;

  private MaxLengthTextField homeDirectoryField;

  private MaxLengthTextField loginShellField;

  private MinMaxNumberField<Integer> sambaSIDNumberField;

  private MinMaxNumberField<Integer> sambaPrimaryGroupSIDNumberField;

  public UserEditForm(final UserEditPage parentPage, final PFUserDO data)
  {
    super(parentPage, data);
  }

  public static void createFirstName(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // First name
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("firstName"));
    final RequiredMaxLengthTextField firstName = new RequiredMaxLengthTextField(fs.getTextFieldId(),
        new PropertyModel<String>(user,
            "firstname"));
    firstName.setMarkupId("firstName").setOutputMarkupId(true);
    WicketUtils.setStrong(firstName);
    fs.add(firstName);
  }

  public static void createLastName(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // Last name
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("name"));
    final RequiredMaxLengthTextField name = new RequiredMaxLengthTextField(fs.getTextFieldId(),
        new PropertyModel<String>(user, "lastname"));
    name.setMarkupId("lastName").setOutputMarkupId(true);
    WicketUtils.setStrong(name);
    fs.add(name);
  }

  public static void createOrganization(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // Organization
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("organization"));
    MaxLengthTextField organization = new MaxLengthTextField(fs.getTextFieldId(),
        new PropertyModel<String>(user, "organization"));
    organization.setMarkupId("organization").setOutputMarkupId(true);
    fs.add(organization);
  }

  public static void createEMail(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // E-Mail
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("email"));
    MaxLengthTextField email = new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(user, "email"));
    email.setMarkupId("email").setOutputMarkupId(true);
    fs.add(email);
  }

  @SuppressWarnings("serial")
  public static void createAuthenticationToken(final GridBuilder gridBuilder, final PFUserDO user,
      final UserDao userDao,
      final Form<?> form)
  {
    // Authentication token
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("user.authenticationToken"))
        .suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
    {
      @Override
      public String getObject()
      {
        if (ThreadLocalUserContext.getUserId().equals(user.getId()) == true) {
          return userDao.getAuthenticationToken(user.getId());
        } else {
          // Administrators shouldn't see the token.
          return "*****";
        }
      }
    }));
    fs.addHelpIcon(gridBuilder.getString("user.authenticationToken.tooltip"));
    final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("renewAuthenticationKey"))
    {
      @Override
      public final void onSubmit()
      {
        userDao.renewAuthenticationToken(user.getId());
        form.error(getString("user.authenticationToken.renew.successful"));
      }
    };
    button.add(
        WicketUtils.javaScriptConfirmDialogOnClick(form.getString("user.authenticationToken.renew.securityQuestion")));
    fs.add(new SingleButtonPanel(fs.newChildId(), button, gridBuilder.getString("user.authenticationToken.renew"),
        SingleButtonPanel.DANGER));
    WicketUtils.addTooltip(button, gridBuilder.getString("user.authenticationToken.renew.tooltip"));
  }

  public static void createJIRAUsername(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // JIRA user name
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("user.jiraUsername"));
    final MaxLengthTextField jiraUsername = new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(user, "jiraUsername"));
    jiraUsername
        .setMarkupId("jiraUser")
        .setOutputMarkupId(true)
        .add(AttributeModifier.append("autocomplete", "off"));
    fs.add(jiraUsername);
    fs.addHelpIcon(gridBuilder.getString("user.jiraUsername.tooltip"));
  }

  public static void createLastLoginAndDeleteAllStayLogins(final GridBuilder gridBuilder, final PFUserDO user,
      final UserService userService,
      final Form<?> form)
  {
    // Last login and deleteAllStayLoggedInSessions
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("login.lastLogin"))
        .suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), DateTimeFormatter.instance().getFormattedDateTime(user.getLastLogin())));
    @SuppressWarnings("serial")
    final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("invalidateStayLoggedInSessions"))
    {
      @Override
      public final void onSubmit()
      {
        userService.renewStayLoggedInKey(user.getId());
        form.error(getString("login.stayLoggedIn.invalidateAllStayLoggedInSessions.successfullDeleted"));
      }
    };
    button.setMarkupId("invalidateStayLoggedInSessions").setOutputMarkupId(true);
    fs.add(new SingleButtonPanel(fs.newChildId(), button,
        gridBuilder.getString("login.stayLoggedIn.invalidateAllStayLoggedInSessions"),
        SingleButtonPanel.DANGER));
    WicketUtils.addTooltip(button,
        gridBuilder.getString("login.stayLoggedIn.invalidateAllStayLoggedInSessions.tooltip"));
  }

  public static void createLocale(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // Locale
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("user.locale"));
    final LabelValueChoiceRenderer<Locale> localeChoiceRenderer = new LabelValueChoiceRenderer<Locale>();
    localeChoiceRenderer.addValue(null, gridBuilder.getString("user.defaultLocale"));
    for (final String str : Const.LOCALIZATIONS) {
      localeChoiceRenderer.addValue(new Locale(str), gridBuilder.getString("locale." + str));
    }
    @SuppressWarnings("serial")
    final DropDownChoice<Locale> localeChoice = new DropDownChoice<Locale>(fs.getDropDownChoiceId(),
        new PropertyModel<Locale>(user,
            "locale"),
        localeChoiceRenderer.getValues(), localeChoiceRenderer)
    {
      /**
       * @see org.apache.wicket.markup.html.form.AbstractSingleSelectChoice#getDefaultChoice(java.lang.String)
       */
      @Override
      protected CharSequence getDefaultChoice(final String selectedValue)
      {
        return "";
      }

      @Override
      protected Locale convertChoiceIdToChoice(final String id)
      {
        if (StringHelper.isIn(id, Const.LOCALIZATIONS) == true) {
          return new Locale(id);
        } else {
          return null;
        }
      }
    };
    fs.add(localeChoice);
  }

  public static void createLanguage(final GridBuilder gridBuilder, final PFUserDO user)
  {
  }

  public static void createDateFormat(final GridBuilder gridBuilder, final PFUserDO user)
  {
    addDateFormatCombobox(gridBuilder, user, "dateFormat", "dateFormat", Configuration.getInstance().getDateFormats(),
        false);
  }

  public static void createExcelDateFormat(final GridBuilder gridBuilder, final PFUserDO user)
  {
    addDateFormatCombobox(gridBuilder, user, "dateFormat.xls", "excelDateFormat",
        Configuration.getInstance().getExcelDateFormats(), true);
  }

  public static void createTimeNotation(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // Time notation
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("timeNotation"));
    final LabelValueChoiceRenderer<TimeNotation> timeNotationChoiceRenderer = new LabelValueChoiceRenderer<TimeNotation>();
    timeNotationChoiceRenderer.addValue(TimeNotation.H12, gridBuilder.getString("timeNotation.12"));
    timeNotationChoiceRenderer.addValue(TimeNotation.H24, gridBuilder.getString("timeNotation.24"));
    final DropDownChoice<TimeNotation> timeNotationChoice = new DropDownChoice<TimeNotation>(fs.getDropDownChoiceId(),
        new PropertyModel<TimeNotation>(user, "timeNotation"), timeNotationChoiceRenderer.getValues(),
        timeNotationChoiceRenderer);
    timeNotationChoice.setNullValid(true);
    fs.add(timeNotationChoice);
  }

  public static void createTimeZone(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // Time zone
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("timezone"));
    final TimeZoneField timeZone = new TimeZoneField(fs.getTextFieldId(),
        new PropertyModel<TimeZone>(user, "timeZoneObject"));
    fs.addKeyboardHelpIcon(gridBuilder.getString("tooltip.autocomplete.timeZone"));
    fs.add(timeZone);
  }

  /**
   * If no telephone system url is set in config.xml nothing will be done.
   *
   * @param gridBuilder
   * @param user
   */
  public static void createPhoneIds(final GridBuilder gridBuilder, final PFUserDO user)
  {
    if (StringUtils.isNotEmpty(ApplicationContextProvider.getApplicationContext().getBean(ConfigurationService.class)
        .getTelephoneSystemUrl()) == true) {
      // Personal phone identifiers
      final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("user.personalPhoneIdentifiers"));
      MaxLengthTextField personalPhoneIdentifiers = new MaxLengthTextField(fs.getTextFieldId(),
          new PropertyModel<String>(user, "personalPhoneIdentifiers"));
      personalPhoneIdentifiers.setMarkupId("personalPhoneIdentifiers").setOutputMarkupId(true);
      fs.add(personalPhoneIdentifiers);
      fs.addHelpIcon(new ResourceModel("user.personalPhoneIdentifiers.tooltip.title"), new ResourceModel(
          "user.personalPhoneIdentifiers.tooltip.content"));
    }
  }

  /**
   * If no MEB is configured in config.xml nothing will be done.
   *
   * @param gridBuilder
   * @param user
   */
  public static void createMEBPhoneNumbers(final GridBuilder gridBuilder, final PFUserDO user)
  {
    if (Configuration.getInstance().isMebConfigured() == true) {
      // MEB mobile phone numbers
      final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("user.personalMebMobileNumbers"));
      MaxLengthTextField personalMebMobileNumbers = new MaxLengthTextField(fs.getTextFieldId(),
          new PropertyModel<String>(user, "personalMebMobileNumbers"));
      personalMebMobileNumbers.setMarkupId("personalMebMobileNumbers").setOutputMarkupId(true);
      fs.add(personalMebMobileNumbers);
      fs.addHelpIcon(
          new ResourceModel("user.personalMebMobileNumbers.tooltip.title"),
          Model.of(gridBuilder.getString("user.personalMebMobileNumbers.tooltip.content")
              + " "
              + gridBuilder.getString("user.personalMebMobileNumbers.format")));
    }
  }

  public static void createDescription(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // Description
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("description"));
    MaxLengthTextArea description = new MaxLengthTextArea(fs.getTextAreaId(),
        new PropertyModel<String>(user, "description"));
    description.setMarkupId("description").setOutputMarkupId(true);
    fs.add(description);
  }

  public static void createSshPublicKey(final GridBuilder gridBuilder, final PFUserDO user)
  {
    // SSH public key
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("user.sshPublicKey"));
    MaxLengthTextArea sshPublicKey = new MaxLengthTextArea(fs.getTextAreaId(),
        new PropertyModel<String>(user, "sshPublicKey"));
    sshPublicKey.setMarkupId("sshPublicKey").setOutputMarkupId(true);
    fs.add(sshPublicKey);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    if (isNew() == true && Login.getInstance().hasExternalUsermanagementSystem() == false) {
      getData().setLocalUser(true);
    }
    ldapUserValues = PFUserDOConverter.readLdapUserValues(data.getLdapValues());
    if (ldapUserValues == null) {
      ldapUserValues = new LdapUserValues();
    }
    final boolean adminAccess = accessChecker.isLoggedInUserMemberOfAdminGroup();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // User
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user"));
      if (adminAccess == true) {
        usernameTextField = new RequiredMaxLengthTextField(fs.getTextFieldId(),
            new PropertyModel<String>(data, "username"));
        usernameTextField.setMarkupId("userName").setOutputMarkupId(true);
        WicketUtils.setStrong(usernameTextField);
        fs.add(usernameTextField);
        usernameTextField.add(new AbstractValidator<String>()
        {

          @Override
          protected void onValidate(final IValidatable<String> validatable)
          {
            data.setUsername(validatable.getValue());
            if (StringUtils.isNotEmpty(data.getUsername()) == true
                && ((UserDao) getBaseDao()).doesUsernameAlreadyExist(data) == true) {
              validatable.error(new ValidationError().addMessageKey("user.error.usernameAlreadyExists"));
            }
          }
        });
      } else {
        fs.add(new DivTextPanel(fs.newChildId(), data.getUsername()));
      }
    }
    createFirstName(gridBuilder, data);
    createLastName(gridBuilder, data);
    createOrganization(gridBuilder, data);
    createEMail(gridBuilder, data);
    createAuthenticationToken(gridBuilder, data, (UserDao) getBaseDao(), this);
    createJIRAUsername(gridBuilder, data);
    if (adminAccess == true) {
      gridBuilder.newFieldset(getString("user.hrPlanningEnabled"))
          .addCheckBox(new PropertyModel<Boolean>(data, "hrPlanning"), null)
          .setTooltip(getString("user.hrPlanningEnabled.tooltip"));
      gridBuilder.newFieldset(getString("user.activated")).addCheckBox(new Model<Boolean>()
      {
        @Override
        public Boolean getObject()
        {
          return data.isDeactivated() == false;
        }

        @Override
        public void setObject(final Boolean activated)
        {
          data.setDeactivated(!activated);
        }
      }, null).setTooltip(getString("user.activated.tooltip"));
      addPasswordFields();
      if (Login.getInstance().isWlanPasswordChangeSupported(data)) {
        addWlanPasswordFields();
      }
    }

    gridBuilder.newSplitPanel(GridSize.COL50);
    createLastLoginAndDeleteAllStayLogins(gridBuilder, data, userService, this);
    createLocale(gridBuilder, data);
    createDateFormat(gridBuilder, data);
    createExcelDateFormat(gridBuilder, data);
    createTimeNotation(gridBuilder, data);
    createTimeZone(gridBuilder, data);
    createPhoneIds(gridBuilder, data);
    createMEBPhoneNumbers(gridBuilder, data);
    createSshPublicKey(gridBuilder, data);

    gridBuilder.newGridPanel();
    addAssignedGroups(adminAccess);
    addAssignedTenants();
    if (adminAccess == true && Login.getInstance().hasExternalUsermanagementSystem() == true) {
      addLdapStuff();
    }
    if (adminAccess == true) {
      addRights();
    }

    gridBuilder.newGridPanel();
    createDescription(gridBuilder, data);
  }

  @SuppressWarnings("serial")
  private void addLdapStuff()
  {
    gridBuilder.newGridPanel();
    gridBuilder.newFormHeading(getString("ldap"));
    gridBuilder.newSplitPanel(GridSize.COL50);
    gridBuilder.newFieldset(getString("user.localUser"))
        .addCheckBox(new PropertyModel<Boolean>(data, "localUser"), null)
        .setTooltip(getString("user.localUser.tooltip"));
    final boolean posixConfigured = ldapUserDao.isPosixAccountsConfigured();
    final boolean sambaConfigured = ldapUserDao.isSambaAccountsConfigured();
    if (posixConfigured == false && sambaConfigured == false) {
      return;
    }
    final List<FormComponent<?>> dependentLdapPosixFormComponentsList = new LinkedList<FormComponent<?>>();
    final List<FormComponent<?>> dependentLdapSambaFormComponentsList = new LinkedList<FormComponent<?>>();
    if (posixConfigured == true) {
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("ldap.uidNumber"), getString("ldap.posixAccount"));
        uidNumberField = new MinMaxNumberField<Integer>(fs.getTextFieldId(),
            new PropertyModel<Integer>(ldapUserValues, "uidNumber"), 1,
            65535);
        uidNumberField.setMarkupId("uidNumberField").setOutputMarkupId(true);
        WicketUtils.setSize(uidNumberField, 6);
        fs.add(uidNumberField);
        fs.addHelpIcon(gridBuilder.getString("ldap.uidNumber.tooltip"));
        dependentLdapPosixFormComponentsList.add(uidNumberField);
        if (ldapUserValues.isPosixValuesEmpty() == true) {
          final Button createButton = newCreateButton(dependentLdapPosixFormComponentsList,
              dependentLdapSambaFormComponentsList, true,
              sambaConfigured);
          fs.add(new SingleButtonPanel(fs.newChildId(), createButton, gridBuilder.getString("create"),
              SingleButtonPanel.NORMAL));
          WicketUtils.addTooltip(createButton, gridBuilder.getString("ldap.uidNumber.createDefault.tooltip"));
        }
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("ldap.gidNumber"), getString("ldap.posixAccount"));
        gidNumberField = new MinMaxNumberField<Integer>(fs.getTextFieldId(),
            new PropertyModel<Integer>(ldapUserValues, "gidNumber"), 1,
            65535);
        gidNumberField.setMarkupId("gidNumberField").setOutputMarkupId(true);
        WicketUtils.setSize(gidNumberField, 6);
        fs.add(gidNumberField);
        dependentLdapPosixFormComponentsList.add(gidNumberField);
      }
    }
    final LdapSambaAccountsConfig ldapSambaAccountsConfig = ldapService.getLdapConfig()
        .getSambaAccountsConfig();
    if (sambaConfigured == true) {
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("ldap.sambaSID"));
        final DivTextPanel textPanel = new DivTextPanel(fs.newChildId(),
            ldapSambaAccountsConfig.getSambaSIDPrefix() + "-");
        fs.add(textPanel);
        sambaSIDNumberField = new MinMaxNumberField<Integer>(fs.getTextFieldId(),
            new PropertyModel<Integer>(ldapUserValues,
                "sambaSIDNumber"),
            1, 65535);
        sambaSIDNumberField.setMarkupId("sambaSIDNumberField").setOutputMarkupId(true);
        fs.add(sambaSIDNumberField);
        sambaSIDNumberField.setOutputMarkupId(true);
        WicketUtils.setSize(sambaSIDNumberField, 5);
        fs.addHelpIcon(getString("ldap.sambaSID.tooltip"));
        dependentLdapSambaFormComponentsList.add(sambaSIDNumberField);
        if (ldapUserValues.getSambaSIDNumber() == null) {
          final Button createButton = newCreateButton(dependentLdapPosixFormComponentsList,
              dependentLdapSambaFormComponentsList, false,
              true);
          fs.add(new SingleButtonPanel(fs.newChildId(), createButton, gridBuilder.getString("create"),
              SingleButtonPanel.NORMAL));
          WicketUtils.addTooltip(createButton, gridBuilder.getString("ldap.sambaSID.createDefault.tooltip"));
        }
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("ldap.sambaPrimaryGroupSID"),
            getString("ldap.sambaAccount"));
        final DivTextPanel textPanel = new DivTextPanel(fs.newChildId(),
            ldapSambaAccountsConfig.getSambaSIDPrefix() + "-");
        fs.add(textPanel);
        sambaPrimaryGroupSIDNumberField = new MinMaxNumberField<Integer>(fs.getTextFieldId(),
            new PropertyModel<Integer>(ldapUserValues,
                "sambaPrimaryGroupSIDNumber"),
            1, 65535);
        sambaPrimaryGroupSIDNumberField.setMarkupId("sambaPrimaryGroupSIDNumberField").setOutputMarkupId(true);
        fs.add(sambaPrimaryGroupSIDNumberField);
        sambaPrimaryGroupSIDNumberField.setOutputMarkupId(true);
        WicketUtils.setSize(sambaPrimaryGroupSIDNumberField, 5);
        fs.addHelpIcon(getString("ldap.sambaPrimaryGroupSID.tooltip"));
        dependentLdapSambaFormComponentsList.add(sambaPrimaryGroupSIDNumberField);
      }
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    gridBuilder.newFieldset(getString("user.restrictedUser"))
        .addCheckBox(new PropertyModel<Boolean>(data, "restrictedUser"), null)
        .setTooltip(getString("user.restrictedUser.tooltip"));
    if (posixConfigured == true) {
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("ldap.homeDirectory"),
            getString("ldap.posixAccount"));
        homeDirectoryField = new MaxLengthTextField(fs.getTextFieldId(),
            new PropertyModel<String>(ldapUserValues, "homeDirectory"), 255);
        homeDirectoryField.setMarkupId("homeDirectoryField").setOutputMarkupId(true);
        fs.add(homeDirectoryField);
        dependentLdapPosixFormComponentsList.add(homeDirectoryField);
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("ldap.loginShell"), getString("ldap.posixAccount"));
        loginShellField = new MaxLengthTextField(fs.getTextFieldId(),
            new PropertyModel<String>(ldapUserValues, "loginShell"), 100);
        loginShellField.setMarkupId("loginShellField").setOutputMarkupId(true);
        fs.add(loginShellField);
        dependentLdapPosixFormComponentsList.add(loginShellField);
      }
      if (ldapUserValues.isPosixValuesEmpty() == true) {
        for (final FormComponent<?> component : dependentLdapPosixFormComponentsList) {
          component.setEnabled(false);
        }
      }
    }
    if (sambaConfigured == true) {
      final FieldsetPanel fs = gridBuilder
          .newFieldset(getString("ldap.sambaNTPassword"), getString("ldap.sambaNTPassword.subtitle"))
          .suppressLabelForWarning();
      final DivTextPanel sambaNTPassword = new DivTextPanel(fs.newChildId(), "*****");
      fs.add(sambaNTPassword);
      fs.addHelpIcon(getString("ldap.sambaNTPassword.tooltip"));
      if (ldapUserValues.isSambaValuesEmpty() == true) {
        for (final FormComponent<?> component : dependentLdapSambaFormComponentsList) {
          component.setEnabled(false);
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
          final LdapUserValues values = new LdapUserValues();
          values.setUidNumber(uidNumberField.getConvertedInput());
          values.setGidNumber(gidNumberField.getConvertedInput());
          values.setHomeDirectory(homeDirectoryField.getConvertedInput());
          values.setLoginShell(loginShellField.getConvertedInput());
          if (StringUtils.isBlank(data.getLdapValues()) == true && values.isPosixValuesEmpty() == true) {
            // Nothing to validate: all fields are zero and posix account wasn't set for this user before.
            return;
          }
          if (values.getUidNumber() == null) {
            uidNumberField
                .error(getLocalizedMessage(WebConstants.I18N_KEY_FIELD_REQUIRED, getString("ldap.uidNumber")));
          } else {
            if (ldapPosixAccountsUtils.isGivenNumberFree(data, values.getUidNumber()) == false) {
              uidNumberField.error(
                  getLocalizedMessage("ldap.uidNumber.alreadyInUse", ldapPosixAccountsUtils.getNextFreeUidNumber()));
            }
          }
          if (values.getGidNumber() == null) {
            gidNumberField
                .error(getLocalizedMessage(WebConstants.I18N_KEY_FIELD_REQUIRED, getString("ldap.gidNumber")));
          }
          if (StringUtils.isBlank(values.getHomeDirectory()) == true) {
            homeDirectoryField
                .error(getLocalizedMessage(WebConstants.I18N_KEY_FIELD_REQUIRED, getString("ldap.homeDirectory")));
          }
          if (StringUtils.isBlank(values.getLoginShell()) == true) {
            loginShellField
                .error(getLocalizedMessage(WebConstants.I18N_KEY_FIELD_REQUIRED, getString("ldap.loginShell")));
          }
        }
      });
    }
    if (sambaConfigured == true) {
      add(new IFormValidator()
      {
        @Override
        public FormComponent<?>[] getDependentFormComponents()
        {
          return dependentLdapSambaFormComponentsList.toArray(new FormComponent[0]);
        }

        @Override
        public void validate(final Form<?> form)
        {
          final LdapUserValues values = new LdapUserValues();
          values.setSambaSIDNumber(sambaSIDNumberField.getConvertedInput());
          values.setSambaPrimaryGroupSIDNumber(sambaPrimaryGroupSIDNumberField.getConvertedInput());
          if (StringUtils.isBlank(data.getLdapValues()) == true && values.isSambaValuesEmpty() == true) {
            // Nothing to validate: all fields are zero and posix account wasn't set for this user before.
            return;
          }
          if (values.getSambaSIDNumber() == null) {
            sambaSIDNumberField
                .error(getLocalizedMessage(WebConstants.I18N_KEY_FIELD_REQUIRED, getString("ldap.sambaSID")));
          } else {
            if (ldapSambaAccountsUtils.isGivenNumberFree(data, values.getSambaSIDNumber()) == false) {
              sambaSIDNumberField.error(getLocalizedMessage("ldap.sambaSID.alreadyInUse",
                  ldapSambaAccountsUtils.getNextFreeSambaSIDNumber()));
            }
          }
          if (values.getSambaPrimaryGroupSIDNumber() != null && values.getSambaSIDNumber() == null) {
            sambaSIDNumberField
                .error(getLocalizedMessage(WebConstants.I18N_KEY_FIELD_REQUIRED, getString("ldap.sambaSID")));
          }
        }
      });
    }
  }

  @SuppressWarnings("serial")
  private Button newCreateButton(final List<FormComponent<?>> dependentPosixLdapFormComponentsList,
      final List<FormComponent<?>> dependentSambaLdapFormComponentsList, final boolean updatePosixAccount,
      final boolean updateSambaAccount)
  {
    final AjaxButton createButton = new AjaxButton(SingleButtonPanel.WICKET_ID, this)
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
      {
        data.setUsername(usernameTextField.getRawInput());
        if (updatePosixAccount == true) {
          ldapPosixAccountsUtils.setDefaultValues(ldapUserValues, data);
          if (updateSambaAccount == true) {
            ldapSambaAccountsUtils.setDefaultValues(ldapUserValues, data);
            sambaSIDNumberField.modelChanged();
            sambaPrimaryGroupSIDNumberField.modelChanged();
            target.add(sambaSIDNumberField, sambaPrimaryGroupSIDNumberField);
          }
        } else if (updateSambaAccount == true) {
          ldapSambaAccountsUtils.setDefaultValues(ldapUserValues, data);
          sambaSIDNumberField.modelChanged();
          sambaPrimaryGroupSIDNumberField.modelChanged();
          target.add(sambaSIDNumberField, sambaPrimaryGroupSIDNumberField);
        }
        if (updatePosixAccount == true) {
          for (final FormComponent<?> component : dependentPosixLdapFormComponentsList) {
            component.modelChanged();
            component.setEnabled(true);
          }
        }
        if (updateSambaAccount == true) {
          for (final FormComponent<?> component : dependentSambaLdapFormComponentsList) {
            component.modelChanged();
            component.setEnabled(true);
          }
        }
        this.setVisible(false);
        for (final FormComponent<?> comp : dependentPosixLdapFormComponentsList) {
          target.add(comp);
        }
        for (final FormComponent<?> comp : dependentSambaLdapFormComponentsList) {
          target.add(comp);
        }
        target.add(this, UserEditForm.this.feedbackPanel);
        target.appendJavaScript("hideAllTooltips();"); // Otherwise a tooltip is left as zombie.
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final Form<?> form)
      {
        target.add(UserEditForm.this.feedbackPanel);
      }
    };
    createButton.setDefaultFormProcessing(false);
    return createButton;
  }

  @SuppressWarnings("serial")
  private void addPasswordFields()
  {
    // Password
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("password"), getString("passwordRepeat"));
    final PasswordTextField passwordField = new PasswordTextField(fs.getTextFieldId(), new PropertyModel<>(this, "password"))
    {
      @Override
      protected void onComponentTag(final ComponentTag tag)
      {
        super.onComponentTag(tag);
        if (passwordUser == null) {
          tag.put("value", "");
        }
      }
    };
    passwordField.setMarkupId("password").setOutputMarkupId(true);
    passwordField.setResetPassword(false).setRequired(isNew());

    // Password repeat
    final PasswordTextField passwordRepeatField = new PasswordTextField(fs.getTextFieldId(), new PropertyModel<>(this, "passwordRepeat"))
    {
      @Override
      protected void onComponentTag(final ComponentTag tag)
      {
        super.onComponentTag(tag);
        if (passwordUser == null) {
          tag.put("value", "");
        }
      }
    };
    passwordRepeatField.setMarkupId("passwordRepeat").setOutputMarkupId(true);
    passwordRepeatField.setResetPassword(false).setRequired(false);

    // validation
    passwordRepeatField.add((INullAcceptingValidator<String>) validatable -> {
      final String passwordRepeatInput = validatable.getValue();
      passwordField.validate();
      final String passwordInput = passwordField.getConvertedInput();
      if (StringUtils.isEmpty(passwordInput) == true && StringUtils.isEmpty(passwordRepeatInput) == true) {
        passwordUser = null;
        return;
      }
      if (StringUtils.equals(passwordInput, passwordRepeatInput) == false) {
        passwordUser = null;
        validatable.error(new ValidationError().addKey("user.error.passwordAndRepeatDoesNotMatch"));
        return;
      }
      if (passwordUser == null) {
        final String errorMsgKey = userService.checkPasswordQuality(passwordInput);
        if (errorMsgKey != null) {
          validatable.error(new ValidationError().addKey(errorMsgKey));
        } else {
          passwordUser = new PFUserDO();
          userService.createEncryptedPassword(passwordUser, passwordInput);
        }
      }
    });

    WicketUtils.setPercentSize(passwordField, 50);
    WicketUtils.setPercentSize(passwordRepeatField, 50);
    fs.add(passwordField);
    fs.add(passwordRepeatField);
    fs.addHelpIcon(getString(Const.MESSAGE_KEY_PASSWORD_QUALITY_CHECK));
  }

  private void addWlanPasswordFields()
  {
    // wlan password
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("ldap.wlanSambaPassword"), getString("passwordRepeat"));
    final PasswordTextField passwordField = new PasswordTextField(fs.getTextFieldId(), new PropertyModel<>(this, "wlanPassword"))
    {
      @Override
      protected void onComponentTag(final ComponentTag tag)
      {
        super.onComponentTag(tag);
        if (wlanPasswordValid == false) {
          tag.put("value", "");
        }
      }
    };
    passwordField.setMarkupId("wlanPassword").setOutputMarkupId(true);
    passwordField.setResetPassword(false).setRequired(isNew());

    // wlan password repeat
    final PasswordTextField passwordRepeatField = new PasswordTextField(fs.getTextFieldId(), new PropertyModel<>(this, "wlanPasswordRepeat"))
    {
      @Override
      protected void onComponentTag(final ComponentTag tag)
      {
        super.onComponentTag(tag);
        if (wlanPasswordValid == false) {
          tag.put("value", "");
        }
      }
    };
    passwordRepeatField.setMarkupId("wlanPasswordRepeat").setOutputMarkupId(true);
    passwordRepeatField.setResetPassword(false).setRequired(false);

    // validation
    passwordRepeatField.add((INullAcceptingValidator<String>) validatable -> {
      wlanPasswordValid = false;
      final String passwordRepeatInput = validatable.getValue();
      passwordField.validate();
      final String passwordInput = passwordField.getConvertedInput();

      if (StringUtils.isEmpty(passwordInput) && StringUtils.isEmpty(passwordRepeatInput)) {
        return;
      }

      if (StringUtils.equals(passwordInput, passwordRepeatInput) == false) {
        validatable.error(new ValidationError().addKey("user.error.passwordAndRepeatDoesNotMatch"));
        return;
      }

      final String errorMsgKey = userService.checkPasswordQuality(passwordInput);
      if (errorMsgKey != null) {
        validatable.error(new ValidationError().addKey(errorMsgKey));
      } else {
        wlanPasswordValid = true;
      }
    });

    WicketUtils.setPercentSize(passwordField, 50);
    WicketUtils.setPercentSize(passwordRepeatField, 50);
    fs.add(passwordField);
    fs.add(passwordRepeatField);
    fs.addHelpIcon(getString(Const.MESSAGE_KEY_PASSWORD_QUALITY_CHECK));
  }

  private static void addDateFormatCombobox(final GridBuilder gridBuilder, final PFUserDO user, final String labelKey,
      final String property, final String[] dateFormats, final boolean convertExcelFormat)
  {
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString(labelKey));
    final LabelValueChoiceRenderer<String> dateFormatChoiceRenderer = new LabelValueChoiceRenderer<String>();
    for (final String str : dateFormats) {
      String dateString = "???";
      final String pattern = convertExcelFormat == true ? str.replace('Y', 'y').replace('D', 'd') : str;
      try {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateString = dateFormat.format(new Date());
      } catch (final Exception ex) {
        log.warn("Invalid date format in config.xml: " + pattern);
      }
      dateFormatChoiceRenderer.addValue(str, str + ": " + dateString);
    }
    final DropDownChoice<String> dateFormatChoice = new DropDownChoice<String>(fs.getDropDownChoiceId(),
        new PropertyModel<String>(user,
            property),
        dateFormatChoiceRenderer.getValues(), dateFormatChoiceRenderer);
    dateFormatChoice.setNullValid(true);
    fs.add(dateFormatChoice);
  }

  private void addRights()
  {
    final List<UserRightVO> userRights = userRightDao.getUserRights(data);
    boolean first = true;
    boolean odd = true;
    for (final UserRightVO rightVO : userRights) {
      final UserRight right = rightVO.getRight();
      final UserGroupCache userGroupCache = getTenantRegistry().getUserGroupCache();
      final UserRightValue[] availableValues = right.getAvailableValues(userGroupCache, data);
      if (right.isConfigurable(userGroupCache, data) == false) {
        continue;
      }
      if (first == true) {
        gridBuilder.newGridPanel();
        gridBuilder.newFormHeading(getString("access.rights"));
        rightsData = new UserRightsEditData();
        first = false;
      }
      if (odd == true) {
        // gridBuilder.newNestedRowPanel();
      }
      odd = !odd;
      gridBuilder.newSplitPanel(GridSize.COL50);
      rightsData.addRight(rightVO);
      final String label = getString(right.getId().getI18nKey());
      final FieldsetPanel fs = gridBuilder.newFieldset(label);
      if (right.isBooleanType() == true) {
        fs.addCheckBox(new PropertyModel<Boolean>(rightVO, "booleanValue"), null);
      } else {
        final LabelValueChoiceRenderer<UserRightValue> valueChoiceRenderer = new LabelValueChoiceRenderer<UserRightValue>(
            fs,
            availableValues);
        final DropDownChoice<UserRightValue> valueChoice = new DropDownChoice<UserRightValue>(fs.getDropDownChoiceId(),
            new PropertyModel<UserRightValue>(rightVO, "value"), valueChoiceRenderer.getValues(), valueChoiceRenderer);
        valueChoice.setNullValid(true);
        fs.add(valueChoice);
      }
    }
  }

  private void addAssignedGroups(final boolean adminAccess)
  {
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("user.assignedGroups")).setLabelSide(false);
    final Collection<Integer> set = ((UserDao) getBaseDao()).getAssignedGroups(data);
    assignGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator()).setFullList(
        groupService.getSortedGroups());
    if (set != null) {
      for (final Integer groupId : set) {
        final GroupDO group = groupService.getGroup(groupId);
        if (group != null) {
          assignGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
        }
      }
    }

    final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
        new PropertyModel<Collection<GroupDO>>(this.assignGroupsListHelper, "assignedItems"),
        new GroupsWicketProvider(groupService));
    groups.setMarkupId("groups").setOutputMarkupId(true);
    fs.add(groups);
  }

  private void addAssignedTenants()
  {
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("multitenancy.assignedTenants")).setLabelSide(false);
    final Collection<Integer> set = userService.getAssignedTenants(data);
    final TenantsProvider tenantsProvider = new TenantsProvider(tenantService);
    assignTenantsListHelper = new MultiChoiceListHelper<TenantDO>().setComparator(new TenantsComparator()).setFullList(
        tenantsProvider.getSortedTenants());
    if (set != null) {
      for (final Integer tenantId : set) {
        final TenantDO tenant = tenantService.getTenant(tenantId);
        if (tenant != null) {
          assignTenantsListHelper.addOriginalAssignedItem(tenant).assignItem(tenant);
        }
      }
    }
    final Select2MultiChoice<TenantDO> tenants = new Select2MultiChoice<TenantDO>(fs.getSelect2MultiChoiceId(),
        new PropertyModel<Collection<TenantDO>>(this.assignTenantsListHelper, "assignedItems"), tenantsProvider);
    fs.add(tenants);
  }

  /**
   * @return the passwordUser
   */
  PFUserDO getPasswordUser()
  {
    return passwordUser;
  }

  /**
   * @return The clear text password (if given). Please check {@link #getPasswordUser()} first.
   */
  String getPassword()
  {
    return password;
  }

  /**
   * @return The clear text wlan password if given and valid.
   */
  String getWlanPassword()
  {
    return wlanPassword;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
