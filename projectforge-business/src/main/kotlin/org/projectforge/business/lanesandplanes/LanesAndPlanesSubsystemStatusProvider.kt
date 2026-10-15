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

import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/** The push of the employees with their cost units to Lanes & Planes (`projectforge.lanesandplanes.enabled`). */
@Component
class LanesAndPlanesSubsystemStatusProvider : SubsystemStatusProvider {
    @Autowired
    private lateinit var config: ObjectProvider<LanesAndPlanesConfig>

    override val id = "lanesAndPlanes"

    override val titleKey = "system.admin.adminErrors.subsystem.lanesAndPlanes"

    override val problems = SubsystemProblemMatch(
        codePrefixes = listOf("lanesAndPlanes."),
        locationPrefixes = listOf("LanesAndPlanes"),
        syncTypes = listOf(LanesAndPlanesSyncService.SYNC_TYPE),
    )

    override fun status(): SubsystemStatus? {
        val config = config.ifAvailable ?: return null
        return SubsystemStatus.ofSyncs(problems.syncTypes, detail = SubsystemStatus.hostOf(config.url))
    }
}
