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

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent

object LanesAndPlanesLogEvents {
    @JvmField
    val PUSH_FAILED = LogEvent(
        code = "lanesAndPlanes.push.failed",
        category = LogCategory.EXTERNAL,
        explanation = "The user import of Lanes & Planes (projectforge.lanesandplanes.url) rejected the push or isn't " +
                "reachable: new employees and changed cost units aren't available in Lanes & Planes.",
        action = "401/403: check projectforge.lanesandplanes.apiKey. 422: see the user import log in the Lanes & " +
                "Planes company account. Otherwise the push is repeated on the next run.",
    )

    @JvmField
    val PUSH_REFUSED = LogEvent(
        code = "lanesAndPlanes.push.refused",
        category = LogCategory.CONFIG,
        explanation = "The push to Lanes & Planes wasn't sent, because the configuration is incomplete or the user " +
                "list is suspiciously small (a push deactivates every user not sent).",
        action = "See the message. Check projectforge.lanesandplanes.* and the active employees (with email).",
    )

    @JvmField
    val EMAIL_CHANGED = LogEvent(
        code = "lanesAndPlanes.emailChanged",
        category = LogCategory.CONFIG,
        explanation = "The email of an employee changed in ProjectForge. Lanes & Planes identifies users by their " +
                "email only: without a change by hand in Lanes & Planes, the push would deactivate the user and " +
                "create a new one with the default role. So the old email is still sent.",
        action = "Change the email of the user in Lanes & Planes by hand, then add the new email to the confirmed " +
                "email changes of the configuration parameter 'lanesAndPlanes'.",
    )

    @JvmField
    val STATE_MISSING = LogEvent(
        code = "lanesAndPlanes.stateMissing",
        category = LogCategory.CONFIG,
        explanation = "The state of the last push to Lanes & Planes (lanes-and-planes-state.json in the work " +
                "directory) is missing or not readable. Without it, a changed email of an employee isn't held " +
                "back (L&P would deactivate the user and create a new one) and a dropped number of users isn't " +
                "detected.",
        action = "Expected on the very first push only. Otherwise restore the file from a backup (e.g. after " +
                "moving the work directory) or check the emails changed since the last push in Lanes & Planes.",
    )
}
