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

package org.projectforge.framework.persistence.user.api;

import org.apache.commons.lang3.Validate;
import org.projectforge.business.task.LegacyTaskFavorite;
import org.projectforge.business.timesheet.TimesheetDO;

import java.io.Serializable;

/**
 * User preferences are supported by different areas. These areas are defined inside this enum.
 *
 * Only the areas whose legacy entries (UserPrefEntryDO) are still read are left: TASK_FAVORITE (task favorites,
 * see {@link org.projectforge.business.task.TaskFavoritesService}) and TIMESHEET_TEMPLATE (migrated on demand into
 * the new timesheet favorites, see {@link org.projectforge.business.timesheet.TimesheetFavoritesService}). The
 * Wicket-only areas KUNDE_FAVORITE, PROJEKT_FAVORITE and USER_FAVORITE were dropped with the Wicket user preference
 * pages; their database rows are left untouched but no longer read.
 *
 * Will be replaced by {@link org.projectforge.favorites.AbstractFavorite}.
 * See {@link org.projectforge.business.task.TaskFavorite} as an example.
 *
 * @author Kai Reinhard
 */
@Deprecated
public class UserPrefArea implements Serializable, Comparable<UserPrefArea>
{
  private static final long serialVersionUID = -6594785391128587090L;

  public static final int MAX_ID_LENGTH = 255;

  public static final UserPrefArea TASK_FAVORITE = new UserPrefArea("TASK_FAVORITE", LegacyTaskFavorite.class);

  public static final UserPrefArea TIMESHEET_TEMPLATE = new UserPrefArea("TIMESHEET_TEMPLATE", TimesheetDO.class);

  private final String id;

  private final Class<?> beanType;

  /**
   * The id is used as identity in the data-base.
   */
  public String getId()
  {
    return id;
  }

  /**
   * The type corresponding to this UserPrefArea. This is the bean for which the annotated fields are stored as
   * UserPrefParameterDO's.
   *
   * @return
   */
  public Class<?> getBeanType()
  {
    return beanType;
  }

  /**
   * @param id Used as identity in the data-base (max-length = 20). Please don't change this id later, otherwise
   *          (de)-serialization will fail (could not read data-base entries).
   * @param clazz The class which contains the user pref parameters.
   */
  public UserPrefArea(final String id, final Class<?> clazz)
  {
    Validate.isTrue(id.length() <= MAX_ID_LENGTH);
    this.id = id;
    this.beanType = clazz;
  }

  public boolean isIn(final UserPrefArea... userPrefAreas)
  {
    for (final UserPrefArea area : userPrefAreas) {
      if (this == area) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString()
  {
    return String.valueOf(id);
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final UserPrefArea arg0)
  {
    return id.compareTo(arg0.id);
  }
}
