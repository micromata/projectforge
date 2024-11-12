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

package org.projectforge.business.teamcal.admin;

import org.hibernate.Hibernate;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.jpa.PfPersistenceService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Caches all calendars.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class TeamCalCache extends AbstractCache {
    private static final long serialVersionUID = 1742102774636598280L;

    private static Logger log = LoggerFactory.getLogger(TeamCalCache.class);

    @Autowired
    PfPersistenceService persistenceService;

    @Autowired
    UserRightService userRights;

    private transient TeamCalRight teamCalRight;

    private Map<Long, TeamCalDO> calendarMap;

    public TeamCalRight getTeamCalRight() {
        return teamCalRight;
    }

    public TeamCalDO getCalendar(final Long calendarId) {
        checkRefresh();
        return calendarMap.get(calendarId);
    }

    /**
     * Returns the TeamCalDO if it is initialized (Hibernate). Otherwise, it will be loaded from the database.
     * Prevents lazy loadings.
     */
    public TeamCalDO getCalendarIfNotInitialized(TeamCalDO teamCalDO) {
        Long id = teamCalDO.getId();
        if (id == null) {
            return teamCalDO;
        }
        if (Hibernate.isInitialized(teamCalDO)) {
            return teamCalDO;
        }
        return getCalendar(id);
    }

    /**
     * Get ordered calendars (by title and id).
     *
     * @return All accessible calendars of the context user (as owner or with full, read-only or minimal access).
     */
    public Collection<TeamCalDO> getAllAccessibleCalendars() {
        checkRefresh();
        final Set<TeamCalDO> set = new TreeSet<>(new TeamCalsComparator());
        final PFUserDO loggedInUser = ThreadLocalUserContext.getLoggedInUser();
        for (final TeamCalDO cal : calendarMap.values()) {
            if (teamCalRight.hasSelectAccess(loggedInUser, cal) && !cal.getDeleted()) {
                set.add(cal);
            }
        }
        return set;
    }

    /**
     * Get ordered calendars (by title and id).
     *
     * @return All accessible calendars of the context user (as owner or with full, read-only or minimal access).
     */
    public Collection<TeamCalDO> getAllFullAccessCalendars() {
        checkRefresh();
        final Set<TeamCalDO> set = new TreeSet<>(new TeamCalsComparator());
        final PFUserDO loggedInUser = ThreadLocalUserContext.getLoggedInUser();
        for (final TeamCalDO cal : calendarMap.values()) {
            if (teamCalRight.hasFullAccess(cal, loggedInUser.getId()) && !cal.getDeleted()) {
                set.add(cal);
            }
        }
        return set;
    }

    /**
     * Get ordered calendars (by title and id).
     *
     * @return All accessible calendars of the context user (as owner or with full, read-only or minimal access).
     */
    public Collection<TeamCalDO> getAllOwnCalendars() {
        checkRefresh();
        final Set<TeamCalDO> set = new TreeSet<>(new TeamCalsComparator());
        final Long loggedInUserId = ThreadLocalUserContext.getLoggedInUserId();
        for (final TeamCalDO cal : calendarMap.values()) {
            if (teamCalRight.isOwner(loggedInUserId, cal)) {
                set.add(cal);
            }
        }
        return set;
    }

    public Collection<TeamCalDO> getCalendars(final Collection<Long> calIds) {
        final Set<TeamCalDO> set = new TreeSet<>(new TeamCalsComparator());
        if (calIds != null) {
            for (final Long calId : calIds) {
                final TeamCalDO cal = getCalendar(calId);
                if (cal == null) {
                    log.warn("Calendar with id " + calId + " not found in cache.");
                    continue;
                }
                if (teamCalRight.hasSelectAccess(ThreadLocalUserContext.getLoggedInUser())) {
                    set.add(cal);
                }
            }
        }
        return set;
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    @Override
    protected void refresh() {
        log.info("Initializing TeamCalCache ...");
        persistenceService.runIsolatedReadOnly(true, context -> {
            TeamCalDao dao = ApplicationContextProvider.getApplicationContext().getBean(TeamCalDao.class);
            if (dao == null || teamCalRight == null) {
                teamCalRight = (TeamCalRight) userRights.getRight(UserRightId.PLUGIN_CALENDAR);
            }
            // This method must not be synchronized because it works with a new copy of maps.
            final Map<Long, TeamCalDO> map = new HashMap<>();
            final List<TeamCalDO> list = dao.selectAll(false);
            for (final TeamCalDO cal : list) {
                TeamCalDO put = map.put(cal.getId(), cal);
                if (put != null) {
                    log.info("Adding team cal with id: " + cal.getId() + " to cache.");
                }
            }
            this.calendarMap = map;
            log.info("Initializing of TeamCalCache done: " + context.formatStats(true));
            return null;
        });
    }
}
