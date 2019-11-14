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

import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Repository
public class TrainingAttendeeDao extends BaseDao<TrainingAttendeeDO> {
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"trainingAttendee.firstname",
          "trainingAttendee.lastname", "trainingAttendee.username", "training.title",
          "training.skill.title"};

  @Autowired
  private TrainingDao trainingDao;

  @Autowired
  private UserDao userDao;

  public TrainingAttendeeDao() {
    super(TrainingAttendeeDO.class);
    userRightId = SkillmatrixPluginUserRightId.PLUGIN_SKILL_MATRIX_TRAINING_ATTENDEE;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public TrainingAttendeeDO newInstance() {
    return new TrainingAttendeeDO();
  }

  public TrainingDao getTraingDao() {
    return trainingDao;
  }

  public TrainingAttendeeDao setTraingDao(final TrainingDao trainingDao) {
    this.trainingDao = trainingDao;
    return this;
  }

  public UserDao getUserDao() {
    return userDao;
  }

  public TrainingAttendeeDao setUserDao(final UserDao userDao) {
    this.userDao = userDao;
    return this;
  }

  @Override
  public List<TrainingAttendeeDO> getList(final BaseSearchFilter filter) {
    final TrainingAttendeeFilter myFilter;
    if (filter instanceof TrainingAttendeeFilter) {
      myFilter = (TrainingAttendeeFilter) filter;
    } else {
      myFilter = new TrainingAttendeeFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final String searchString = myFilter.getSearchString();

    if (myFilter.getAttendeeId() != null) {
      final PFUserDO attendee = new PFUserDO();
      attendee.setId(myFilter.getAttendeeId());
      queryFilter.add(QueryFilter.eq("trainingAttendee", attendee));
    }
    if (myFilter.getTrainingId() != null) {
      final TrainingDO training = new TrainingDO();
      training.setId(myFilter.getTrainingId());
      queryFilter.add(QueryFilter.eq("training", training));
    }
    queryFilter.addOrder(SortProperty.desc("created"));
    final List<TrainingAttendeeDO> list = getList(queryFilter);
    myFilter.setSearchString(searchString); // Restore search string.
    return list;
  }
}
