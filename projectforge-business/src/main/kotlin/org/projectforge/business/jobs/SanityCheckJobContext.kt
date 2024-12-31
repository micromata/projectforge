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

import org.projectforge.business.jobs.SanityCheckContext.Companion.addBoxedLine
import org.projectforge.business.jobs.SanityCheckContext.Companion.addErrorBoxedLineMarker
import org.projectforge.business.jobs.SanityCheckContext.Companion.addSeparatorLine
import org.projectforge.framework.time.PFDateTime

class SanityCheckJobContext(val producer: AbstractSanityCheckJob) {
    enum class Status {
        OK, WARNINGS, ERRORS
    }

    class Message(val message: String, val status: Status) {
        val date = PFDateTime.now()
    }

    val status: Status
        get() = when {
            errors.isNotEmpty() -> Status.ERRORS
            warnings.isNotEmpty() -> Status.WARNINGS
            else -> Status.OK
        }
    val warnings = mutableListOf<Message>()
    val errors = mutableListOf<Message>()

    val allMessages = mutableListOf<Message>()

    /**
     * Info messages, log messages etc.
     */
    val messages = mutableListOf<Message>()

    fun addError(msg: String) {
        Message(msg, Status.ERRORS).also {
            errors.add(it)
            allMessages.add(it)
        }
    }

    fun addWarning(msg: String) {
        Message(msg, Status.WARNINGS).also {
            warnings.add(it)
            allMessages.add(it)
        }
    }

    fun addMessage(msg: String) {
        Message(msg, Status.OK).also {
            messages.add(it)
            allMessages.add(it)
        }
    }

    fun addReportAsText(sb: StringBuilder) {
        addIntro(sb)
        if (errors.isNotEmpty()) {
            sb.appendLine("*** Errors:")
            errors.forEach { sb.appendLine("*** ERROR: ${it.date}: ${it.message}") }
            sb.appendLine()
        }
        if (allMessages.isNotEmpty()) {
            sb.appendLine("Messages:")
            allMessages.forEach { msg ->
                val marker = when (msg.status) {
                    Status.OK -> "    INFO     "
                    Status.WARNINGS -> "!!! WARN     "
                    Status.ERRORS -> "*** ERROR ***"
                }

                sb.appendLine("${msg.date.isoStringSeconds} $marker: ${msg.message}")
            }
            sb.appendLine()
        }
    }

    fun addIntro(sb: StringBuilder) {
        addSeparatorLine(sb)
        addBoxedLine(sb, "${producer::class.simpleName}:${producer.title}")
        addSeparatorLine(sb)
        when (status) {
            Status.OK -> addBoxedLine(sb, "  Status: OK")
            Status.WARNINGS -> addBoxedLine(sb, "  Status: WARNINGS")
            Status.ERRORS -> addErrorBoxedLineMarker(sb)
        }
        addSeparatorLine(sb)
    }
}
