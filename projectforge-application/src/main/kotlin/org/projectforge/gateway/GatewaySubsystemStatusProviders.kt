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

package org.projectforge.gateway

import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.projectforge.gateway.push.GatewaySyncPushConfig
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** The push of users, addresses and calendars to a gateway (`projectforge.gateway.push.enabled`). */
@Component
class GatewayPushSubsystemStatusProvider : SubsystemStatusProvider {
    @Autowired
    private lateinit var pushConfig: ObjectProvider<GatewaySyncPushConfig>

    override val id = "gatewayPush"

    override val titleKey = "system.admin.adminErrors.subsystem.gatewayPush"

    override val problems = SubsystemProblemMatch(
        codePrefixes = listOf("gateway.push."),
        locationPrefixes = listOf("GatewaySyncPush", "GatewaySyncScheduler"),
        syncTypes = listOf("gateway-push"),
    )

    override fun status(): SubsystemStatus? {
        val config = pushConfig.ifAvailable ?: return null
        return SubsystemStatus.ofSyncs(problems.syncTypes, detail = SubsystemStatus.hostOf(config.url))
    }
}

/** This instance as a gateway (`projectforge.gateway.enabled`): receives the pushes and serves the gateway pages. */
@Component
class GatewayReceiveSubsystemStatusProvider : SubsystemStatusProvider {
    @Value("\${projectforge.gateway.enabled:false}")
    private var enabled: Boolean = false

    override val id = "gatewayReceive"

    override val titleKey = "system.admin.adminErrors.subsystem.gatewayReceive"

    override val problems = SubsystemProblemMatch(
        locationPrefixes = listOf(
            "GatewaySyncController", "GatewaySyncService", "GatewayIcs", "GatewayPage", "GatewaySession",
            "GatewayEndpoint", "GatewaySecurity",
        ),
        syncTypes = listOf("gateway-receive-"),
    )

    override fun status(): SubsystemStatus? = if (enabled) SubsystemStatus.ofSyncs(problems.syncTypes) else null
}
