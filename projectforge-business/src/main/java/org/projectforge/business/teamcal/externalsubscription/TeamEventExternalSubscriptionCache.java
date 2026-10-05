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

package org.projectforge.business.teamcal.externalsubscription;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.common.DataobjectAccessType;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightId;
import org.projectforge.Constants;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.integration.RetryBackoff;
import org.projectforge.framework.integration.SyncStats;
import org.projectforge.framework.integration.SyncStatsRegistry;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.jpa.PfPersistenceService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
@Component
public class TeamEventExternalSubscriptionCache {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(TeamEventExternalSubscriptionCache.class);

    private static final long MINUTE = 60L * 1000;

    private static final long HOUR = 60 * MINUTE;

    private static final long DAY = 24 * HOUR;

    /**
     * Failing subscriptions are retried 3 times every 15 minutes, then hourly until one day after the first failure,
     * then daily. The tolerance fits the 15 minutes tick of {@link org.projectforge.business.jobs.ExternalCalendarSubscriptionJob}.
     */
    static final RetryBackoff RETRY_BACKOFF = new RetryBackoff(
            List.of(new RetryBackoff.Phase(15 * MINUTE, 45 * MINUTE), new RetryBackoff.Phase(HOUR, DAY)),
            DAY, 5 * MINUTE);

    private static final int MAX_ERROR_LENGTH_IN_NOTE = 500;

    /**
     * If at least this number of subscriptions and more than half of all subscriptions fail for more than a day, the
     * cause is probably on our side (bug, network): nothing is deactivated, and the administrators are informed.
     */
    static final int MIN_SYSTEMIC_FAILURES = 3;

    private static final int MAX_CALENDARS_IN_MAIL = 20;

    /**
     * Result of the update of one subscribed calendar.
     */
    public enum UpdateResult {UPDATED, FAILED, DEACTIVATION_DUE, SKIPPED}

    private Long lastSystemicFailureMail;

    private final Map<Long, TeamEventSubscription> subscriptions = new HashMap<>();

    private static final Long SUBSCRIPTION_UPDATE_TIME = 5L * 60 * 1000; // 5 min

    private transient TeamCalRight teamCalRight;

    private boolean initialized;

    private final SyncStats syncStats = SyncStatsRegistry.get("ical-subscriptions");

    /**
     * Subscriptions failing without interruption for this number of days are deactivated (0: never).
     */
    @Value("${projectforge.calendar.subscription.deactivateAfterDays:90}")
    private int deactivateAfterDays = 90;

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

    @Autowired
    private SendMail sendMail;

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
            // Start updateCache as may-be long-running thread. Avoids blocking of the caller (the calendar view).
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

            syncStats.execute(null, run -> run.step("calendars", counts -> {
                final List<TeamCalDO> deactivationCandidates = new ArrayList<>();
                for (final TeamCalDO calendar : subscribedCalendars) {
                    switch (updateCache(calendar)) {
                        case UPDATED -> counts.setUpdated(counts.getUpdated() + 1);
                        case FAILED -> counts.setErrors(counts.getErrors() + 1);
                        case DEACTIVATION_DUE -> deactivationCandidates.add(calendar);
                        // Not due (refresh interval or retry back-off) or skipped (e.g. deactivated owner).
                        case SKIPPED -> counts.setUnchanged(counts.getUnchanged() + 1);
                    }
                }
                final int deactivated = handleFailingSubscriptions(subscribedCalendars, deactivationCandidates,
                        System.currentTimeMillis());
                counts.setDeleted(deactivated);
                counts.setErrors(counts.getErrors() + deactivationCandidates.size() - deactivated);
                return null;
            }));

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

    public UpdateResult updateCache(final TeamCalDO calendar) {
        return updateCache(calendar, false);
    }

