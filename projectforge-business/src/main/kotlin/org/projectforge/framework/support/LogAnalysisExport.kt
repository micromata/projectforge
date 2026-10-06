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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.projectforge.common.logging.LogAudience
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEventRegistry
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogNotify
import java.time.Instant

/**
 * The problems of the log aggregation as one self-explanatory JSON document, for an analysis and classification by
 * an AI (or any other tool): downloaded from the problem dashboard ([LogGroupAdminService.export], source
 * [SOURCE_DASHBOARD]) and attached to every error digest ([ofDigest], source [SOURCE_DIGEST]). Both share this
 * format, so one prompt fits both. The [guide] explains the fields and values inside the file itself.
 *
 * No user names: only the number of distinct users. Messages and requests are as logged, though, and may contain
 * personal data.
 */
@JsonPropertyOrder("format", "version", "source", "instance", "generatedAt", "periodFrom", "periodTo", "filter", "guide")
class LogAnalysisExport(
    /** [SOURCE_DASHBOARD] or [SOURCE_DIGEST]. */
    val source: String,
    /** The domain of the installation. */
    val instance: String?,
    val generatedAt: String,
    /** The digest's period. */
    val periodFrom: String? = null,
    val periodTo: String? = null,
    /** The dashboard's filter. */
    val filter: LogGroupFilter? = null,
    val problems: List<LogAnalysisProblem>,
) {
    val format = FORMAT
    val version = VERSION
    val guide = LogAnalysisGuide

    fun toJson(): String = MAPPER.writeValueAsString(this)

    companion object {
        const val FORMAT = "projectforge-log-analysis"
        const val VERSION = 1
        const val SOURCE_DASHBOARD = "dashboard"
        const val SOURCE_DIGEST = "digest"

        private val MAPPER = jacksonObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT)

        internal fun iso(millis: Long?): String? = millis?.let { Instant.ofEpochMilli(it).toString() }

        /**
         * The groups of a digest period. A group known in the database is completed by its [stored] problem (status,
         * totals, trend), whose id is [ErrorGroup.problemId].
         * @param dashboardUrl The problem dashboard, each problem is linked as `?id=<id>`.
         */
        fun ofDigest(
            snapshot: ErrorDigestCollector.Snapshot,
            fromMillis: Long,
            toMillis: Long,
            instance: String?,
            dashboardUrl: String?,
            stored: Map<Long, LogAnalysisProblem> = emptyMap(),
        ) = LogAnalysisExport(
            source = SOURCE_DIGEST,
            instance = instance,
            generatedAt = iso(toMillis)!!,
            periodFrom = iso(fromMillis),
            periodTo = iso(toMillis),
            problems = snapshot.groups.map { group ->
                val id = group.problemId
                (id?.let { stored[it] } ?: problemOf(group)).copy(
                    id = id,
                    dashboardUrl = dashboardUrl?.let { url -> id?.let { "$url?id=$it" } },
                    periodCount = group.count,
                    periodFirst = iso(group.firstMillis),
                    periodLast = iso(group.lastMillis),
                    novelty = group.novelty,
                    hourlyMean = group.hourlyMean,
                    samples = group.samples.map {
                        LogAnalysisSample(iso(it.timestampMillis)!!, it.level, it.message, it.request, it.stackTrace)
                    },
                )
            },
        )

        /** A group not (yet) in the database: only what the digest knows. */
        private fun problemOf(group: ErrorGroup): LogAnalysisProblem {
            val event = group.event
            return LogAnalysisProblem(
                code = event.code,
                registered = LogEventRegistry.get(event.code) != null,
                category = group.category,
                level = group.level,
                notify = event.notify,
                audience = event.audience,
                threshold = event.threshold,
                explanation = event.explanation,
                action = event.action,
                exceptionClass = group.exceptionClass,
                location = group.location,
                message = group.message,
            )
        }
    }
}

/**
 * One problem (a group of equal errors or warnings) of a [LogAnalysisExport]. Times are ISO-8601 (UTC). The fields
 * up to [sampleRequest] are those of the database, the others those of a digest period.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LogAnalysisProblem(
    val id: Long? = null,
    val code: String,
    val registered: Boolean,
    val category: LogCategory,
    val level: LogLevel,
    val status: LogGroupStatus? = null,
    val notify: LogNotify,
    val overrideNotify: LogNotify? = null,
    val audience: LogAudience,
    val threshold: Int,
    val explanation: String? = null,
    val action: String? = null,
    val exceptionClass: String? = null,
    val location: String? = null,
    val message: String? = null,
    val firstSeen: String? = null,
    val lastSeen: String? = null,
    val totalCount: Long? = null,
    val count24h: Int? = null,
    val distinctUsers24h: Int? = null,
    val regression: Boolean? = null,
    val mutedUntil: String? = null,
    val daily30: List<Int>? = null,
    val sampleStackTrace: String? = null,
    val sampleRequest: String? = null,
    val dashboardUrl: String? = null,
    val periodCount: Int? = null,
    val periodFirst: String? = null,
    val periodLast: String? = null,
    val novelty: DigestNovelty? = null,
    val hourlyMean: Double? = null,
    val samples: List<LogAnalysisSample>? = null,
)

/** A single occurrence of a digest period, without its user. */
@JsonInclude(JsonInclude.Include.NON_NULL)
class LogAnalysisSample(
    val timestamp: String,
    val level: LogLevel,
    val message: String?,
    val request: String?,
    val stackTrace: String?,
)

