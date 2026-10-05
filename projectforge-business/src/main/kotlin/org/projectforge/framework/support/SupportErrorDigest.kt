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

import jakarta.annotation.PostConstruct
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.Constants
import org.projectforge.ShutdownListener
import org.projectforge.ShutdownService
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.common.logging.LogAudience
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogEventListener
import org.projectforge.common.logging.LogNotify
import org.projectforge.common.logging.LoggerMemoryAppender
import org.projectforge.common.logging.LoggingEventData
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.convert.DurationStyle
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

/**
 * Collects the errors of the running system and mails them to the support team
 * ([ConfigurationService.pfSupportMailAddress]) as one digest per interval
 * (`projectforge.support.errorDigest.interval`, default 1 hour, 0 disables it), instead of one mail per
 * error as Wicket's error page did. Nothing collected, no mail.
 *
 * Sources:
 * - unexpected exceptions of requests ([recordRequestError], called by the REST exception handler),
 * - every logged ERROR, and WARNs that look like an unreachable system, via [LoggerMemoryAppender] — most
 *   failures of external systems are caught in background jobs and only logged (see [ErrorOccurrenceFactory]),
 * - every logged event classified by a [LogEvent] (of the logging call or the exception), whatever its level,
 * - sync runs that failed since the last digest ([SyncProblemTracker]).
 *
 * Whether an error is reported is decided by the notify rule of its [LogEvent], not by its level: [LogNotify.NONE]
 * is never mailed, [LogNotify.DIGEST_IF_NEW] only once, groups below the event's threshold not at all and
 * [LogNotify.IMMEDIATE] sends the digest early (see [DigestNotifyFilter]), as does a new problem (or regression) of the
 * [alarm categories][ALARM_CATEGORIES], security and data, once it reaches its threshold; early, but at most every
 * 5 minutes. An admin may ignore, mute or resolve a
 * problem or override its rule, see [LogGroupDO].
 *
 * Every collected occurrence, reported or not, is also counted in the database by [LogAggregationService] (if
 * enabled), also without a support mail address.
 *
 * The digest groups equal errors and lists the new problems, regressions and spikes first, then the known ones by
 * category, unreachable external systems first (see [ErrorDigestRenderer]); each group shows the explanation and
 * recommended action of its event and links the problem in the problem dashboard, the single occurrences with their
 * stack traces are attached as a text file, and everything once more as JSON for an analysis by an AI
 * ([LogAnalysisExport]).
 *
 * Each problem is mailed to the recipients of its event's [audience][LogEvent.audience]
 * (`projectforge.support.errorDigest.recipients.<developer|admin|security>`, default: the support mail address),
 * one mail per recipients ([splitByRecipients]).
 *
 * On shutdown (before the database is closed, see [ShutdownService]) the last digest is sent and the log
 * aggregation is closed.
 */
@Service
class SupportErrorDigest : LogEventListener, ShutdownListener {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var shutdownService: ShutdownService

    /** Optional: tests without Spring work without it. */
    @Autowired(required = false)
    internal var logAggregation: LogAggregationService? = null

    /** Optional as well: completes the analysis attachment by the problems' data in the database. */
    @Autowired(required = false)
    internal var logGroupAdminService: LogGroupAdminService? = null

    @Value("\${projectforge.support.errorDigest.interval:1h}")
    private var intervalProperty: String = "1h"

    @Value("\${projectforge.support.errorDigest.recipients.developer:}")
    internal var developerRecipients: String = ""

    @Value("\${projectforge.support.errorDigest.recipients.admin:}")
    internal var adminRecipients: String = ""

    @Value("\${projectforge.support.errorDigest.recipients.security:}")
    internal var securityRecipients: String = ""

    internal val collector = ErrorDigestCollector()

    private val notifyFilter by lazy { DigestNotifyFilter(logAggregation?.takeIf { it.enabled }) }

