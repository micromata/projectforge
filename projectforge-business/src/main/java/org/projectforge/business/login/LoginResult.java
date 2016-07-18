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

import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Holder for LoginResultStatus and user (if login was successful).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LoginResult
{
  private PFUserDO user;

  private LoginResultStatus loginResultStatus;

  /**
   * @return the user
   */
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   * @return this for chaining.
   */
  public LoginResult setUser(final PFUserDO user)
  {
    this.user = user;
    return this;
  }

  /**
   * @return the loginResultStatus
   */
  public LoginResultStatus getLoginResultStatus()
  {
    return loginResultStatus;
  }

  /**
   * @param loginResultStatus the loginResultStatus to set
   * @return this for chaining.
   */
  public LoginResult setLoginResultStatus(final LoginResultStatus loginResultStatus)
  {
    this.loginResultStatus = loginResultStatus;
    return this;
  }

  /**
   * @param msgParams
   * @return this for chaining.
   * @see LoginResultStatus#setMsgParams(Object...)
   */
  public LoginResult setMsgParams(final Object... msgParams)
  {
    this.loginResultStatus.setMsgParams(msgParams);
    return this;
  }
}
