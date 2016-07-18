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

package org.projectforge.web.admin;

import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.AbstractForm;
import org.projectforge.web.wicket.CsrfTokenHandler;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.components.TimeZonePanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.ParTextPanel;
import org.projectforge.web.wicket.flowlayout.PasswordPanel;
import org.projectforge.web.wicket.flowlayout.RadioGroupPanel;

public class SetupForm extends AbstractForm<SetupForm, SetupPage>
{
  private static final long serialVersionUID = -277853572580468505L;

  private static final String MAGIC_PASSWORD = "******";

  @SpringBean
  private UserService userService;

  private final SetupTarget setupMode = SetupTarget.TEST_DATA;

  private final TimeZone timeZone = TimeZone.getDefault();

  private String sysopEMail;

  private String feedbackEMail;

  private String calendarDomain;

  // @SuppressWarnings("unused")
  // private String organization;

  @SuppressWarnings("unused")
  private String password;

  @SuppressWarnings("unused")
  private String passwordRepeat;

  /** User for storing adminUsername and password with salt. */
  private final PFUserDO adminUser;

  /**
   * Cross site request forgery token.
   */
  private final CsrfTokenHandler csrfTokenHandler;

  public SetupForm(final SetupPage parentPage)
  {
    super(parentPage, "setupform");
    csrfTokenHandler = new CsrfTokenHandler(this);
    adminUser = new PFUserDO();
    adminUser.setUsername(InitDatabaseDao.DEFAULT_ADMIN_USER);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
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
      final RadioGroupPanel<SetupTarget> radioGroup = new RadioGroupPanel<SetupTarget>(radioPanel.newChildId(),
          "setuptarget",
          new PropertyModel<SetupTarget>(this, "setupMode"));
      radioPanel.add(radioGroup);
      for (final SetupTarget target : SetupTarget.values()) {
        radioGroup.add(new Model<SetupTarget>(target), getString(target.getI18nKey()),
            getString(target.getI18nKey() + ".tooltip"));
      }
    }
    // final RequiredMaxLengthTextField organizationField = new RequiredMaxLengthTextField(this, "organization", getString("organization"),
    // new PropertyModel<String>(this, "organization"), 100);
    // add(organizationField);
    {
      // User name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("username"));
      fs.add(
          new RequiredMaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(adminUser, "username"), 100));
    }
    final PasswordTextField passwordField = new PasswordTextField(PasswordPanel.WICKET_ID,
        new PropertyModel<String>(this, "password"))
    {
      @Override
      protected void onComponentTag(final ComponentTag tag)
      {
        super.onComponentTag(tag);
        if (adminUser.getPassword() == null) {
          tag.put("value", "");
        } else if (StringUtils.isEmpty(getConvertedInput()) == false) {
          tag.put("value", MAGIC_PASSWORD);
        }
      }
    };
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
          new PropertyModel<String>(this,
              "passwordRepeat"))
      {
        @Override
        protected void onComponentTag(final ComponentTag tag)
        {
          super.onComponentTag(tag);
          if (adminUser.getPassword() == null) {
            tag.put("value", "");
          } else if (StringUtils.isEmpty(getConvertedInput()) == false) {
            tag.put("value", MAGIC_PASSWORD);
          }
        }
      };
      passwordRepeatField.setRequired(true); // No setReset(true), otherwise uploading and re-entering passwords is a real pain.
      passwordRepeatField.add(new IValidator<String>()
      {
        @Override
        public void validate(final IValidatable<String> validatable)
        {
          final String input = validatable.getValue();
          final String passwordInput = passwordField.getConvertedInput();
          if (StringUtils.equals(input, passwordInput) == false) {
            passwordRepeatField.error(getString("user.error.passwordAndRepeatDoesNotMatch"));
            adminUser.setPassword(null);
            return;
          }
          if (MAGIC_PASSWORD.equals(passwordInput) == false || adminUser.getPassword() == null) {
            final String errorMsgKey = userService.checkPasswordQuality(passwordInput);
            if (errorMsgKey != null) {
              adminUser.setPassword(null);
              passwordField.error(getString(errorMsgKey));
            } else {
              userService.createEncryptedPassword(adminUser, passwordInput);
            }
          }
        }
      });
      fs.add(passwordRepeatField);
    }
    {
      // Time zone
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("administration.configuration.param.timezone"));
      final TimeZonePanel timeZone = new TimeZonePanel(fs.newChildId(),
          new PropertyModel<TimeZone>(this, "timeZone"));
      fs.setLabelFor(timeZone);
      fs.add(timeZone);
      fs.addHelpIcon(getString("administration.configuration.param.timezone.description"));
    }
    {
      // Calendar domain
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("administration.configuration.param.calendarDomain"));
      final RequiredMaxLengthTextField textField = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(this,
              "calendarDomain"),
          ConfigurationDO.PARAM_LENGTH);
      fs.add(textField);
      textField.add(new IValidator<String>()
      {
        @Override
        public void validate(final IValidatable<String> validatable)
        {
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
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(this, "sysopEMail"),
          ConfigurationDO.PARAM_LENGTH));
      fs.addHelpIcon(getString("administration.configuration.param.systemAdministratorEMail.description"));
    }
    {
      // E-Mail sysops
      final FieldsetPanel fs = gridBuilder.newFieldset(
          getString("administration.configuration.param.feedbackEMail.label"),
          getString("email"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(this, "feedbackEMail"),
          ConfigurationDO.PARAM_LENGTH));
      fs.addHelpIcon(getString("administration.configuration.param.feedbackEMail.description"));
    }
    final RepeatingView actionButtons = new RepeatingView("buttons");
    add(actionButtons);
    {
      final Button finishButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("finish"))
      {
        @Override
        public final void onSubmit()
        {
          csrfTokenHandler.onSubmit();
          parentPage.finishSetup();
        }
      };
      final SingleButtonPanel finishButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), finishButton,
          getString("administration.setup.finish"), SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(finishButtonPanel);
      setDefaultButton(finishButton);
    }
  }

  @Override
  protected void onSubmit()
  {
    super.onSubmit();
    csrfTokenHandler.onSubmit();
  }

  public SetupTarget getSetupMode()
  {
    return setupMode;
  }

  public TimeZone getTimeZone()
  {
    return timeZone;
  }

  /**
   * @return the calendarDomain
   */
  public String getCalendarDomain()
  {
    return calendarDomain;
  }

  public String getSysopEMail()
  {
    return sysopEMail;
  }

  public String getFeedbackEMail()
  {
    return feedbackEMail;
  }

  /**
   * @return the adminUser containing the desired username and password with the used salt-string.
   */
  public PFUserDO getAdminUser()
  {
    return adminUser;
  }
}
