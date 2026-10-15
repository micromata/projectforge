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

package org.projectforge.gateway.push

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object GatewayLogEvents {
    @JvmField
    val UNREACHABLE = LogEvent(
        code = "gateway.push.unreachable",
        category = LogCategory.EXTERNAL,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "The gateway (projectforge.gateway.push.url) doesn't answer its heartbeat: users, addresses " +
                "and calendars aren't pushed, the gateway serves outdated data.",
        action = "Check whether the gateway is running and reachable from this server. The sync retries on every " +
                "run and pushes everything once the gateway is back.",
    )

    @JvmField
    val NOT_A_GATEWAY = LogEvent(
        code = "gateway.push.notAGateway",
        category = LogCategory.CONFIG,
        explanation = "The push url answers, but not as a gateway (e.g. a normal ProjectForge instance): nothing is pushed.",
        action = "Correct projectforge.gateway.push.url, or enable the gateway mode of the target " +
                "(projectforge.gateway.enabled=true).",
    )
}
