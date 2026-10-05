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
import jakarta.annotation.PreDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.common.logging.LogEventListener
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
 * - sync runs that failed since the last digest ([SyncProblemTracker]).
 *
 * The digest groups equal errors and sorts them by severity, unreachable external systems first; the
 * single occurrences with their stack traces are attached as a text file.
 */
@Service
class SupportErrorDigest : LogEventListener {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var sendMail: SendMail

    @Value("\${projectforge.support.errorDigest.interval:1h}")
    private var intervalProperty: String = "1h"

    internal val collector = ErrorDigestCollector()

    private val syncProblemTracker = SyncProblemTracker()

    private var interval: Duration = Duration.ZERO

    @Volatile
    private var active = false

    @Volatile
    private var periodStart = System.currentTimeMillis()

    /** Set while the digest is sent: errors of the mail delivery itself are not collected again. */
    private val sending = ThreadLocal.withInitial { false }

    @PostConstruct
    internal fun init() {
        interval = parseInterval(intervalProperty)
        if (interval.isZero) {
            log.info { "Support error digest disabled (projectforge.support.errorDigest.interval=0)." }
            return
        }
        if (configurationService.pfSupportMailAddress.isNullOrBlank() || !configurationService.isSendMailConfigured) {
            log.info { "Support error digest inactive: no support mail address (projectforge.support.mail) or no mail configured." }
            return
        }
        if (!LoggerMemoryAppender.isInitialized()) {
            log.warn { "Support error digest: LoggerMemoryAppender not initialized, logged errors aren't collected." }
        } else {
            LoggerMemoryAppender.getInstance().addListener(this)
        }
        active = true
        periodStart = System.currentTimeMillis()
        log.info { "Support error digest active, sent at most every $interval to ${configurationService.pfSupportMailAddress}." }
    }

    override fun onEvent(event: LoggingEventData) {
        if (!active || sending.get()) {
            return
        }
        ErrorOccurrenceFactory.fromLogEvent(event)?.let { collector.add(it) }
    }

    /**
     * An unexpected exception of a request, already checked by the caller to be worth reporting.
     * @param request Method and uri of the request.
     * @param external True if a remote system isn't reachable (default: detected from the exception).
     */
    fun recordRequestError(ex: Throwable, request: String?, external: Boolean? = null) {
        if (!active) {
            return
        }
        try {
            val user = ThreadLocalUserContext.loggedInUser?.username
            val occurrence = if (external == null) {
                ErrorOccurrenceFactory.fromRequestException(ex, user, request)
            } else {
                ErrorOccurrenceFactory.fromRequestException(ex, user, request, external)
            }
            collector.add(occurrence)
        } catch (t: Throwable) {
            log.warn { "Couldn't record error for the support digest: ${t.message}" }
        }
    }

    @Scheduled(fixedDelay = Constants.MILLIS_PER_MINUTE, initialDelay = Constants.MILLIS_PER_MINUTE)
    fun sendIfDue() {
        if (active && System.currentTimeMillis() - periodStart >= interval.toMillis()) {
            send()
        }
    }

    @PreDestroy
    internal fun shutdown() {
        if (!active) {
            return
        }
        if (LoggerMemoryAppender.isInitialized()) {
            LoggerMemoryAppender.getInstance().removeListener(this)
        }
        send()
        active = false
    }

    private fun send() {
        val from = periodStart
        val to = System.currentTimeMillis()
        periodStart = to
        val snapshot = collector.drain()
        val syncProblems = syncProblemTracker.collect()
        if (snapshot.groups.isEmpty() && syncProblems.isEmpty()) {
            return
        }
        sending.set(true)
        try {
            val renderer = ErrorDigestRenderer(domainService.domain)
            val attachmentName = if (snapshot.groups.isNotEmpty()) {
                "error-digest-${LocalDateTime.now().format(FILENAME_FORMAT)}.txt"
            } else null
            val mail = Mail()
            mail.addTo(configurationService.pfSupportMailAddress)
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
                listOf(MailAttachment(it, renderer.details(snapshot).toByteArray(Charsets.UTF_8)))
            }
            log.info { "Sending support error digest: ${snapshot.occurrences} occurrences, ${syncProblems.size} sync problems." }
            // Synchronously: in the scheduler's thread (or on shutdown), where [sending] keeps its errors out.
            sendMail.send(mail, null, attachments, async = false)
        } catch (t: Throwable) {
            log.warn { "Failed to send the support error digest: ${t.message}" }
        } finally {
            sending.set(false)
        }
    }

    companion object {
        private val FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")

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
