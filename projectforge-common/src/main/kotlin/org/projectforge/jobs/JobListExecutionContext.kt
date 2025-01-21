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

import org.projectforge.common.extensions.abbreviate
import org.projectforge.common.extensions.formatMillis
import org.projectforge.common.html.*
import org.projectforge.jobs.JobExecutionContext.Status
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class JobListExecutionContext {
    val jobs = mutableListOf<JobExecutionContext>()
    val startTime = System.currentTimeMillis()

    val status: Status
        get() = when {
            jobs.any { it.status == Status.ERRORS } -> Status.ERRORS
            jobs.any { it.status == Status.WARNINGS } -> Status.WARNINGS
            else -> Status.OK
        }

    fun add(job: AbstractJob): JobExecutionContext {
        return JobExecutionContext(job).also { jobs.add(it) }
    }

    fun getReportAsHtml(showAllMessages: Boolean = true, intro: HtmlElement? = null): String {
        val html = HtmlDocument(title)
        intro?.let {
            html.add(it)
        }
        html.add(Html.H1(title))
        val time =
            "(Execution time in total: ${(System.currentTimeMillis() - startTime).formatMillis(showMillis = false)})"
        when (status) {
            Status.OK -> html.add(Html.Alert(Html.Alert.Type.SUCCESS, "All checks passed successfully. $time"))
            Status.WARNINGS -> html.add(Html.Alert(Html.Alert.Type.WARNING, "Some checks passed with warnings. $time"))
            Status.ERRORS -> html.add(
                Html.Alert(
                    Html.Alert.Type.DANGER,
                    "Some errors occurred! $time\n\nIt's recommended to re-run the checks for double-check."
                )
            )
        }
        html.add(HtmlTable().also { table ->
            table.addHeadRow().also { tr ->
                tr.addTH("Producer", cssClass = CssClass.FIXED_WIDTH_NO_WRAP)
                tr.addTH("Status", cssClass = CssClass.FIXED_WIDTH_NO_WRAP)
                tr.addTH("Duration", cssClass = CssClass.FIXED_WIDTH_NO_WRAP)
                tr.addTH("Job", cssClass = CssClass.EXPAND)
            }
            sortedJobs.forEachIndexed { index, job ->
                val cssClass = getCssClass(job.status) ?: CssClass.SUCCESS
                table.addRow().also { tr ->
                    tr.addTD().also { it.add(Html.A("#job$index", job.producer::class.simpleName!!)) }
                    tr.addTD(cssClass = cssClass).also { it.add(Html.A("#job$index", job.status.toString())) }
                    tr.addTD(job.durationInMs.formatMillis())
                    tr.addTD().also { it.add(Html.A("#job$index", job.producer.title)) }
                }
            }
        })
        sortedJobs.forEachIndexed { index, jobExecutionContext ->
            jobExecutionContext.addReportAsHtml(html, index, jobExecutionContext, showAllMessages)
        }
        return html.toString()
    }

    fun getReportAsText(showAllMessages: Boolean = true): String {
        val sb = StringBuilder()
        addSeparatorLine(sb)
        addSeparatorLine(sb)
        addBoxedLine(sb, title)
        addSeparatorLine(sb)
        when (status) {
            Status.OK -> addBoxedLine(sb, "  All checks passed successfully.")
            Status.WARNINGS -> addBoxedLine(sb, "  Some checks passed with warnings.")
            Status.ERRORS -> addErrorBoxedLineMarker(sb)
        }
        addSeparatorLine(sb)
        sortedJobs.forEach { job ->
            job.addStatusLineAsText(sb)
        }
        addSeparatorLine(sb)
        sb.appendLine()
        sortedJobs.forEach { job ->
            job.addReportAsText(sb, showAllMessages)
        }
        sb.appendLine()
        return sb.toString()
    }

    private val sortedJobs: List<JobExecutionContext>
        get() = jobs.sortedWith(compareBy<JobExecutionContext> { it.status }
            .thenBy { it.lastUpdate })

    companion object {
        val title: String
            get() {
                return "Sanity Check Report: ${format(Date())}"
            }
        internal const val LINE_LENGTH = 80
        private val isoDateTimeFormatterSeconds =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)

        internal fun format(date: Date): String {
            return isoDateTimeFormatterSeconds.format(date.toInstant())
        }

        internal fun addBoxedLine(sb: StringBuilder, title: String) {
            addCell(sb, title, LINE_LENGTH)
        }

        internal fun addCell(sb: StringBuilder, text: String, length: Int, lineCompleted: Boolean = true) {
            val useLength = if (lineCompleted) length - 4 else length - 2
            sb.append("| ${text.abbreviate(useLength).padEnd(useLength)}")
            if (lineCompleted) {
                sb.appendLine(" |")
            }
        }

        internal fun addErrorBoxedLineMarker(sb: StringBuilder) {
            addBoxedLine(sb, "  *************************************")
            addBoxedLine(sb, "  ******* Some checks failed!!! *******")
            addBoxedLine(sb, "  *************************************")
        }

        internal fun addSeparatorLine(sb: StringBuilder) {
            sb.appendLine("-".repeat(LINE_LENGTH))
        }

        internal fun getCssClass(status: Status): CssClass? {
            return when (status) {
                Status.OK -> null
                Status.WARNINGS -> CssClass.WARNING
                Status.ERRORS -> CssClass.ERROR
            }
        }

        internal fun getBoldCssClass(status: Status): CssClass? {
            return when (status) {
                Status.OK -> null
                Status.WARNINGS -> CssClass.BOLD
                Status.ERRORS -> CssClass.BOLD
            }
        }
    }
}
