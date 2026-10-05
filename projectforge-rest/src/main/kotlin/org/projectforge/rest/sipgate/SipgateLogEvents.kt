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

package org.projectforge.rest.sipgate

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object SipgateLogEvents {
    @JvmField
    val STORAGE_PARSE_FAILED = LogEvent(
        code = "sipgate.storageParseFailed",
        category = LogCategory.DATA,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "The local copy of the Sipgate data (users, devices, numbers) couldn't be read. " +
                "It is read again from Sipgate, so nothing is lost.",
        action = "Delete the storage file named in the message if this occurs repeatedly.",
    )

    @JvmField
    val NUMBERS_NOT_READABLE = LogEvent(
        code = "sipgate.numbersNotReadable",
        category = LogCategory.CONFIG,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "The phone numbers couldn't be read from Sipgate, most likely the token has no access to them. " +
                "Users and devices are synchronized anyway.",
        action = "Check the scopes of the Sipgate token (projectforge.sipgate.*), it needs read access to the numbers.",
    )
}
