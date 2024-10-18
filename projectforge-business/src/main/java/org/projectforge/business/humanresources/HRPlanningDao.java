/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import kotlin.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightId;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.history.HistoryEntryDO;
import org.projectforge.framework.persistence.history.HistoryFormatUtils;
import org.projectforge.framework.persistence.history.HistoryService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * @author Mario Groß (m.gross@micromata.de)
 */
@Service
public class HRPlanningDao extends BaseDao<HRPlanningDO> {
    public static final UserRightId USER_RIGHT_ID = UserRightId.PM_HR_PLANNING;

    private static final Logger log = LoggerFactory.getLogger(HRPlanningDao.class);

    private static final Class<?>[] ADDITIONAL_SEARCH_DOS = new Class[]{HRPlanningEntryDO.class};

    @Autowired
    private AccessChecker accessChecker;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProjektDao projektDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserGroupCache userGroupCache;

    protected HRPlanningDao() {
        super(HRPlanningDO.class);
        userRightId = USER_RIGHT_ID;
    }

    @Override
    public String[] getAdditionalSearchFields() {
        return new String[]{"entries.projekt.name", "entries.projekt.kunde.name", "user.username", "user.firstname",
                "user.lastname"};
    }

    /**
     * @param sheet
     * @param projektId If null, then projekt will be set to null;
     */
    public void setProjekt(final HRPlanningEntryDO sheet, final Long projektId) {
        final ProjektDO projekt = projektDao.findOrLoad(projektId);
        sheet.setProjekt(projekt);
    }

    /**
     * @param sheet
     * @param userId If null, then user will be set to null;
     */
    public void setUser(final HRPlanningDO sheet, final Long userId) {
        final PFUserDO user = userDao.findOrLoad(userId);
        sheet.setUser(user);
    }

    /**
     * Does an entry with the same user and week of year already exist?
     *
     * @param planning
     * @return If week or user id is not given, return false.
     */
    public boolean doesEntryAlreadyExist(final HRPlanningDO planning) {
        Objects.requireNonNull(planning);
        return doesEntryAlreadyExist(planning.getId(), planning.getUserId(), planning.getWeek());
    }

    /**
     * Does an entry with the same user and week of year already exist?
     *
     * @param planningId Id of the current planning or null if new.
     * @param userId
     * @param week
     * @return If week or user id is not given, return false.
     */
    public boolean doesEntryAlreadyExist(final Long planningId, final Long userId, final LocalDate week) {
        if (week == null || userId == null) {
            return false;
        }
        final HRPlanningDO other;
        if (planningId == null) {
            // New entry
            other = persistenceService.selectNamedSingleResult(
                    HRPlanningDO.FIND_BY_USER_AND_WEEK,
                    HRPlanningDO.class,
                    new Pair<>("userId", userId),
                    new Pair<>("week", week));
        } else {
            // Entry already exists. Check collision:
            other = persistenceService.selectNamedSingleResult(
                    HRPlanningDO.FIND_OTHER_BY_USER_AND_WEEK,
                    HRPlanningDO.class,
                    new Pair<>("userId", userId),
                    new Pair<>("week", week),
                    new Pair<>("id", planningId));
        }
        return other != null;
    }

    public HRPlanningDO getEntry(final PFUserDO user, final LocalDate week) {
        return getEntry(user.getId(), week);
    }

    public HRPlanningDO getEntry(final Long userId, final LocalDate week) {
        PFDay day = PFDay.from(week);
        if (!day.isBeginOfWeek()) {
            log.error("Date is not begin of week, try to change date: " + day.getIsoString());
            day = day.getBeginOfWeek();
        }
        final HRPlanningDO planning = persistenceService.selectNamedSingleResult(
                HRPlanningDO.FIND_BY_USER_AND_WEEK,
                HRPlanningDO.class,
                new Pair<>("userId", userId),
                new Pair<>("week", day.getLocalDate()));
        if (planning == null) {
            return null;
        }
        if (accessChecker.hasLoggedInUserSelectAccess(userRightId, planning, false)) {
            return planning;
        } else {
            return null;
        }
    }

    @Override
    public List<HRPlanningDO> select(final BaseSearchFilter filter) {
        final HRPlanningFilter myFilter = (HRPlanningFilter) filter;
        if (myFilter.getStopDay() != null) {
            PFDateTime dateTime = PFDateTime.fromOrNow(myFilter.getStopDay()).getEndOfDay();
            myFilter.setStopDay(dateTime.getLocalDate());
        }
        final QueryFilter queryFilter = buildQueryFilter(myFilter);
        final List<HRPlanningDO> result = select(queryFilter);
        return result;
    }

