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

package org.projectforge.plugins.skillmatrix;

import org.projectforge.web.tree.TreeTableNode;

/**
 * Represents a single node as part of a TreeTable.
 * 
 * @author Billy Duong (b.duong@micromata.de)
 *
 */
public class SkillTreeTableNode extends TreeTableNode
{
  private static final long serialVersionUID = 6436021236494684413L;

  private final SkillNode skillNode;

  public SkillTreeTableNode(final SkillTreeTableNode parent, final SkillNode skillNode)
  {
    super(parent, parent.getHashId());
    this.skillNode = skillNode;
  }

  /**
   * @return the skillNode
   */
  public SkillNode getSkillNode()
  {
    return skillNode;
  }

  public SkillDO getSkill()
  {
    return skillNode.getSkill();
  }

  public String getSkillTitle()
  {
    return getSkill().getTitle();
  }

  public Integer getId()
  {
    return getSkill().getId();
  }

  /**
   * Return a String representation of this object.
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder("SkillTreeTableNode[skillName=");
    sb.append(getSkillTitle());
    sb.append(",id=");
    sb.append(getId());
    sb.append("]");
    return (sb.toString());
  }

  /** Should be overwrite by derived classes. */
  @Override
  public int compareTo(final TreeTableNode obj)
  {
    final SkillTreeTableNode node = (SkillTreeTableNode) obj;
    return skillNode.getSkill().getTitle().compareTo(node.getSkill().getTitle());
  }

}
