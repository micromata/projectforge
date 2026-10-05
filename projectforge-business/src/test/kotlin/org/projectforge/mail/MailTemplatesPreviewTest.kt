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
package org.projectforge.mail

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsPositionsArt
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.MailPreview
import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.integration.SyncStats
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.FlatDisplayHistoryEntry
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.support.ErrorCategory
import org.projectforge.framework.support.ErrorDigestCollector
import org.projectforge.framework.support.ErrorDigestRenderer
import org.projectforge.framework.support.ErrorOccurrence
import org.projectforge.framework.support.SyncProblemTracker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.AopTestUtils
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

/**
 * Renders the mail templates with sample data, as the production code does, and writes them as .eml files, see
 * [MailPreview]. For a visual check, open the .eml files of projectforge-business/build/tmp/mail-preview. The
 * vacation mails are written by VacationSendMailServiceTest, the plugin mails by the tests of the plugins.
 */
class MailTemplatesPreviewTest : AbstractTestBase() {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var sendMail: SendMail

    private val recipient = PFUserDO().also {
        it.firstname = "Kai"
        it.lastname = "Reinhard"
        it.email = "k.reinhard@example.org"
        it.locale = Locale.GERMAN
    }

    /** All mails of this test show the default logo, embedded as inline image. */
    @BeforeEach
    fun configureLogo() {
        // The resource may be packed in a jar, so it is copied to a file first.
        val logo = File(MailPreview.DIR, "default-logo.png").also { file ->
            file.parentFile.mkdirs()
            javaClass.getResourceAsStream("/images/default-logo.png")!!.use { file.writeBytes(it.readBytes()) }
        }
        setLogoFile(logo)
    }

    @AfterEach
    fun resetLogo() {
        setLogoFile(null)
    }

    /** Sets the logo file resolved and cached by ConfigurationService, its setters are protected. */
    private fun setLogoFile(file: File?) {
        val target = AopTestUtils.getUltimateTargetObject<ConfigurationService>(configurationService)
        ConfigurationService::class.java.getDeclaredField("logoFileObject").also {
            it.isAccessible = true
            it.set(target, file)
        }
    }

    @Test
    fun passwordResetMail() {
        val mail = htmlMail(translate(Locale.GERMAN, "password.forgotten.mail.subject"))
        mail.content = sendMail.renderGroovyTemplate(
            mail, "mail/passwordResetMail.html",
            mutableMapOf("link" to "https://projectforge.example.org/next/public/password-reset?token=4711"),
            mail.subject, recipient,
        )
        val eml = MailPreview.write(sendMail, "passwordResetMail", mail)
        Assertions.assertTrue(mail.content.contains("class=\"button\""), mail.content)
        Assertions.assertTrue(mail.content.contains("src=\"${SendMail.LOGO_CID_URL}\""), mail.content)
        val message = eml.inputStream().use { MimeMessage(Session.getInstance(Properties()), it) }
        Assertions.assertTrue(message.contentType.startsWith("multipart/related"), message.contentType)
        val related = message.content as MimeMultipart
        Assertions.assertEquals(2, related.count)
        Assertions.assertTrue(related.getBodyPart(1).contentType.startsWith("image/png"))
        Assertions.assertArrayEquals(arrayOf("<logo@projectforge>"), related.getBodyPart(1).getHeader("Content-ID"))
    }

    @Test
    fun otpMail() {
        val mail = htmlMail(translate(Locale.GERMAN, "user.My2FACode.sendCode.mail.title"))
        mail.content = sendMail.renderGroovyTemplate(
            mail, "mail/otpMail.html", mutableMapOf("otp" to "123456"), mail.subject, recipient,
        )
        MailPreview.write(sendMail, "otpMail", mail)
    }

    @Test
    fun birthdayButlerCronMail() {
        val subject = "${translate(Locale.GERMAN, "birthdayButler.email.subject")} Oktober"
        val mail = htmlMail(subject, to = false)
        mail.setTo("hr@example.org")
        mail.content = sendMail.renderGroovyTemplate(
            mail, "mail/birthdayButlerCronMail.html",
            mutableMapOf("content" to "birthdayButler.email.content", "month" to "Oktober", "listSize" to 7),
            subject, null, Locale.GERMAN,
        )
        Assertions.assertTrue(mail.content.contains("Im Monat"), "German text expected: ${mail.content}")
        val attachment = MailAttachment("Geburtstage_Oktober.docx", "Sample".toByteArray())
        MailPreview.write(sendMail, "birthdayButlerCronMail", mail, listOf(attachment))
    }

