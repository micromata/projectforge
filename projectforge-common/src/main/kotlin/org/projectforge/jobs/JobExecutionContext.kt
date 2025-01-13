/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.common.extensions.formatMillis
import org.projectforge.common.html.Html
import org.projectforge.common.html.HtmlDocument
import org.projectforge.common.html.HtmlTable
import org.projectforge.jobs.JobListExecutionContext.Companion.addBoxedLine
import org.projectforge.jobs.JobListExecutionContext.Companion.addCell
import org.projectforge.jobs.JobListExecutionContext.Companion.addErrorBoxedLineMarker
import org.projectforge.jobs.JobListExecutionContext.Companion.addSeparatorLine
import org.projectforge.jobs.JobListExecutionContext.Companion.format
import org.projectforge.jobs.JobListExecutionContext.Companion.getBoldCssClass
import org.projectforge.jobs.JobListExecutionContext.Companion.getCssClass
import java.util.*

class JobExecutionContext(val producer: AbstractJob) {
    enum class Status {
        ERRORS, WARNINGS, OK
    }

    class Message(val message: String, val status: Status) {
        val date = Date()
    }

    private val attributes = mutableMapOf<String, Any?>()
    private var startTime: Long? = null
    private var finishedTime: Long? = null

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

    val durationInMs: Long
        get() {
            val start = startTime ?: return 0
            val finished = finishedTime ?: System.currentTimeMillis()
            return finished - start
        }

    fun started() {
        startTime = System.currentTimeMillis()
    }

    fun finished() {
        finishedTime = System.currentTimeMillis()
    }

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

    /**
     * @param index The index of the job in the list (for a href anchors).
     */
    fun addReportAsHtml(
        html: HtmlDocument,
        index: Int,
        jobExecutionContext: JobExecutionContext,
        showAllMessages: Boolean = true
    ) {
        addIntro(html, index, jobExecutionContext)
        if (errors.isNotEmpty()) {
            html.add(Html.H3("Errors:"))
            html.add(createLogTable(errors))
        }
        if (showAllMessages && allMessages.isNotEmpty()) {
            html.add(Html.H3("All messages:"))
            html.add(createLogTable(allMessages))
        }
    }

    fun addReportAsText(sb: StringBuilder, showAllMessages: Boolean = true) {
        addIntro(sb)
        if (errors.isNotEmpty()) {
            sb.appendLine("*** Errors:")
            errors.forEach { sb.appendLine("*** ERROR: ${it.date}: ${it.message}") }
            sb.appendLine()
        }
        if (showAllMessages && allMessages.isNotEmpty()) {
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

    private fun addIntro(html: HtmlDocument, index: Int, jobExecutionContext: JobExecutionContext) {
        val time = "(Execution time: ${jobExecutionContext.durationInMs.formatMillis()})"
        html.add(Html.H2("${producer::class.simpleName}: ${producer.title}", id = "job$index"))
        when (status) {
            Status.OK -> html.add(Html.Alert(Html.Alert.Type.SUCCESS, "Status: OK $time"))
            Status.WARNINGS -> html.add(Html.Alert(Html.Alert.Type.WARNING, "Status: WARNINGS $time"))
            Status.ERRORS -> html.add(Html.Alert(Html.Alert.Type.DANGER, "Status: ERRORS $time"))
        }
    }

    private fun addIntro(sb: StringBuilder) {
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

    private fun createLogTable(messages: List<Message>): HtmlTable {
        return HtmlTable().also { table ->
            table.addHeadRow().also {
                it.addTH("Level")
                it.addTH("Date")
                it.addTH("Message")
            }
            messages.forEach { msg ->
                val level = when (msg.status) {
                    Status.OK -> "Info"
                    Status.WARNINGS -> "Warning"
                    Status.ERRORS -> "ERROR"
                }
                val cssClass = getCssClass(msg.status)
                table.addRow(cssClass).also { tr ->
                    tr.addTD(level, cssClass = getBoldCssClass(msg.status))
                    tr.addTD(format(msg.date))
                    tr.addTD(msg.message)
                }
            }
        }
    }
}
