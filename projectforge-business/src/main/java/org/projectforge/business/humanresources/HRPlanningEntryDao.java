/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.criteria.JoinType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.PFDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kai Reinhard
 */
@Service
public class HRPlanningEntryDao extends BaseDao<HRPlanningEntryDO> {
    public static final UserRightId USER_RIGHT_ID = UserRightId.PM_HR_PLANNING;

    private static final Logger log = LoggerFactory.getLogger(HRPlanningEntryDao.class);

    @Autowired
    private ProjektDao projektDao;

    @Autowired
    private HRPlanningDao hrPlanningDao;

    @Override
    public String[] getAdditionalSearchFields() {
        return new String[]{"projekt.name", "projekt.kunde.name", "planning.user.username", "planning.user.firstname",
                "planning.user.lastname"};
    }

    protected HRPlanningEntryDao() {
        super(HRPlanningEntryDO.class);
        userRightId = USER_RIGHT_ID;
    }

    /**
     * @param sheet
     * @param projektId If null, then projekt will be set to null;
     */
    public void setProjekt(final HRPlanningEntryDO sheet, final Long projektId) {
        final ProjektDO projekt = projektDao.findOrLoad(projektId);
        sheet.setProjekt(projekt);
    }

    @Override
    public List<HRPlanningEntryDO> select(final BaseSearchFilter filter) {
        final HRPlanningFilter myFilter = (HRPlanningFilter) filter;
        if (myFilter.getStopDay() != null) {
            final PFDateTime date = PFDateTime.from(myFilter.getStopDay()).getEndOfDay();
            myFilter.setStopDay(date.getLocalDate());
        }
        final QueryFilter queryFilter = buildQueryFilter(myFilter);
        myFilter.setIgnoreDeleted(true); // Ignore deleted flag of HRPlanningEntryDOs, use instead:
        if (myFilter.getDeleted()) {
            queryFilter.add(QueryFilter.or(QueryFilter.eq("deleted", true), QueryFilter.eq("planning.deleted", true)));
        } else {
            queryFilter.add(QueryFilter.and(QueryFilter.eq("deleted", false), QueryFilter.eq("planning.deleted", false)));
        }
        final List<HRPlanningEntryDO> list = select(queryFilter);
        if (list == null) {
            return null;
        }
        initializePlanningEntries(list);
        return groupAndFilter(list, myFilter.isGroupEntries(), myFilter.isOnlyMyProjects());
    }

    /**
     * Joins the associations every list row reads into the query itself: the planning and its employee (access
     * check, week, user), the project and its customer (project name and customer column). The list query
     * scrolls its result, so Hibernate's batch fetching never sees more than one pending proxy and each lazy
     * association would cost one select per row.
     */
    public static void addListFetchJoins(final QueryFilter queryFilter) {
        queryFilter.createJoin("planning", JoinType.INNER, true)
                .createJoin("user", JoinType.INNER, true, "planning")
                .createJoin("projekt", JoinType.LEFT, true)
                .createJoin("kunde", JoinType.LEFT, true, "projekt");
    }

    /**
     * Sets the entries of the plannings of the given (detached, read-only) list entries, loaded with one query
     * per batch of plannings instead of one lazy load per planning. Needed by the planning's sums
     * ({@link HRPlanningDO#getTotalHours()}) and by {@link #groupAndFilter(List, boolean, boolean)}.
     */
    public void initializePlanningEntries(final List<HRPlanningEntryDO> list) {
        final Map<Long, List<HRPlanningDO>> plannings = new HashMap<>();
        for (final HRPlanningEntryDO entry : list) {
            final HRPlanningDO planning = entry.getPlanning();
            if (planning != null && planning.getId() != null) {
                plannings.computeIfAbsent(planning.getId(), id -> new ArrayList<>()).add(planning);
            }
        }
        if (plannings.isEmpty()) {
            return;
        }
        final List<HRPlanningEntryDO> entries = getPersistenceService().executeQueryBatched(
                "select e from HRPlanningEntryDO e left join fetch e.projekt where e.planning.id in :planningIds order by e.id",
                HRPlanningEntryDO.class, "planningIds", plannings.keySet());
        final Map<Long, List<HRPlanningEntryDO>> entriesByPlanning = new HashMap<>();
        for (final HRPlanningEntryDO entry : entries) {
            entriesByPlanning.computeIfAbsent(entry.getPlanningId(), id -> new ArrayList<>()).add(entry);
        }
        plannings.forEach((id, instances) -> {
            for (final HRPlanningDO planning : instances) {
                planning.setEntries(new ArrayList<>(entriesByPlanning.getOrDefault(id, List.of())));
            }
        });
    }

