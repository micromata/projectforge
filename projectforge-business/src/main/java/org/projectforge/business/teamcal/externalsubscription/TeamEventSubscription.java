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

package org.projectforge.business.teamcal.externalsubscription;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.ical.ICalParser;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.time.DateHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Holds and updates events of a subscribed calendar.
 *
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class TeamEventSubscription implements Serializable {
  private static final long serialVersionUID = -9200146874015146227L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamEventSubscription.class);

  private Long teamCalId;

  private boolean initialized = false;

  private SubscriptionHolder subscription;

  private List<TeamEventDO> recurrenceEvents;

  private String currentInitializedHash;

  private Long lastUpdated, lastFailedUpdate;

  private int numberOfFailedUpdates = 0;

  private String lastErrorMessage;

  private static final Long TIME_IN_THE_PAST = 60L * 24 * 60 * 60 * 1000; // 60 days in millis in the past to subscribe

  public TeamEventSubscription() {
  }

  /**
   * @return the lastErrorMessage
   */
  public String getLastErrorMessage() {
    return lastErrorMessage;
  }

  /**
   * @return the numberOfFailedUpdates
   */
  public int getNumberOfFailedUpdates() {
    return numberOfFailedUpdates;
  }

  /**
   * @return the lastFailedUpdate
   */
  public Long getLastFailedUpdate() {
    return lastFailedUpdate;
  }

  /**
   * We update the cache softly, therefore we create a new instance and replace the old instance in the cached map then
   * creation and update is therefore the same two lines of code, but semantically different things.
   */
  public void update(final TeamCalDao teamCalDao, final TeamCalDO teamCalDO) {
    this.teamCalId = teamCalDO.getId();
    currentInitializedHash = null;
    lastUpdated = null;
    this.initialized = true;
    String url = teamCalDO.getExternalSubscriptionUrl();
    if (!teamCalDO.getExternalSubscription() || StringUtils.isEmpty(url)) {
      // No external subscription.
      clear();
      return;
    }
    url = StringUtils.replace(url, "webcal", "http");
    final String displayUrl = teamCalDO.getExternalSubscriptionUrlAnonymized();
    log.info("Getting subscribed calendar #" + teamCalDO.getId() + " from: " + displayUrl);
    byte[] bytes = null;

    // Create a method instance.
    try (final CloseableHttpClient client = HttpClients.createDefault()) {
      final HttpGet method = new HttpGet(url);

      bytes = client.execute(method, response -> {
        final int statusCode = response.getCode();
        if (statusCode != HttpStatus.SC_OK) {
          error("Unable to gather subscription calendar #"
              + teamCalDO.getId()
              + " information, using database from url '"
              + displayUrl
              + "'. Received statusCode: "
              + statusCode, null);
          return null;
        }
        final HttpEntity responseEntity = response.getEntity();
        if (responseEntity == null) {
          return null;
        }
        try (InputStream inputStream = responseEntity.getContent()) {
          return IOUtils.toByteArray(inputStream);
        }
      });
    } catch (IOException ex) {
      log.error(ex.getMessage());
      return;
    }
    if (bytes == null) {
      return;
    }

    try {

      final MessageDigest md = MessageDigest.getInstance("MD5");

      final String md5 = calcHexHash(md.digest(bytes));
      if (!StringUtils.equals(md5, teamCalDO.getExternalSubscriptionHash())) {
        teamCalDO.setExternalSubscriptionHash(md5);
        teamCalDO.setExternalSubscriptionCalendarBinary(bytes);
        teamCalDO.setMinorChange(true); // Don't need to re-index (failed).
        // internalUpdate is valid at this point, because we are calling this method in an async thread
        teamCalDao.update(teamCalDO, false);
      }
    } catch (final Exception e) {
      bytes = teamCalDO.getExternalSubscriptionCalendarBinary();
      error("Unable to gather subscription calendar #"
          + teamCalDO.getId()
          + " information, using database from url '"
          + displayUrl
          + "': "
          + e.getMessage());
    }
    if (bytes == null) {
      error("Unable to use database subscription calendar #" + teamCalDO.getId() + " information, quit from url '"
              + displayUrl + "'.",
          null);
      return;
    }
    if (currentInitializedHash != null
        && StringUtils.equals(currentInitializedHash, teamCalDO.getExternalSubscriptionHash())) {
      // nothing to do here if the hashes are equal
      log.info("No modification of subscribed calendar #" + teamCalDO.getId() + " found from: " + displayUrl
          + " (OK, nothing to be done).");
      clear();
      return;
    }

    final SubscriptionHolder newSubscription = new SubscriptionHolder();
    final ArrayList<TeamEventDO> newRecurrenceEvents = new ArrayList<>();
    try {
      final Date timeInPast = new Date(System.currentTimeMillis() - TIME_IN_THE_PAST);
      Long startId = -1L;
      ICalParser parser = new ICalParser();

      // the event id must (!) be negative and decrementing (different on each event)
      for (TeamEventDO event : parser.parse(bytes)) {
        if (event.getStartDate().getTime() < timeInPast.getTime() && event.getRecurrenceRule() == null) {
          continue;
        }
        event.setId(startId);
        event.setCalendar(teamCalDO);

        if (event.hasRecurrence()) {
          // special treatment for recurrence events ..
          newRecurrenceEvents.add(event);
        } else {
          newSubscription.add(event);
        }

        startId--;
      }

      // OK, update the subscription:
      recurrenceEvents = newRecurrenceEvents;
      subscription = newSubscription;
      lastUpdated = System.currentTimeMillis();
      currentInitializedHash = teamCalDO.getExternalSubscriptionHash();
      clear();
      log.info("Subscribed calendar #" + teamCalDO.getId() + " successfully received from: " + displayUrl);
    } catch (final Exception e) {
      error("Unable to instantiate team event list for calendar #"
          + teamCalDO.getId()
          + " information, quit from url '"
          + displayUrl
          + "': "
          + e.getMessage(), e);
    }
  }

  private void clear() {
    this.lastErrorMessage = null;
    this.lastFailedUpdate = null;
    this.numberOfFailedUpdates = 0;
    this.initialized = true;
  }

  private void error(final String errorMessage) {
    error(errorMessage, null);
  }

  private void error(final String errorMessage, final Exception ex) {
    this.numberOfFailedUpdates++;
    final StringBuilder sb = new StringBuilder();
    sb.append(errorMessage).append(" (").append(this.numberOfFailedUpdates).append(". failed attempts");
    if (this.lastUpdated != null) {
      sb.append(", last successful update ").append(DateHelper.formatAsUTC(new Date(this.lastUpdated))).append(" UTC");
    }
    sb.append(".)");
    this.lastErrorMessage = sb.toString();
    if (ex != null) {
      log.error(this.lastErrorMessage, ex);
    } else {
      log.error(this.lastErrorMessage);
    }
    this.lastFailedUpdate = System.currentTimeMillis();
  }

  /**
   * calculates hexadecimal representation of
   *
   * @param md5
   * @return
   */
  private String calcHexHash(final byte[] md5) {
    String result = null;
    if (md5 != null) {
      result = new BigInteger(1, md5).toString(16);
    }
    return result;
  }


  public TeamEventDO getEvent(final String uid) {
    if (StringUtils.isEmpty(uid)) {
      return null;
    }
    if (subscription == null && recurrenceEvents == null) {
      return null;
    }
    TeamEventDO teamEvent = subscription.getEvent(uid);
    if (teamEvent != null) {
      return teamEvent;
    }
    for (final TeamEventDO teamEventDO : recurrenceEvents) {
      if (teamEventDO.getUid() != null && Objects.equals(uid, teamEventDO.getUid()))
        return teamEventDO;
    }
    return null;
  }

  public List<TeamEventDO> getEvents(final Long startTime, final Long endTime, final boolean minimalAccess) {
    if (subscription == null) {
      return new ArrayList<>();
    }
    // final Long perfStart = System.currentTimeMillis();
    final List<TeamEventDO> result = subscription.getResultList(startTime, endTime, minimalAccess);
    // final Long perfDuration = System.currentTimeMillis() - perfStart;
    // log.info("calculation of team events took "
    // + perfDuration
    // + " ms for "
    // + result.size()
    // + " events of "
    // + eventDurationAccess.size()
    // + " in total from calendar #"
    // + teamCalId
    // + ".");
    return result;
  }

  public Long getTeamCalId() {
    return teamCalId;
  }

  /**
   * @return Time of last update (successfully).
   */
  public Long getLastUpdated() {
    return lastUpdated;
  }

  public List<TeamEventDO> getRecurrenceEvents() {
    return recurrenceEvents;
  }

  public boolean isInitialized() {
    return initialized;
  }

  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }
}
