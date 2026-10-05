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

package org.projectforge.business.sipgate

import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemState
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Sipgate, if configured: the contact sync (`SipgateContactSyncService`, only with the cron active) and the direct
 * calls. Without the cron, the state is the one of its errors.
 */
@Component
class SipgateSubsystemStatusProvider : SubsystemStatusProvider {
    @Autowired
    private lateinit var sipgateConfiguration: SipgateConfiguration

    override val id = "sipgate"

    override val titleKey = "system.admin.adminErrors.subsystem.sipgate"

    override val problems = SubsystemProblemMatch(
        codePrefixes = listOf("sipgate."),
        locationPrefixes = listOf("Sipgate"),
        syncTypes = listOf("sipgate"),
    )

    override fun status(): SubsystemStatus? {
        if (!sipgateConfiguration.isConfigured()) {
            return null
        }
        val detail = SubsystemStatus.hostOf(sipgateConfiguration.baseUri)
        return if (sipgateConfiguration.cronActive) {
            SubsystemStatus.ofSyncs(problems.syncTypes, detail)
        } else {
            SubsystemStatus(SubsystemState.OK, detail)
        }
    }
}
