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

package org.projectforge.web.admin;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.projectforge.business.password.PasswordQualityService;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractForm;
import org.projectforge.web.wicket.CsrfTokenHandler;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.components.TimeZonePanel;
import org.projectforge.web.wicket.flowlayout.*;

import java.util.List;
import java.util.TimeZone;

public class SetupForm extends AbstractForm<SetupForm, SetupPage> {
  private static final long serialVersionUID = -277853572580468505L;

  private SetupTarget setupTarget = SetupTarget.TEST_DATA;

  // this value is changed by a PropertyModel
  private TimeZone timeZone = TimeZone.getDefault();

  private String password, passwordRepeat;

  private final IModel<String> sysopEMailModel = new Model<>();

  private final IModel<String> feedbackEMailModel = new Model<>();

  private final IModel<String> calendarDomainModel = new Model<>();

  // @SuppressWarnings("unused")
  // private String organization;

  /**
   * User for storing adminUsername and password with salt.
   */
  private final PFUserDO adminUser = new PFUserDO();

  /**
   * Cross site request forgery token.
   */
  private final CsrfTokenHandler csrfTokenHandler = new CsrfTokenHandler(this);

  public SetupForm(final SetupPage parentPage) {
    super(parentPage, "setupform");
    adminUser.setUsername(DatabaseService.DEFAULT_ADMIN_USER);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init() {
    add(createFeedbackPanel());
    final GridBuilder gridBuilder = newGridBuilder(this, "flowform");
    gridBuilder.newFormHeading(getString("administration.setup.heading"));
    final DivPanel panel = gridBuilder.getPanel();
    panel.add(new ParTextPanel(panel.newChildId(), getString("administration.setup.heading.subtitle")));
    {
      // RadioChoice mode
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("administration.setup.target"));
      final DivPanel radioPanel = fs.addNewRadioBoxButtonDiv();
      fs.add(radioPanel);
      fs.setLabelFor(radioPanel);
      final RadioGroupPanel<SetupTarget> radioGroup = new RadioGroupPanel<>(radioPanel.newChildId(), "setuptarget",
          new PropertyModel<>(this, "setupTarget"));
      radioPanel.add(radioGroup);
      for (final SetupTarget target : SetupTarget.values()) {
        radioGroup.add(new Model<>(target), getString(target.getI18nKey()),
            getString(target.getI18nKey() + ".tooltip"));
      }
    }
    // final RequiredMaxLengthTextField organizationField = new RequiredMaxLengthTextField(this, "organization", getString("organization"),
    // new PropertyModel<String>(this, "organization"), 100);
    // add(organizationField);
    {
      // User name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("username"));
      RequiredMaxLengthTextField usernameTextField = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(adminUser, "username"), 100);
      usernameTextField.setMarkupId("username");
      usernameTextField.setOutputMarkupId(true);
      fs.add(usernameTextField);
    }
    final PasswordTextField passwordField = new PasswordTextField(PasswordPanel.WICKET_ID,
        new PropertyModel<String>(this, "password"));
    passwordField.setMarkupId("password").setOutputMarkupId(true);
    {
      // Password
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("password"));
      passwordField.setRequired(true); // No setReset(true), otherwise uploading and re-entering passwords is a real pain.
      fs.add(passwordField);
      WicketUtils.setFocus(passwordField);
    }
    {
      // Password repeat
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("passwordRepeat"));
      final PasswordTextField passwordRepeatField = new PasswordTextField(PasswordPanel.WICKET_ID,
          new PropertyModel<String>(this, "passwordRepeat"));
      passwordRepeatField.setRequired(true); // No setReset(true), otherwise uploading and re-entering passwords is a real pain.
      passwordRepeatField.setMarkupId("passwordRepeat").setOutputMarkupId(true);
      passwordRepeatField.add((IValidator<String>) validatable -> {
        final String input = validatable.getValue();
        final String passwordInput = passwordField.getConvertedInput();
        if (StringUtils.equals(input, passwordInput) == false) {
          passwordRepeatField.error(getString("user.error.passwordAndRepeatDoesNotMatch"));
          return;
        }
        final List<I18nKeyAndParams> errorMsgKeys = WicketSupport.get(PasswordQualityService.class).checkPasswordQuality(passwordInput.toCharArray());
        if (errorMsgKeys.isEmpty() == false) {
          for (I18nKeyAndParams errorMsgKey : errorMsgKeys) {
            passwordField.error(I18nHelper.getLocalizedMessage(errorMsgKey));
          }
        }
      });
      fs.add(passwordRepeatField);
    }
    {
      // Time zone
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("administration.configuration.param.timezone"));
      final TimeZonePanel timeZone = new TimeZonePanel(fs.newChildId(), new PropertyModel<>(this, "timeZone"));
      fs.setLabelFor(timeZone);
      fs.add(timeZone);
      fs.addHelpIcon(getString("administration.configuration.param.timezone.description"));
    }
    {
      // Calendar domain
      calendarDomainModel.setObject("local");
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("administration.configuration.param.calendarDomain"));
      final RequiredMaxLengthTextField textField = new RequiredMaxLengthTextField(InputPanel.WICKET_ID, calendarDomainModel, ConfigurationDO.Companion.getParamLength());
      fs.add(textField);
      textField.setMarkupId("calendarDomain").setOutputMarkupId(true);
      textField.add(new IValidator<String>() {
        @Override
        public void validate(final IValidatable<String> validatable) {
          if (Configuration.isDomainValid(validatable.getValue()) == false) {
            textField.error(getString("validation.error.generic"));
          }
        }
      });
      fs.addHelpIcon(getString("administration.configuration.param.calendarDomain.description"));
    }
    {
      // E-Mail sysops
      final FieldsetPanel fs = gridBuilder.newFieldset(
          getString("administration.configuration.param.systemAdministratorEMail.label"),
          getString("email"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, sysopEMailModel, ConfigurationDO.Companion.getParamLength()));
      fs.addHelpIcon(getString("administration.configuration.param.systemAdministratorEMail.description"));
    }
    {
      // E-Mail sysops
      final FieldsetPanel fs = gridBuilder.newFieldset(
          getString("administration.configuration.param.feedbackEMail.label"),
          getString("email"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, feedbackEMailModel, ConfigurationDO.Companion.getParamLength()));
      fs.addHelpIcon(getString("administration.configuration.param.feedbackEMail.description"));
    }
    final RepeatingView actionButtons = new RepeatingView("buttons");
    add(actionButtons);
    {
      final Button finishButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("finish")) {
        @Override
        public final void onSubmit() {
          csrfTokenHandler.onSubmit();
          parentPage.finishSetup();
        }
      };
      finishButton.setMarkupId("finish").setOutputMarkupId(true);
      final SingleButtonPanel finishButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), finishButton,
          getString("administration.setup.finish"), SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(finishButtonPanel);
      setDefaultButton(finishButton);
    }
  }

  @Override
  protected void onSubmit() {
    super.onSubmit();
    csrfTokenHandler.onSubmit();
  }

  public void setSetupTarget(SetupTarget setupTarget) {
    this.setupTarget = setupTarget;
  }

  public SetupTarget getSetupTarget() {
    return setupTarget;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public String getPassword() {
    return password;
  }

  /**
   * @return the calendarDomain
   */
  String getCalendarDomain() {
    return calendarDomainModel.getObject();
  }

  String getSysopEMail() {
    return sysopEMailModel.getObject();
  }

  String getFeedbackEMail() {
    return feedbackEMailModel.getObject();
  }

  /**
   * @return the adminUser containing the desired username and password with the used salt-string.
   */
  PFUserDO getAdminUser() {
    return adminUser;
  }
}
