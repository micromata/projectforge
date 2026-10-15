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

package org.projectforge.rest.core

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object RestLogEvents {
    @JvmField
    val SYSTEM_NOT_AVAILABLE = LogEvent(
        code = "rest.systemNotAvailable",
        category = LogCategory.CONFIG,
        notify = LogNotify.NONE,
        explanation = "A rest call was denied, because the system isn't up and running (start-up phase or maintenance mode).",
        action = "Nothing to do during start-up. Otherwise check the log of the start-up for errors.",
    )

    @JvmField
    val SERVER_ERROR_RESPONSE = LogEvent(
        code = "rest.serverErrorResponse",
        category = LogCategory.BUG,
        notify = LogNotify.NONE,
        explanation = "A rest call was answered with a status 5xx. The cause itself is reported by the exception handler.",
    )

    @JvmField
    val AUTH_FAILED = LogEvent(
        code = "rest.authenticationFailed",
        category = LogCategory.SECURITY,
        threshold = 20,
        explanation = "Many rest calls failed to authenticate (wrong or expired token, unknown user). " +
                "Reported only from 20 failures per digest period on.",
        action = "Check the security log for the ip addresses and users: a misconfigured client or a brute force attack?",
    )

    @JvmField
    val NO_CREDENTIALS = LogEvent(
        code = "rest.noCredentials",
        category = LogCategory.CLIENT,
        explanation = "A rest call without session or token, mostly an expired session of an open browser tab.",
    )

    @JvmField
    val CALENDAR_SUBSCRIPTION_BAD_REQUEST = LogEvent(
        code = "rest.calendarSubscription.badRequest",
        category = LogCategory.CLIENT,
        explanation = "A calendar subscription asked for time sheets of an unknown user or of another user.",
    )
}
