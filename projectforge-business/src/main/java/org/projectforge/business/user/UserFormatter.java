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

package org.projectforge.business.user;

import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class UserFormatter implements Serializable
{

  /**
   * Does not escape characters.
   *
   * @param user (must not be initialized, the user will be get from the {@link UserGroupCache})
   * @return User's full name.
   * @see PFUserDO#getFullname()
   */
  public String formatUser(final PFUserDO user)
  {
    if (user == null) {
      return "";
    }
    return formatUser(user.getId());
  }

  /**
   * Does not escape characters.
   *
   * @param userId
   * @return User's full name.
   * @see PFUserDO#getFullname()
   */
  public String formatUser(final Long userId)
  {
    final PFUserDO u = UserGroupCache.getInstance().getUser(userId);
    return u != null ? u.getFullname() : "";
  }

  /**
   * Escapes xml characters.
   */
  public String getFormattedUser(final PFUserDO user)
  {
    if (user == null) {
      return "";
    }
    return HtmlHelper.escapeXml(user.getFullname());
  }

  public String getFormattedUser(final Long userId)
  {
    if (userId == null) {
      return "";
    }
    final PFUserDO user = UserGroupCache.getInstance().getUser(userId);
    return getFormattedUser(user);
  }

  public void appendFormattedUser(final StringBuilder buf, final Long userId)
  {
    final PFUserDO user = UserGroupCache.getInstance().getUser(userId);
    appendFormattedUser(buf, user);
  }

  public void appendFormattedUser(final StringBuilder buf, final PFUserDO user)
  {
    buf.append(getFormattedUser(user));
  }
}
