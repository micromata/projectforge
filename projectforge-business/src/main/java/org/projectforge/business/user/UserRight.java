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

import org.projectforge.framework.DisplayNameCapable;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class UserRight implements Serializable, DisplayNameCapable {
  private static final long serialVersionUID = -4356329396089081134L;

  private final IUserRightId id;

  protected UserRightValue[] values;

  protected UserRightCategory category = UserRightCategory.MISC;

  protected UserRight dependsOn;

  public UserRight(final IUserRightId id, final UserRightCategory category) {
    this.id = id;
    this.category = category;
  }

  public UserRight(final IUserRightId id, final UserRightCategory category, final UserRightValue... values) {
    this(id, category);
    this.values = values;
  }

  public UserRight(final IUserRightId id, final UserRightCategory category, final UserRight dependsOn) {
    this(id, category, dependsOn, new UserRightValue[]{UserRightValue.FALSE, UserRightValue.TRUE});
  }

  /**
   * @param id
   * @param dependsOn
   * @param values    FALSE, TRUE at default.
   */
  public UserRight(final IUserRightId id, final UserRightCategory category, final UserRight dependsOn,
                   final UserRightValue... values) {
    this(id, category);
    this.dependsOn = dependsOn;
    this.values = values;
  }

  public IUserRightId getId() {
    return id;
  }

  /**
   * Available values {TRUE, FALSE} at default.
   *
   * @return
   */
  public UserRightValue[] getValues() {
    return values;
  }

  /**
   * For some users and right combinations it's possible, that the user has an access value at default because he's
   * member of a group with a single right. <br/>
   *
   * @param user
   * @return true if all available values for the user matches automatically, otherwise false (this right seems to be
   * configurable for the given user in the UserEditForm).
   */
  public boolean isConfigurable(final PFUserDO user, final Collection<GroupDO> userGroups) {
    if (values == null) {
      return false;
    }
    final UserRightValue[] availableValues = getAvailableValues(user, userGroups);
    if (availableValues == null || availableValues.length <= 1) {
      return false;
    }
    for (final UserRightValue value : availableValues) {
      if (value == UserRightValue.FALSE) {
        continue; // Should not be configurable.
      }
      if (!matches(user, userGroups, value)) {
        final UserRightValue[] includedByValues = value.includedBy();
        if (includedByValues != null) {
          for (final UserRightValue includedByValue : includedByValues) {
            if (matches(user, userGroups, includedByValue)) {
              // This available value is automatically set for the user, so the right seems to be not configurable:
              break;
            }
          }
        } else {
          // This available value is not automatically set for the user, so the right seems to be configurable:
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Get all right values which are potentially available for the user. If the user value stored in the user data is not
   * part of available values then the AccessChecker returns that the user has no right.
   *
   * @param user
   * @return
   */
  public UserRightValue[] getAvailableValues(final PFUserDO user, final Collection<GroupDO> userGroups) {
    if (values == null) {
      return null;
    }
    final List<UserRightValue> list = new ArrayList<>();
    for (final UserRightValue value : values) {
      if (isAvailable(user, userGroups, value)) {
        list.add(value);
      }
    }
    final UserRightValue[] oa = new UserRightValue[list.size()];
    list.toArray(oa);
    return oa;
  }

  /**
   * Is this right for the given user potentially available (independent from the configured value)?
   *
   * @param assignedGroups
   * @return true at default or if dependsOn is given dependsOn.available(AccessChecker)
   */
  public boolean isAvailable(final PFUserDO user, final Collection<GroupDO> assignedGroups) {
    if (this.dependsOn == null) {
      return true;
    } else {
      return this.dependsOn.isAvailable(user, assignedGroups);
    }
  }

  /**
   * @param assignedGroups
   * @return isAvailable(user, userGroups) at default or if dependsOn is given dependsOn.available(userGroupCache,
   * user, value);
   */
  public boolean isAvailable(final PFUserDO user, final Collection<GroupDO> assignedGroups, final UserRightValue value) {
    if (this.dependsOn == null) {
      return isAvailable(user, assignedGroups);
    } else {
      return this.dependsOn.isAvailable(user, assignedGroups, value);
    }
  }

  /**
   * If a right should match independent on the value set for the given user. Should only be called, if the UserRightDO
   * or its value is set to null.
   *
   * @param user
   * @param value
   * @return false.
   */
  public boolean matches(final PFUserDO user, final Collection<GroupDO> userGroups, final UserRightValue value) {
    return false;
  }

  public boolean isBooleanType() {
    return values != null && values.length == 2 && values[0] == UserRightValue.FALSE
        && values[1] == UserRightValue.TRUE;
  }

  public UserRight getDependsOn() {
    return dependsOn;
  }

  public boolean isDependsOn() {
    return dependsOn != null;
  }

  public UserRightCategory getCategory() {
    return category;
  }

  public UserRight setCategory(final UserRightCategory category) {
    this.category = category;
    return this;
  }

  /**
   * @see org.projectforge.framework.DisplayNameCapable#getDisplayName()
   */
  @Override
  public String getDisplayName() {
    return this.id.toString();
  }

  @Override
  public String toString() {
    return this.id.toString();
  }
}