    /**
     * Failing subscriptions are retried less and less often (see {@link #RETRY_BACKOFF}). After
     * {@link #deactivateAfterDays} days of failures, {@link UpdateResult#DEACTIVATION_DUE} is returned: the
     * deactivation itself is done by {@link #updateCache()} (see {@link #handleFailingSubscriptions(List, List, long)}).
     * The start of the failures is persisted ({@link TeamCalDO#getExternalSubscriptionFailingSince()}), so this works
     * across restarts.
     *
     * @param calendar
     * @param force    If true then update is forced (independent of last update time, refresh interval and back-off).
     */
    public UpdateResult updateCache(final TeamCalDO calendar, final boolean force) {
        final Long calId = calendar.getId();
        if (calId == null) {
            log.error("Oups, calId is null (can't update subscription): " + calendar);
            return UpdateResult.SKIPPED;
        }
        final PFUserDO owner = userGroupCache.getUser(calendar.getOwnerId());
        if (owner == null) {
            log.error("Oups, owner is null (can't update subscription): " + calendar);
            return UpdateResult.SKIPPED;
        }
        if (owner.getDeactivated() || owner.getDeleted()) {
            // Don't update calendars of deactivated users....
            log.info("Ignoring subscriptions of deactivated/deleted owner #" + owner.getId() + " of calendar #" + calId);
            return UpdateResult.SKIPPED;
        }
        TeamEventSubscription teamEventSubscription = subscriptions.get(calId);
        if (teamEventSubscription == null) {
            teamEventSubscription = new TeamEventSubscription();
            subscriptions.put(calId, teamEventSubscription);
        }
        final long now = System.currentTimeMillis();
        final Date failingSince = calendar.getExternalSubscriptionFailingSince();
        if (!force) {
            if (failingSince != null) {
                if (!teamEventSubscription.isInitialized()) {
                    // E.g. after a restart: show the events last fetched until the next retry.
                    teamEventSubscription.loadFromDatabase(calendar);
                }
                // After a restart, the last attempt is unknown: the back-off then waits for the next retry.
                if (!RETRY_BACKOFF.isDue(failingSince.getTime(), teamEventSubscription.getLastFailedUpdate(), now)) {
                    return UpdateResult.SKIPPED;
                }
                log.info("Retrying subscribed calendar #" + calId + ", failing since (UTC) "
                        + DateHelper.formatAsUTC(failingSince) + ".");
            } else if (teamEventSubscription.isInitialized() && teamEventSubscription.getLastUpdated() != null) {
                final long interval = calendar.getExternalSubscriptionUpdateInterval() == null ? SUBSCRIPTION_UPDATE_TIME
                        : 1000L * calendar.getExternalSubscriptionUpdateInterval();
                if (teamEventSubscription.getLastUpdated() + interval > now) {
                    return UpdateResult.SKIPPED;
                }
            }
        }
        final Long lastFailedUpdateBefore = teamEventSubscription.getLastFailedUpdate();
        teamEventSubscription.update(teamCalDao, calendar);
        if (!calendar.getExternalSubscription() || StringUtils.isEmpty(calendar.getExternalSubscriptionUrl())) {
            // No subscription (anymore).
            return UpdateResult.SKIPPED;
        }
        final Long lastFailedUpdate = teamEventSubscription.getLastFailedUpdate();
        if (lastFailedUpdate == null || lastFailedUpdate.equals(lastFailedUpdateBefore)) {
            if (teamEventSubscription.getLastUpdated() == null) {
                return UpdateResult.SKIPPED;
            }
            if (failingSince != null) {
                log.info("Subscribed calendar #" + calId + " recovered, was failing since (UTC) "
                        + DateHelper.formatAsUTC(failingSince) + ".");
                setFailingSince(calendar, null);
            }
            return UpdateResult.UPDATED;
        }
        if (failingSince == null) {
            setFailingSince(calendar, new Date(lastFailedUpdate));
            return UpdateResult.FAILED;
        }
        if (deactivateAfterDays > 0 && now - failingSince.getTime() >= deactivateAfterDays * DAY) {
            return UpdateResult.DEACTIVATION_DUE;
        }
        return UpdateResult.FAILED;
    }

    /**
     * Deactivates the candidates, unless most of the subscriptions fail (probably a bug or a network problem on our
     * side): then nothing is deactivated, and the administrators are informed by mail (at most once a day).
     *
     * @param subscribedCalendars All subscribed calendars.
     * @param candidates          Subscriptions failing for more than {@link #deactivateAfterDays} days.
     * @return Number of deactivated subscriptions.
     */
    int handleFailingSubscriptions(final List<TeamCalDO> subscribedCalendars, final List<TeamCalDO> candidates,
                                   final long now) {
        final List<TeamCalDO> active = new ArrayList<>();
        final List<TeamCalDO> failingForADay = new ArrayList<>();
        for (final TeamCalDO calendar : subscribedCalendars) {
            final PFUserDO owner = userGroupCache.getUser(calendar.getOwnerId());
            if (!calendar.getExternalSubscription() || owner == null || owner.getDeactivated() || owner.getDeleted()) {
                continue;
            }
            active.add(calendar);
            final Date failingSince = calendar.getExternalSubscriptionFailingSince();
            if (failingSince != null && failingSince.getTime() + DAY <= now) {
                failingForADay.add(calendar);
            }
        }
        if (isSystemicFailure(failingForADay.size(), active.size())) {
            log.error(failingForADay.size() + " of " + active.size()
                    + " subscribed calendars are failing for more than a day. Probably a bug or a network problem,"
                    + " so no subscription is deactivated" + (candidates.isEmpty() ? "." : " (" + candidates.size()
                    + " candidates)."));
            notifyAdministrators(failingForADay, active.size(), now);
            return 0;
        }
        for (final TeamCalDO calendar : candidates) {
            deactivate(calendar);
        }
        return candidates.size();
    }

    static boolean isSystemicFailure(final int failing, final int total) {
        return failing >= MIN_SYSTEMIC_FAILURES && 2 * failing > total;
    }

    /**
     * Mail to the system administrator and feedback addresses (if configured), at most once a day.
     */
    private void notifyAdministrators(final List<TeamCalDO> failing, final int total, final long now) {
        if (lastSystemicFailureMail != null && lastSystemicFailureMail + DAY > now) {
            return;
        }
        lastSystemicFailureMail = now;
        final Set<String> recipients = getAdministratorRecipients();
        if (recipients.isEmpty()) {
            log.warn("No system administrator or feedback e-mail configured, can't inform about failing calendar subscriptions.");
            return;
        }
        try {
            sendMail.send(createSystemicFailureMail(recipients, failing, total));
        } catch (final Exception ex) {
            log.error("Can't send mail about failing calendar subscriptions: " + ex.getMessage(), ex);
        }
    }

