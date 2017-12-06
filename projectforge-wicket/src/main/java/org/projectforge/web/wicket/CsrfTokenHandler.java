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

package org.projectforge.web.wicket;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.web.session.MySession;

/**
 * Every form should use this handler for preventing cross site request forgery attacks.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class CsrfTokenHandler implements Serializable
{
  private static final long serialVersionUID = -9129345307409567900L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CsrfTokenHandler.class);

  private String csrfToken;

  /**
   * The given form should contain a hidden field named 'csrfToken'.
   *
   * @param form
   */
  public CsrfTokenHandler(final Form<?> form)
  {
    csrfToken = getCsrfSessionToken();
    form.add(new HiddenField<String>("csrfToken", new PropertyModel<String>(this, "csrfToken")));
  }

  /**
   * This parameter should be set as hidden field in every formular and should be tested on every submit action for
   * preventing CSRF attacks.
   *
   * @return the randomized cross site request forgery token.
   */
  private String getCsrfSessionToken()
  {
    final MySession session = (MySession) Session.get();
    return session.getCsrfToken();
  }

  /**
   * Checks the cross site request forgery token (as posted hidden field) and if it doesn't match an exception is
   * thrown.
   *
   * @see org.apache.wicket.markup.html.form.Form#onSubmit()
   */
  public void onSubmit()
  {
    final String sessionCsrfToken = getCsrfSessionToken();
    if (StringUtils.equals(sessionCsrfToken, csrfToken) == false) {
      log.error("Cross site request forgery alert. csrf token doesn't match! session csrf token="
          + sessionCsrfToken
          + ", posted csrf token="
          + csrfToken);
      throw new InternalErrorException("errorpage.csrfError");
    }
  }
}
