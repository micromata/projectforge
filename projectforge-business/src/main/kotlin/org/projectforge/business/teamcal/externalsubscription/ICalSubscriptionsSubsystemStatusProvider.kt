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

package org.projectforge.business.teamcal.externalsubscription

import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/** The subscribed external calendars (iCal), if there are any. */
@Component
class ICalSubscriptionsSubsystemStatusProvider : SubsystemStatusProvider {
    @Autowired
    private lateinit var subscriptionCache: TeamEventExternalSubscriptionCache

    override val id = "icalSubscriptions"

    override val titleKey = "system.admin.adminErrors.subsystem.icalSubscriptions"

    override val problems = SubsystemProblemMatch(
        locationPrefixes = listOf("TeamEventExternalSubscription", "TeamEventSubscription"),
        syncTypes = listOf("ical-subscriptions"),
    )

    override fun status(): SubsystemStatus? {
        val count = subscriptionCache.subscriptionCount.takeIf { it > 0 } ?: return null
        val detail = translateMsg("system.admin.adminErrors.subsystems.calendars", count)
        return SubsystemStatus.ofSyncs(problems.syncTypes, detail)
    }
}
