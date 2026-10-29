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

package org.projectforge.business.fibu

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.framework.integration.IntegrationErrors
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.I18nHelper.getLocalizedMessage
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Sends the daily digest of the order changes of the users who chose [OrderNotificationMode.DAILY]: one mail per user,
 * listing each changed order once with a link (the details are in the order itself). The changes are queued by
 * [AuftragDao.sendNotificationIfRequired] ([OrderNotificationQueueDao]).
 */
@Service
class OrderNotificationDigestService {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var queueDao: OrderNotificationQueueDao

    @Autowired
    private lateinit var sendMail: SendMail

    /** Not set in tests without Spring. */
    @Autowired(required = false)
    internal var schedulerJobRunner: SchedulerJobRunner? = null

    @PostConstruct
    internal fun postConstruct() {
        schedulerJobRunner?.register(SCHEDULER_JOB, ::digestJob)
    }

    @Scheduled(cron = CRON)
    fun digestJob() {
        val runner = schedulerJobRunner ?: return run { Thread({ sendDigests() }, "OrderNotificationDigest").start() }
        // A run is skipped while the previous one is still running.
        runner.runAsync(SCHEDULER_JOB) { run -> run.note("${sendDigests()} digest mails sent.") }
    }

    /**
     * Sends one mail per recipient with queued changes and removes the entries. Entries of a mail that couldn't be sent
     * stay queued for the next run; entries of recipients who are deleted or deactivated are dropped. Orders the
     * recipient can't read any more are left out of the mail.
     * @return The number of digest mails sent.
     */
    fun sendDigests(): Int {
        if (!configurationService.isSendMailConfigured) {
            return 0
        }
        var sent = 0
        val userGroupCache = UserGroupCache.getInstance()
        for ((recipientId, entries) in queueDao.selectAllByRecipient()) {
            try {
                val recipient = userGroupCache.getUser(recipientId)
                if (recipient == null || recipient.deleted || recipient.deactivated) {
                    queueDao.delete(entries)
                    continue
                }
                val readable = entries.filter { isReadable(recipient, it) }
                if (readable.isNotEmpty() && !sendDigest(recipient, readable)) {
                    continue // Mail not sent: try again in the next run.
                }
                queueDao.delete(entries)
                if (readable.isNotEmpty()) sent++
            } catch (ex: Exception) {
                log.error(ex) { "Can't send the order digest to user #$recipientId: ${ex.message}" }
                if (IntegrationErrors.isConnectionError(ex)) {
                    // The other mails would fail the same way, each one waiting for the SMTP timeout.
                    log.warn { "Mail server unreachable, the remaining order digests stay queued." }
                    return sent
                }
            }
        }
        return sent
    }

    // A deleted order isn't loadable any more: the recipient was allowed to read it when the change was queued.
    private fun isReadable(recipient: PFUserDO, entry: OrderNotificationQueueDO): Boolean {
        if (entry.operation == OperationType.DELETE.name) return true
        val auftrag = auftragDao.find(entry.orderId, checkAccess = false) ?: return false
        return auftragDao.hasAccess(recipient, auftrag, null, OperationType.SELECT, false)
    }

    private fun sendDigest(recipient: PFUserDO, entries: List<OrderNotificationQueueDO>): Boolean {
        val subject = getLocalizedMessage(recipient.locale, "fibu.auftrag.mail.digest.subject", entries.size)
        val msg = Mail()
        msg.setTo(recipient)
        msg.setProjectForgeSubject(subject)
        val data: MutableMap<String, Any?> = HashMap()
        data["subject"] = subject
        data["entries"] = entries.map {
            mapOf(
                "number" to it.orderNumber,
                "title" to it.orderTitle,
                "operation" to it.operation,
                "link" to it.link,
            )
        }
        msg.content = sendMail.renderGroovyTemplate(
            msg, "mail/orderDigestNotification.html", data, getLocalizedMessage(recipient.locale, "fibu.auftrag"), recipient,
        )
        msg.contentType = Mail.CONTENTTYPE_HTML
        return sendMail.send(msg, null, null, async = false)
    }

    companion object {
        private const val CRON = "\${projectforge.order.notification.digestCron:0 0 7 * * *}"

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "fibu.orderNotificationDigest", SchedulerJobArea.BUSINESS, OrderNotificationDigestService::class.java,
            "digestJob", SchedulerSchedule.Cron(CRON),
        )
    }
}