/** What the fields and values of a [LogAnalysisExport] mean, for the reader of the file (an AI). */
@Suppress("unused")
object LogAnalysisGuide {
    val purpose =
        "Errors and warnings of a ProjectForge installation (a Java/Kotlin Spring Boot web application), grouped into " +
                "problems by the log aggregation: equal errors share a problem (same code, or same exception class, " +
                "location and message with numbers and ids normalized). Use it to analyse causes, to suggest or check a " +
                "classification (category, notify rule) and to propose fixes. Problems with registered=false have no " +
                "classification at the logging call yet: their category is a heuristic one."

    val categories = LogCategory.entries.map { category ->
        mapOf(
            "name" to category.name,
            "title" to category.title,
            "description" to describe(category),
            "defaultNotify" to category.defaultNotify.name,
            "defaultAudience" to category.defaultAudience.name,
        )
    }

    val notify = mapOf(
        LogNotify.NONE.name to "Never mailed, only counted in the dashboard.",
        LogNotify.DIGEST.name to "Listed in every error digest mail (by default hourly) in which it occurred.",
        LogNotify.DIGEST_IF_NEW.name to "Mailed once when new (or occurring again after it was resolved).",
        LogNotify.IMMEDIATE.name to "Sends the error digest early, at most every 5 minutes.",
    )

    val audiences = mapOf(
        LogAudience.DEVELOPER.name to "The developers: bugs and data problems.",
        LogAudience.ADMIN.name to "The administrators of the installation: configuration and external systems.",
        LogAudience.SECURITY.name to "Those responsible for security.",
    )

    val statuses = mapOf(
        LogGroupStatus.NEW.name to "Not looked at yet.",
        LogGroupStatus.ACKNOWLEDGED.name to "Known, being looked at; still reported.",
        LogGroupStatus.IGNORED.name to "Not reported any more.",
        LogGroupStatus.RESOLVED.name to "Fixed; occurring again makes it NEW again (a regression).",
    )

    val novelty = mapOf(
        DigestNovelty.NEW.name to "Never reported before.",
        DigestNovelty.REGRESSION.name to "Resolved by an admin and occurred again.",
        DigestNovelty.SPIKE.name to "Known, but much more often than in the 7 days before.",
        DigestNovelty.KNOWN.name to "Known and reported before.",
    )

    val fields = mapOf(
        "code" to "Stable id of the classified logging call (e.g. sipgate.sync.failed), else a generic one of the category.",
        "registered" to "The code is a known, explicitly classified event with explanation and action.",
        "level" to "The most severe log level of the occurrences.",
        "notify" to "The effective notify rule: overrideNotify (set by an admin) if given, else the event's one.",
        "threshold" to "Reported only from this number of occurrences per digest period on.",
        "location" to "Class and line of the logging call or of the exception (Class:line).",
        "message" to "A sample message, the stack trace and request are of the same occurrence.",
        "totalCount" to "All occurrences since firstSeen.",
        "count24h" to "Occurrences within the last 24 hours.",
        "daily30" to "Occurrences per 24 hours of the last 30 days, oldest first, the last one ending now.",
        "regression" to "A resolved problem that occurred again.",
        "periodCount" to "Digest only: occurrences within the digest period (periodFrom - periodTo).",
        "hourlyMean" to "Digest only: mean occurrences per hour in the 7 days before the period.",
        "samples" to "Digest only: up to ${ErrorGroup.MAX_SAMPLES} single occurrences of the period.",
    )

    private fun describe(category: LogCategory): String = when (category) {
        LogCategory.EXTERNAL -> "A remote system (LDAP, IdP, Sipgate, SMTP, d.velop, ...) isn't reachable or fails."
        LogCategory.SECURITY -> "Suspicious requests, failed authentications, locked accounts."
        LogCategory.DATA -> "Inconsistent or corrupt data in the database or in files."
        LogCategory.BUG -> "Unexpected errors: a bug."
        LogCategory.CONFIG -> "A missing or wrong configuration of this installation."
        LogCategory.CLIENT -> "A client got something wrong (bad request, validation error, expired session): nothing to do on the server."
        LogCategory.UNCLASSIFIED -> "Not classified at all (e.g. a plain WARN)."
    }
}