    /** An occurrence with [LogNotify.IMMEDIATE] is waiting: the digest is sent early. */
    @Volatile
    internal var immediatePending = false
        private set

    private val syncProblemTracker = SyncProblemTracker()

    private var interval: Duration = Duration.ZERO

    /** The digest is mailed: a support mail address and mail are configured. */
    @Volatile
    internal var active = false

    /** Occurrences are collected: for the digest or for the log aggregation. */
    @Volatile
    private var listening = false

    @Volatile
    private var periodStart = System.currentTimeMillis()

    /** Set while the digest is sent: errors of the mail delivery itself are not collected again. */
    private val sending = ThreadLocal.withInitial { false }

    @PostConstruct
    internal fun init() {
        shutdownService.registerListener(this)
        interval = parseInterval(intervalProperty)
        if (interval.isZero) {
            log.info { "Support error digest disabled (projectforge.support.errorDigest.interval=0)." }
        } else if (LogAudience.entries.all { recipientsOf(it) == null } || !configurationService.isSendMailConfigured) {
            log.info {
                "Support error digest inactive: no support mail address (projectforge.support.mail or " +
                        "projectforge.support.errorDigest.recipients.*) or no mail configured."
            }
        } else {
            active = true
            periodStart = System.currentTimeMillis()
            val recipients = LogAudience.entries.joinToString { "${it.name.lowercase()}: ${recipientsOf(it) ?: "-"}" }
            log.info { "Support error digest active, sent at most every $interval to $recipients." }
        }
        if (!active && logAggregation?.enabled != true) {
            return
        }
        if (!LoggerMemoryAppender.isInitialized()) {
            log.warn { "Support error digest: LoggerMemoryAppender not initialized, logged errors aren't collected." }
        } else {
            LoggerMemoryAppender.getInstance().addListener(this)
        }
        listening = true
    }

    override fun onEvent(event: LoggingEventData) {
        if (!listening || sending.get()) {
            return
        }
        ErrorOccurrenceFactory.fromLogEvent(event)?.let { collect(it) }
    }

    /**
     * An unexpected exception of a request, already checked by the caller to be worth reporting.
     * @param request Method and uri of the request.
     * @param external True if a remote system isn't reachable (default: detected from the exception).
     * @param logEvent The classification, if known (e.g. by `GlobalExceptionRegistry`).
     */
    fun recordRequestError(ex: Throwable, request: String?, external: Boolean? = null, logEvent: LogEvent? = null) {
        if (!listening) {
            return
        }
        try {
            val user = ThreadLocalUserContext.loggedInUser?.username
            val occurrence = if (external == null) {
                ErrorOccurrenceFactory.fromRequestException(ex, user, request, logEvent = logEvent)
            } else {
                ErrorOccurrenceFactory.fromRequestException(ex, user, request, external, logEvent = logEvent)
            }
            collect(occurrence)
        } catch (t: Throwable) {
            log.warn { "Couldn't record error for the support digest: ${t.message}" }
        }
    }

    /**
     * Counted by the log aggregation in any case. Collected for the digest by the notify rule (of the problem's
     * override or the occurrence's event), not by its level: [LogNotify.NONE] isn't reported at all.
     */
    internal fun collect(occurrence: ErrorOccurrence) {
        val key = ErrorDigestCollector.keyOf(occurrence)
        val aggregation = logAggregation?.takeIf { it.enabled }
        aggregation?.add(occurrence, key)
        if (!active) {
            return
        }
        val state = aggregation?.stateOf(key)
        val notify = DigestNotifyFilter.notifyOf(occurrence, state)
        if (notify == LogNotify.NONE) {
            return
        }
        val count = collector.add(occurrence)
        if (immediatePending || state?.muted() == true) {
            return
        }
        if (notify == LogNotify.IMMEDIATE ||
            occurrence.category in ALARM_CATEGORIES && count == maxOf(1, occurrence.event.threshold) &&
            notifyFilter.isUnreported(key, state)
        ) {
            immediatePending = true
        }
    }

