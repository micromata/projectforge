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


package org.projectforge.business.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.UserDao
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.json.JsonUtils
import org.projectforge.mail.MailSendException
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import jakarta.mail.MessagingException
import java.net.SocketTimeoutException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class NotificationServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var creditorInvoicesDueRuleHandler: CreditorInvoicesDueRuleHandler

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var notificationRuleDao: NotificationRuleDao

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var userDao: UserDao

    @Test
    fun cascadeTest() {
        val user = initTestDB.addUser("notification-cascade-user")
        val rule = insertRule(
            NotificationRuleType.MANUAL, user,
            NotificationDelivery(
                listOf(
                    NotificationDeliveryStep(NotificationChannel.IN_APP, 0),
                    NotificationDeliveryStep(NotificationChannel.MAIL, 60, onlyIfUnacknowledged = true),
                )
            ),
        )
        assertEquals(1, notificationService.trigger(rule).created)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        assertEquals(user.id, notification.recipientId)
        assertEquals("Hello ${user.firstname ?: ""}".trim(), notification.title)
        assertNotNull(notification.inAppSince, "The in-app step is delivered at once.")
        assertTrue(notification.isVisibleInApp)
        assertEquals(Date(notification.created!!.time + 60 * 60_000L), notification.nextDeliveryAt)
        assertEquals(listOf(notification.id), notificationDao.findVisible(user.id!!).map { it.id })
        assertEquals(1, notificationDao.summary(user.id!!).openCount)
        // Not yet due:
        notificationService.runDeliveryJob(Date(notification.created!!.time + 30 * 60_000L))
        assertFalse(notificationDao.findById(notification.id)!!.readDeliveryState().steps[1].isProcessed)
        // Acknowledged by the recipient: no mail any more.
        val acknowledged = notificationDao.acknowledge(notification.id!!, user)!!
        assertEquals(NotificationStatus.ACKNOWLEDGED, acknowledged.status)
        val reread = notificationDao.findById(notification.id)!!
        assertEquals(user.id, reread.acknowledgedById)
        assertNotNull(reread.acknowledgedAt)
        assertNull(reread.nextDeliveryAt)
        val mailStep = reread.readDeliveryState().steps[1]
        assertTrue(mailStep.skipped)
        assertNull(mailStep.sentAt)
        assertNull(mailStep.error)
        assertFalse(reread.isVisibleInApp, "A banner? No: the test rule is a toast.")

        // A second run, not acknowledged: the mail step is processed by the delivery job (sent or, without mail
        // server, skipped with a reason).
        assertEquals(1, notificationService.trigger(rule).created, "A manual rule notifies on every trigger.")
        val second = notificationDao.select(NotificationDao.Filter(ruleId = rule.id))
            .single { it.status == NotificationStatus.OPEN }
        notificationService.runDeliveryJob(Date(second.created!!.time + 61 * 60_000L))
        val delivered = notificationDao.findById(second.id)!!
        assertTrue(delivered.readDeliveryState().steps.all { it.isProcessed })
        assertNull(delivered.nextDeliveryAt)
        assertNotNull(notificationService.deliveryJobStats.toSync())
    }

    @Test
    fun timesheetsMissingTest() {
        val user = initTestDB.addUser("notification-timesheets-user")
        insertEmployee(user)
        val task = initTestDB.addTask("notification-timesheets-task", "root")
        val rule = insertRule(NotificationRuleType.TIMESHEETS_MISSING, user, NotificationDelivery())
        // Wednesday, March 3rd 2032: the 1st, 2nd and 3rd are unbooked.
        val today = LocalDate.of(2032, 3, 3)
        val now = Date.from(today.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant())
        val result = notificationService.runRule(rule, today, now)
        assertEquals(1, result.created)
        assertEquals(0, result.errors)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        assertEquals("2032-03", notification.periodKey)
        assertEquals("${rule.id}:${user.id}:2032-03", notification.dedupKey)
        assertTrue(notification.body!!.contains("3"), notification.body)
        assertEquals(0, notificationService.runRule(rule, today, now).created, "Not twice per period.")
        // Partly booked: still open.
        insertTimesheet(task, user, 1)
        insertTimesheet(task, user, 2)
        notificationService.resolvePending(today)
        assertEquals(NotificationStatus.OPEN, notificationDao.findById(notification.id)!!.status)
        insertTimesheet(task, user, 3)
        notificationService.resolvePending(today)
        val resolved = notificationDao.findById(notification.id)!!
        assertEquals(NotificationStatus.RESOLVED, resolved.status)
        assertNotNull(resolved.resolvedAt)
        assertFalse(resolved.isVisibleInApp)
    }

    @Test
    fun passwordNotChangedTest() {
        val user = initTestDB.addUser("notification-password-user")
        val rule = insertRule(NotificationRuleType.PASSWORD_NOT_CHANGED, user, NotificationDelivery())
        rule.params = JsonUtils.toJson(NotificationParams(passwordMaxAgeDays = 90))
        val today = LocalDate.of(2032, 3, 3)
        val now = Date.from(today.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant())
        // Never changed: the creation of the user (today) is long ago in 2032.
        assertEquals(1, notificationService.runRule(rule, today, now).created)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        assertEquals("password-${user.created!!.time}", notification.periodKey)
        assertEquals(0, notificationService.runRule(rule, today, now).created, "Not twice per password.")
        notificationService.resolvePending(today)
        assertEquals(NotificationStatus.OPEN, notificationDao.findById(notification.id)!!.status)
        // The password is changed: resolved, and no notification until the new password is older than 90 days.
        user.lastPasswordChange = Date.from(today.minusDays(1).atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant())
        userDao.update(user, checkAccess = false)
        notificationService.resolvePending(today)
        assertEquals(NotificationStatus.RESOLVED, notificationDao.findById(notification.id)!!.status)
        assertEquals(0, notificationService.runRule(rule, today, now).created)
        assertEquals(0, notificationService.runRule(rule, today.plusDays(89), now).created, "90 days old: not older.")
        assertEquals(1, notificationService.runRule(rule, today.plusDays(90), now).created, "The new password is too old.")
    }

    @Test
    fun timesheetsWithoutKost2Test() {
        val user = initTestDB.addUser("notification-kost2-user")
        val task = initTestDB.addTask("notification-kost2-task", "root")
        val rule = insertRule(NotificationRuleType.TIMESHEETS_WITHOUT_KOST2, user, NotificationDelivery())
        rule.manualDone = true
        // Wednesday, March 3rd 2032: a time sheet of 2 hours on a task without cost 2 units.
        val today = LocalDate.of(2032, 3, 3)
        val now = Date.from(today.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant())
        val sheet = insertTimesheet(task, user, 1)
        val costConfigured = Configuration.instance.isCostConfigured
        try {
            setBoolean(ConfigurationParam.COST_CONFIGURED, true)
            assertEquals(0, notificationService.runRule(rule, today, now).created, "Kost2 not required (default).")
            setBoolean(ConfigurationParam.TIMESHEET_WARN_MISSING_KOST2, true)
            setBoolean(ConfigurationParam.COST_CONFIGURED, false)
            assertEquals(0, notificationService.runRule(rule, today, now).created, "Cost units not configured.")
            setBoolean(ConfigurationParam.COST_CONFIGURED, true)
            assertEquals(1, notificationService.runRule(rule, today, now).created)
            val first = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
            assertEquals("2032-03-03", first.periodKey)
            assertTrue(first.body!!.contains("1 sheets, 2 hours"), first.body)
            assertTrue(first.body!!.contains("notification-kost2-task"), first.body)
            assertEquals(0, notificationService.runRule(rule, today, now).created, "Not twice per day.")
            // Marked as done: back with the run of the next day.
            notificationDao.done(first.id!!, user)
            assertEquals(1, notificationService.runRule(rule, today.plusDays(1), now).created)
            val second = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single { it.status == NotificationStatus.OPEN }
            assertEquals("2032-03-04", second.periodKey)
            // Not marked as done: the notification of the next run replaces it.
            assertEquals(1, notificationService.runRule(rule, today.plusDays(2), now).created)
            notificationService.resolvePending(today.plusDays(2))
            assertEquals(NotificationStatus.EXPIRED, notificationDao.findById(second.id)!!.status)
            val third = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single { it.status.isPending }
            assertEquals("2032-03-05", third.periodKey)
            // No time sheet without cost 2 unit left: resolved.
            asAdmin { timesheetDao.markAsDeleted(sheet, checkAccess = false) }
            notificationService.resolvePending(today.plusDays(2))
            assertEquals(NotificationStatus.RESOLVED, notificationDao.findById(third.id)!!.status)
            assertEquals(0, notificationService.runRule(rule, today.plusDays(3), now).created)
        } finally {
            setBoolean(ConfigurationParam.COST_CONFIGURED, costConfigured)
            setBoolean(ConfigurationParam.TIMESHEET_WARN_MISSING_KOST2, false)
        }
    }

    @Test
    fun creditorInvoicesDueTest() {
        val financeUser = initTestDB.getUser(TEST_FINANCE_USER)!!
        val otherUser = initTestDB.getUser(TEST_USER)!!
        val rule = insertRule(NotificationRuleType.CREDITOR_INVOICES_DUE, financeUser, NotificationDelivery()) {
            it.recipients = JsonUtils.toJson(NotificationRecipients(userIds = listOf(financeUser.id!!, otherUser.id!!)))
            it.params = JsonUtils.toJson(NotificationParams(dueWorkingDays = 2))
        }
        // Far in the past, so no invoice of another test is due: Monday, March 12th 2001.
        val invoice = insertCreditorInvoice("R-2001-1", LocalDate.of(2001, 3, 12))
        val wednesday = LocalDate.of(2001, 3, 7)
        val thursday = LocalDate.of(2001, 3, 8)
        val now = Date.from(thursday.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant())
        assertEquals(0, notificationService.runRule(rule, wednesday, now).created, "Due in 3 working days.")
        // Only the recipient with read access to the creditor invoices:
        assertTrue(creditorInvoicesDueRuleHandler.isRelevant(rule, thursday, financeUser))
        assertFalse(creditorInvoicesDueRuleHandler.isRelevant(rule, thursday, otherUser), "No finance member.")
        assertEquals(1, notificationService.runRule(rule, thursday, now).created)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        assertEquals(financeUser.id, notification.recipientId)
        assertTrue(notification.body!!.contains("1 invoices, 2 working days"), notification.body)
        assertTrue(notification.body!!.contains("Due Creditor, R-2001-1"), notification.body)
        // Overdue for 90 days: included; for more: left out (old unpaid ones would be reported every day).
        val overdue90 = insertCreditorInvoice("R-2000-2", thursday.minusDays(90))
        insertCreditorInvoice("R-2000-1", wednesday.minusDays(91))
        assertEquals(listOf(overdue90.id, invoice.id), creditorInvoicesDueRuleHandler.dueInvoices(rule, thursday).map { it.invoice.id })
        eingangsrechnungDao.markAsDeleted(overdue90, checkAccess = false)
        // More working days in advance: due already on Wednesday.
        rule.params = JsonUtils.toJson(NotificationParams(dueWorkingDays = 3))
        assertEquals(1, creditorInvoicesDueRuleHandler.dueInvoices(rule, wednesday).size)
        rule.params = JsonUtils.toJson(NotificationParams(dueWorkingDays = 0))
        assertEquals(0, creditorInvoicesDueRuleHandler.dueInvoices(rule, thursday).size, "Not before the due date.")
        assertEquals(1, creditorInvoicesDueRuleHandler.dueInvoices(rule, LocalDate.of(2001, 3, 12)).size)
        // Paid: resolved.
        rule.params = JsonUtils.toJson(NotificationParams(dueWorkingDays = 2))
        val stored = eingangsrechnungDao.find(invoice.id, checkAccess = false)!!
        stored.bezahlDatum = thursday
        stored.zahlBetrag = BigDecimal("100")
        eingangsrechnungDao.update(stored, checkAccess = false)
        notificationService.resolvePending(thursday)
        assertEquals(NotificationStatus.RESOLVED, notificationDao.findById(notification.id)!!.status)
    }

    @Test
    fun exampleVariablesTest() {
        val user = initTestDB.addUser("notification-examples-user")
        NotificationRuleType.entries.filter { it != NotificationRuleType.MANUAL }.forEach { type ->
            val examples = notificationService.exampleVariables(type, user)
            notificationService.variables(type).forEach { key ->
                assertTrue(!examples[key].isNullOrBlank(), "Example of $key ($type) missing.")
            }
        }
        val rule = NotificationRuleDO().also {
            it.ruleType = NotificationRuleType.CREDITOR_INVOICES_DUE
            it.subject = "{{creditorInvoicesDueCount}} invoices"
            it.text = "<p>{{creditorInvoicesDueList}}</p>"
        }
        val preview = notificationService.preview(rule, user)
        assertEquals("2 invoices", preview.exampleSubject)
        assertTrue(preview.exampleText.contains("RE-4711"), preview.exampleText)
        assertFalse(preview.exampleText.contains("{{"), preview.exampleText)
    }

    @Test
    fun expireOfDeletedRuleTest() {
        val user = initTestDB.addUser("notification-deleted-rule-user")
        val rule = insertRule(NotificationRuleType.MANUAL, user, NotificationDelivery())
        notificationService.trigger(rule)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        notificationRuleDao.markAsDeleted(rule, checkAccess = false)
        // At once, not only by the next rule job:
        assertEquals(NotificationStatus.EXPIRED, notificationDao.findById(notification.id)!!.status)
    }

    @Test
    fun manualBannerTest() {
        val user = initTestDB.addUser("notification-manual-banner-user")
        val rule = insertRule(NotificationRuleType.MANUAL, user, NotificationDelivery()) { it.display = NotificationDisplay.BANNER }
        assertTrue(notificationRuleDao.findInternal(rule.id)!!.manualDone, "Forced for a manual banner.")
        notificationService.trigger(rule)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        assertTrue(notification.isManualDoneAllowed)
        // Acknowledged, the banner stays until done:
        notificationDao.acknowledge(notification.id!!, user)
        assertTrue(notificationDao.findById(notification.id)!!.isVisibleInApp)
        assertEquals(NotificationStatus.DONE, notificationDao.done(notification.id!!, user)!!.status)
        assertTrue(notificationDao.findVisible(user.id!!).isEmpty())

        // Ended by an editor:
        notificationService.trigger(rule)
        Thread.sleep(2) // The period of a manual run is its time in millis.
        notificationService.trigger(rule)
        assertEquals(2, notificationRuleDao.expirePending(rule))
        assertTrue(notificationDao.findVisible(user.id!!).isEmpty())
        assertEquals(0, notificationRuleDao.expirePending(rule))
    }

    @Test
    fun archivedTest() {
        val user = initTestDB.addUser("notification-archived-user")
        val otherUser = initTestDB.addUser("notification-archived-other-user")
        val since = Date(System.currentTimeMillis() - 60_000L)
        val toast = insertRule(NotificationRuleType.MANUAL, user, NotificationDelivery())
        val mailOnly = insertRule(
            NotificationRuleType.MANUAL, user,
            NotificationDelivery(listOf(NotificationDeliveryStep(NotificationChannel.MAIL, 0))),
        )
        val other = insertRule(NotificationRuleType.MANUAL, otherUser, NotificationDelivery())
        notificationService.trigger(toast)
        notificationService.trigger(mailOnly)
        notificationService.trigger(other)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = toast.id)).single()
        assertTrue(notificationDao.findArchived(user.id!!, since).isEmpty(), "Still visible, mail only isn't listed.")
        // Confirmed by mistake: the toast isn't visible any more, but archived.
        notificationDao.acknowledge(notification.id!!, user)
        assertTrue(notificationDao.findVisible(user.id!!).isEmpty())
        assertEquals(listOf(notification.id), notificationDao.findArchived(user.id!!, since).map { it.id })
        assertTrue(notificationDao.findArchived(otherUser.id!!, since).isEmpty(), "The other one is still open.")
        assertTrue(notificationDao.findArchived(user.id!!, Date(System.currentTimeMillis() + 60_000L)).isEmpty())
    }

    @Test
    fun expireOfDeactivatedRuleTest() {
        val user = initTestDB.addUser("notification-deactivated-rule-user")
        val delivery = NotificationDelivery(
            listOf(
                NotificationDeliveryStep(NotificationChannel.IN_APP, 0),
                NotificationDeliveryStep(NotificationChannel.MAIL, 60),
            )
        )
        val rule = insertRule(NotificationRuleType.MANUAL, user, delivery)
        notificationService.trigger(rule)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        rule.active = false
        notificationRuleDao.update(rule, checkAccess = false)
        assertEquals(NotificationStatus.EXPIRED, notificationDao.findById(notification.id)!!.status, "At once.")
        // The delivery job sends no mail of a deactivated rule, even before the next rule job:
        notificationService.runDeliveryJob(Date(notification.created!!.time + 120 * 60_000L))
        val expired = notificationDao.findById(notification.id)!!
        assertEquals(NotificationStatus.EXPIRED, expired.status)
        assertNull(expired.readDeliveryState().steps[1].sentAt, "No mail of a deactivated rule.")
        assertNull(expired.nextDeliveryAt)

        // The rule job expires them as well:
        rule.active = true
        notificationRuleDao.update(rule, checkAccess = false)
        notificationService.trigger(rule)
        val second = notificationDao.select(NotificationDao.Filter(ruleId = rule.id))
            .single { it.status == NotificationStatus.OPEN }
        rule.active = false
        notificationRuleDao.update(rule, checkAccess = false)
        notificationService.resolvePending(LocalDate.now())
        assertEquals(NotificationStatus.EXPIRED, notificationDao.findById(second.id)!!.status)
    }

    @Test
    fun mailServerUnreachableTest() {
        val first = initTestDB.addUser("notification-unreachable-user1")
        val second = initTestDB.addUser("notification-unreachable-user2")
        val mail = NotificationDelivery(listOf(NotificationDeliveryStep(NotificationChannel.MAIL, 0)))
        val rule = insertRule(NotificationRuleType.MANUAL, first, mail) {
            it.recipients = JsonUtils.toJson(NotificationRecipients(userIds = listOf(first.id!!, second.id!!)))
        }
        var calls = 0
        val unreachable = object : NotificationChannelSender {
            override val channel = NotificationChannel.MAIL
            override fun deliver(notification: NotificationDO, recipient: PFUserDO, now: Date): String? {
                ++calls
                throw MailSendException(MessagingException("Couldn't connect", SocketTimeoutException("Connect timed out")))
            }
        }
        val senderList = ReflectionTestUtils.getField(notificationService, "senderList")
        ReflectionTestUtils.setField(notificationService, "senderList", listOf(unreachable))
        try {
            assertEquals(2, notificationService.trigger(rule).created)
        } finally {
            ReflectionTestUtils.setField(notificationService, "senderList", senderList)
        }
        assertEquals(1, calls, "The second mail isn't tried once the mail server isn't reachable.")
        val steps = notificationDao.select(NotificationDao.Filter(ruleId = rule.id))
            .map { it.readDeliveryState().steps.single() }.sortedByDescending { it.attempts }
        assertEquals(1, steps[0].attempts)
        assertEquals("Connect timed out", steps[0].error, "The cause, not the i18n key of the MailSendException.")
        assertFalse(steps[0].isProcessed, "Retried later.")
        assertEquals(0, steps[1].attempts, "Postponed: no attempt counted.")
        assertNull(steps[1].error)
        assertFalse(steps[1].isProcessed)
    }

    @Test
    fun smsTest() {
        val user = initTestDB.addUser("notification-sms-user")
        val sms = NotificationDelivery(listOf(NotificationDeliveryStep(NotificationChannel.SMS, 0)))
        val rule = insertRule(NotificationRuleType.MANUAL, user, sms) { it.smsText = "SMS for {{firstName}} <b>" }
        notificationService.trigger(rule)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        assertEquals("SMS for ${user.firstname ?: ""} <b>", notification.smsText, "Plain text, not escaped.")
        val step = notification.readDeliveryState().steps.single()
        assertTrue(step.skipped, "No SMS gateway configured in the tests.")
        assertEquals("SMS isn't configured.", step.error)
        // Without an SMS step, no SMS text:
        val inApp = insertRule(NotificationRuleType.MANUAL, user, NotificationDelivery()) { it.smsText = "SMS" }
        notificationService.trigger(inApp)
        assertNull(notificationDao.select(NotificationDao.Filter(ruleId = inApp.id)).single().smsText)
        // Variables make the text longer than its column: cut, the notification is created anyway.
        val longUser = initTestDB.addUser(PFUserDO().also {
            it.username = "notification-sms-long-user"
            it.firstname = "Bartholomew-Maximilian"
            it.email = "devnull@localhost"
        })
        val long = insertRule(NotificationRuleType.MANUAL, longUser, sms) {
            it.smsText = "{{firstName}} ".repeat(1000 / 14)
        }
        notificationService.trigger(long)
        val longText = notificationDao.select(NotificationDao.Filter(ruleId = long.id)).single().smsText!!
        assertEquals(NotificationDO.SMS_TEXT_LENGTH, longText.length)
        assertTrue(longText.endsWith("…"), longText)
    }

    @Test
    fun linkTest() {
        val user = initTestDB.addUser("notification-link-user")
        val rule = insertRule(NotificationRuleType.MANUAL, user, NotificationDelivery())
        fun linkOfNextTrigger(params: NotificationParams): String? {
            rule.params = JsonUtils.toJson(params)
            notificationRuleDao.update(rule, checkAccess = false)
            notificationService.trigger(rule)
            return notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).maxBy { it.id!! }.link
        }
        assertNull(linkOfNextTrigger(NotificationParams()), "A manual rule has no default link.")
        val menuItem = NotificationParams(
            linkMode = NotificationParams.LinkMode.MENU_ITEM,
            linkMenuItem = MenuItemDefId.MONTHLY_EMPLOYEE_REPORT.id,
        )
        assertEquals(MenuItemDefId.MONTHLY_EMPLOYEE_REPORT.url, linkOfNextTrigger(menuItem))
        assertNull(linkOfNextTrigger(NotificationParams(linkMode = NotificationParams.LinkMode.NONE)))
    }

    @Test
    fun accessTest() {
        val financeUser = initTestDB.getUser(TEST_FINANCE_USER)!!
        val adminUser = initTestDB.getUser(TEST_ADMIN_USER)!!
        val otherUser = initTestDB.getUser(TEST_USER)!!
        val group = asAdmin { initTestDB.addGroup("notification-editors", TEST_USER) }
        val rule = NotificationRuleDO().also { it.editableByGroupIds = "${group.id}" }
        assertTrue(notificationRuleDao.hasAccess(financeUser, rule, null, OperationType.SELECT, false))
        assertFalse(notificationRuleDao.hasAccess(financeUser, rule, rule, OperationType.UPDATE, false))
        assertTrue(notificationRuleDao.hasAccess(adminUser, rule, rule, OperationType.UPDATE, false))
        assertFalse(notificationRuleDao.hasAccess(otherUser, rule, null, OperationType.SELECT, false), "No finance member.")
        assertFalse(notificationRuleDao.hasInsertAccess(otherUser))
        assertTrue(notificationRuleDao.hasInsertAccess(financeUser))
        // A finance member of an editor group:
        val financeGroup = asAdmin { initTestDB.addGroup("notification-finance-editors", TEST_FINANCE_USER) }
        rule.editableByGroupIds = "${group.id},${financeGroup.id}"
        assertTrue(notificationRuleDao.hasAccess(financeUser, rule, rule, OperationType.UPDATE, false))
        // The notification of another user can't be acknowledged:
        val recipient = initTestDB.addUser("notification-access-user")
        val manual = insertRule(NotificationRuleType.MANUAL, recipient, NotificationDelivery())
        notificationService.trigger(manual)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = manual.id)).single()
        assertFails { notificationDao.acknowledge(notification.id!!, otherUser) }
        assertFails { notificationDao.done(notification.id!!, recipient) } // manualDone isn't set.
    }

    private fun assertFails(block: () -> Unit) {
        val failed = try {
            block()
            false
        } catch (ex: Exception) {
            true
        }
        assertTrue(failed, "Exception expected.")
    }

    private fun insertCreditorInvoice(
        referenz: String,
        faelligkeit: LocalDate,
    ): EingangsrechnungDO {
        val invoice = EingangsrechnungDO().also {
            it.datum = faelligkeit.minusMonths(1)
            it.kreditor = "Due Creditor"
            it.referenz = referenz
            it.faelligkeit = faelligkeit
            it.addPosition(EingangsrechnungsPositionDO().also { pos ->
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal("100")
            })
        }
        eingangsrechnungDao.insert(invoice, checkAccess = false)
        return invoice
    }

    private fun insertRule(
        type: NotificationRuleType,
        recipient: PFUserDO,
        delivery: NotificationDelivery,
        init: (NotificationRuleDO) -> Unit = {},
    ): NotificationRuleDO {
        val rule = NotificationRuleDO()
        rule.name = "Test rule $type ${recipient.username}"
        rule.active = true
        rule.ruleType = type
        rule.display = NotificationDisplay.TOAST
        rule.schedule = JsonUtils.toJson(NotificationSchedule())
        rule.recipients = JsonUtils.toJson(NotificationRecipients(userIds = listOf(recipient.id!!)))
        rule.delivery = JsonUtils.toJson(delivery)
        rule.subject = "Hello {{firstName}}"
        rule.text = if (type == NotificationRuleType.TIMESHEETS_MISSING) {
            "<p>{{unbookedDaysCount}} days: {{unbookedDays}}</p>"
        } else if (type == NotificationRuleType.TIMESHEETS_WITHOUT_KOST2) {
            "<p>{{timesheetsWithoutKost2Count}} sheets, {{timesheetsWithoutKost2Hours}} hours: {{timesheetsWithoutKost2Tasks}}</p>"
        } else if (type == NotificationRuleType.CREDITOR_INVOICES_DUE) {
            "<p>{{creditorInvoicesDueCount}} invoices, {{dueWorkingDays}} working days: {{creditorInvoicesDueList}}</p>"
        } else if (type == NotificationRuleType.PASSWORD_NOT_CHANGED) {
            "<p>Changed {{passwordAgeDays}} days ago: {{passwordLink}}</p>"
        } else {
            "<p>Hello {{fullName}}</p>"
        }
        init(rule)
        notificationRuleDao.insert(rule, checkAccess = false)
        return rule
    }

    private fun insertEmployee(user: PFUserDO): EmployeeDO {
        val employee = EmployeeDO()
        employee.user = user
        employee.eintrittsDatum = LocalDate.of(2020, 1, 1)
        employeeDao.insert(employee, checkAccess = false)
        return employee
    }

    private fun setBoolean(param: ConfigurationParam, value: Boolean) {
        val entry = configurationDao.getEntry(param)
            ?: configurationDao.checkAndUpdateDatabaseEntries().let { configurationDao.getEntry(param)!! }
        entry.booleanValue = value
        configurationDao.update(entry, checkAccess = false)
    }

    private fun insertTimesheet(task: TaskDO, user: PFUserDO, day: Int): TimesheetDO {
        val start = LocalDateTime.of(2032, 3, day, 8, 0).atZone(ZoneId.of("UTC"))
        val sheet = TimesheetDO()
        sheet.task = task
        sheet.user = user
        sheet.startTime = Date.from(start.toInstant())
        sheet.stopTime = Date.from(start.plusHours(2).toInstant())
        sheet.description = "Notification test"
        asAdmin { timesheetDao.insert(sheet, checkAccess = false) }
        return sheet
    }

    /**
     * Runs the given test setup logged in as admin: the overlap check of time sheets and the history of groups need a
     * logged-in user. The notification service itself runs without one (as the job does).
     */
    private fun <T> asAdmin(block: () -> T): T {
        val previous = ThreadLocalUserContext.loggedInUser
        logon(TEST_ADMIN_USER)
        try {
            return block()
        } finally {
            ThreadLocalUserContext.setUser(previous)
        }
    }
}
