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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.ExceptionStackTracePrinter
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * Mails an unexpected server error to the support team ([ConfigurationService.pfSupportMailAddress]), as
 * Wicket's ErrorPage did (`sendProactiveMessageToSupportTeam`): the date, the user and the root cause's stack
 * trace. Only for exceptions [GlobalExceptionRegistry.sendMailToDevelopers] doesn't exempt, and only where
 * a support address and mail are configured.
 *
 * Unlike the error page, which a user had to reach, this is called for every failing request, so one bug
 * hit in a loop must not flood the support inbox: the same exception (class and message) is mailed at most
 * once per [THROTTLE_MILLIS]. Sent asynchronously and never throws, the request's answer is not delayed.
 */
@Service
class SupportErrorMailer {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var sendMail: SendMail

    internal val throttle = Throttle(THROTTLE_MILLIS)

    fun report(ex: Throwable) {
        try {
            if (!GlobalExceptionRegistry.sendMailToDevelopers(ex)) {
                return
            }
            val supportAddress = configurationService.pfSupportMailAddress
            if (supportAddress.isNullOrBlank() || !configurationService.isSendMailConfigured) {
                log.debug { "No support mail address or no mail configured, error not mailed to the support team." }
                return
            }
            if (!throttle.acquire(throttleKey(ex))) {
                log.info { "Same error mailed to the support team within the last 10 minutes, not mailed again." }
                return
            }
            log.info { "Sending proactive mail to support." }
            val mail = Mail()
            mail.addTo(supportAddress)
            sendMail.mailFromStandardEmailSender?.takeIf { it.isNotBlank() }?.let { mail.setFrom(it) }
            mail.setProjectForgeSubject("Error occurred on ${domainService.domain}: ${ex::class.java.simpleName}")
            mail.content = description(ex)
            mail.contentType = Mail.CONTENTTYPE_TEXT
            sendMail.send(mail, async = true)
        } catch (t: Throwable) {
            log.error("Failed to mail the error to the support team: ${t.message}", t)
        }
    }

    private fun description(ex: Throwable): String {
        val now = ZonedDateTime.now()
        val user = ThreadLocalUserContext.loggedInUser
        val rootCause = generateSequence(ex) { it.cause.takeIf { cause -> cause !== it } }.last()
        return "Error occurred at: ${now.format(DATE_FORMAT)} (${now.zone.id})" +
                " from user: ${user?.let { "${it.username} (${it.getFullname()})" } ?: "-"}\n\n" +
                "Exception: ${ex::class.java.name}: ${ex.message}\n\n" +
                "Root cause stack trace:\n" +
                ExceptionStackTracePrinter.toString(rootCause, stopBeforeForeignPackages = false, depth = 40)
    }

    /**
     * Lets a key pass at most once per [intervalMillis]. Bounded: once [MAX_KEYS] keys are held, the expired
     * ones are dropped, and if that isn't enough, all of them (an error storm of distinct messages is worth a
     * mail again then).
     */
    internal class Throttle(
        private val intervalMillis: Long,
        private val clock: () -> Long = System::currentTimeMillis,
    ) {
        private val lastSent = ConcurrentHashMap<String, Long>()

        fun acquire(key: String): Boolean {
            val now = clock()
            if (lastSent.size >= MAX_KEYS) {
                lastSent.entries.removeIf { now - it.value >= intervalMillis }
                if (lastSent.size >= MAX_KEYS) {
                    lastSent.clear()
                }
            }
            var acquired = false
            lastSent.compute(key) { _, last ->
                if (last == null || now - last >= intervalMillis) {
                    acquired = true
                    now
                } else {
                    last
                }
            }
            return acquired
        }
    }

    companion object {
        internal const val THROTTLE_MILLIS = 10 * 60 * 1000L
        private const val MAX_KEYS = 1000
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        internal fun throttleKey(ex: Throwable): String = "${ex::class.java.name}: ${ex.message}"
    }
}
