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

import java.util.Set;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.projectforge.business.user.service.UserPreferencesHelper;
import org.projectforge.web.wicket.tree.TableTreeExpansion;

/**
 * @author Billy Duong (b.duong@micromata.de)
 *
 */
public class SkillTreeExpansion extends TableTreeExpansion<Integer, SkillNode>
{

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SkillTreeExpansion.class);

  private static final long serialVersionUID = 5151537746424532422L;

  private static SkillTreeExpansion get()
  {
    final SkillTreeExpansion expansion = new SkillTreeExpansion();
    try {
      @SuppressWarnings("unchecked")
      final Set<Integer> ids = (Set<Integer>) UserPreferencesHelper.getEntry(SkillTreePage.USER_PREFS_KEY_OPEN_SKILLS);
      if (ids != null) {
        expansion.setIds(ids);
      } else {
        // Persist the open entries in the data-base.
        UserPreferencesHelper.putEntry(SkillTreePage.USER_PREFS_KEY_OPEN_SKILLS, expansion.getIds(), true);
      }
    } catch (final Exception ex) {
      log.error(ex.getMessage(), ex);
    }
    return expansion;
  }

  /**
   * @return The expansion model. Any previous persisted state of open rows will be restored from
   *         {@link UserPreferencesHelper}.
   */
  @SuppressWarnings("serial")
  public static IModel<Set<SkillNode>> getExpansionModel()
  {
    return new AbstractReadOnlyModel<Set<SkillNode>>()
    {
      /**
       * @see org.apache.wicket.model.AbstractReadOnlyModel#getObject()
       */
      @Override
      public Set<SkillNode> getObject()
      {
        return get();
      }
    };
  }

}
