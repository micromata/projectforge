/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.externalsubscription;

import org.apache.commons.collections4.CollectionUtils;
import org.projectforge.business.common.DataobjectAccessType;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.jpa.PfPersistenceService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
@Component
public class TeamEventExternalSubscriptionCache {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(TeamEventExternalSubscriptionCache.class);

    private static final long MAX_WAIT_MS_AFTER_FAILED_UPDATE = 1000 * 60 * 60 * 24; // 24 h

    private final Map<Long, TeamEventSubscription> subscriptions = new HashMap<>();

    private static final Long SUBSCRIPTION_UPDATE_TIME = 5L * 60 * 1000; // 5 min

    private transient TeamCalRight teamCalRight;

    private boolean initialized;

    @Autowired
    private TeamCalDao teamCalDao;

    @Autowired
    private PfPersistenceService persistenceService;

    @Autowired
    private TeamCalCache teamCalCache;

    @Autowired
    private UserRightService userRights;

    @Autowired
    private UserGroupCache userGroupCache;

    // @PostConstruct doesn't work (it will be called to early before TenantRegistryMap is ready).
    private synchronized void init() {
        if (!initialized) {
            persistenceService.runIsolatedReadOnly(context -> {
                final QueryFilter filter = new QueryFilter();
                filter.add(QueryFilter.eq("externalSubscription", true));
                final List<TeamCalDO> subscribedCalendars = teamCalDao.select(filter, false);
                for (final TeamCalDO calendar : subscribedCalendars) {
                    TeamEventSubscription teamEventSubscription = new TeamEventSubscription();
                    subscriptions.put(calendar.getId(), teamEventSubscription);
                }
                initialized = true;
                return null;
            });
            // Start updateCache as may-be long-running thread. Avoids blocking of callee (CalendarPage).
            new Thread(() -> {
                updateCache();
            }).start();
        }
    }

    public void updateCache() {
        log.info("Start updating TeamEventExternalSubscriptionCache.");
        persistenceService.runInNewTransaction(context -> {
            final QueryFilter filter = new QueryFilter();
            filter.add(QueryFilter.eq("externalSubscription", true));
            // internalGetList is valid at this point, because we are calling this method in an asyn thread
            final List<TeamCalDO> subscribedCalendars = teamCalDao.select(filter, false);

            for (final TeamCalDO calendar : subscribedCalendars) {
                updateCache(calendar);
            }

            final List<Long> idsToRemove = new ArrayList<>();
            for (final Long calendarId : subscriptions.keySet()) {
                // if calendar is not subscribed anymore, remove them
                if (!calendarListContainsId(subscribedCalendars, calendarId)) {
                    idsToRemove.add(calendarId);
                }
            }
            removeCalendarsFromCache(idsToRemove);
            return null;
        });
    }

    private void removeCalendarsFromCache(final List<Long> idsToRemove) {
        for (final Long calendarId : idsToRemove) {
            subscriptions.remove(calendarId);
        }
    }

    private boolean calendarListContainsId(final List<TeamCalDO> subscribedCalendars, final Long calendarId) {
        for (final TeamCalDO teamCal : subscribedCalendars) {
            if (teamCal.getId().equals(calendarId)) {
                return true;
            }
        }
        return false;
    }

    public void updateCache(final TeamCalDO calendar) {
        updateCache(calendar, false);
    }

    /**
     * @param calendar
     * @param force    If true then update is forced (independent of last update time and refresh interval).
     */
    public void updateCache(final TeamCalDO calendar, final boolean force) {
        final Long calId = calendar.getId();
        if (calId == null) {
            log.error("Oups, calId is null (can't update subscription): " + calendar);
            return;
        }
        final PFUserDO owner = userGroupCache.getUser(calendar.getOwnerId());
        if (owner == null) {
            log.error("Oups, owner is null (can't update subscription): " + calendar);
            return;
        }
        if (owner.getDeactivated() || owner.getDeleted()) {
            // Don't update calendars of deactivated users....
            log.info("Ignoring subscriptions of deactivated/deleted owner #" + owner.getId() + " of calendar #" + calId);
            return;
        }
        TeamEventSubscription teamEventSubscription = subscriptions.get(calId);
        final Long now = System.currentTimeMillis();
        final Long addedTime = calendar.getExternalSubscriptionUpdateInterval() == null ? SUBSCRIPTION_UPDATE_TIME
                : 1000L * calendar
                .getExternalSubscriptionUpdateInterval();
        if (teamEventSubscription == null || !teamEventSubscription.isInitialized()) {
            // First update of subscribed calendar:
            if (teamEventSubscription == null) {
                teamEventSubscription = new TeamEventSubscription();
                subscriptions.put(calendar.getId(), teamEventSubscription);
            }
            teamEventSubscription.update(teamCalDao, calendar);
        } else if (force || teamEventSubscription.getLastUpdated() == null
                || teamEventSubscription.getLastUpdated() + addedTime <= now) {
            if (!force && teamEventSubscription.getNumberOfFailedUpdates() > 0) {
                // Errors occurred and update not forced. Don't update e. g. every 5 minutes if a permanently error occurs.
                Long lastRun = teamEventSubscription.getLastUpdated();
                if (lastRun == null) {
                    lastRun = teamEventSubscription.getLastFailedUpdate();
                }
                if (lastRun == null || lastRun + teamEventSubscription.getNumberOfFailedUpdates() * addedTime <= now) {
                    teamEventSubscription.update(teamCalDao, calendar);
                } else if (lastRun + MAX_WAIT_MS_AFTER_FAILED_UPDATE > now) {
                    log.info("Try to update subscribed calendar after "
                            + (MAX_WAIT_MS_AFTER_FAILED_UPDATE / 1000 / 60 / 60)
                            + " hours. Number of failed updates: "
                            + teamEventSubscription.getNumberOfFailedUpdates()
                            + ", time of last successful update (UTC): "
                            + (teamEventSubscription.getLastUpdated() != null
                            ? DateHelper.formatAsUTC(new Date(teamEventSubscription.getLastUpdated()))
                            : "-"));
                    teamEventSubscription.update(teamCalDao, calendar);
                }
            } else {
                // update the calendar
                teamEventSubscription.update(teamCalDao, calendar);
            }
        }
    }

