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

package org.projectforge.jobs

import org.projectforge.jobs.JobListExecutionContext.Companion.addBoxedLine
import org.projectforge.jobs.JobListExecutionContext.Companion.addCell
import org.projectforge.jobs.JobListExecutionContext.Companion.addErrorBoxedLineMarker
import org.projectforge.jobs.JobListExecutionContext.Companion.addSeparatorLine
import org.projectforge.jobs.JobListExecutionContext.Companion.format
import java.util.*

class JobExecutionContext(val producer: AbstractJob) {
    enum class Status {
        ERRORS, WARNINGS, OK
    }

    class Message(val message: String, val status: Status) {
        val date = Date()
    }

    private val attributes = mutableMapOf<String, Any?>()

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

    val lastUpdate: Date
        get() = allMessages.maxByOrNull { it.date }?.date ?: Date()

    fun setAttribute(key: String, value: Any?) {
        attributes[key] = value
    }

    fun getAttribute(key: String): Any? {
        return attributes[key]
    }

    fun getAttributeAsLong(key: String): Long? {
        return attributes[key] as? Long
    }

    fun getAttributeAsInt(key: String): Int? {
        return attributes[key] as? Int
    }

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

                sb.appendLine("${format(msg.date)} $marker: ${msg.message}")
            }
            sb.appendLine()
        }
    }

    fun addStatusLineAsText(sb: StringBuilder) {
        addCell(sb, producer.title, 50, lineCompleted = false)
        val statusString = if (status == Status.ERRORS) "*** ERRORS ***" else status.toString()
        addCell(sb, statusString, 30)
    }

    internal fun addIntro(sb: StringBuilder) {
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
