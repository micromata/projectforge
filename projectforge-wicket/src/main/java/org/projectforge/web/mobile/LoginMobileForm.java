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

package org.projectforge.web.mobile;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;

public class LoginMobileForm extends AbstractMobileForm<LoginMobileForm, LoginMobilePage>
{
  private static final long serialVersionUID = 563729268811279098L;

  private boolean stayLoggedIn;

  private String username, password;

  public LoginMobileForm(final LoginMobilePage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  protected void init()
  {
    add(new CheckBox("stayLoggedIn", new PropertyModel<Boolean>(this, "stayLoggedIn")));
    add(new TextField<String>("username", new PropertyModel<String>(this, "username")));
    add(new PasswordTextField("password", new PropertyModel<String>(this, "password")).setResetPassword(true).setRequired(true));
    final SubmitLink loginButton = new SubmitLink("login") {
      @Override
      public final void onSubmit()
      {
        parentPage.checkLogin();
      }
    };
    add(loginButton);
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(final String username)
  {
    this.username = username;
  }

  public String getPassword()
  {
    return password;
  }

  public void setPassword(final String password)
  {
    this.password = password;
  }

  public boolean isStayLoggedIn()
  {
    return stayLoggedIn;
  }

  public void setStayLoggedIn(final boolean stayLoggedIn)
  {
    this.stayLoggedIn = stayLoggedIn;
  }
}
