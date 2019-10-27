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

package org.projectforge.web;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.login.LoginResultStatus;
import org.projectforge.web.wicket.AbstractForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.LabelPanel;

public class LoginForm extends AbstractForm<LoginForm, LoginPage> {
  private static final long serialVersionUID = -422822736093879603L;

  private boolean stayLoggedIn;

  private String username, password, originalDestination;

  public LoginForm(final LoginPage parentPage) {
    super(parentPage);
    this.originalDestination = WicketUtils.getAsString(parentPage.getPageParameters(), "url");
  }

  @Override
  @SuppressWarnings("serial")
  protected void init() {
    add(new FeedbackPanel("feedback").setOutputMarkupId(true));
    {
      final FieldsetPanel fs = new FieldsetPanel("username", getString("username"));
      add(fs);
      final RequiredTextField<String> username = new RequiredTextField<String>(fs.getTextFieldId(),
              new PropertyModel<String>(this,
                      "username"));
      username.setRequired(true).setMarkupId("username").setOutputMarkupId(true);
      fs.add(username);
      WicketUtils.setFocus(username);
    }
    {
      final FieldsetPanel fs = new FieldsetPanel("password", getString("password"));
      add(fs);
      final PasswordTextField password = new PasswordTextField(fs.getTextFieldId(),
              new PropertyModel<String>(this, "password"));
      password.setResetPassword(true).setRequired(true).setMarkupId("password").setOutputMarkupId(true);
      fs.add(password);
    }
    {
      final FieldsetPanel fs = new FieldsetPanel("stayLoggedIn", "").suppressLabelForWarning();
      add(fs);
      final CheckBoxPanel stayLoggedInCheckBox = fs.addCheckBox(new PropertyModel<Boolean>(this, "stayLoggedIn"), null);
      stayLoggedInCheckBox.getCheckBox().setMarkupId("loggedIn").setOutputMarkupId(true);
      final DivPanel divPanel = new DivPanel(fs.newChildId());
      fs.add(divPanel);
      final LabelPanel labelPanel = new LabelPanel(divPanel.newChildId(), getString("login.stayLoggedIn"));
      stayLoggedInCheckBox.setMarkupId("loggedIn").setOutputMarkupId(true);
      labelPanel.setLabelFor(stayLoggedInCheckBox.getCheckBox().getMarkupId());
      divPanel.add(labelPanel);
      WicketUtils.addTooltip(labelPanel.getLabel(), getString("login.stayLoggedIn"),
              getString("login.stayLoggedIn.tooltip"));
      stayLoggedInCheckBox.setTooltip(getString("login.stayLoggedIn"), getString("login.stayLoggedIn.tooltip"));
    }
    final Button loginButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("login")) {
      @Override
      public final void onSubmit() {
        final LoginResultStatus status = parentPage.checkLogin();
        if (status != null) {
          parentPage.addError(status.getLocalizedMessage());
        }
      }
    };
    loginButton.setMarkupId("loginButton").setOutputMarkupId(true);
    setDefaultButton(loginButton);
    add(new SingleButtonPanel("loginButton", loginButton, getString("login"), SingleButtonPanel.DEFAULT_SUBMIT));
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getOriginalDestination() {
    return originalDestination;
  }

  public boolean isStayLoggedIn() {
    return stayLoggedIn;
  }

  public void setStayLoggedIn(final boolean stayLoggedIn) {
    this.stayLoggedIn = stayLoggedIn;
  }
}
