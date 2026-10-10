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

package org.projectforge.business.jobs;

import jakarta.annotation.PostConstruct;
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache;
import org.projectforge.common.scheduling.SchedulerJobArea;
import org.projectforge.common.scheduling.SchedulerJobDefinition;
import org.projectforge.common.scheduling.SchedulerJobRunner;
import org.projectforge.common.scheduling.SchedulerSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExternalCalendarSubscriptionJob {
    private static final String CRON = "${projectforge.cron.externalCalendar}";

    public static final SchedulerJobDefinition SCHEDULER_JOB = new SchedulerJobDefinition(
            "calendar.externalSubscriptions", SchedulerJobArea.INTEGRATION, ExternalCalendarSubscriptionJob.class,
            "execute", new SchedulerSchedule.Cron(CRON));

    @Autowired
    private TeamEventExternalSubscriptionCache teamEventExternalSubscriptionCache;

    @Autowired
    private SchedulerJobRunner schedulerJobRunner;

    @PostConstruct
    private void postConstruct() {
        schedulerJobRunner.register(SCHEDULER_JOB, this::execute, null);
    }

    //@Scheduled(cron = "0 */15 * * * *")
    @Scheduled(cron = CRON)
    public void execute() {
        // Exceptions are logged by the scheduler (job failed).
        schedulerJobRunner.runAsync(SCHEDULER_JOB, run -> teamEventExternalSubscriptionCache.updateCache());
    }

}
