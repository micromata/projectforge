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

package org.projectforge.security

import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.slf4j.LoggerFactory

/**
 * Centralized handler for security incidents where a user mismatch is detected
 * (e.g., between Wicket session and HTTP session, or within UserGroupCache lookups).
 *
 * Logs the incident with full details using [EmphasizedLogSupport] and sends an alert
 * email to the configured support address.
 */
object SessionUserMismatchHandler {
    private val log = LoggerFactory.getLogger(SessionUserMismatchHandler::class.java)

    /**
     * Reports a user mismatch security incident. Logs emphasized error details and sends
     * an alert email to the configured support address (if mail is configured).
     *
     * @param title Short title for the incident (used in log heading and email subject).
     * @param description Multi-line description of the incident for the log output.
     * @param incidentDetails Structured details (usernames, IDs, session info, etc.) for log and email.
     * @param action Description of the action taken (e.g., "Invalidating session and forcing re-login.").
     */
    @JvmStatic
    fun handleUserMismatch(
        title: String,
        description: List<String>,
        incidentDetails: String,
        action: String,
    ) {
        val emphasizedLog = EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.VERY_IMPORTANT, EmphasizedLogSupport.Alignment.LEFT)
            .setLogLevel(EmphasizedLogSupport.LogLevel.ERROR)
            .log(title)
            .log("")
        description.forEach { emphasizedLog.log(it) }
        emphasizedLog
            .log("")
            .log(incidentDetails)
            .log("")
            .log("Action taken: $action")
            .logEnd()

        sendAlertEmail(title, incidentDetails, action)
    }

    /**
     * Builds a formatted incident details string from the given user and context information.
     *
     * @param expectedUser The user that was expected (e.g., from HTTP session or original request).
     * @param actualUser The user that was actually found (e.g., from Wicket session or cache).
     * @param expectedLabel Label for the expected user (e.g., "HTTP session user").
     * @param actualLabel Label for the actual user (e.g., "Wicket session user").
     * @param additionalInfo Additional key-value pairs to include in the details (e.g., session IDs, IPs).
     */
    @JvmStatic
    @JvmOverloads
    fun buildIncidentDetails(
        expectedUser: PFUserDO,
        actualUser: PFUserDO,
        expectedLabel: String,
        actualLabel: String,
        additionalInfo: Map<String, String?> = emptyMap(),
    ): String {
        val maxLabelLen = maxOf(expectedLabel.length, actualLabel.length, additionalInfo.keys.maxOfOrNull { it.length } ?: 0)
        val sb = StringBuilder()
        sb.appendLine("${actualLabel.padEnd(maxLabelLen)}: ${actualUser.username} (id=${actualUser.id}, name=${actualUser.getFullname()})")
        sb.appendLine("${expectedLabel.padEnd(maxLabelLen)}: ${expectedUser.username} (id=${expectedUser.id}, name=${expectedUser.getFullname()})")
        additionalInfo.forEach { (key, value) ->
            sb.appendLine("${key.padEnd(maxLabelLen)}: ${value ?: "n/a"}")
        }
        return sb.toString().trimEnd()
    }

    private fun sendAlertEmail(title: String, incidentDetails: String, action: String) {
        try {
            val appContext = ApplicationContextProvider.getApplicationContext() ?: run {
                log.warn("ApplicationContext not available. Cannot send security alert email.")
                return
            }
            val configService = appContext.getBean(ConfigurationService::class.java)
            val supportAddress = configService.pfSupportMailAddress
            if (supportAddress.isNullOrBlank() || !configService.isSendMailConfigured) {
                log.info("Mail not configured or no support address set. Security alert email not sent.")
                return
            }
            val sendMail = appContext.getBean(SendMail::class.java)
            val domain = appContext.getBean(DomainService::class.java).domain ?: "unknown"

            val mail = Mail()
            mail.addTo(supportAddress)
            sendMail.mailFromStandardEmailSender?.takeIf { it.isNotBlank() }?.let { mail.setFrom(it) }
            mail.setProjectForgeSubject("SECURITY: $title on $domain")
            mail.content = "A user mismatch security incident was detected and the affected user session has been invalidated.\n\n" +
                    incidentDetails +
                    "\n\nAction taken: $action" +
                    "\n\nThis may indicate DiskPageStore corruption, session mix-up, or a cross-session data leak." +
                    "\nPlease investigate server logs around this timestamp for further details."
            sendMail.send(mail, null, null, true) // synchronous to ensure delivery before session invalidation
        } catch (ex: Exception) {
            log.error("Failed to send security alert email for user mismatch incident.", ex)
        }
    }
}
