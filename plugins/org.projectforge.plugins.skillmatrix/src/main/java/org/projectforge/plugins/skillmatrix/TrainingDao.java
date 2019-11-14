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
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Repository
public class TrainingDao extends BaseDao<TrainingDO> {

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"skill.title"};

  @Autowired
  private SkillDao skillDao;

  @Autowired
  private GroupService groupService;

  public TrainingDao() {
    super(TrainingDO.class);
    userRightId = SkillmatrixPluginUserRightId.PLUGIN_SKILL_MATRIX_TRAINING;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public TrainingDO newInstance() {
    return new TrainingDO();
  }

  /**
   * @param skillId If null, then skill will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public TrainingDO setSkill(final TrainingDO training, final Integer skillId) {
    final SkillDO skill = skillDao.getOrLoad(skillId);
    training.setSkill(skill);
    return training;
  }

  public TrainingDO getTraining(final String title) {
    if (title == null) {
      return null;
    }
    return SQLHelper.ensureUniqueResult(em
            .createNamedQuery(TrainingDO.FIND_BY_TITLE, TrainingDO.class)
            .setParameter("title", title));
  }

  /**
   * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
   *
   * @param training
   * @param fullAccessGroups
   */
  public void setFullAccessGroups(final TrainingDO training, final Collection<GroupDO> fullAccessGroups) {
    training.setFullAccessGroupIds(groupService.getGroupIds(fullAccessGroups));
  }

  public Collection<GroupDO> getSortedFullAccessGroups(final TrainingDO training) {
    return groupService.getSortedGroups(training.getFullAccessGroupIds());
  }

  /**
   * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
   *
   * @param training
   * @param readonlyAccessGroups
   */
  public void setReadOnlyAccessGroups(final TrainingDO training, final Collection<GroupDO> readonlyAccessGroups) {
    training.setReadOnlyAccessGroupIds(groupService.getGroupIds(readonlyAccessGroups));
  }

  public Collection<GroupDO> getSortedReadOnlyAccessGroups(final TrainingDO training) {
    return groupService.getSortedGroups(training.getReadOnlyAccessGroupIds());
  }

  @Override
  public List<TrainingDO> getList(final BaseSearchFilter filter) {
    final TrainingFilter myFilter;
    if (filter instanceof TrainingFilter) {
      myFilter = (TrainingFilter) filter;
    } else {
      myFilter = new TrainingFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final String searchString = myFilter.getSearchString();

    if (myFilter.getSkillId() != null) {
      final SkillDO skill = new SkillDO();
      skill.setId(myFilter.getSkillId());
      queryFilter.add(QueryFilter.eq("skill", skill));
    }
    if (myFilter.getTrainingId() != null) {
      queryFilter.add(QueryFilter.eq("id", myFilter.getTrainingId()));
    }
    queryFilter.addOrder(SortProperty.desc("created"));
    final List<TrainingDO> list = getList(queryFilter);
    myFilter.setSearchString(searchString); // Restore search string.
    return list;
  }
}
