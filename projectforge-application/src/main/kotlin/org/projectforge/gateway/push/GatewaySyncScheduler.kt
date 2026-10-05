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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.SystemStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["projectforge.gateway.push.enabled"], havingValue = "true")
class GatewaySyncScheduler(
    private val pushService: GatewaySyncPushService,
    private val systemStatus: SystemStatus,
) {
    /**
     * Delta sync: only changed addresses and calendars (users and groups are always pushed completely).
     */
    @Scheduled(
        fixedDelayString = "\${projectforge.gateway.push.syncIntervalMs:900000}",
        initialDelayString = "\${projectforge.gateway.push.initialDelayMs:120000}",
    )
    fun scheduledSync() {
        if (!ready()) return
        log.info { "Starting scheduled gateway sync..." }
        pushService.pushAll()
    }

    /**
     * Nightly full sync: pushes everything, the gateway removes data deleted on the main instance.
     */
    @Scheduled(cron = "\${projectforge.gateway.push.fullSyncCron:0 0 3 * * *}")
    fun scheduledFullSync() {
        if (!ready()) return
        log.info { "Starting scheduled full gateway sync..." }
        pushService.pushAll(fullSync = true)
    }

    /**
     * Without an initial delay a fixed-delay job runs as soon as the scheduler starts, i.e. before
     * [org.springframework.boot.context.event.ApplicationReadyEvent] — before plugins and WicketSupport are
     * registered (NPE in the access checks). A long start-up (production database) can outlast the initial
     * delay, hence the check as well.
     */
    private fun ready(): Boolean {
        if (!systemStatus.upAndRunning) {
            log.info { "Gateway sync skipped: ProjectForge is not up and running yet." }
            return false
        }
        return true
    }
}
