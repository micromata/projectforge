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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class OrderNotificationQueueTest : AbstractTestBase() {
    @Autowired
    private lateinit var queueDao: OrderNotificationQueueDao

    @Autowired
    private lateinit var prefService: OrderNotificationPrefService

    @Test
    fun `the latest change of an order replaces the queued one`() {
        val userId = getUser(TEST_USER).id!!
        val orderId = 987654321L
        queueDao.enqueue(userId, orderId, 77, "First", "INSERT", "https://pf/order/1")
        queueDao.enqueue(userId, orderId, 77, "Second", "UPDATE", null)
        val entries = queueDao.selectAllByRecipient()[userId]!!.filter { it.orderId == orderId }
        assertEquals(1, entries.size)
        assertEquals("Second", entries[0].orderTitle)
        assertEquals("INSERT", entries[0].operation, "An order created and changed again stays created.")
        queueDao.delete(entries)
        assertTrue(queueDao.selectAllByRecipient()[userId].orEmpty().none { it.orderId == orderId })
    }

    @Test
    fun `a change queued while the digest was sent stays queued`() {
        val userId = getUser(TEST_USER).id!!
        val orderId = 987654322L
        queueDao.enqueue(userId, orderId, 78, "Before", "UPDATE", null)
        val loaded = queueDao.selectAllByRecipient()[userId]!!.filter { it.orderId == orderId }
        Thread.sleep(5)
        queueDao.enqueue(userId, orderId, 78, "During the send", "UPDATE", null)
        queueDao.delete(loaded)
        val remaining = queueDao.selectAllByRecipient()[userId]!!.filter { it.orderId == orderId }
        assertEquals(1, remaining.size)
        assertEquals("During the send", remaining[0].orderTitle)
        queueDao.delete(remaining)
    }

    @Test
    fun `preferences are stored per user and default to immediate`() {
        val user = getUser(TEST_USER).id!!
        val other = getUser(TEST_FINANCE_USER).id!!
        assertEquals(OrderNotificationMode.IMMEDIATE, prefService.get(user).defaultMode)
        prefService.save(
            user,
            OrderNotificationPrefs(
                OrderNotificationMode.DAILY,
                projectModes = mutableMapOf(5L to OrderNotificationMode.MUTED),
            ),
        )
        val loaded = prefService.get(user)
        assertEquals(OrderNotificationMode.DAILY, loaded.defaultMode)
        assertEquals(OrderNotificationMode.MUTED, loaded.projectModes[5L])
        assertEquals(OrderNotificationMode.IMMEDIATE, prefService.get(other).defaultMode)
    }
}
