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

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.login.Login;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.MessagePage;

public class ChangeWlanPasswordPage extends AbstractStandardFormPage
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChangeWlanPasswordPage.class);

  @SpringBean
  private UserService userService;

  private final ChangeWlanPasswordForm form;

  public ChangeWlanPasswordPage(final PageParameters parameters)
  {
    super(parameters);
    if (Login.getInstance().isWlanPasswordChangeSupported(getUser()) == false) {
      throw new RestartResponseException(new MessagePage("user.changePassword.notSupported"));
    }
    form = new ChangeWlanPasswordForm(this);
    body.add(form);
    form.init();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredBasePage#isAccess4restrictedUsersAllowed()
   */
  @Override
  public boolean isAccess4restrictedUsersAllowed()
  {
    return true;
  }

  protected void cancel()
  {
    setResponsePage(new MessagePage("message.cancelAction", getString("user.changeWlanPassword.title")));
  }

  protected void update()
  {
    if (StringUtils.equals(form.getNewWlanPassword(), form.getWlanPasswordRepeat()) == false) {
      form.addError("user.error.passwordAndRepeatDoesNotMatch");
      return;
    }

    log.info("User wants to change his WLAN password.");
    final Set<I18nKeyAndParams> errorMsgKeys = userService.changeWlanPassword(getUser(), form.getLoginPassword(), form.getNewWlanPassword());
    if (errorMsgKeys != null) {
      for (I18nKeyAndParams errorMsgKey : errorMsgKeys) {
        form.addError(errorMsgKey);
      }
      return;
    }

    setResponsePage(new MessagePage("user.changePassword.msg.passwordSuccessfullyChanged"));
  }

  @Override
  protected String getTitle()
  {
    return getString("user.changeWlanPassword.title");
  }
}