    public boolean isExternalSubscribedCalendar(final Long calendarId) {
        init();
        return subscriptions.keySet().contains(calendarId);
    }

    public List<TeamEventDO> getEvents(final Long calendarId, final Long startTime, final Long endTime) {
        init();
        final TeamEventSubscription eventSubscription = subscriptions.get(calendarId);
        final DataobjectAccessType accessType = getAccessType(eventSubscription);
        if (accessType == null)
            return null;
        return eventSubscription.getEvents(startTime, endTime, accessType == DataobjectAccessType.MINIMAL);
    }

    public TeamEventDO getEvent(final Long calendarId, final String uid) {
        init();
        final TeamEventSubscription eventSubscription = subscriptions.get(calendarId);
        final DataobjectAccessType accessType = getAccessType(eventSubscription);
        if (accessType == null)
            return null;
        return eventSubscription.getEvent(uid);
    }

    /**
     * Checks also the access.
     *
     * @return The accessType if available and the logged in user has access to, otherwise null.
     */
    private DataobjectAccessType getAccessType(TeamEventSubscription eventSubscription) {
        if (eventSubscription == null) {
            return null;
        }
        final Long userId = ThreadLocalUserContext.getLoggedInUserId();
        final DataobjectAccessType accessType = getAccessType(eventSubscription.getTeamCalId(), userId);
        if (!accessType.hasAnyAccess()) {
            return null;
        }
        return accessType;
    }

    public List<TeamEventDO> getRecurrenceEvents(final TeamEventFilter filter) {
        init();
        final List<TeamEventDO> result = new ArrayList<>();
        // precondition: existing teamcals ins filter
        final Collection<Long> teamCals = new LinkedList<>();
        final Long userId = ThreadLocalUserContext.getLoggedInUserId();
        if (CollectionUtils.isNotEmpty(filter.getTeamCals())) {
            for (final Long calendarId : filter.getTeamCals()) {
                final TeamEventSubscription eventSubscription = subscriptions.get(calendarId);
                if (eventSubscription == null) {
                    continue;
                }
                final TeamCalDO calendar = teamCalCache.getCalendar(calendarId);
                if (!getTeamCalRight().getAccessType(calendar, userId).hasAnyAccess()) {
                    continue;
                }
                teamCals.add(calendarId);
            }
        }
        if (filter.getTeamCalId() != null) {
            final TeamEventSubscription eventSubscription = subscriptions.get(filter.getTeamCalId());
            if (eventSubscription != null) {
                final TeamCalDO cal = teamCalCache.getCalendar(filter.getTeamCalId());
                if (getTeamCalRight().getAccessType(cal, userId).hasAnyAccess()) {
                    teamCals.add(filter.getTeamCalId());
                }
            }
        }
        if (teamCals != null) {
            for (final Long calendarId : teamCals) {
                final TeamEventSubscription eventSubscription = subscriptions.get(calendarId);
                if (eventSubscription != null) {
                    final List<TeamEventDO> recurrenceEvents = eventSubscription.getRecurrenceEvents();
                    if (recurrenceEvents != null && recurrenceEvents.size() > 0) {
                        for (final TeamEventDO event : recurrenceEvents) {
                            final TeamCalDO calendar = teamCalCache.getCalendar(calendarId);
                            if (getTeamCalRight().getAccessType(calendar, userId) == DataobjectAccessType.MINIMAL) {
                                result.add(event.createMinimalCopy());
                            } else {
                                result.add(event);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private DataobjectAccessType getAccessType(final Long calendarId, final Long userId) {
        final TeamCalDO cal = teamCalCache.getCalendar(calendarId);
        return getTeamCalRight().getAccessType(cal, userId);
    }

    /**
     * @return the teamCalRight
     */
    public TeamCalRight getTeamCalRight() {
        if (teamCalRight == null) {
            teamCalRight = (TeamCalRight) userRights.getRight(UserRightId.PLUGIN_CALENDAR);
        }
        return teamCalRight;
    }
}
