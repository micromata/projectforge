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

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * An order change waiting for the daily digest mail of a recipient who chose [OrderNotificationMode.DAILY].
 * One row per recipient and order, the latest change overwrites the former. Deliberately no
 * [org.projectforge.framework.persistence.entities.DefaultBaseDO]: written by the order save without history, deleted
 * after the digest is sent. Number and title are copied, so the digest can name an order that is deleted meanwhile.
 */
@Entity
@Table(
    name = "T_ORDER_NOTIFICATION_QUEUE",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_order_notification_queue", columnNames = ["recipient_fk", "order_fk"])],
    indexes = [Index(name = "idx_t_order_notification_queue_recipient", columnList = "recipient_fk")],
)
@NamedQueries(
    NamedQuery(
        name = OrderNotificationQueueDO.FIND_BY_RECIPIENT_AND_ORDER,
        query = "from OrderNotificationQueueDO q where q.recipientId = :recipientId and q.orderId = :orderId",
    ),
    NamedQuery(
        name = OrderNotificationQueueDO.FIND_ALL,
        query = "from OrderNotificationQueueDO q order by q.recipientId, q.orderNumber",
    ),
)
open class OrderNotificationQueueDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(name = "recipient_fk", nullable = false)
    open var recipientId: Long? = null

    /** The pk of the order (plain id: the order may be deleted before the digest is sent). */
    @get:Column(name = "order_fk", nullable = false)
    open var orderId: Long? = null

    @get:Column(name = "order_number")
    open var orderNumber: Int? = null

    @get:Column(name = "order_title", length = 1000)
    open var orderTitle: String? = null

    /** `INSERT`, `UPDATE` or `DELETE` (name of the OperationType). */
    @get:Column(length = 20, nullable = false)
    open var operation: String? = null

    /** The link to the order, null if the change was saved without a request url. */
    @get:Column(length = 1000)
    open var link: String? = null

    @get:Column(nullable = false)
    open var created: Date? = null

    companion object {
        internal const val FIND_BY_RECIPIENT_AND_ORDER = "OrderNotificationQueueDO_FindByRecipientAndOrder"
        internal const val FIND_ALL = "OrderNotificationQueueDO_FindAll"
    }
}
