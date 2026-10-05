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

import org.projectforge.common.logging.LogLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The digest as html mail ([htmlData] for the template `mail/errorDigestMail.html`) and as plain text: [body] is
 * the summary sorted by severity (one entry per group, the sync problems right after the unreachable systems), the
 * fallback if the template fails, [details] the attachment with the single occurrences and stack traces.
 */
class ErrorDigestRenderer(
    private val domain: String?,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    /** The digest prepared for the template, which only prints these values. */
    class DigestView(
        val domain: String?,
        val period: String,
        val errors: Int,
        val external: Int,
        val occurrences: Int,
        val groups: Int,
        val dropped: Int,
        val sections: List<SectionView>,
        val attachmentName: String?,
        val maxSamples: Int = ErrorGroup.MAX_SAMPLES,
    )

    /** One category, or the sync problems (then [syncProblems] is filled instead of [groups]). */
    class SectionView(
        val title: String,
        val count: Int,
        val groups: List<GroupView> = emptyList(),
        val syncProblems: List<SyncView> = emptyList(),
    )

    class GroupView(
        val count: Int,
        /** `at 06:10:01` or `06:10:01 - 07:06:41`. */
        val period: String,
        val level: String,
        /** Css class of the level's badge, see `mail/mailHead.html`. */
        val levelCss: String,
        /** Short name of the exception class, if any. */
        val exceptionClass: String?,
        val location: String,
        /** First line of the message, at most [MAX_MESSAGE_LENGTH] characters. */
        val message: String,
        val users: String?,
    )

    class SyncView(
        val type: String,
        /** E.g. `1 aborted, 2 with errors`. */
        val counts: String,
        val lastStatus: String,
        val lastError: String?,
    )

    private val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone)
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zone)

    fun subject(snapshot: ErrorDigestCollector.Snapshot, syncProblems: List<SyncProblemTracker.Problem>): String {
        val external = snapshot.count(ErrorCategory.EXTERNAL_UNREACHABLE) + syncProblems.size
        val errors = snapshot.occurrences - snapshot.count(ErrorCategory.EXTERNAL_UNREACHABLE)
        return "Error digest ${domain ?: ""}: $errors errors, $external external failures".replace("  ", " ")
    }

    fun view(
        snapshot: ErrorDigestCollector.Snapshot,
        syncProblems: List<SyncProblemTracker.Problem>,
        fromMillis: Long,
        toMillis: Long,
        attachmentName: String?,
    ): DigestView {
        val sections = mutableListOf<SectionView>()
        ErrorCategory.entries.forEach { category ->
            val groups = snapshot.groups.filter { it.category == category }
            if (groups.isNotEmpty()) {
                sections.add(SectionView(category.title, groups.sumOf { it.count }, groups = groups.map { groupView(it) }))
            }
            if (category == ErrorCategory.EXTERNAL_UNREACHABLE && syncProblems.isNotEmpty()) {
                sections.add(SectionView(SYNC_TITLE, syncProblems.size, syncProblems = syncProblems.map { syncView(it) }))
            }
        }
        val externalOccurrences = snapshot.count(ErrorCategory.EXTERNAL_UNREACHABLE)
        return DigestView(
            domain = domain,
            period = "${dateTime.format(Instant.ofEpochMilli(fromMillis))} - ${dateTime.format(Instant.ofEpochMilli(toMillis))} (${zone.id})",
            errors = snapshot.occurrences - externalOccurrences,
            external = externalOccurrences + syncProblems.size,
            occurrences = snapshot.occurrences,
            groups = snapshot.groups.size,
            dropped = snapshot.dropped,
            sections = sections,
            attachmentName = attachmentName,
        )
    }

    /** The variables of the template `mail/errorDigestMail.html`. */
    fun htmlData(
        snapshot: ErrorDigestCollector.Snapshot,
        syncProblems: List<SyncProblemTracker.Problem>,
        fromMillis: Long,
        toMillis: Long,
        attachmentName: String?,
    ): MutableMap<String, Any?> =
        mutableMapOf("digest" to view(snapshot, syncProblems, fromMillis, toMillis, attachmentName))

    fun body(
        snapshot: ErrorDigestCollector.Snapshot,
        syncProblems: List<SyncProblemTracker.Problem>,
        fromMillis: Long,
        toMillis: Long,
        attachmentName: String?,
    ): String = buildString {
        val view = view(snapshot, syncProblems, fromMillis, toMillis, attachmentName)
        appendLine("ProjectForge error digest${domain?.let { " of $it" } ?: ""}")
        appendLine("Period: ${view.period}")
        append("${view.occurrences} occurrences in ${view.groups} groups")
        if (view.dropped > 0) append(", ${view.dropped} further occurrences dropped (group limit reached)")
        appendLine()
        view.sections.forEach { section ->
            appendLine()
            appendLine("== ${section.title} (${section.count}) ==")
            section.groups.forEach { appendGroup(it) }
            section.syncProblems.forEach { appendSyncProblem(it) }
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

    private fun groupView(group: ErrorGroup): GroupView {
        val period = if (group.count == 1) {
            "at ${time.format(Instant.ofEpochMilli(group.firstMillis))}"
        } else {
            "${time.format(Instant.ofEpochMilli(group.firstMillis))} - ${time.format(Instant.ofEpochMilli(group.lastMillis))}"
        }
        return GroupView(
            count = group.count,
            period = period,
            level = group.level.name,
            levelCss = when (group.level) {
                LogLevel.FATAL, LogLevel.ERROR -> "badge-error"
                LogLevel.WARN -> "badge-warn"
                else -> "badge-info"
            },
            exceptionClass = group.exceptionClass?.substringAfterLast('.'),
            location = group.location,
            message = (group.message ?: "").lineSequence().firstOrNull()?.take(MAX_MESSAGE_LENGTH) ?: "",
            users = group.users.takeIf { it.isNotEmpty() }?.joinToString(", "),
        )
    }

    private fun syncView(problem: SyncProblemTracker.Problem): SyncView {
        val counts = listOfNotNull(
            problem.abortedRuns.takeIf { it > 0 }?.let { "$it aborted" },
            problem.timeouts.takeIf { it > 0 }?.let { "$it timeouts" },
            problem.runsWithErrors.takeIf { it > 0 }?.let { "$it with errors" },
        ).joinToString(", ")
        val lastError = problem.lastError?.let { error ->
            val at = problem.lastErrorDate?.let { " (${dateTime.format(it.toInstant())})" } ?: ""
            "last error$at: ${error.take(MAX_MESSAGE_LENGTH)}"
        }
        return SyncView(problem.type, counts, problem.lastStatus?.toString() ?: "-", lastError)
    }

    private fun StringBuilder.appendGroup(group: GroupView) {
        appendLine("${group.count.toString().padStart(5)}x  ${group.period}  ${group.level}  ${group.exceptionClass ?: "-"}  ${group.location}")
        appendLine("        ${group.message}")
        group.users?.let { appendLine("        users: $it") }
    }

    private fun StringBuilder.appendSyncProblem(problem: SyncView) {
        appendLine("  ${problem.type}: ${problem.counts}; last status ${problem.lastStatus}")
        problem.lastError?.let { appendLine("        $it") }
    }

    companion object {
        private const val SYNC_TITLE = "Sync runs with problems"
        private const val MAX_MESSAGE_LENGTH = 300
    }
}