    @Scheduled(fixedDelay = Constants.MILLIS_PER_MINUTE, initialDelay = Constants.MILLIS_PER_MINUTE)
    fun sendIfDue() {
        val elapsed = System.currentTimeMillis() - periodStart
        if (active && (elapsed >= interval.toMillis() || immediatePending && elapsed >= IMMEDIATE_MIN_GAP_MILLIS)) {
            send()
        }
    }

    /** Called by [ShutdownService] before the database is closed: a later `@PreDestroy` couldn't write anymore. */
    override fun shutdown() {
        if (listening) {
            if (LoggerMemoryAppender.isInitialized()) {
                LoggerMemoryAppender.getInstance().removeListener(this)
            }
            listening = false
            if (active) {
                send()
                active = false
            }
        }
        logAggregation?.close()
    }

    /**
     * The addresses (comma separated) of an audience: `projectforge.support.errorDigest.recipients.<audience>`, else
     * the support mail address. Null if neither is configured.
     */
    internal fun recipientsOf(audience: LogAudience): String? =
        when (audience) {
            LogAudience.DEVELOPER -> developerRecipients
            LogAudience.ADMIN -> adminRecipients
            LogAudience.SECURITY -> securityRecipients
        }.takeIf { it.isNotBlank() } ?: configurationService.pfSupportMailAddress?.takeIf { it.isNotBlank() }

    private fun send() {
        val from = periodStart
        val to = System.currentTimeMillis()
        periodStart = to
        immediatePending = false
        // The problems' states must be up to date: a new one is in the database only after its first flush.
        logAggregation?.flush()
        val snapshot = notifyFilter.apply(collector.drain(), to, from)
        val syncProblems = syncProblemTracker.collect()
        splitByRecipients(snapshot, syncProblems) { recipientsOf(it) }.forEach { send(it, from, to) }
    }

