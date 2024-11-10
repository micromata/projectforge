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
import org.projectforge.common.logging.LogDuration
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

/**
 * Open needed by Wicket's SpringBean.
 */
@Service
class AuftragsCache : AbstractCache(8 * TICKS_PER_HOUR) {
    @Autowired
    private lateinit var auftragsCacheService: AuftragsCacheService

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var auftragDao: AuftragDao

    private var orderInfoMap = mutableMapOf<Long, OrderInfo>()

    private var orderPositionIdsMapByOrder = mutableMapOf<Long, MutableList<Long>>()

    private var orderPositionMapByPosId = mutableMapOf<Long, OrderPositionInfo>()

    private var toBeInvoicedCounter: Int? = null

    @PostConstruct
    private fun init() {
        instance = this
        auftragDao.auftragsCache = this
        rechnungDao.register(rechnungListener)
        auftragDao.register(auftragListener)
    }

    fun getOrderPositionInfosByAuftragId(auftragId: Long?): Collection<OrderPositionInfo>? {
        auftragId ?: return null
        checkRefresh()
        synchronized(orderPositionMapByPosId) {
            // val list = orderPositionMapByPosId.values.filter { it.auftragId == auftragId }
            return orderPositionMapByPosId.values.filter { it.auftragId == auftragId }
        }
    }

    fun getOrderPositionInfo(auftragsPositionId: Long?): OrderPositionInfo? {
        auftragsPositionId ?: return null
        checkRefresh()
        synchronized(orderPositionMapByPosId) {
            return orderPositionMapByPosId[auftragsPositionId]
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
            log.debug {
                "To be invoiced counter=$counter: ${
                    orderInfoMap.values.filter { it.toBeInvoiced }.joinToString { it.nummer.toString() }
                }"
            }
            toBeInvoicedCounter = counter
            return counter
        }
    }

    fun setOrderInfo(order: AuftragDO, checkRefresh: Boolean = true) {
        order.info = getOrderInfo(order, checkRefresh = checkRefresh)
    }

    fun getOrderInfo(orderId: Long?): OrderInfo? {
        orderId ?: return null
        checkRefresh()
        synchronized(orderInfoMap) {
            return orderInfoMap[orderId]
        }
    }

    fun getOrderInfoByPositionId(positionInfoId: Long?): OrderInfo? {
        positionInfoId ?: return null
        checkRefresh()
        synchronized(orderPositionMapByPosId) {
            return orderPositionMapByPosId[positionInfoId]?.auftrag
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
            return orderInfoMap[order.id] ?: OrderInfo()
        }
        /*
        val info = readOrderInfo(order)
        order.id?.let { id -> // id might be null on test cases.
            synchronized(orderInfoMap) {
                orderInfoMap[id] = info
            }
        }
        return info*/
    }

    fun setExpired(order: AuftragDO) {
        setExpired()
        /*val orderId = order.id ?: return
        synchronized(orderInfoMap) {
            orderInfoMap[orderId] = readOrderInfo(order)
        }*/
        toBeInvoicedCounter = null // Force recalculation.
    }

    override fun setExpired() {
        /*synchronized(orderPositionMapByPosId) {
            orderPositionMapByPosId.clear()
        }*/
        super.setExpired()
        toBeInvoicedCounter = null // Force recalculation.
    }

    override fun refresh() {
        log.info("Refreshing AuftragsCache...")
        val duration = LogDuration()
        // Don't use fetch.
        val orderPositions = auftragsCacheService.selectNonDeletedAuftragsPositions().groupBy { it.auftragId }
        val orders = auftragsCacheService.selectAuftragsList()
        val paymentSchedules = auftragsCacheService.selectNonDeletedPaymentSchedules().groupBy { it.auftragId }
        val nOrderInfoMap = mutableMapOf<Long, OrderInfo>()
        val nOrderPositionMapByPosId = mutableMapOf<Long, OrderPositionInfo>()
        val nOrderPositionIdsMapByOrder = mutableMapOf<Long, MutableList<Long>>()
        val nOrderPositionInfosByOrderId = mutableMapOf<Long, MutableList<OrderPositionInfo>>()
        orders.forEach { order ->
            nOrderInfoMap[order.id!!] = order.info.also {
                it.updateFields(order)
            }
        }
        orderPositions.forEach orderPositions@{ (auftragId, positions) ->
            positions.forEach { pos ->
                val orderInfo = nOrderInfoMap[auftragId]
                if (orderInfo == null) {
                    log.error { "Internal error: Order #$auftragId not found for position $pos" }
                    return@orderPositions
                }
                val posInfo = OrderPositionInfo(pos, orderInfo)
                nOrderPositionMapByPosId[pos.id!!] = posInfo
                nOrderPositionIdsMapByOrder.computeIfAbsent(auftragId!!) { mutableListOf() }.add(pos.id!!)
                nOrderPositionInfosByOrderId.computeIfAbsent(auftragId) { mutableListOf() }.add(posInfo)
            }
        }
        orders.forEach { order ->
            log.debug { "Cached payment schedules for order ${order.id}: ${paymentSchedules[order.id]?.size}" }
            order.info.calculateAll(
                order,
                nOrderPositionInfosByOrderId[order.id],
                paymentSchedules[order.id]
            )
        }
        orderInfoMap = nOrderInfoMap
        orderPositionMapByPosId = nOrderPositionMapByPosId
        orderPositionIdsMapByOrder = nOrderPositionIdsMapByOrder
        toBeInvoicedCounter = null // Force recalculation.
        log.info { "AuftragsCache.refresh done: ${duration.toSeconds()}" }
    }

    private val rechnungListener = object : BaseDOModifiedListener<RechnungDO> {
        /**
         * Set order as expired, if any invoice on this order was changed.
         */
        override fun afterInsertOrModify(obj: RechnungDO, operationType: OperationType) {
            setExpired()
            /*obj.positionen?.forEach { pos ->
                pos.auftragsPosition?.auftrag?.let {
                    setExpired(it)
                }
            }*/
        }
    }

    private val auftragListener = object : BaseDOModifiedListener<AuftragDO> {
        /**
         * Set order as expired, if any invoice on this order was changed.
         */
        override fun afterInsertOrModify(obj: AuftragDO, operationType: OperationType) {
            setExpired()
        }
    }

    companion object {
        lateinit var instance: AuftragsCache
            private set
    }
}
