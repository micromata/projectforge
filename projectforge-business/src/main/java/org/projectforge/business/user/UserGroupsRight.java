/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections4.CollectionUtils;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Right depending on the member-ship of at least one group.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class UserGroupsRight extends UserRight implements Serializable
{
  private static final long serialVersionUID = 5686379506538006417L;

  protected ProjectForgeGroup[] dependsOnGroups;

  /**
   * Optional available values for single groups for reducing the rights. E. g. some groups have only READ_ONLY access.
   */
  protected Map<ProjectForgeGroup, UserRightValue[]> availableGroupRightValues;

  /**
   * @param id
   * @param dependsOnGroups At least one of these groups is required for enabling this right (visibility)
   */
  public UserGroupsRight(final UserRightId id, final UserRightCategory category, final UserRightValue[] values,
      final ProjectForgeGroup... dependsOnGroups)
  {
    super(id, category);
    this.dependsOnGroups = dependsOnGroups;
    this.values = values;
  }

  /**
   * @return false if the logged in user is not a member of the dependent groups, otherwise super.isAvailable(...).
   */
  @Override
  public boolean isAvailable(final PFUserDO user, final Collection<GroupDO> assignedGroups)
  {
    if (CollectionUtils.isNotEmpty(assignedGroups)) {
      if (UserGroupCache.isUserMemberOfGroup(assignedGroups, dependsOnGroups)) {
        return super.isAvailable(user, assignedGroups);
      }
    }
    return false;
  }

  /**
   * Checks first {@link #isAvailable(PFUserDO, Collection)}. Checks then if the right value is available for one of
   * the user groups. If no right value found for all of the user's groups then return false.
   */
  @Override
  public boolean isAvailable(final PFUserDO user, final Collection<GroupDO> assignedGroups, final UserRightValue value)
  {
    if (availableGroupRightValues == null) {
      return isAvailable(user, assignedGroups);
    }
    if (!isAvailable(user, assignedGroups)) {
      return false;
    }
    for (final ProjectForgeGroup group : dependsOnGroups) {
      final UserRightValue[] vals = availableGroupRightValues.get(group);
      if (vals != null) {
        for (final UserRightValue val : vals) {
          if (val == value) {
            if (UserGroupCache.isUserMemberOfGroup(assignedGroups, group)) {
              return true;
            }
          }
        }
      } else {
        // All right values are available for this group:
        if (UserGroupCache.isUserMemberOfGroup(assignedGroups, group)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * If the user is a member of one group for which only one value is available and this single value matches the given
   * value then true is returned. This is use-full if the user is member of a group for which all members should have
   * access automatically independent on the user's setting.
   */
  @Override
  public boolean matches(final PFUserDO user, final Collection<GroupDO> userGroups, final UserRightValue value)
  {
    if (availableGroupRightValues == null) {
      return false;
    }
    for (final ProjectForgeGroup group : dependsOnGroups) {
      if (!UserGroupCache.isUserMemberOfGroup(userGroups, group)) {
        continue;
      }
      final UserRightValue[] vals = availableGroupRightValues.get(group);
      if (vals != null && vals.length == 1 && vals[0] == value) {
        return true;
      }
    }
    for (Entry<ProjectForgeGroup, UserRightValue[]> entry : this.availableGroupRightValues.entrySet()) {
      // Check all group right values.
      final ProjectForgeGroup group = entry.getKey();
      if (!UserGroupCache.isUserMemberOfGroup(userGroups, group)) {
        // User is not member of this group, skip this group.
        continue;
      }
      final UserRightValue[] vals = availableGroupRightValues.get(group);
      if (vals != null && vals.length == 1 && vals[0] == value) {
        // For this group only one value is set so this value is automatically available for the user.
        return true;
      }
    }
    return false;
  }

  /**
   * Convenience method for allowing only READONLY right value for the ProjectForgeGroup.CONTROLLING_GROUP.
   *
   * @return
   */
  public UserGroupsRight setReadOnlyForControlling()
  {
    setAvailableGroupRightValues(ProjectForgeGroup.CONTROLLING_GROUP, UserRightValue.READONLY);
    return this;
  }

  /**
   * Use this for reducing the availability of right values for groups. If not set for one group then all right values
   * are available.
   *
   * @param group The group for setting the available right values.
   * @param values The available right values.
   * @return
   */
  public UserGroupsRight setAvailableGroupRightValues(final ProjectForgeGroup group, final UserRightValue... values)
  {
    synchronized (this) {
      if (availableGroupRightValues == null) {
        availableGroupRightValues = new HashMap<>();
      }
    }
    availableGroupRightValues.put(group, values);
    return this;
  }
}
