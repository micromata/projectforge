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

package org.projectforge.framework.support

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The digest as plain text: [body] is the summary sorted by severity (one entry per group, the sync problems
 * right after the unreachable systems), [details] the attachment with the single occurrences and stack traces.
 */
class ErrorDigestRenderer(
    private val domain: String?,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone)
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zone)

    fun subject(snapshot: ErrorDigestCollector.Snapshot, syncProblems: List<SyncProblemTracker.Problem>): String {
        val external = snapshot.count(ErrorCategory.EXTERNAL_UNREACHABLE) + syncProblems.size
        val errors = snapshot.occurrences - snapshot.count(ErrorCategory.EXTERNAL_UNREACHABLE)
        return "Error digest ${domain ?: ""}: $errors errors, $external external failures".replace("  ", " ")
    }

    fun body(
        snapshot: ErrorDigestCollector.Snapshot,
        syncProblems: List<SyncProblemTracker.Problem>,
        fromMillis: Long,
        toMillis: Long,
        attachmentName: String?,
    ): String = buildString {
        appendLine("ProjectForge error digest${domain?.let { " of $it" } ?: ""}")
        appendLine("Period: ${dateTime.format(Instant.ofEpochMilli(fromMillis))} - ${dateTime.format(Instant.ofEpochMilli(toMillis))} (${zone.id})")
        append("${snapshot.occurrences} occurrences in ${snapshot.groups.size} groups")
        if (snapshot.dropped > 0) append(", ${snapshot.dropped} further occurrences dropped (group limit reached)")
        appendLine()
        ErrorCategory.entries.forEach { category ->
            val groups = snapshot.groups.filter { it.category == category }
            if (groups.isNotEmpty()) {
                appendLine()
                appendLine("== ${category.title} (${groups.sumOf { it.count }}) ==")
                groups.forEach { appendGroup(it) }
            }
            if (category == ErrorCategory.EXTERNAL_UNREACHABLE && syncProblems.isNotEmpty()) {
                appendLine()
                appendLine("== Sync runs with problems (${syncProblems.size}) ==")
                syncProblems.forEach { appendSyncProblem(it) }
            }
        }
        if (attachmentName != null) {
            appendLine()
            appendLine("Single occurrences with stack traces (up to ${ErrorGroup.MAX_SAMPLES} per group): $attachmentName")
        }
    }

    fun details(snapshot: ErrorDigestCollector.Snapshot): String = buildString {
        snapshot.groups.forEachIndexed { index, group ->
            appendLine("=".repeat(100))
            appendLine("#${index + 1} [${group.category.title}] ${group.count}x ${group.exceptionClass ?: ""} at ${group.location}")
            appendLine(group.message ?: "")
            group.samples.forEach { sample ->
                appendLine("-".repeat(100))
                append(dateTime.format(Instant.ofEpochMilli(sample.timestampMillis))).append(" ").append(sample.level)
                sample.user?.let { append(", user=").append(it) }
                sample.request?.let { append(", ").append(it) }
                appendLine()
                appendLine(sample.message ?: "")
                sample.stackTrace?.let { appendLine(it.trimEnd()) }
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendGroup(group: ErrorGroup) {
        val period = if (group.count == 1) {
            "at ${time.format(Instant.ofEpochMilli(group.firstMillis))}"
        } else {
            "${time.format(Instant.ofEpochMilli(group.firstMillis))} - ${time.format(Instant.ofEpochMilli(group.lastMillis))}"
        }
        appendLine("${group.count.toString().padStart(5)}x  $period  ${group.level}  ${group.exceptionClass?.substringAfterLast('.') ?: "-"}  ${group.location}")
        appendLine("        ${(group.message ?: "").lineSequence().firstOrNull()?.take(300) ?: ""}")
        if (group.users.isNotEmpty()) {
            appendLine("        users: ${group.users.joinToString(", ")}")
        }
    }

    private fun StringBuilder.appendSyncProblem(problem: SyncProblemTracker.Problem) {
        val counts = listOfNotNull(
            problem.abortedRuns.takeIf { it > 0 }?.let { "$it aborted" },
            problem.timeouts.takeIf { it > 0 }?.let { "$it timeouts" },
            problem.runsWithErrors.takeIf { it > 0 }?.let { "$it with errors" },
        ).joinToString(", ")
        appendLine("  ${problem.type}: $counts; last status ${problem.lastStatus ?: "-"}")
        problem.lastError?.let { error ->
            val at = problem.lastErrorDate?.let { " (${dateTime.format(it.toInstant())})" } ?: ""
            appendLine("        last error$at: ${error.take(300)}")
        }
    }
}
