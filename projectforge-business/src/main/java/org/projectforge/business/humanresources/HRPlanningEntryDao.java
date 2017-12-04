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

package org.projectforge.business.humanresources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Repository
public class HRPlanningEntryDao extends BaseDao<HRPlanningEntryDO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.PM_HR_PLANNING;

  private static final Logger log = LoggerFactory.getLogger(HRPlanningEntryDao.class);

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private HRPlanningDao hrPlanningDao;

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return new String[] { "projekt.name", "projekt.kunde.name", "planning.user.username", "planning.user.firstname",
        "planning.user.lastname" };
  }

  protected HRPlanningEntryDao()
  {
    super(HRPlanningEntryDO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * @param sheet
   * @param projektId If null, then projekt will be set to null;
   */
  public void setProjekt(final HRPlanningEntryDO sheet, final Integer projektId)
  {
    final ProjektDO projekt = projektDao.getOrLoad(projektId);
    sheet.setProjekt(projekt);
  }

  @Override
  public List<HRPlanningEntryDO> getList(final BaseSearchFilter filter)
  {
    final HRPlanningFilter myFilter = (HRPlanningFilter) filter;
    if (myFilter.getStopTime() != null) {
      final DateHolder date = new DateHolder(myFilter.getStopTime());
      date.setEndOfDay();
      myFilter.setStopTime(date.getDate());
    }
    final QueryFilter queryFilter = buildQueryFilter(myFilter);
    myFilter.setIgnoreDeleted(true); // Ignore deleted flag of HRPlanningEntryDOs, use instead:
    if (myFilter.isDeleted() == true) {
      queryFilter.add(Restrictions.or(Restrictions.eq("deleted", true), Restrictions.eq("p.deleted", true)));
    } else {
      queryFilter.add(Restrictions.and(Restrictions.eq("deleted", false), Restrictions.eq("p.deleted", false)));
    }
    final List<HRPlanningEntryDO> list = getList(queryFilter);
    if (list == null) {
      return null;
    }
    for (final HRPlanningEntryDO entry : list) {
      @SuppressWarnings("unchecked")
      final List<HRPlanningEntryDO> entries = (List<HRPlanningEntryDO>) CollectionUtils.select(
          entry.getPlanning().getEntries(),
          PredicateUtils.uniquePredicate());
      entry.getPlanning().setEntries(entries);
    }
    if (myFilter.isGroupEntries() == false && myFilter.isOnlyMyProjects() == false) {
      return list;
    }
    final List<HRPlanningEntryDO> result = new ArrayList<HRPlanningEntryDO>();
    final Set<Integer> set = (myFilter.isGroupEntries() == true) ? new HashSet<Integer>() : null;
    for (final HRPlanningEntryDO entry : list) {
      if (myFilter.isOnlyMyProjects() == true) {
        if (entry.getProjekt() == null) {
          continue;
        }
        final ProjektDO projekt = entry.getProjekt();
        if (projekt.getProjektManagerGroup() == null) {
          continue;
        }
        if (getUserGroupCache().isLoggedInUserMemberOfGroup(projekt.getProjektManagerGroupId()) == false) {
          continue;
        }
      }
      if (myFilter.isGroupEntries() == true) {
        if (set.contains(entry.getPlanningId()) == true) {
          // Entry is already in result list.
          continue;
        }
        final HRPlanningEntryDO sumEntry = new HRPlanningEntryDO();
        final HRPlanningDO planning = entry.getPlanning();
        sumEntry.setPlanning(planning);
        sumEntry.setUnassignedHours(planning.getTotalUnassignedHours());
        sumEntry.setMondayHours(planning.getTotalMondayHours());
        sumEntry.setTuesdayHours(planning.getTotalTuesdayHours());
        sumEntry.setWednesdayHours(planning.getTotalWednesdayHours());
        sumEntry.setThursdayHours(planning.getTotalThursdayHours());
        sumEntry.setFridayHours(planning.getTotalFridayHours());
        sumEntry.setWeekendHours(planning.getTotalWeekendHours());
        final StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (final HRPlanningEntryDO pos : planning.getEntries()) {
          final String str = pos.getProjektNameOrStatus();
          if (StringUtils.isNotBlank(str) == true) {
            if (first == true) {
              first = false;
            } else {
              buf.append("; ");
            }
            buf.append(str);
          }
        }
        sumEntry.setDescription(buf.toString());
        result.add(sumEntry);
        set.add(planning.getId());
      } else {
        result.add(entry);
      }
    }
    return result;
  }

  public QueryFilter buildQueryFilter(final HRPlanningFilter filter)
  {
    final QueryFilter queryFilter = new QueryFilter(filter);
    queryFilter.createAlias("planning", "p").createAlias("p.user", "u");
    if (filter.getUserId() != null) {
      final PFUserDO user = new PFUserDO();
      user.setId(filter.getUserId());
      queryFilter.add(Restrictions.eq("p.user", user));
    }
    if (filter.getStartTime() != null && filter.getStopTime() != null) {
      queryFilter.add(Restrictions.between("p.week", filter.getStartTime(), filter.getStopTime()));
    } else if (filter.getStartTime() != null) {
      queryFilter.add(Restrictions.ge("p.week", filter.getStartTime()));
    } else if (filter.getStopTime() != null) {
      queryFilter.add(Restrictions.le("p.week", filter.getStopTime()));
    }
    if (filter.getProjektId() != null) {
      queryFilter.add(Restrictions.eq("projekt.id", filter.getProjektId()));
    }
    queryFilter.addOrder(Order.desc("p.week")).addOrder(Order.asc("u.firstname"));
    if (log.isDebugEnabled() == true) {
      log.debug(ToStringBuilder.reflectionToString(filter));
    }
    return queryFilter;
  }

  /**
   * Checks week date on: monday, 0:00:00.000 and if check fails then the date will be set to.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onSaveOrModify(final HRPlanningEntryDO obj)
  {
    throw new UnsupportedOperationException(
        "Please do not save or HRPlanningEntryDO directly, save or update HRPlanningDO instead.");
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#prepareHibernateSearch(org.projectforge.core.ExtendedBaseDO,
   *      org.projectforge.framework.access.OperationType)
   */
  @Override
  protected void prepareHibernateSearch(final HRPlanningEntryDO obj, final OperationType operationType)
  {
    projektDao.initializeProjektManagerGroup(obj.getProjekt());
  }

  /**
   * @see HRPlanningDao#hasSelectAccess(PFUserDO, boolean)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return hrPlanningDao.hasSelectAccess(user, throwException);
  }

  /**
   * @see HRPlanningDao#hasAccess(HRPlanningDO, OperationType, boolean)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final HRPlanningEntryDO obj, final HRPlanningEntryDO oldObj,
      final OperationType operationType, final boolean throwException)
  {
    final HRPlanningDO old = oldObj != null ? oldObj.getPlanning() : null;
    return hrPlanningDao.hasAccess(user, obj.getPlanning(), old, operationType, throwException);
  }

  /**
   * @see HRPlanningDao#hasUserSelectAccess(HRPlanningDO, boolean)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final HRPlanningEntryDO obj, final boolean throwException)
  {
    return hrPlanningDao.hasSelectAccess(user, obj.getPlanning(), throwException);
  }

  @Override
  public HRPlanningEntryDO newInstance()
  {
    return new HRPlanningEntryDO();
  }
}
