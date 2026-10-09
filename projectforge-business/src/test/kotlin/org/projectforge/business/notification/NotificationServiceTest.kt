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
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class NotificationServiceTest : AbstractTestBase() {
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
    fun expireOfDeletedRuleTest() {
        val user = initTestDB.addUser("notification-deleted-rule-user")
        val rule = insertRule(NotificationRuleType.MANUAL, user, NotificationDelivery())
        notificationService.trigger(rule)
        val notification = notificationDao.select(NotificationDao.Filter(ruleId = rule.id)).single()
        notificationRuleDao.markAsDeleted(rule, checkAccess = false)
        notificationService.resolvePending(LocalDate.now())
        assertEquals(NotificationStatus.EXPIRED, notificationDao.findById(notification.id)!!.status)
    }

    @Test
    fun accessTest() {
        val financeUser = initTestDB.getUser(TEST_FINANCE_USER)!!
        val adminUser = initTestDB.getUser(TEST_ADMIN_USER)!!
        val otherUser = initTestDB.getUser(TEST_USER)!!
        val group = initTestDB.addGroup("notification-editors", TEST_USER)
        val rule = NotificationRuleDO().also { it.editableByGroupIds = "${group.id}" }
        assertTrue(notificationRuleDao.hasAccess(financeUser, rule, null, OperationType.SELECT, false))
        assertFalse(notificationRuleDao.hasAccess(financeUser, rule, rule, OperationType.UPDATE, false))
        assertTrue(notificationRuleDao.hasAccess(adminUser, rule, rule, OperationType.UPDATE, false))
        assertFalse(notificationRuleDao.hasAccess(otherUser, rule, null, OperationType.SELECT, false), "No finance member.")
        assertFalse(notificationRuleDao.hasInsertAccess(otherUser))
        assertTrue(notificationRuleDao.hasInsertAccess(financeUser))
        // A finance member of an editor group:
        val financeGroup = initTestDB.addGroup("notification-finance-editors", TEST_FINANCE_USER)
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

    private fun insertRule(type: NotificationRuleType, recipient: PFUserDO, delivery: NotificationDelivery): NotificationRuleDO {
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
        } else {
            "<p>Hello {{fullName}}</p>"
        }
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

    private fun insertTimesheet(task: TaskDO, user: PFUserDO, day: Int) {
        val start = LocalDateTime.of(2032, 3, day, 8, 0).atZone(ZoneId.of("UTC"))
        val sheet = TimesheetDO()
        sheet.task = task
        sheet.user = user
        sheet.startTime = Date.from(start.toInstant())
        sheet.stopTime = Date.from(start.plusHours(2).toInstant())
        sheet.description = "Notification test"
        timesheetDao.insert(sheet, checkAccess = false)
    }
}
