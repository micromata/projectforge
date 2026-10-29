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

import jakarta.persistence.PersistenceException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/** The queue of the order changes for the daily digest ([OrderNotificationQueueDO]). */
@Service
class OrderNotificationQueueDao {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Queues the change, replacing the entry of the recipient for the same order. An order created and changed again
     * before the digest stays "created".
     */
    fun enqueue(
        recipientId: Long,
        orderId: Long,
        orderNumber: Int?,
        orderTitle: String?,
        operation: String,
        link: String?,
    ) {
        try {
            enqueueOnce(recipientId, orderId, orderNumber, orderTitle, operation, link)
        } catch (ex: PersistenceException) {
            // Concurrent save of the same order: the other transaction inserted the row first, now it's an update.
            enqueueOnce(recipientId, orderId, orderNumber, orderTitle, operation, link)
        }
    }

    private fun enqueueOnce(
        recipientId: Long,
        orderId: Long,
        orderNumber: Int?,
        orderTitle: String?,
        operation: String,
        link: String?,
    ) {
        persistenceService.runInTransaction { context ->
            val existing = persistenceService.selectNamedSingleResult(
                OrderNotificationQueueDO.FIND_BY_RECIPIENT_AND_ORDER,
                OrderNotificationQueueDO::class.java,
                Pair("recipientId", recipientId),
                Pair("orderId", orderId),
            )
            if (existing != null) {
                val op = if (existing.operation == OperationType.INSERT.name && operation == OperationType.UPDATE.name) {
                    existing.operation
                } else {
                    operation
                }
                context.executeUpdate(
                    "update OrderNotificationQueueDO q set q.orderNumber = :nr, q.orderTitle = :title," +
                            " q.operation = :op, q.link = :link, q.created = :now where q.id = :id",
                    Pair("nr", orderNumber),
                    Pair("title", orderTitle?.take(1000)),
                    Pair("op", op),
                    Pair("link", link?.take(1000)),
                    Pair("now", Date()),
                    Pair("id", existing.id),
                )
            } else {
                context.em.persist(OrderNotificationQueueDO().also {
                    it.recipientId = recipientId
                    it.orderId = orderId
                    it.orderNumber = orderNumber
                    it.orderTitle = orderTitle?.take(1000)
                    it.operation = operation
                    it.link = link?.take(1000)
                    it.created = Date()
                })
            }
        }
    }

    /** All queued entries, grouped by recipient. */
    fun selectAllByRecipient(): Map<Long, List<OrderNotificationQueueDO>> {
        return persistenceService.executeNamedQuery(
            OrderNotificationQueueDO.FIND_ALL,
            OrderNotificationQueueDO::class.java,
        ).groupBy { it.recipientId!! }
    }

    /**
     * Removes the entries (after they were sent or dropped). An entry replaced by a newer change meanwhile (its
     * `created` differs) stays queued for the next digest.
     */
    fun delete(entries: Collection<OrderNotificationQueueDO>) {
        if (entries.isEmpty()) return
        persistenceService.runInTransaction { context ->
            entries.forEach {
                context.executeUpdate(
                    "delete from OrderNotificationQueueDO q where q.id = :id and q.created = :created",
                    Pair("id", it.id),
                    Pair("created", it.created),
                )
            }
        }
    }
}
