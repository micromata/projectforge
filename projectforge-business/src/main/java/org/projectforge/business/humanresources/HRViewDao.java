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

import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.KundeCache;
import org.projectforge.business.fibu.kost.ProjektCache;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kai Reinhard
 */
@Service
public class HRViewDao implements IDao<HRViewData> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRViewDao.class);

    @Autowired
    private HRPlanningDao hrPlanningDao;

    @Autowired
    private HRPlanningEntryDao hrPlanningEntryDao;

    @Autowired
    private KundeCache kundeCache;

    @Autowired
    private ProjektCache projektCache;

    @Autowired
    private TaskTree taskTree;

    @Autowired
    private TimesheetDao timesheetDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserGroupCache userGroupCache;

    /**
     * Rows contains the users and the last row contains the total sums. Columns of each rows are the man days of the
     * projects (see getProjectNames)
     * <p>
     * Projects and customers are taken from the caches and the planning entries are loaded with their planning and
     * project in one query ({@link HRPlanningEntryDao#select(BaseSearchFilter)}), so the view costs a constant number
     * of statements, not one per planning, entry or project.
     */
    public HRViewData getResources(final HRFilter filter) {
        final HRViewData data = new HRViewData(filter);
        if (filter.getStartDay() == null) {
            filter.setStartDay(PFDay.today().getLocalDate());
        }
        if (filter.getStopDay() == null) {
            filter.setStopDay(PFDay.from(filter.getStartDay()).getEndOfWeek().getLocalDate());
        }
        if (filter.isShowBookedTimesheets()) {
            final TimesheetFilter tsFilter = new TimesheetFilter();
            tsFilter.setStartTime(PFDateTime.fromOrNull(filter.getStartDay()).getBeginOfDay().getUtilDate());
            tsFilter.setStopTime(PFDateTime.fromOrNull(filter.getStopDay()).getEndOfDay().getUtilDate());
            final List<TimesheetDO> sheets = timesheetDao.select(tsFilter);
            for (final TimesheetDO sheet : sheets) {
                final PFUserDO user = userGroupCache.getUser(sheet.getUserId());
                if (user == null) {
                    log.error("Oups, user of time sheet is null or unknown? Ignoring entry: " + sheet);
                    continue;
                }
                final Object targetObject = getTargetObject(filter, taskTree.getProjekt(sheet.getTaskId()));
                if (targetObject == null) {
                    data.addTimesheet(sheet, user);
                } else if (targetObject instanceof ProjektDO) {
                    data.addTimesheet(sheet, user, (ProjektDO) targetObject);
                } else if (targetObject instanceof KundeDO) {
                    data.addTimesheet(sheet, user, (KundeDO) targetObject);
                } else {
                    log.error("Target object of type " + targetObject + " not supported.");
                    data.addTimesheet(sheet, user);
                }
            }
        }
        if (filter.isShowPlanning()) {
            final HRPlanningFilter hrFilter = new HRPlanningFilter();
            hrFilter.setStartDay(PFDay.fromOrNow(filter.getStartDay()).getLocalDate());
            hrFilter.setStopDay(PFDay.fromOrNow(filter.getStopDay()).getLocalDate());
            // Neither deleted entries nor entries of deleted plannings:
            final List<HRPlanningEntryDO> entries = hrPlanningEntryDao.select(hrFilter);
            for (final HRPlanningEntryDO entry : entries) {
                final PFUserDO user = userGroupCache.getUser(entry.getPlanning().getUserId());
                if (user == null) {
                    log.error("Oups, user of planning is null or unknown? Ignoring entry: " + entry);
                    continue;
                }
                final Object targetObject = getTargetObject(filter, entry.getProjekt());
                if (targetObject == null) {
                    data.addHRPlanningEntry(entry, user);
                } else if (targetObject instanceof ProjektDO) {
                    data.addHRPlanningEntry(entry, user, (ProjektDO) targetObject);
                } else if (targetObject instanceof KundeDO) {
                    data.addHRPlanningEntry(entry, user, (KundeDO) targetObject);
                } else {
                    log.error("Target object of type " + targetObject + " not supported.");
                    data.addHRPlanningEntry(entry, user);
                }
            }
        }
        if (filter.isOnlyMyProjects()) {
            // remove all user entries which have no planning or booking on my projects.
            data.userDatas.values().removeIf(entry -> entry.entries.stream()
                    .noneMatch(entryData -> entryData.projekt != null || entryData.kunde != null));
        }
        return data;
    }

    /**
     * Returns a list of all users which are accessible by the current logged in user, take part in the HR planning,
     * have system access and are not planned in the given HRViewData object.
     *
     * @return Result list (may be empty but never null).
     */
    public List<PFUserDO> getUnplannedResources(final HRViewData data) {
        final List<PFUserDO> users = new ArrayList<>();
        final QueryFilter queryFilter = new QueryFilter(new BaseSearchFilter());
        queryFilter.addOrder(SortProperty.asc("firstname")).addOrder(SortProperty.asc("lastname"));
        final List<PFUserDO> allUsers = userDao.select(queryFilter);
        if (allUsers != null) {
            for (final PFUserDO user : allUsers) {
                if (!user.getHrPlanning() || !user.hasSystemAccess()) {
                    continue;
                }
                final HRViewUserData userData = data.getUserData(user);
                if (userData == null || !NumberHelper.isNotZero(userData.getPlannedDaysSum())) {
                    users.add(user);
                }
            }
        }
        return users;
    }

    /**
     * Return the target object (ProjektDO, KundeDO or null) to which the entry (time sheet or planning) should be
     * assigned to. The results depends on the filter settings.
     * <p>
     * Both are taken from the caches: a project of the task tree or the project cache holds its customer as
     * uninitialized (detached) proxy.
     */
    private Object getTargetObject(final HRFilter filter, final ProjektDO projektOrProxy) {
        final ProjektDO projekt = projektCache.getProjektIfNotInitialized(projektOrProxy);
        if (projekt == null) {
            return null;
        }
        final KundeDO kunde = kundeCache.getKundeIfNotInitialized(projekt.getKunde());
        if (filter.isOnlyMyProjects()) {
            if (isMyProject(projekt)) {
                if (filter.isAllProjectsGroupedByCustomer()) {
                    return kunde;
                } else {
                    return projekt;
                }
            } else {
                return null;
            }
        } else if (filter.isAllProjectsGroupedByCustomer()) {
            return kunde;
        } else if (filter.isOtherProjectsGroupedByCustomer()) {
            if (isMyProject(projekt)) {
                return projekt;
            } else {
                return kunde;
            }
        } else {
            // Show all projects
            return projekt;
        }
    }

    private boolean isMyProject(final ProjektDO projekt) {
        return (projekt != null && projekt.getProjektManagerGroup() != null
                && userGroupCache.isLoggedInUserMemberOfGroup(projekt.getProjektManagerGroupId()));
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @see org.projectforge.framework.persistence.api.IDao#select(org.projectforge.framework.persistence.api.BaseSearchFilter)
     */
    @Override
    public List<HRViewData> select(final BaseSearchFilter filter) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return false.
     * @see org.projectforge.framework.persistence.api.IDao#isHistorizable()
     */
    @Override
    public boolean isHistorizable() {
        return false;
    }

    /**
     * @return true.
     */
    @Override
    public boolean hasInsertAccess(final PFUserDO user) {
        return hrPlanningDao.hasInsertAccess(user);
    }
}
