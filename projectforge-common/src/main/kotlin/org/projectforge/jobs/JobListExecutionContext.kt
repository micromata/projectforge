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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class JobListExecutionContext {
    val jobs = mutableListOf<JobExecutionContext>()

    val status: JobExecutionContext.Status
        get() = when {
            jobs.any { it.status == JobExecutionContext.Status.ERRORS } -> JobExecutionContext.Status.ERRORS
            jobs.any { it.status == JobExecutionContext.Status.WARNINGS } -> JobExecutionContext.Status.WARNINGS
            else -> JobExecutionContext.Status.OK
        }

    fun add(job: AbstractJob): JobExecutionContext {
        return JobExecutionContext(job).also { jobs.add(it) }
    }

    fun getReportAsText(): String {
        val sb = StringBuilder()
        addSeparatorLine(sb)
        addSeparatorLine(sb)
        addBoxedLine(sb, "Sanity Check Report: ${format(Date())}")
        addSeparatorLine(sb)
        when (status) {
            JobExecutionContext.Status.OK -> addBoxedLine(sb, "  All checks passed successfully.")
            JobExecutionContext.Status.WARNINGS -> addBoxedLine(sb, "  Some checks passed with warnings.")
            JobExecutionContext.Status.ERRORS -> addErrorBoxedLineMarker(sb)
        }
        addSeparatorLine(sb)
        sortedJobs.forEach { job ->
            job.addStatusLineAsText(sb)
        }
        addSeparatorLine(sb)
        sb.appendLine()
        sortedJobs.forEach { job ->
            job.addReportAsText(sb)
        }
        sb.appendLine()
        return sb.toString()
    }

    private val sortedJobs: List<JobExecutionContext>
        get() = jobs.sortedWith(compareBy<JobExecutionContext> { it.status }
            .thenBy { it.lastUpdate })

    companion object {
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
            val useLength = if (lineCompleted) length - 4 else length -2
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
    }
}
