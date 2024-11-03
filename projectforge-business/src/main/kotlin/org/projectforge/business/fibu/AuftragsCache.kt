/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

/**
 * Open needed by Wicket's SpringBean.
 */
@Service
class AuftragsCache : AbstractCache(8 * TICKS_PER_HOUR), BaseDOModifiedListener<RechnungDO> {
    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private var orderInfoMap = mutableMapOf<Long, OrderInfo>()

    private var orderPositionMap = mutableMapOf<Long, OrderPositionInfo>()

    private var toBeInvoicedCounter: Int? = null

    @PostConstruct
    private fun init() {
        instance = this
        rechnungDao.register(this)
    }

    fun getOrderPositionInfosByAuftragId(auftragId: Long?): Collection<OrderPositionInfo>? {
        auftragId ?: return null
        checkRefresh()
        synchronized(orderPositionMap) {
            return orderPositionMap.values.filter { it.auftragId == auftragId }
        }
    }

    fun getOrderPositionInfo(auftragsPositionId: Long?): OrderPositionInfo? {
        auftragsPositionId ?: return null
        synchronized(orderPositionMap) {
            var ret = orderPositionMap[auftragsPositionId]
            if (ret == null) {
                ret = persistenceService.find(AuftragsPositionDO::class.java, auftragsPositionId)?.let {
                    val order = getOrderInfo(it.auftragId) ?: return null
                    val posInfo = OrderPositionInfo(it, order)
                    orderPositionMap[auftragsPositionId] = posInfo
                    posInfo
                }
            }
            return ret
        }
    }

    fun getFakturiertSum(order: AuftragDO?): BigDecimal {
        if (order == null) {
            return BigDecimal.ZERO
        }
        checkRefresh()
        return getOrderInfo(order).invoicedSum
    }

    fun isVollstaendigFakturiert(order: AuftragDO): Boolean {
        checkRefresh()
        return getOrderInfo(order).isVollstaendigFakturiert
    }

    fun isPositionAbgeschlossenUndNichtVollstaendigFakturiert(order: AuftragDO): Boolean {
        checkRefresh()
        return getOrderInfo(order).positionAbgeschlossenUndNichtVollstaendigFakturiert
    }

    fun isPaymentSchedulesReached(order: AuftragDO): Boolean {
        checkRefresh()
        return getOrderInfo(order).paymentSchedulesReached
    }

    /**
     * Number of all orders (finished, signed or escalated) which has to be invoiced.
     */
    fun getToBeInvoicedCounter(): Int {
        if (toBeInvoicedCounter != null) {
            return toBeInvoicedCounter!!
        }
        synchronized(orderInfoMap) {
            val counter = orderInfoMap.values.count { it.toBeInvoiced }
            toBeInvoicedCounter = counter
            return counter
        }
    }

    fun setOrderInfo(order: AuftragDO, checkRefresh: Boolean = false) {
        order.info = getOrderInfo(order, checkRefresh = checkRefresh)
    }

    fun getOrderInfo(orderId: Long?): OrderInfo? {
        orderId ?: return null
        checkRefresh()
        synchronized(orderInfoMap) {
            return orderInfoMap[orderId]
        }
    }

    /**
     * @param checkRefresh If true, the cache will be checked for refresh (needed for avoiding deadlocks).
     */
    @JvmOverloads
    fun getOrderInfo(order: AuftragDO, checkRefresh: Boolean = true): OrderInfo {
        if (checkRefresh) {
            checkRefresh()
        }
        synchronized(orderInfoMap) {
            orderInfoMap[order.id]?.let {
                return it
            }
        }
        val info = readOrderInfo(order)
        order.id?.let { id -> // id might be null on test cases.
            synchronized(orderInfoMap) {
                orderInfoMap[id] = info
            }
        }
        return info
    }

    /**
     * Order with positions and payment schedules (fetched if attached). If not attached and not available, this method will fail.
     */
    private fun readOrderInfo(order: AuftragDO): OrderInfo {
        return readOrderInfo(order, order.positionen, order.paymentSchedules)
    }

    private fun readOrderInfo(
        order: AuftragDO,
        positions: List<AuftragsPositionDO>?,
        paymentSchedules: List<PaymentScheduleDO>?
    ): OrderInfo {
        order.info.calculateAll(order, positions, paymentSchedules)
        synchronized(orderPositionMap) {
            positions?.forEach { pos ->
                val posInfo = OrderPositionInfo(pos, order.info)
                orderPositionMap[pos.id!!] = posInfo
            }
        }
        return order.info
    }

    fun setExpired(order: AuftragDO) {
        val orderId = order.id ?: return
        synchronized(orderInfoMap) {
            orderInfoMap[orderId] = readOrderInfo(order)
        }
        toBeInvoicedCounter = null
    }

    override fun setExpired() {
        synchronized(orderPositionMap) {
            orderPositionMap.clear()
        }
    }

    override fun refresh() {
        log.info("Refreshing AuftragsCache...")
        val map = mutableMapOf<Long, OrderInfo>()
        // Don't use fetch.
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            val orderPositions = context.executeQuery(
                "SELECT pos FROM AuftragsPositionDO pos WHERE pos.deleted = false",
                AuftragsPositionDO::class.java
            ).groupBy { it.auftragId }
            val paymentSchedules = context.executeQuery(
                "SELECT pos FROM PaymentScheduleDO pos WHERE pos.deleted = false",
                PaymentScheduleDO::class.java
            ).groupBy { it.auftragId }
            val orders = context.executeQuery(
                "SELECT t FROM AuftragDO t WHERE t.deleted = false",
                AuftragDO::class.java
            )
            orders.forEach { order ->
                map[order.id!!] = readOrderInfo(order, orderPositions[order.id], paymentSchedules[order.id])
            }
            val posMap = mutableMapOf<Long, OrderPositionInfo>()
            orderPositions.forEach { (_, positions) ->
                positions.forEach { pos ->
                    posMap[pos.id!!] = OrderPositionInfo(pos, map[pos.auftragId])
                }
            }

            orderInfoMap = map
            orderPositionMap = posMap
            toBeInvoicedCounter = null
            log.info { "AuftragsCache.refresh done. ${context.formatStats()}" }
        }
    }

    /**
     * Set order as expired, if any invoice on this order was changed.
     */
    override fun afterInsertOrModify(obj: RechnungDO, operationType: OperationType) {
        obj.positionen?.forEach { pos ->
            pos.auftragsPosition?.auftrag?.let {
                setExpired(it)
            }
        }
    }

    companion object {
        lateinit var instance: AuftragsCache
            private set
    }
}
