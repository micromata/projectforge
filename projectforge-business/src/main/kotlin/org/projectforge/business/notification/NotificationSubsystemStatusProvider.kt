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


package org.projectforge.business.notification

import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemState
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Date

/**
 * The engine of the notification system ([NotificationService]) on the problem dashboard: the runs of the rule job
 * and of the delivery job, the active rules, the pending notifications and the due deliveries. Degraded, if the
 * delivery job hasn't run for [MAX_DELIVERY_JOB_SILENCE_MS] (it runs every 15 minutes).
 */
@Component
class NotificationSubsystemStatusProvider : SubsystemStatusProvider {
    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var notificationRuleDao: NotificationRuleDao

    @Autowired
    private lateinit var notificationService: NotificationService

    override val id = "notification"

    override val titleKey = "system.admin.adminErrors.subsystem.notification"

    override val problems = SubsystemProblemMatch(
        codePrefixes = listOf("notification."),
        locationPrefixes = listOf("Notification"),
    )

    override fun status(): SubsystemStatus? {
        val now = Date()
        val syncs = listOfNotNull(notificationService.ruleJobStats.toSync(), notificationService.deliveryJobStats.toSync())
        val detail = translateMsg(
            "system.admin.adminErrors.subsystem.notification.detail",
            notificationRuleDao.selectActive().size,
            notificationDao.countPending(),
            notificationDao.countDueDeliveries(now),
        )
        var state = syncs.maxOfOrNull { SubsystemStatus.stateOf(it.lastStatus) } ?: SubsystemState.UNKNOWN
        val lastDelivery = notificationService.deliveryJobStats.lastRunStart
        if (lastDelivery != null && now.time - lastDelivery > MAX_DELIVERY_JOB_SILENCE_MS && state < SubsystemState.DEGRADED) {
            state = SubsystemState.DEGRADED
        }
        return SubsystemStatus(state, detail, syncs)
    }

    companion object {
        /** Three runs of the delivery job missed. */
        const val MAX_DELIVERY_JOB_SILENCE_MS = 3 * 15 * 60_000L
    }
}
