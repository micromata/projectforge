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

package org.projectforge.framework.persistence.api;

import java.util.List;

import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserRight;
import org.projectforge.business.user.UserRightValue;

/**
 * Gather user Rights.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface UserRightService
{
  /**
   * FALSE, TRUE;
   */
  public static final UserRightValue[] FALSE_TRUE = new UserRightValue[] { UserRightValue.FALSE, UserRightValue.TRUE };

  /**
   * FALSE, READONLY, READWRITE;
   */
  public static final UserRightValue[] FALSE_READONLY_READWRITE = new UserRightValue[] { UserRightValue.FALSE,
      UserRightValue.READONLY,
      UserRightValue.READWRITE };

  /**
   * FALSE, READONLY, PARTLY_READWRITE, READWRITE;
   */
  public static final UserRightValue[] FALSE_READONLY_PARTLYREADWRITE_READWRITE = new UserRightValue[] {
      UserRightValue.FALSE,
      UserRightValue.READONLY, UserRightValue.PARTLYREADWRITE, UserRightValue.READWRITE };

  /**
   * READONLY, READWRITE;
   */
  public static final UserRightValue[] READONLY_READWRITE = new UserRightValue[] { UserRightValue.READONLY,
      UserRightValue.READWRITE };

  /**
   * READONLY, PARTY_READWRITE, READWRITE;
   */
  public static final UserRightValue[] READONLY_PARTLYREADWRITE_READWRITE = new UserRightValue[] {
      UserRightValue.READONLY, UserRightValue.PARTLYREADWRITE, UserRightValue.READWRITE };

  public static final ProjectForgeGroup[] FIBU_GROUPS = { ProjectForgeGroup.FINANCE_GROUP,
      ProjectForgeGroup.CONTROLLING_GROUP };

  public static final ProjectForgeGroup[] FIBU_ORGA_GROUPS = { ProjectForgeGroup.FINANCE_GROUP,
      ProjectForgeGroup.ORGA_TEAM,
      ProjectForgeGroup.CONTROLLING_GROUP };

  public static final ProjectForgeGroup[] FIBU_ORGA_HR_GROUPS = { ProjectForgeGroup.FINANCE_GROUP,
      ProjectForgeGroup.ORGA_TEAM,
      ProjectForgeGroup.CONTROLLING_GROUP,
      ProjectForgeGroup.HR_GROUP };

  public static final ProjectForgeGroup[] FIBU_ORGA_PM_GROUPS = { ProjectForgeGroup.FINANCE_GROUP,
      ProjectForgeGroup.ORGA_TEAM,
      ProjectForgeGroup.CONTROLLING_GROUP, ProjectForgeGroup.PROJECT_MANAGER, ProjectForgeGroup.PROJECT_ASSISTANT };

  UserRight getRight(String userRightId);

  UserRight getRight(IUserRightId id);

  /**
   * 
   * @param userRightId
   * @return
   * @throws IllegalArgumentException if not found
   */
  IUserRightId getRightId(final String userRightId) throws IllegalArgumentException;

  List<UserRight> getOrderedRights();

  void addRight(final UserRight right);
}
