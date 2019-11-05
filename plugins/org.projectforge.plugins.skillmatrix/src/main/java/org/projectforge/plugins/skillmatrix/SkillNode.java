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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.IdObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single skill as part of the SkillTree. The data of a skill node is stored in the database.
 * @author Billy Duong (b.duong@micromata.de)
 */
public class SkillNode implements IdObject<Integer>, Serializable
{
  private static final Logger log = LoggerFactory.getLogger(SkillNode.class);

  private static final long serialVersionUID = 8016231590126097371L;

  /** Reference to the parent skill node with the parentSkillId. */
  SkillNode parent = null;

  /**
   * References to all child nodes.
   */
  List<SkillNode> children = null;

  /** The data of this SkillNode. */
  SkillDO skill = null;

  public SkillNode()
  {
  }

  /**
   * @return True, if the parent skill id of the underlying skill is null, false otherwise.
   */
  public boolean isRootNode()
  {
    return this.skill.getParentId() == null;
  }
  public void setSkill(final SkillDO skill)
  {
    this.skill = skill;
  }

  public SkillDO getSkill()
  {
    return skill;
  }

  /** The id of this skill given by the database. */
  @Override
  public Integer getId()
  {
    return skill.getId();
  }

  public Integer getParentId()
  {
    if (parent == null) {
      return null;
    }
    return parent.getId();
  }

  /** Returns the parent skill. */
  public SkillNode getParent()
  {
    return this.parent;
  }

  public void internalSetParent(final SkillNode parent)
  {
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    }
  }


  /**
   * Returns all children of this skill.
   */
  public List<SkillNode> getChildren()
  {
    return this.children;
  }

  /** Does this skill has any children? */
  public boolean hasChildren()
  {
    return this.children != null && this.children.size() > 0 ? true : false;
  }

  /** Checks if the given node is a child / descendant of this node. */
  public boolean isParentOf(final SkillNode node)
  {
    if (this.children == null) {
      return false;
    }
    for (final SkillNode child : this.children) {
      if (child.equals(node)) {
        return true;
      } else if (child.isParentOf(node)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the path to the root node in an ArrayList. The list contains also the current skill.
   */
  public List<SkillNode> getPathToRoot()
  {
    return getPathToAncestor(null);
  }

  /**
   * Returns the path to the parent node in an ArrayList.
   */
  public List<SkillNode> getPathToAncestor(final Integer ancestorSkillId)
  {
    if (this.parent == null || this.getId().equals(ancestorSkillId)) {
      return new ArrayList<>();
    }
    final List<SkillNode> path = this.parent.getPathToAncestor(ancestorSkillId);
    path.add(this);
    return path;
  }

  /**
   * Sets / changes the parent of this node. This method does not modify the parent skill! So it should be called only by SkillTree.
   */
  void setParent(final SkillNode parent)
  {
    if (parent != null) {
      if (parent.getId().equals(getId()) || this.isParentOf(parent)) {
        log.error("Oups, cyclic reference detection: skillId = " + getId() + ", parentSkillId = " + parent.getId());
        throw new UserException(SkillDao.I18N_KEY_ERROR_CYCLIC_REFERENCE);
      }
      this.parent = parent;
      this.skill.setParent(parent.getSkill());
    }
  }

  /**
   * Adds a new skill as a child of this node. It does not check whether this skill already exist as child or not! This method does not modify
   * the child skill!
   */
  void addChild(final SkillNode child)
  {
    if (child != null) {
      if (child.getId().equals(getId()) || child.isParentOf(this)) {
        log.error("Oups, cyclic reference detection: skillId = " + getId() + ", parentSkillId = " + parent.getId());
        return;
      }
      if (this.children == null) {
        this.children = new ArrayList<>();
      }
      this.children.add(child);
    }
  }

  /**
   * Removes a child skill of this node. This method does not modify the child skill!
   */
  void removeChild(final SkillNode child)
  {
    if (child == null) {
      log.error("Oups, child is null, can't remove it from parent.");
    } else if (this.children == null) {
      log.error("Oups, this node has no children to remove.");
    } else if (!this.children.contains(child)) {
      log.error("Oups, this node doesn't contain given child.");
    } else {
      log.debug("Removing child " + child.getId() + " from parent " + this.getId());
      this.children.remove(child);
    }
  }

  @Override
  public String toString()
  {
    final ToStringBuilder sb = new ToStringBuilder(this);
    sb.append("id", getId());
    Object parentId = null;
    if (this.parent != null) {
      parentId = this.parent.getId();
    }
    log.debug("id: " + this.getId() + ", parentId: " + parentId);
    sb.append("parent", parentId);
    sb.append("title", skill.getTitle());
    sb.append("children", this.children);
    return sb.toString();
  }

  public List<Integer> getAncestorIds()
  {
    final List<Integer> ancestors = new ArrayList<>();
    getAncestorIds(ancestors);
    return ancestors;
  }

  private void getAncestorIds(final List<Integer> ancestors)
  {
    if (this.parent != null) {
      if (!ancestors.contains(this.parent.getId())) {
        // Paranoia setting for cyclic references.
        ancestors.add(this.parent.getId());
        this.parent.getAncestorIds(ancestors);
      }
    }
  }

}
