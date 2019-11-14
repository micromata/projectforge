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

import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.task.TaskDao;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * DAO for SkillDO. Handles constraint validation and database access.
 *
 * @author Billy Duong (b.duong@micromata.de)
 */
@Repository
public class SkillDao extends BaseDao<SkillDO> {
  public static final String I18N_KEY_ERROR_CYCLIC_REFERENCE = "plugins.skillmatrix.error.cyclicReference";

  public static final String I18N_KEY_ERROR_DUPLICATE_CHILD_SKILL = "plugins.skillmatrix.error.duplicateChildSkill";

  public static final String I18N_KEY_ERROR_PARENT_SKILL_NOT_FOUND = "plugins.skillmatrix.error.parentNotFound";

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"parent.title"};

  @Autowired
  SkillTree skillTree;

  @Autowired
  GroupService groupService;

  // private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SkillDao.class);

  public SkillDao() {
    super(SkillDO.class);
    userRightId = SkillmatrixPluginUserRightId.PLUGIN_SKILL_MATRIX_SKILL;
  }

  @Override
  public SkillDO newInstance() {
    return new SkillDO();
  }

  @Override
  protected void onSaveOrModify(final SkillDO obj) {
    synchronized (this) {
      checkConstraintViolation(obj);
    }
    if (obj.getParent() == null && !skillTree.isRootNode(obj)) {
      obj.setParent(skillTree.getRootSkillNode().getSkill());
    }
  }

  /**
   * Sets the tree as expired to force a refresh (rebuild of tree).
   */
  @Override
  protected void afterSaveOrModify(final SkillDO obj) {
    skillTree.setExpired();
  }

  /**
   * @param skill that needs to be validated.
   * @throws UserException is thrown when the user wants to create a duplicate.
   */
  public void checkConstraintViolation(final SkillDO skill) throws UserException {
    // TODO: Check for valid Tree structure (root) -> example TaskDao.checkConstraintVilation
    SkillDO other = null;
    if (skill.getParentId() != null) {
      if (skill.getId() != null) {
        other = SQLHelper.ensureUniqueResult(em
                .createNamedQuery(SkillDO.FIND_OTHER_BY_TITLE_AND_PARENT, SkillDO.class)
                .setParameter("title", skill.getTitle())
                .setParameter("parentId", skill.getParentId())
                .setParameter("id", skill.getId()));
      } else {
        other = SQLHelper.ensureUniqueResult(em
                .createNamedQuery(SkillDO.FIND_BY_TITLE_AND_PARENT, SkillDO.class)
                .setParameter("title", skill.getTitle())
                .setParameter("parentId", skill.getParentId()));
      }
    } else {
      if (skill.getId() != null) {
        other = SQLHelper.ensureUniqueResult(em
                .createNamedQuery(SkillDO.FIND_OTHER_BY_TITLE_ON_TOPLEVEL, SkillDO.class)
                .setParameter("title", skill.getTitle())
                .setParameter("id", skill.getId()));
      } else {
        other = SQLHelper.ensureUniqueResult(em
                .createNamedQuery(SkillDO.FIND_BY_TITLE_ON_TOPLEVEL, SkillDO.class)
                .setParameter("title", skill.getTitle()));
      }
    }
    if (other != null) {
      throw new UserException(I18N_KEY_ERROR_DUPLICATE_CHILD_SKILL);
    }
  }

  private void checkCyclicReference(final SkillDO obj) {
    if (obj.getId().equals(obj.getParentId())) {
      // Self reference
      throw new UserException(I18N_KEY_ERROR_CYCLIC_REFERENCE);
    }
    final SkillNode parent = skillTree.getSkillNodeById(obj.getParentId());
    if (parent == null && !skillTree.isRootNode(obj)) {
      // Task is orphan because it has no parent task.
      throw new UserException(I18N_KEY_ERROR_PARENT_SKILL_NOT_FOUND);
    }
    final SkillNode node = skillTree.getSkillNodeById(obj.getId());
    if (node.isParentOf(parent)) {
      // Cyclic reference because task is ancestor of itself.
      throw new UserException(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE);
    }
  }

  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final SkillDO obj, final SkillDO dbObj,
                                 final boolean throwException) {
    checkCyclicReference(obj);
    return super.hasUpdateAccess(user, obj, dbObj, throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#getAdditionalSearchFields()
   */
  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * @param skill
   * @param parentId If null, then skill will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public SkillDO setParentSkill(final SkillDO skill, final Integer parentId) {
    final SkillDO parentSkill = getOrLoad(parentId);
    skill.setParent(parentSkill);
    return skill;
  }

  /**
   * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
   *
   * @param skill
   * @param fullAccessGroups
   */
  public void setFullAccessGroups(final SkillDO skill, final Collection<GroupDO> fullAccessGroups) {
    skill.setFullAccessGroupIds(groupService.getGroupIds(fullAccessGroups));
  }

  public Collection<GroupDO> getSortedFullAccessGroups(final SkillDO skill) {
    return groupService.getSortedGroups(skill.getFullAccessGroupIds());
  }

  /**
   * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
   *
   * @param skill
   * @param readonlyAccessGroups
   */
  public void setReadonlyAccessGroups(final SkillDO skill, final Collection<GroupDO> readonlyAccessGroups) {
    skill.setReadOnlyAccessGroupIds(groupService.getGroupIds(readonlyAccessGroups));
  }

  public Collection<GroupDO> getSortedReadonlyAccessGroups(final SkillDO skill) {
    return groupService.getSortedGroups(skill.getReadOnlyAccessGroupIds());
  }

  /**
   * Please note: Only the string group.trainingAccessGroupIds will be modified (but not be saved)!
   *
   * @param skill
   * @param trainingAccessGroups
   */
  public void setTrainingAccessGroups(final SkillDO skill, final Collection<GroupDO> trainingAccessGroups) {
    skill.setTrainingGroupsIds(groupService.getGroupIds(trainingAccessGroups));
  }

  public Collection<GroupDO> getSortedTrainingAccessGroups(final SkillDO skill) {
    return groupService.getSortedGroups(skill.getTrainingGroupsIds());
  }

  /**
   * Has the current logged in user select access to the given skill?
   *
   * @param node
   * @return
   */
  public boolean hasSelectAccess(final SkillNode node) {
    return hasLoggedInUserSelectAccess(node.getSkill(), false);
  }

  /**
   * TODO: Skills with same title are possible but result in an exception here.
   *
   * @param title
   * @return
   */
  public SkillDO getSkill(final String title) {
    if (title == null) {
      return null;
    }
    return SQLHelper.ensureUniqueResult(em
            .createNamedQuery(SkillDO.FIND_BY_TITLE, SkillDO.class)
            .setParameter("title", title));
  }

  public SkillTree getSkillTree() {
    return skillTree;
  }
}