    private boolean entryHasUpdates(final HRPlanningEntryDO entry, final HRPlanningDO existingPlanning) {
        if (entry.getId() == null) {
            return true;
        }
        if (existingPlanning != null) {
            for (HRPlanningEntryDO existingEntry : existingPlanning.getEntries()) {
                if (!existingEntry.getDeleted() && existingEntry.getId().equals(entry.getId())) {
                    return !existingEntry.hasNoFieldChanges(entry);
                }
            }
        }
        return false;
    }

    public QueryFilter buildQueryFilter(final HRPlanningFilter filter) {
        final QueryFilter queryFilter = new QueryFilter(filter);
        if (filter.getUserId() != null) {
            final PFUserDO user = new PFUserDO();
            user.setId(filter.getUserId());
            queryFilter.add(QueryFilter.eq("user", user));
        }
        if (filter.getStartDay() != null && filter.getStopDay() != null) {
            queryFilter.add(QueryFilter.between("week", filter.getStartDay(), filter.getStopDay()));
        } else if (filter.getStartDay() != null) {
            queryFilter.add(QueryFilter.ge("week", filter.getStartDay()));
        } else if (filter.getStopDay() != null) {
            queryFilter.add(QueryFilter.le("week", filter.getStopDay()));
        }
        if (filter.getProjektId() != null) {
            queryFilter.add(QueryFilter.eq("projekt.id", filter.getProjektId()));
        }
        queryFilter.addOrder(SortProperty.desc("week"));
        if (log.isDebugEnabled()) {
            log.debug(ToStringBuilder.reflectionToString(filter));
        }
        return queryFilter;
    }

    /**
     * <ul>
     * <li>Checks week date on: monday, 0:00:00.000 and if check fails then the date will be set to.</li>
     * <li>Check deleted entries and re-adds them instead of inserting a new entry, if exist.</li>
     * <ul>
     */
    @Override
    public void onInsertOrModify(final HRPlanningDO obj, final OperationType operationType) {
        PFDay day = PFDay.from(obj.getWeek());
        if (!day.isBeginOfWeek()) {
            log.error("Date is not begin of week, try to change date: " + day.getIsoString());
            day = day.getBeginOfWeek();
            obj.setWeek(day.getDate());
        }

        if (!accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.HR_GROUP, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)) {
            HRPlanningDO existingPlanning = null;
            if (obj.getId() != null) {
                existingPlanning = find(obj.getId(), false);
            }
            for (HRPlanningEntryDO entry : obj.getEntries()) {
                ProjektDO projekt = entry.getProjekt();
                if (entryHasUpdates(entry, existingPlanning) && projekt != null) {
                    boolean userHasRightForProject = false;
                    Long userId = ThreadLocalUserContext.getLoggedInUser().getId();
                    Long headOfBusinessManagerId = projekt.getHeadOfBusinessManager() != null ? projekt.getHeadOfBusinessManager().getId() : null;
                    Long projectManagerId = projekt.getProjectManager() != null ? projekt.getProjectManager().getId() : null;
                    Long salesManageId = projekt.getSalesManager() != null ? projekt.getSalesManager().getId() : null;
                    if (userId != null && (userId.equals(headOfBusinessManagerId) || userId.equals(projectManagerId) || userId.equals(salesManageId))) {
                        userHasRightForProject = true;
                    }

                    if (projekt.getProjektManagerGroup() != null
                            && userGroupCache.isUserMemberOfGroup(userId, projekt.getProjektManagerGroupId())) {
                        userHasRightForProject = true;
                    }
                    if (!userHasRightForProject) {
                        throw new UserException("hr.planning.entry.error.noRightForProject", projekt.getName());
                    }
                }
            }
        }
    }

    @Override
    public void prepareHibernateSearch(final HRPlanningDO obj, final OperationType operationType) {
        final List<HRPlanningEntryDO> entries = obj.getEntries();
        if (entries != null) {
            for (final HRPlanningEntryDO entry : entries) {
                projektDao.initializeProjektManagerGroup(entry.getProjekt());
            }
        }
        final PFUserDO user = obj.getUser();
        if (user != null) {
            obj.setUser(userGroupCache.getUser(user.getId()));
        }
    }

    @Override
    public HRPlanningDO newInstance() {
        return new HRPlanningDO();
    }

    /**
     * Gets history entries of super and adds all history entries of the HRPlanningEntryDO children.
     */
    @Override
    protected void customizeHistoryEntries(HRPlanningDO obj, @NotNull List<HistoryEntryDO> list) {
        if (CollectionUtils.isNotEmpty(obj.getEntries())) {
            for (final HRPlanningEntryDO position : obj.getEntries()) {
                var entries = historyService.loadHistory(position);
                var prefix = position.getProjekt() != null ? position.getProjektName() : String.valueOf(position.getStatus());
                HistoryFormatUtils.setPropertyNameForListEntries(entries, prefix);
                mergeHistoryEntries(list, entries);
            }
        }
    }

    @Override
    protected Class<?>[] getAdditionalHistorySearchDOs() {
        return ADDITIONAL_SEARCH_DOS;
    }

}
