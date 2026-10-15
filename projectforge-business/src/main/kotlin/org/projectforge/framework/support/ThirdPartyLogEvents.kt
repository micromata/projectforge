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
import org.projectforge.common.logging.LogNotify

/**
 * Known messages of libraries, whose logging calls can't carry a [LogEvent]: classified by logger and text
 * ([find], used by [ErrorOccurrenceFactory]).
 */
object ThirdPartyLogEvents {
    @JvmField
    val NETTY_MACOS_DNS = LogEvent(
        code = "thirdParty.netty.macosDns",
        category = LogCategory.CONFIG,
        notify = LogNotify.NONE,
        explanation = "Netty's native DNS resolver for macOS isn't on the classpath, the system defaults are used. " +
                "Occurs only on macOS, i.e. on development machines.",
        action = "Nothing, as long as host names are resolved. Otherwise add io.netty:netty-resolver-dns-native-macos.",
    )

    private class Rule(val loggerPrefix: String, val messagePart: String, val event: LogEvent)

    private val RULES = listOf(
        Rule("io.netty.resolver.dns.", "MacOSDnsServerAddressStreamProvider", NETTY_MACOS_DNS),
    )

    /** The event of a known message, else null. */
    fun find(loggerName: String?, message: String?): LogEvent? {
        loggerName ?: return null
        message ?: return null
        return RULES.firstOrNull { loggerName.startsWith(it.loggerPrefix) && message.contains(it.messagePart) }?.event
    }
}
