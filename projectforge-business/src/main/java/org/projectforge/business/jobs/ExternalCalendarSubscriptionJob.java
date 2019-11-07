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

package org.projectforge.business.jobs;

import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExternalCalendarSubscriptionJob
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(ExternalCalendarSubscriptionJob.class);

  @Autowired
  private TeamEventExternalSubscriptionCache teamEventExternalSubscriptionCache;

  //@Scheduled(cron = "0 */15 * * * *")
  @Scheduled(cron = "${projectforge.cron.externalCalendar}")
  public void execute()
  {
    log.info("External calendar subscriptions job started.");
    try {
      teamEventExternalSubscriptionCache.updateCache();
    } catch (final Throwable ex) {
      log.error("Exception while executing ExternalCalendarSubscriptionJob: " + ex.getMessage());
    }
    log.info("External calendar subscriptions job finished.");
  }

}
