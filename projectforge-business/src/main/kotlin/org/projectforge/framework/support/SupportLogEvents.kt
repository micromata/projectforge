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

package org.projectforge.framework.support

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent

/**
 * The generic events [ErrorOccurrenceFactory] classifies errors with if neither the logging call nor the exception
 * gives an event. Their code doesn't identify a problem: the occurrences are grouped by exception, location and
 * message.
 */
object SupportLogEvents {
    @JvmField
    val EXTERNAL_UNREACHABLE = LogEvent(
        code = "support.externalUnreachable",
        category = LogCategory.EXTERNAL,
        explanation = "A connection to a remote system failed or timed out (detected from the message or exception).",
        action = "Check whether the system named in the message is up and reachable from this server (network, firewall, proxy, credentials).",
    )

    @JvmField
    val REQUEST_ERROR = LogEvent(
        code = "support.requestError",
        category = LogCategory.BUG,
        explanation = "An unexpected exception in a request of a logged-in user.",
        action = "Analyze the stack trace in the attachment; the request id in the log file leads to all log lines of the request.",
    )

    @JvmField
    val LOGGED_ERROR = LogEvent(
        code = "support.loggedError",
        category = LogCategory.BUG,
        explanation = "An error logged without classification (e.g. a caught exception in a background job).",
        action = "Analyze message and stack trace. If it isn't a bug, classify the logging call with a LogEvent.",
    )
}
