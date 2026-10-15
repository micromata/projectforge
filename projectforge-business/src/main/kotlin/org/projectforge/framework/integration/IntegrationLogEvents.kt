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

package org.projectforge.framework.integration

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object IntegrationLogEvents {
    /**
     * Only counted: the cause is logged (and classified) where it happens, and the support error digest lists the
     * failed runs in its own section (`SyncProblemTracker`).
     */
    @JvmField
    val SYNC_RUN_FAILED = LogEvent(
        code = "integration.sync.runFailed",
        category = LogCategory.EXTERNAL,
        notify = LogNotify.NONE,
        explanation = "The summary of a sync run that was aborted or had errors (the sync type is in the message).",
        action = "See the log messages logged before by the sync for the cause.",
    )

    /**
     * Logged at most once a day (TeamEventExternalSubscriptionCache), not on every update run.
     */
    @JvmField
    val ICAL_SUBSCRIPTIONS_SYSTEMIC_FAILURE = LogEvent(
        code = "ical.subscriptions.systemicFailure",
        category = LogCategory.EXTERNAL,
        explanation = "Most of the subscribed calendars are failing for more than a day. Probably a network problem " +
                "(proxy, DNS, firewall) or a bug on our side, so no failing subscription is deactivated.",
        action = "See the log messages 'Unable to gather subscription calendar #...' for the cause. If the calendars " +
                "are really gone, ask their owners to remove the subscriptions.",
    )

    /**
     * The sync runs on every refresh of the user group cache, so a failure while the IdP restarts is repeated soon.
     */
    @JvmField
    val IDP_SYNC_FAILED = LogEvent(
        code = "idp.sync.failed",
        category = LogCategory.EXTERNAL,
        threshold = 3,
        explanation = "The sync of users and groups to the identity provider (IdP master mode) failed, e.g. with " +
                "502/503 while the IdP restarts or with a closed connection pool while ProjectForge shuts down. " +
                "Reported only from 3 failures per digest period on.",
        action = "Check that the IdP is up and reachable and that its admin token is valid (projectforge.idp.*).",
    )
}