    @Test
    fun orderChangeNotification() {
        val order = AuftragDO().also {
            it.id = 4711L
            it.nummer = 4711
            it.titel = "Relaunch Kundenportal (Phase 2)"
            it.kunde = KundeDO().also { k -> k.nummer = 12345L; k.name = "ACME Müller & Söhne GmbH" }
            it.projekt = ProjektDO().also { p -> p.nummer = 2; p.name = "Kundenportal" }
            it.status = AuftragsStatus.BEAUFTRAGT
            it.referenz = "PO-2026-0815"
            it.angebotsDatum = LocalDate.of(2026, 9, 1)
            it.bindungsFrist = LocalDate.of(2026, 10, 31)
            it.beauftragungsDatum = LocalDate.of(2026, 9, 15)
            it.contactPerson = recipient
            it.bemerkung = "Abrechnung monatlich nach Aufwand."
            it.addPosition(position("Konzeption und Design", "12000", AuftragsPositionsArt.NEUENTWICKLUNG))
            it.addPosition(position("Umsetzung Frontend", "48500", AuftragsPositionsArt.NEUENTWICKLUNG))
        }
        val subject = "Auftrag #4711 wurde geändert."
        val mail = htmlMail(subject)
        val data = mutableMapOf<String, Any?>(
            "contactPerson" to recipient,
            "auftrag" to order,
            "requestUrl" to "https://projectforge.example.org/next/order/4711",
            "subject" to subject,
            "history" to listOf(
                history("status", "BEAUFTRAGT", "LOI"),
                history("beauftragungsDatum", "2026-09-15", null),
            ),
        )
        mail.content = sendMail.renderGroovyTemplate(
            mail, "mail/orderChangeNotification.html", data, translate(Locale.GERMAN, "fibu.auftrag"), recipient,
        )
        MailPreview.write(sendMail, "orderChangeNotification", mail)
    }

    @Test
    fun errorDigestMail() {
        val collector = ErrorDigestCollector()
        collector.add(digestOccurrence(ErrorCategory.EXTERNAL_UNREACHABLE, "Gateway not reachable at https://gw.example.org/heartbeat, sync skipped: 502 Bad Gateway", "GatewaySyncPushService:490", LogLevel.WARN))
        repeat(3) { collector.add(digestOccurrence(ErrorCategory.REQUEST_ERROR, "Cannot invoke \"String.length()\" because \"name\" is null", "AddressDao:212", LogLevel.ERROR, user = "kai", stackTrace = "java.lang.NullPointerException: name is null\n\tat org.projectforge.business.address.AddressDao.select(AddressDao.kt:212)\n")) }
        collector.add(digestOccurrence(ErrorCategory.ERROR_NO_TRACE, "Unable to gather subscription calendar #1322396 information, received statusCode: 404", "TeamEventSubscription:282", LogLevel.WARN))
        val snapshot = collector.drain()
        val stats = SyncStats("gateway-push").also { it.startRun().abort("Gateway not reachable") }
        val problems = SyncProblemTracker { listOf(stats) }.collect()
        val renderer = ErrorDigestRenderer("https://projectforge.example.org", ZoneOffset.UTC)
        val mail = htmlMail(renderer.subject(snapshot, problems), to = false)
        mail.setTo("support@example.org")
        mail.content = sendMail.renderGroovyTemplate(
            mail, "mail/errorDigestMail.html",
            renderer.htmlData(snapshot, problems, 0L, 3_600_000L, "error-digest.txt"), "Error digest", null,
        )
        val attachment = MailAttachment("error-digest.txt", renderer.details(snapshot).toByteArray(Charsets.UTF_8))
        MailPreview.write(sendMail, "errorDigestMail", mail, listOf(attachment))
    }

    private fun htmlMail(subject: String, to: Boolean = true) = Mail().also {
        it.subject = subject
        it.contentType = Mail.CONTENTTYPE_HTML
        if (to) {
            it.setTo(recipient)
        }
    }

    private fun position(titel: String, netSum: String, art: AuftragsPositionsArt) = AuftragsPositionDO().also {
        it.titel = titel
        it.nettoSumme = BigDecimal(netSum)
        it.art = art
        it.status = AuftragsStatus.BEAUFTRAGT
    }

    private fun history(property: String, newValue: String?, oldValue: String?) = FlatDisplayHistoryEntry().also {
        it.timestamp = Date()
        it.user = recipient
        it.opType = EntityOpType.Update
        it.propertyName = property
        it.newValue = newValue
        it.oldValue = oldValue
    }

    private fun digestOccurrence(
        category: ErrorCategory,
        message: String,
        location: String,
        level: LogLevel,
        user: String? = null,
        stackTrace: String? = null,
    ) = ErrorOccurrence(
        timestampMillis = 1000L,
        level = level,
        category = category,
        exceptionClass = stackTrace?.let { "java.lang.NullPointerException" },
        message = message,
        location = location,
        stackTrace = stackTrace,
        user = user,
    )
}
