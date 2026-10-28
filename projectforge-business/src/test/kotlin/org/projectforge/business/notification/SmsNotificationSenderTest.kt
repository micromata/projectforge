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
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

class SmsNotificationSenderTest : AbstractTestBase() {
    @Autowired
    private lateinit var smsNotificationSender: SmsNotificationSender

    @Test
    fun buildMessageTest() {
        val notification = NotificationDO().also {
            it.title = "Time sheets missing"
            it.link = "/timesheet"
        }
        val message = smsNotificationSender.buildMessage(notification)
        assertEquals("Time sheets missing", message.lines().first())
        assertEquals(2, message.lines().size, "Subject and absolute link.")
        assertEquals(true, message.lines()[1].endsWith("/timesheet"))
        notification.link = null
        assertEquals("Time sheets missing", smsNotificationSender.buildMessage(notification))
        notification.smsText = "Own text"
        assertEquals("Own text", smsNotificationSender.buildMessage(notification))
    }

    @Test
    fun truncateTest() {
        assertEquals("abc", SmsNotificationSender.truncate("abc", 3))
        assertEquals("ab…", SmsNotificationSender.truncate("abcd", 3))
        assertEquals("a…", SmsNotificationSender.truncate("a bcd", 3), "Without trailing blank.")
    }

    @Test
    fun deliverTest() {
        val notification = NotificationDO().also { it.title = "Hello" }
        val recipient = PFUserDO().also { it.mobilePhone = "+49 170 1234567" }
        // No SMS gateway configured in the tests:
        assertEquals("SMS isn't configured.", smsNotificationSender.deliver(notification, recipient, Date()))
    }
}
