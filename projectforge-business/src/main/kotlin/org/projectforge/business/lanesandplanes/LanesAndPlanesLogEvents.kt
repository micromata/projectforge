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
}
