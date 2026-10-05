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

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The digest as html mail ([htmlData] for the template `mail/errorDigestMail.html`) and as plain text: [body] is
 * the summary, the fallback if the template fails, [details] the attachment with the single occurrences and stack
 * traces.
 *
 * What needs attention comes first: the new problems, the regressions and the spikes (see [DigestNovelty]), then
 * the [MAX_KNOWN_GROUPS] most frequent known ones sorted by severity (one section per category, the sync problems
 * right after the unreachable systems). Ignored and muted problems are only counted.
 *
 * @param dashboardUrl The error dashboard (`next/adminErrors`): linked by the digest and by each problem.
 */
class ErrorDigestRenderer(
    private val domain: String?,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val dashboardUrl: String? = null,
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
        /** Occurrences not listed: below their event's threshold, already reported or muted. */
        val suppressed: Int,
        val sections: List<SectionView>,
        val attachmentName: String?,
        val maxSamples: Int = ErrorGroup.MAX_SAMPLES,
        val newProblems: Int = 0,
        val regressions: Int = 0,
        val spikes: Int = 0,
        /** Problems not listed because an admin ignored or muted them. */
        val muted: Int = 0,
        /** Known problems not listed beyond the [MAX_KNOWN_GROUPS] most frequent ones. */
        val omitted: Int = 0,
        val dashboardUrl: String? = null,
    )

    /**
     * New problems, regressions, spikes ([showCategory]: their groups are of any category), one category of the
     * known ones, or the sync problems (then [syncProblems] is filled instead of [groups]).
     */
    class SectionView(
        val title: String,
        val count: Int,
        val groups: List<GroupView> = emptyList(),
        val syncProblems: List<SyncView> = emptyList(),
        val showCategory: Boolean = false,
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
        /** The code of the log event, if it identifies the problem (not for the generic events). */
        val code: String?,
        /** What it means, from the log event. */
        val explanation: String?,
        /** What to do, from the log event. */
        val action: String?,
        val category: String,
        /** The problem in the error dashboard, if it is in the database. */
        val link: String? = null,
        /** For a spike: `usually 0.4 per hour`. */
        val usual: String? = null,
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
        val external = snapshot.count(LogCategory.EXTERNAL) + syncProblems.size
        val errors = snapshot.occurrences - snapshot.count(LogCategory.EXTERNAL)
        val news = NOVELTY_COUNTS.mapNotNull { (novelty, label) ->
            snapshot.groups.count { it.novelty == novelty }.takeIf { it > 0 }?.let { "$it $label" }
        }.joinToString(", ")
        return ("Error digest ${domain ?: ""}: $errors errors, $external external failures" +
                if (news.isEmpty()) "" else " ($news)").replace("  ", " ")
    }

    fun view(
        snapshot: ErrorDigestCollector.Snapshot,
        syncProblems: List<SyncProblemTracker.Problem>,
        fromMillis: Long,
        toMillis: Long,
        attachmentName: String?,
    ): DigestView {
        val sections = mutableListOf<SectionView>()
        NOVELTY_SECTIONS.forEach { (novelty, title) ->
            val groups = snapshot.groups.filter { it.novelty == novelty }
            if (groups.isNotEmpty()) {
                sections.add(SectionView(title, groups.sumOf { it.count }, groups.map { groupView(it) }, showCategory = true))
            }
        }
        val known = snapshot.groups.filter { it.novelty == DigestNovelty.KNOWN }
        val listed = known.sortedByDescending { it.count }.take(MAX_KNOWN_GROUPS).toSet()
        LogCategory.entries.forEach { category ->
            // In the order of the snapshot: by severity.
            val groups = known.filter { it.category == category && it in listed }
            if (groups.isNotEmpty()) {
                sections.add(SectionView(category.title, groups.sumOf { it.count }, groups = groups.map { groupView(it) }))
            }
            if (category == LogCategory.EXTERNAL && syncProblems.isNotEmpty()) {
                sections.add(SectionView(SYNC_TITLE, syncProblems.size, syncProblems = syncProblems.map { syncView(it) }))
            }
        }
        val externalOccurrences = snapshot.count(LogCategory.EXTERNAL)
        return DigestView(
            domain = domain,
            period = "${dateTime.format(Instant.ofEpochMilli(fromMillis))} - ${dateTime.format(Instant.ofEpochMilli(toMillis))} (${zone.id})",
            errors = snapshot.occurrences - externalOccurrences,
            external = externalOccurrences + syncProblems.size,
            occurrences = snapshot.occurrences,
            groups = snapshot.groups.size,
            dropped = snapshot.dropped,
            suppressed = snapshot.suppressed,
            sections = sections,
            attachmentName = attachmentName,
            newProblems = snapshot.groups.count { it.novelty == DigestNovelty.NEW },
            regressions = snapshot.groups.count { it.novelty == DigestNovelty.REGRESSION },
            spikes = snapshot.groups.count { it.novelty == DigestNovelty.SPIKE },
            muted = snapshot.muted,
            omitted = known.size - listed.size,
            dashboardUrl = dashboardUrl,
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
        if (view.suppressed > 0) append(", ${view.suppressed} occurrences not listed (below threshold, already reported or muted)")
        appendLine()
        if (view.muted > 0) appendLine("${view.muted} ignored or muted problems not listed")
        view.dashboardUrl?.let { appendLine("Error dashboard: $it") }
        view.sections.forEach { section ->
            appendLine()
            appendLine("== ${section.title} (${section.count}) ==")
            section.groups.forEach { appendGroup(it, section.showCategory) }
            section.syncProblems.forEach { appendSyncProblem(it) }
        }
        if (view.omitted > 0) {
            appendLine()
            appendLine("${view.omitted} further known problems not listed, see the error dashboard.")
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
            if (group.groupByCode) {
                appendLine("Code: ${group.event.code}")
                group.event.explanation?.let { appendLine("Explanation: $it") }
                group.event.action?.let { appendLine("Action: $it") }
            }
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
            // The generic events would only repeat the section's title in every group.
            code = group.event.code.takeIf { group.groupByCode },
            explanation = group.event.explanation.takeIf { group.groupByCode },
            action = group.event.action.takeIf { group.groupByCode },
            category = group.category.title,
            link = dashboardUrl?.let { url -> group.problemId?.let { "$url?id=$it" } },
            usual = group.hourlyMean?.takeIf { group.novelty == DigestNovelty.SPIKE }
                ?.let { "usually ${"%.1f".format(Locale.ENGLISH, it)} per hour" },
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

    private fun StringBuilder.appendGroup(group: GroupView, showCategory: Boolean) {
        appendLine("${group.count.toString().padStart(5)}x  ${group.period}  ${group.level}  ${group.exceptionClass ?: "-"}  ${group.location}")
        appendLine("        ${group.message}")
        if (showCategory) appendLine("        category: ${group.category}")
        group.usual?.let { appendLine("        $it") }
        group.users?.let { appendLine("        users: $it") }
        group.code?.let { appendLine("        code: $it") }
        group.explanation?.let { appendLine("        explanation: $it") }
        group.action?.let { appendLine("        action: $it") }
        group.link?.let { appendLine("        $it") }
    }

    private fun StringBuilder.appendSyncProblem(problem: SyncView) {
        appendLine("  ${problem.type}: ${problem.counts}; last status ${problem.lastStatus}")
        problem.lastError?.let { appendLine("        $it") }
    }

    companion object {
        private const val SYNC_TITLE = "Sync runs with problems"
        private const val MAX_MESSAGE_LENGTH = 300

        /** The known problems listed at most, the most frequent ones; the others only counted. */
        const val MAX_KNOWN_GROUPS = 30

        private val NOVELTY_SECTIONS = listOf(
            DigestNovelty.NEW to "New problems",
            DigestNovelty.REGRESSION to "Regressions: occurred again after being resolved",
            DigestNovelty.SPIKE to "Spikes: much more often than in the last 7 days",
        )

        private val NOVELTY_COUNTS = listOf(
            DigestNovelty.NEW to "new",
            DigestNovelty.REGRESSION to "regressions",
            DigestNovelty.SPIKE to "spikes",
        )
    }
}
