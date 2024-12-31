/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.jobs

import java.util.*
import kotlin.reflect.KClass

class SanityCheckJobContext(val producer: KClass<*>) {
    enum class Status {
        OK, WARNINGS, ERRORS
    }

    class Message(val date: Date, val message: String)

    val status: Status
        get() = when {
            errors.isNotEmpty() -> Status.ERRORS
            warnings.isNotEmpty() -> Status.WARNINGS
            else -> Status.OK
        }
    val warnings = mutableListOf<Message>()
    val errors = mutableListOf<Message>()

    /**
     * Info messages, log messages etc.
     */
    val messages = mutableListOf<Message>()

    fun addError(msg: String) {
        errors.add(Message(Date(), msg))
    }

    fun addWarning(msg: String) {
        warnings.add(Message(Date(), msg))
    }

    fun addMessage(msg: String) {
        messages.add(Message(Date(), msg))
    }
}
