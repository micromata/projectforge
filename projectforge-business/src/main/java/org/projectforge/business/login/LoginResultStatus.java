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

package org.projectforge.business.login;

import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum LoginResultStatus implements I18nEnum
{
  ADMIN_LOGIN_REQUIRED("adminLoginRequired"), /** This account is locked for x seconds due to failed login attempts. */
  LOGIN_TIME_OFFSET("timeOffset"), FAILED("error.loginFailed"), LOGIN_EXPIRED("error.loginExpired"), SUCCESS("success");

  private String key;

  private Object[] msgParams;

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  LoginResultStatus(final String key)
  {
    this.key = key;
  }

  public boolean isIn(final LoginResultStatus... loginResult)
  {
    for (final LoginResultStatus status : loginResult) {
      if (this == status) {
        return true;
      }
    }
    return false;
  }

  public String getI18nKey()
  {
    return "login." + key;
  }

  public String getLocalizedMessage()
  {
    if (this == LOGIN_TIME_OFFSET) {
      // msgParam is seconds.
      return ThreadLocalUserContext.getLocalizedMessage(getI18nKey(), msgParams);
    }
    return ThreadLocalUserContext.getLocalizedString(getI18nKey());
  }

  /**
   * Used for {@link #LOGIN_TIME_OFFSET} as parameter for seconds.
   * @param msgParams the msgParam to set
   * @return this for chaining.
   */
  public LoginResultStatus setMsgParams(final Object... msgParams)
  {
    this.msgParams = msgParams;
    return this;
  }

  /**
   * @return the msgParam
   */
  public Object[] getMsgParams()
  {
    return msgParams;
  }
}