    private fun send(digest: DigestMail, from: Long, to: Long) {
        val snapshot = digest.snapshot
        val syncProblems = digest.syncProblems
        sending.set(true)
        try {
            val dashboardUrl = domainService.getDomain(LogGroupAdminService.DASHBOARD_PATH)
            val renderer = ErrorDigestRenderer(domainService.domain, dashboardUrl = dashboardUrl)
            val baseName = "error-digest-${LocalDateTime.now().format(FILENAME_FORMAT)}"
            val attachmentName = if (snapshot.groups.isNotEmpty()) "$baseName.txt" else null
            val mail = Mail()
            digest.recipients.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }.forEach { mail.addTo(it) }
            sendMail.mailFromStandardEmailSender?.takeIf { it.isNotBlank() }?.let { mail.setFrom(it) }
            mail.setProjectForgeSubject(renderer.subject(snapshot, syncProblems))
            val html = try {
                sendMail.renderGroovyTemplate(
                    mail, "mail/errorDigestMail.html",
                    renderer.htmlData(snapshot, syncProblems, from, to, attachmentName), "Error digest", null,
                ).takeIf { it.isNotBlank() }
            } catch (t: Throwable) {
                log.warn { "Can't render the html support error digest, sending it as plain text: ${t.message}" }
                null
            }
            if (html != null) {
                mail.content = html
                mail.contentType = Mail.CONTENTTYPE_HTML
            } else {
                // The digest must never fail because of its template.
                mail.content = renderer.body(snapshot, syncProblems, from, to, attachmentName)
                mail.contentType = Mail.CONTENTTYPE_TEXT
            }
            val attachments = attachmentName?.let {
                listOfNotNull(
                    MailAttachment(it, renderer.details(snapshot).toByteArray(Charsets.UTF_8)),
                    analysisJson(snapshot, from, to, dashboardUrl)?.let { json ->
                        MailAttachment("$baseName.json", json.toByteArray(Charsets.UTF_8))
                    },
                )
            }
            log.info {
                "Sending support error digest to ${digest.recipients}: ${snapshot.occurrences} occurrences, ${syncProblems.size} sync problems."
            }
            // Synchronously: in the scheduler's thread (or on shutdown), where [sending] keeps its errors out.
            sendMail.send(mail, null, attachments, async = false)
        } catch (t: Throwable) {
            log.warn { "Failed to send the support error digest: ${t.message}" }
        } finally {
            sending.set(false)
        }
    }

    /**
     * The groups as [LogAnalysisExport], for an analysis by an AI; completed by the problems' data in the database, if
     * the log aggregation is enabled. Null if that fails: the digest is sent anyway.
     */
    private fun analysisJson(snapshot: ErrorDigestCollector.Snapshot, from: Long, to: Long, dashboardUrl: String?): String? =
        try {
            val stored = logGroupAdminService?.takeIf { logAggregation?.enabled == true }
                ?.analysisProblems(snapshot.groups.mapNotNull { it.problemId }, to)
                ?.associateBy { it.id!! }
                .orEmpty()
            LogAnalysisExport.ofDigest(snapshot, from, to, domainService.domain, dashboardUrl, stored).toJson()
        } catch (t: Throwable) {
            log.warn { "Can't build the analysis attachment (json) of the support error digest: ${t.message}" }
            null
        }

    /** One mail of a digest: the groups of the audiences mailed to [recipients]. */
    internal class DigestMail(
        val recipients: String,
        val snapshot: ErrorDigestCollector.Snapshot,
        val syncProblems: List<SyncProblemTracker.Problem>,
    )

    companion object {
        private val FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")

        /** The problem dashboard, linked by the digest. */

        /**
         * One mail per recipients, with the groups of the audiences ([LogEvent.audience]) they are configured for;
         * the sync problems go to the admins. Groups whose audience has no recipients aren't mailed. The counts of
         * the groups not listed are repeated in every mail. Nothing reported, no mail.
         */
        internal fun splitByRecipients(
            snapshot: ErrorDigestCollector.Snapshot,
            syncProblems: List<SyncProblemTracker.Problem>,
            recipientsOf: (LogAudience) -> String?,
        ): List<DigestMail> {
            val groups = snapshot.groups.groupBy { recipientsOf(it.event.audience) }
            val syncRecipients = recipientsOf(LogAudience.ADMIN).takeIf { syncProblems.isNotEmpty() }
            return (groups.keys + syncRecipients).filterNotNull().distinct().map { to ->
                DigestMail(
                    to,
                    ErrorDigestCollector.Snapshot(groups[to].orEmpty(), snapshot.dropped, snapshot.suppressed, snapshot.muted),
                    if (to == syncRecipients) syncProblems else emptyList(),
                )
            }
        }

        /** [LogNotify.IMMEDIATE] and alarms send the digest early, but not more often than this. */
        private const val IMMEDIATE_MIN_GAP_MILLIS = 5 * Constants.MILLIS_PER_MINUTE

        /**
         * A new problem of these categories (or a regression) is an alarm: it sends the digest early, as soon as it
         * reaches its event's threshold, instead of waiting for the end of the interval.
         */
        internal val ALARM_CATEGORIES = setOf(LogCategory.SECURITY, LogCategory.DATA)

        /** `1h`, `15m`, `PT2H`, a plain number in milliseconds; blank or invalid is the default of 1 hour. */
        internal fun parseInterval(value: String?): Duration {
            if (value.isNullOrBlank()) {
                return Duration.ofHours(1)
            }
            return try {
                DurationStyle.detectAndParse(value.trim()).let { if (it.isNegative) Duration.ZERO else it }
            } catch (ex: IllegalArgumentException) {
                log.warn { "Invalid projectforge.support.errorDigest.interval '$value', using 1h." }
                Duration.ofHours(1)
            }
        }
    }
}
