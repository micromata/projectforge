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

package org.projectforge.business.lanesandplanes

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.SystemStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["projectforge.lanesandplanes.enabled"], havingValue = "true")
class LanesAndPlanesScheduler(
    private val syncService: LanesAndPlanesSyncService,
    private val systemStatus: SystemStatus,
) {
    /**
     * The push is only sent, if anything changed since the last push (see [LanesAndPlanesSyncService.push]).
     * Not before [SystemStatus.upAndRunning], see GatewaySyncScheduler.
     */
    @Scheduled(
        fixedDelayString = "\${projectforge.lanesandplanes.syncIntervalMs:3600000}",
        initialDelayString = "\${projectforge.lanesandplanes.initialDelayMs:300000}",
    )
    fun scheduledPush() {
        if (!systemStatus.upAndRunning) {
            log.info { "Lanes & Planes push skipped: ProjectForge is not up and running yet." }
            return
        }
        syncService.push()
    }
}
