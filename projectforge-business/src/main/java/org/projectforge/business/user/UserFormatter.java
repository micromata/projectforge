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

package org.projectforge.business.user;

import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.stereotype.Service;

@Service
public class UserFormatter
{

  /**
   * Does not escape characters.
   * 
   * @param user (must not be initialized, the user will be get from the {@link UserCache})
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
  public String formatUser(final Integer userId)
  {
    final PFUserDO u = getUserGroupCache().getUser(userId);
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

  public String getFormattedUser(final Integer userId)
  {
    if (userId == null) {
      return "";
    }
    final PFUserDO user = getUserGroupCache().getUser(userId);
    return getFormattedUser(user);
  }

  public void appendFormattedUser(final StringBuffer buf, final Integer userId)
  {
    final PFUserDO user = getUserGroupCache().getUser(userId);
    appendFormattedUser(buf, user);
  }

  public void appendFormattedUser(final StringBuffer buf, final PFUserDO user)
  {
    buf.append(getFormattedUser(user));
  }

  public TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  /**
   * @return the UserGroupCache with groups and rights (tenant specific).
   */
  public UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }
}
