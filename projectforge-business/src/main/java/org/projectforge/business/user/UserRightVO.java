/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.user;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Only used e. g. for editing the user rights (especially for the case if the user has no UserRightDO entry in the data base but this right
 * is available for him).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UserRightVO implements Serializable
{
  private static final long serialVersionUID = -7192649819847342963L;

  private UserRightValue value;

  private UserRight right;

  public UserRightVO(final UserRight right)
  {
    this.right = right;
  }

  public boolean isBooleanValue()
  {
    return value == UserRightValue.TRUE;
  }

  public UserRightVO setBooleanValue(boolean booleanValue)
  {
    if (booleanValue == true) {
      this.value = UserRightValue.TRUE;
    } else {
      this.value = UserRightValue.FALSE;
    }
    return this;
  }

  public UserRightValue getValue()
  {
    return value;
  }

  public UserRightVO setValue(UserRightValue value)
  {
    this.value = value;
    return this;
  }

  public UserRight getRight()
  {
    return right;
  }
  
  @Override
  public String toString()
  {
    return new ToStringBuilder(this).append("right", right).append("value", value).toString();
  }
}
