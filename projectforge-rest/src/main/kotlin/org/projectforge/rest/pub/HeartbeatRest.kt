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

package org.projectforge.rest.pub

import org.projectforge.SystemStatus
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public liveness check, available in every mode (main instance and gateway): e. g. the main instance checks the
 * gateway with it before a sync, monitoring may use it as well. Without any details of version etc.
 */
@RestController
@RequestMapping(Rest.PUBLIC_URL)
class HeartbeatRest(
    private val systemStatus: SystemStatus,
    @Value("\${projectforge.gateway.enabled:false}") private val gatewayMode: Boolean,
) {
    data class Heartbeat(
        /** [STATUS_UP] or [STATUS_STARTING]. */
        val status: String,
        /** "gateway" or "main", so a client can detect a sync url pointing to the wrong instance. */
        val mode: String,
    )

    /**
     * @return 200 if ProjectForge is up and running, 503 while it's still starting.
     */
    @GetMapping("heartbeat")
    fun heartbeat(): ResponseEntity<Heartbeat> {
        val mode = if (gatewayMode) MODE_GATEWAY else MODE_MAIN
        return if (systemStatus.upAndRunning) {
            ResponseEntity.ok(Heartbeat(STATUS_UP, mode))
        } else {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Heartbeat(STATUS_STARTING, mode))
        }
    }

    companion object {
        const val URL = "${Rest.PUBLIC_URL}/heartbeat"
        const val STATUS_UP = "UP"
        const val STATUS_STARTING = "STARTING"
        const val MODE_GATEWAY = "gateway"
        const val MODE_MAIN = "main"
    }
}
