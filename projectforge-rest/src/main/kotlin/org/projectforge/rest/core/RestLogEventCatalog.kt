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

import jakarta.annotation.PostConstruct
import org.projectforge.common.logging.LogEventRegistry
import org.projectforge.rest.dvelop.DvelopLogEvents
import org.projectforge.rest.sipgate.SipgateLogEvents
import org.springframework.stereotype.Component

/**
 * Registers the log events of the rest module in the [LogEventRegistry].
 */
@Component
class RestLogEventCatalog {
    @PostConstruct
    internal fun init() {
        LogEventRegistry.register(*HOLDERS)
    }

    companion object {
        /** New `*LogEvents` objects of this module belong here. */
        val HOLDERS = arrayOf(
            RestLogEvents,
            SipgateLogEvents,
            DvelopLogEvents,
        )
    }
}
