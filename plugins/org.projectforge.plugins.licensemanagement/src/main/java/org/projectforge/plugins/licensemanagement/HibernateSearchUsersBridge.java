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

package org.projectforge.plugins.licensemanagement;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.user.UsersProvider;

import java.util.Collection;

/**
 * User names bridge for hibernate search to search in the user id's (comma separated values of user id's).
 *
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class HibernateSearchUsersBridge implements TwoWayStringBridge
{
  /**
   * Get all full names and user names of all users represented by the string of user id's.
   */
  @Override
  public String objectToString(Object object) {
    if (object instanceof String)
      return (String)object;
    final LicenseDO license = (LicenseDO) object;
    if (StringUtils.isBlank(license.getOwnerIds())) {
      return "";
    }
    //TODO: Nicht null übergeben
    final UsersProvider usersProvider = new UsersProvider(null);
    final Collection<PFUserDO> users = usersProvider.getSortedUsers(license.getOwnerIds());
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (final PFUserDO user : users) {
      first = StringHelper.append(sb, first, user.getFullname(), " ");
      sb.append(" ").append(user.getUsername());
    }
    return sb.toString();
  }

  @Override
  public Object stringToObject(String stringValue) {
    // Not supported.
    return null;
  }
}