    /**
     * The configured system administrator and feedback e-mail addresses (several addresses may be separated by
     * comma, semicolon or blanks).
     */
    static Set<String> getAdministratorRecipients() {
        final Set<String> recipients = new LinkedHashSet<>();
        for (final ConfigurationParam param : List.of(ConfigurationParam.SYSTEM_ADMIN_E_MAIL,
                ConfigurationParam.FEEDBACK_E_MAIL)) {
            final String value = Configuration.getInstance().getStringValue(param);
            if (StringUtils.isNotBlank(value)) {
                for (final String address : value.split("[,;\\s]+")) {
                    if (StringUtils.isNotBlank(address)) {
                        recipients.add(address.trim());
                    }
                }
            }
        }
        return recipients;
    }

    Mail createSystemicFailureMail(final Set<String> recipients, final List<TeamCalDO> failing, final int total) {
        final StringBuilder sb = new StringBuilder();
        sb.append(failing.size()).append(" of ").append(total)
                .append(" subscribed calendars are failing for more than a day.\n")
                .append("This looks like a bug or a network problem of ProjectForge, so no subscription is deactivated")
                .append(" automatically. Please check the log (TeamEventSubscription).\n\n");
        int counter = 0;
        for (final TeamCalDO calendar : failing) {
            if (++counter > MAX_CALENDARS_IN_MAIL) {
                sb.append("...\n");
                break;
            }
            final TeamEventSubscription subscription = subscriptions.get(calendar.getId());
            sb.append("#").append(calendar.getId())
                    .append(" (").append(calendar.getExternalSubscriptionUrlAnonymized()).append(")")
                    .append(", failing since (UTC) ").append(DateHelper.formatAsUTC(calendar.getExternalSubscriptionFailingSince()))
                    .append(": ").append(subscription != null && subscription.getLastErrorMessage() != null
                            ? subscription.getLastErrorMessage() : "-")
                    .append("\n");
        }
        final Mail msg = new Mail();
        recipients.forEach(msg::addTo);
        msg.setProjectForgeSubject("Subscribed calendars: " + failing.size() + " of " + total + " failing.");
        msg.setContent(sb.toString());
        msg.setContentType(Mail.CONTENTTYPE_TEXT);
        return msg;
    }

    private void setFailingSince(final TeamCalDO calendar, final Date failingSince) {
        teamCalDao.updateExternalSubscriptionFailingSince(calendar.getId(), failingSince);
        calendar.setExternalSubscriptionFailingSince(failingSince);
    }

    /**
     * Switches the subscription off (url and interval are kept, so the owner can switch it on again) and notes the
     * reason in the description.
     */
    private void deactivate(final TeamCalDO calendar) {
        final Long calId = calendar.getId();
        final Date failingSince = calendar.getExternalSubscriptionFailingSince();
        final PFUserDO owner = userGroupCache.getUser(calendar.getOwnerId());
        // Remove first: TeamCalDao.afterUpdate would otherwise clear the url of a cached subscription.
        final TeamEventSubscription subscription = subscriptions.remove(calId);
        final String lastError = subscription != null ? subscription.getLastErrorMessage() : null;
        final String note = I18nHelper.getLocalizedMessage(owner, "plugins.teamcal.externalsubscription.autoDeactivated",
                LocalDate.now().toString(),
                String.valueOf(deactivateAfterDays),
                calendar.getExternalSubscriptionUrlAnonymized(),
                StringUtils.abbreviate(StringUtils.defaultString(lastError, "-"),
                        MAX_ERROR_LENGTH_IN_NOTE));
        calendar.setDescription(appendNote(calendar.getDescription(), note, Constants.LENGTH_TEXT));
        calendar.setExternalSubscription(false);
        teamCalDao.update(calendar, false);
        log.warn("Subscribed calendar #" + calId + " deactivated, failing since (UTC) "
                + DateHelper.formatAsUTC(failingSince) + " (more than " + deactivateAfterDays + " days): " + calendar.getExternalSubscriptionUrlAnonymized());
    }

    /**
     * Appends the note to the description. If too long, the description is shortened, so the note is kept.
     */
    static String appendNote(final String description, final String note, final int maxLength) {
        if (StringUtils.isBlank(description)) {
            return StringUtils.abbreviate(note, maxLength);
        }
        final String separator = "\n\n";
        final int maxDescriptionLength = maxLength - note.length() - separator.length();
        if (maxDescriptionLength < 4) {
            return StringUtils.abbreviate(note, maxLength);
        }
        return StringUtils.abbreviate(description, maxDescriptionLength) + separator + note;
    }

    /**
     * @return The number of subscribed calendars, e.g. whether the subscriptions are used at all.
     */
    public int getSubscriptionCount() {
        init();
        return subscriptions.size();
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
