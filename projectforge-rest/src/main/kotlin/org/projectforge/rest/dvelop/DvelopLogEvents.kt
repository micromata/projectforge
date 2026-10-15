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

package org.projectforge.rest.dvelop

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent

object DvelopLogEvents {
    @JvmField
    val CALL_FAILED = LogEvent(
        code = "dvelop.callFailed",
        category = LogCategory.EXTERNAL,
        explanation = "A call of the d.velop API was answered with an error status. The synchronization of this entry failed.",
        action = "Check the status in the message: 401/403 means the api key (projectforge.dvelop.*) is invalid, " +
                "5xx a problem of d.velop.",
    )
}
