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

import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Billy Duong (b.duong@micromata.de)
 *
 */
public class SkillFilter extends BaseSearchFilter
{

  private static final long serialVersionUID = -8140231682294892497L;

  /**
   * Used by match filter for avoiding multiple traversing of the tree. Should be empty before building a skill node list!
   */
  private transient HashMap<Integer, Boolean> skillVisibility;

  /**
   * Used by match filter for storing those skills which matches the search string. Should be empty before building a skill node list!
   */
  private transient HashSet<Integer> skillsMatched;

  public SkillFilter() {
  }

  public SkillFilter(final SkillFilter baseSearchFilter)
  {
    super(baseSearchFilter);
  }

  public void resetMatch()
  {
    skillVisibility = new HashMap<Integer, Boolean>();
    skillsMatched = new HashSet<Integer>();
  }

  /**
   * Needed by SkillTreeTable to show and hide nodes.<br/>
   * Don't forget to call resetMatch before!
   * @param node Node to check.
   * @param skillDao Needed for access checking.
   * @param user Needed for access checking.
   * @see org.projectforge.web.tree.TreeTableFilter#match(org.projectforge.web.tree.TreeTableNode)
   */
  public boolean match(final SkillNode node, final SkillDao skillDao, final PFUserDO user)
  {
    Validate.notNull(node);
    Validate.notNull(node.getSkill());
    if (skillVisibility == null) {
      resetMatch();
    }
    final SkillDO skill = node.getSkill();
    if (StringUtils.isBlank(this.searchString) == true) {
      return isVisibleByDelete(skill);
    } else {
      if (isVisibleBySearchString(node, skill, skillDao, user) == true) {
        return isVisibleByDelete(skill);
      } else {
        if (isAncestorVisible(node)) {
          return isVisibleByDelete(skill);
        } else {
          return false;
        }
      }
    }
  }

  private boolean isAncestorVisible(final SkillNode node) {
    return (node.getParent() != null && node.getParent().isRootNode() == false && isAncestorVisibleBySearchString(node.getParent()) == true);
  }

  private boolean isAncestorVisibleBySearchString(final SkillNode node)
  {
    if (skillsMatched.contains(node.getId()) == true) {
      return true;
    } else if (node.getParent() != null) {
      return isAncestorVisibleBySearchString(node.getParent());
    }
    return false;
  }

  /**
   * @param node
   * @param skill
   * @return true if the search string matches at least one field of the skill of if this methods returns true for any descendant.
   */
  private boolean isVisibleBySearchString(final SkillNode node, final SkillDO skill, final SkillDao skillDao, final PFUserDO user)
  {
    final Boolean cachedVisibility = skillVisibility.get(skill.getId());
    if (cachedVisibility != null) {
      return cachedVisibility;
    }
    if (StringUtils.containsIgnoreCase(skill.getTitle(), this.searchString) == true
        || StringUtils.containsIgnoreCase(skill.getDescription(), this.searchString) == true
        || StringUtils.containsIgnoreCase(skill.getComment(), this.searchString) == true ) {
      skillVisibility.put(skill.getId(), true);
      skillsMatched.add(skill.getId());
      return true;
    } else if (node.hasChilds() == true && node.isRootNode() == false) {
      for (final SkillNode childNode : node.getChilds()) {
        final SkillDO childSkill = childNode.getSkill();
        if (isVisibleBySearchString(childNode, childSkill, skillDao, user) == true) {
          skillVisibility.put(childSkill.getId(), true);
          return true;
        }
      }
    }
    skillVisibility.put(skill.getId(), false);
    return false;
  }

  /**
   * 
   * @param skill
   * @return true if skill is not deleted or deleted skills are visible
   */
  private boolean isVisibleByDelete(final SkillDO skill) {
    if(isDeleted() == false && skill.isDeleted() == true) {
      return false;
    }
    return true;
  }

}
