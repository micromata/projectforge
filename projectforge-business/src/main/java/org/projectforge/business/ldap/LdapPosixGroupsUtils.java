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

package org.projectforge.business.ldap;

import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class LdapPosixGroupsUtils
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LdapPosixGroupsUtils.class);

  @Autowired
  GroupDOConverter groupDOConverter;

  @Autowired
  private UserGroupCache userGroupCache;

  /**
   * Get all given gid numbers of all ProjectForge groups including any deleted group and get the next highest and free
   * number. The number is 1000 if no gid number (with a value greater than 999) is found.
   */
  public int getNextFreeGidNumber()
  {
    final Collection<GroupDO> allGroups = userGroupCache.getAllGroups();
    int currentMaxNumber = 999;
    for (final GroupDO group : allGroups) {
      final LdapGroupValues ldapGroupValues = groupDOConverter.readLdapGroupValues(group.getLdapValues());
      if (ldapGroupValues == null) {
        continue;
      }
      if (ldapGroupValues.getGidNumber() != null && ldapGroupValues.getGidNumber() > currentMaxNumber) {
        currentMaxNumber = ldapGroupValues.getGidNumber();
      }
    }
    return currentMaxNumber + 1;
  }

  /**
   * For preventing double gidNumbers.
   *
   * @param currentGroup
   * @param gidNumber
   * @return Returns true if any group (also deleted group) other than the given group has the given gidNumber,
   *         otherwise false.
   */
  public boolean isGivenNumberFree(final GroupDO currentGroup, final int gidNumber)
  {
    return isGivenNumberFree(currentGroup.getId(), gidNumber);
  }

  /**
   * For preventing double gidNumbers.
   *
   * @param currentGroupId
   * @param gidNumber
   * @return Returns true if any group (also deleted group) other than the given group has the given gidNumber,
   *         otherwise false.
   */
  public boolean isGivenNumberFree(final long currentGroupId, final int gidNumber)
  {
    final Collection<GroupDO> allGroups = userGroupCache.getAllGroups();
    for (final GroupDO group : allGroups) {
      final LdapGroupValues ldapGroupValues = groupDOConverter.readLdapGroupValues(group.getLdapValues());
      if (Objects.equals(group.getId(), currentGroupId)) {
        // The current group may have the given gidNumber already, so ignore this entry.
        continue;
      }
      if (ldapGroupValues != null && ldapGroupValues.getGidNumber() != null
          && ldapGroupValues.getGidNumber() == gidNumber) {
        // Number isn't free.
        log.info("The gidNumber (posix account) '" + gidNumber + "' is already occupied by group: " + group);
        return false;
      }
    }
    return true;
  }

  /**
   * Sets next free gid.
   *
   * @param ldapGroupValues
   */
  public void setDefaultValues(final LdapGroupValues ldapGroupValues)
  {
    ldapGroupValues.setGidNumber(getNextFreeGidNumber());
  }
}