    /**
     * Applies the two list options of the HR planning to the given entries.
     *
     * @param groupEntries   If true, the entries of a planned week are replaced by one synthetic entry (without id)
     *                       holding the sums of the week and the projects (or status) of its entries as description.
     * @param onlyMyProjects If true, only entries of projects whose project manager group the logged-in user is a
     *                       member of are kept.
     * @return The given list itself if neither option is set.
     */
    public List<HRPlanningEntryDO> groupAndFilter(final List<HRPlanningEntryDO> list, final boolean groupEntries,
                                                  final boolean onlyMyProjects) {
        if (!groupEntries && !onlyMyProjects) {
            return list;
        }
        final List<HRPlanningEntryDO> result = new ArrayList<>();
        final Set<Long> set = groupEntries ? new HashSet<>() : null;
        for (final HRPlanningEntryDO entry : list) {
            if (onlyMyProjects) {
                if (entry.getProjekt() == null) {
                    continue;
                }
                final ProjektDO projekt = entry.getProjekt();
                if (projekt.getProjektManagerGroup() == null) {
                    continue;
                }
                if (!userGroupCache.isLoggedInUserMemberOfGroup(projekt.getProjektManagerGroupId())) {
                    continue;
                }
            }
            if (groupEntries) {
                if (set.contains(entry.getPlanningId())) {
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
                final StringBuilder buf = new StringBuilder();
                boolean first = true;
                for (final HRPlanningEntryDO pos : planning.getEntries()) {
                    if (pos.getDeleted()) {
                        continue;
                    }
                    final String str = pos.getProjektNameOrStatus();
                    if (StringUtils.isNotBlank(str)) {
                        if (first) {
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

    public QueryFilter buildQueryFilter(final HRPlanningFilter filter) {
        final QueryFilter queryFilter = new QueryFilter(filter);
        addListFetchJoins(queryFilter);
        if (filter.getUserId() != null) {
            final PFUserDO user = new PFUserDO();
            user.setId(filter.getUserId());
            queryFilter.add(QueryFilter.eq("planning.user", user));
        }
        if (filter.getStartDay() != null && filter.getStopDay() != null) {
            queryFilter.add(QueryFilter.between("planning.week", filter.getStartDay(), filter.getStopDay()));
        } else if (filter.getStartDay() != null) {
            queryFilter.add(QueryFilter.ge("planning.week", filter.getStartDay()));
        } else if (filter.getStopDay() != null) {
            queryFilter.add(QueryFilter.le("planning.week", filter.getStopDay()));
        }
        if (filter.getProjektId() != null) {
            queryFilter.add(QueryFilter.eq("projekt.id", filter.getProjektId()));
        }
        queryFilter.addOrder(SortProperty.desc("planning.week")).addOrder(SortProperty.asc("planning.user.firstname"));
        if (log.isDebugEnabled()) {
            log.debug(ToStringBuilder.reflectionToString(filter));
        }
        return queryFilter;
    }

    /**
     * Checks week date on: monday, 0:00:00.000 and if check fails then the date will be set to.
     */
    @Override
    public void onInsertOrModify(final HRPlanningEntryDO obj, final OperationType operationType) {
        throw new UnsupportedOperationException(
                "Please do not save or HRPlanningEntryDO directly, save or update HRPlanningDO instead.");
    }

    @Override
    public void prepareHibernateSearch(final HRPlanningEntryDO obj, final OperationType operationType) {
        projektDao.initializeProjektManagerGroup(obj.getProjekt());
    }

    /**
     * @see HRPlanningDao#hasUserSelectAccess(PFUserDO, boolean)
     */
    @Override
    public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
        return hrPlanningDao.hasUserSelectAccess(user, throwException);
    }

    @Override
    public boolean hasAccess(final PFUserDO user, final HRPlanningEntryDO obj, final HRPlanningEntryDO oldObj,
                             final OperationType operationType, final boolean throwException) {
        // obj is null for the entity wide question, e.g. whether the list may offer a new entry (listMeta).
        final HRPlanningDO planning = obj != null ? obj.getPlanning() : null;
        final HRPlanningDO old = oldObj != null ? oldObj.getPlanning() : null;
        return hrPlanningDao.hasAccess(user, planning, old, operationType, throwException);
    }

    @Override
    public boolean hasUserSelectAccess(final PFUserDO user, final HRPlanningEntryDO obj, final boolean throwException) {
        return hrPlanningDao.hasUserSelectAccess(user, obj.getPlanning(), throwException);
    }

    @Override
    public HRPlanningEntryDO newInstance() {
        return new HRPlanningEntryDO();
    }
}
