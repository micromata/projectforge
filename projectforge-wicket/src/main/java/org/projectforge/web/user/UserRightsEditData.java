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

package org.projectforge.web.user;

import org.projectforge.business.user.UserRightVO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserRightsEditData implements Serializable
{
  private static final long serialVersionUID = -7546134770731500588L;

  private PFUserDO user;

  private List<UserRightVO> rights = new ArrayList<UserRightVO>();

  public PFUserDO getUser()
  {
    return user;
  }

  public void setUser(PFUserDO user)
  {
    this.user = user;
  }

  protected UserRightVO addRight(final UserRightVO right)
  {
    rights.add(right);
    return right;
  }

  public List<UserRightVO> getRights()
  {
    return rights;
  }
}
