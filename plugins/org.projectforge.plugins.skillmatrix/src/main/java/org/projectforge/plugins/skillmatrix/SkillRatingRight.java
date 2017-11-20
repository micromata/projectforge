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

package org.projectforge.plugins.skillmatrix;

import org.apache.commons.lang3.ObjectUtils;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Billy Duong (b.duong@micromata.de)
 * 
 */
public class SkillRatingRight extends UserRightAccessCheck<SkillRatingDO>
{
  private static final long serialVersionUID = 197678676075684591L;

  /**
   * @param id
   * @param category
   * @param rightValues
   */
  public SkillRatingRight(AccessChecker accessChecker)
  {
    super(accessChecker, SkillmatrixPluginUserRightId.PLUGIN_SKILL_MATRIX_SKILL_RATING,
        UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final SkillRatingDO obj, final SkillRatingDO oldObj,
      final OperationType operationType)
  {
    final SkillRatingDO skill = (oldObj != null) ? oldObj : obj;

    if (skill == null) {
      return true; // General insert and select access given by default.
    }

    switch (operationType) {
      case SELECT:
      case INSERT:
        // Everyone is allowed to read and create skillratings
        return true;
      case UPDATE:
      case DELETE:
        // Only owner is allowed to edit his skillratings
        return ObjectUtils.equals(user.getId(), skill.getUserId());
      default:
        return false;
    }
  }
}
