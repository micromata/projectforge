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

package org.projectforge.gateway.push

import jakarta.annotation.PostConstruct
import org.projectforge.SystemStatus
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["projectforge.gateway.push.enabled"], havingValue = "true")
class GatewaySyncScheduler(
    private val pushService: GatewaySyncPushService,
    private val systemStatus: SystemStatus,
    private val schedulerJobRunner: SchedulerJobRunner,
) {
    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(DELTA_JOB, ::scheduledSync, ::inactiveReason)
        schedulerJobRunner.register(FULL_JOB, ::scheduledFullSync, ::inactiveReason)
    }

    /**
     * Delta sync: only changed addresses and calendars (users and groups are always pushed completely).
     */
    @Scheduled(fixedDelayString = SYNC_INTERVAL, initialDelayString = INITIAL_DELAY)
    fun scheduledSync() {
        schedulerJobRunner.run(DELTA_JOB) { pushService.pushAll() }
    }

    /**
     * Nightly full sync: pushes everything, the gateway removes data deleted on the main instance.
     */
    @Scheduled(cron = FULL_SYNC_CRON)
    fun scheduledFullSync() {
        schedulerJobRunner.run(FULL_JOB) { pushService.pushAll(fullSync = true) }
    }

    /**
     * Without an initial delay a fixed-delay job runs as soon as the scheduler starts, i.e. before
     * [org.springframework.boot.context.event.ApplicationReadyEvent] — before plugins and WicketSupport are
     * registered (NPE in the access checks). A long start-up (production database) can outlast the initial
     * delay, hence the check as well (the job is inactive then, the run isn't counted).
     */
    private fun inactiveReason(): String? =
        if (systemStatus.upAndRunning) null else "ProjectForge is not up and running yet."

    companion object {
        private const val SYNC_INTERVAL = "\${projectforge.gateway.push.syncIntervalMs:900000}"
        private const val INITIAL_DELAY = "\${projectforge.gateway.push.initialDelayMs:120000}"
        private const val FULL_SYNC_CRON = "\${projectforge.gateway.push.fullSyncCron:0 0 3 * * *}"

        val DELTA_JOB = SchedulerJobDefinition(
            "gateway.pushDelta", SchedulerJobArea.INTEGRATION, GatewaySyncScheduler::class.java, "scheduledSync",
            SchedulerSchedule.FixedDelay(SYNC_INTERVAL, INITIAL_DELAY),
        )

        val FULL_JOB = SchedulerJobDefinition(
            "gateway.pushFull", SchedulerJobArea.INTEGRATION, GatewaySyncScheduler::class.java, "scheduledFullSync",
            SchedulerSchedule.Cron(FULL_SYNC_CRON),
        )
    }
}
