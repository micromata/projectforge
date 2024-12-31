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

import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import org.projectforge.common.extensions.abbreviate
import org.projectforge.framework.time.PFDateTime

class SanityCheckContext {
    val jobs = mutableListOf<SanityCheckJobContext>()

    val status: SanityCheckJobContext.Status
        get() = when {
            jobs.any { it.status == SanityCheckJobContext.Status.ERRORS } -> SanityCheckJobContext.Status.ERRORS
            jobs.any { it.status == SanityCheckJobContext.Status.WARNINGS } -> SanityCheckJobContext.Status.WARNINGS
            else -> SanityCheckJobContext.Status.OK
        }

    fun add(job: AbstractSanityCheckJob): SanityCheckJobContext {
        return SanityCheckJobContext(job).also { jobs.add(it) }
    }

    fun getReportAsText(): String {
        val sb = StringBuilder()
        addSeparatorLine(sb)
        addSeparatorLine(sb)
        addBoxedLine(sb, "Sanity Check Report: ${PFDateTime.now().isoStringSeconds}")
        addSeparatorLine(sb)
        when (status) {
            SanityCheckJobContext.Status.OK -> addBoxedLine(sb, "  All checks passed successfully.")
            SanityCheckJobContext.Status.WARNINGS -> addBoxedLine(sb, "  Some checks passed with warnings.")
            SanityCheckJobContext.Status.ERRORS -> addErrorBoxedLineMarker(sb)
        }
        addSeparatorLine(sb)
        addSeparatorLine(sb)
        jobs.forEach { job ->
            job.addReportAsText(sb)
        }
        sb.appendLine()
        return sb.toString()
    }

    companion object {
        internal const val LINE_LENGTH = 80
        internal fun addBoxedLine(sb: StringBuilder, title: String) {
            sb.appendLine("| ${title.abbreviate(LINE_LENGTH - 4).padEnd(LINE_LENGTH - 4)} |")
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
