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
open class AuftragsCache : AbstractCache(8 * TICKS_PER_HOUR), BaseDOModifiedListener<RechnungDO> {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private var orderMap = mutableMapOf<Long, OrderInfo>()

    private var toBeInvoicedCounter: Int? = null

    @PostConstruct
    private fun init() {
        instance = this
        rechnungDao.register(this)
    }

    open fun getFakturiertSum(order: AuftragDO?): BigDecimal {
        if (order == null) {
            return BigDecimal.ZERO
        }
        checkRefresh()
        return getOrderInfo(order).invoicedSum
    }

    open fun isVollstaendigFakturiert(order: AuftragDO): Boolean {
        checkRefresh()
        return getOrderInfo(order).isVollstaendigFakturiert
    }

    open fun isPositionAbgeschlossenUndNichtVollstaendigFakturiert(order: AuftragDO): Boolean {
        checkRefresh()
        return getOrderInfo(order).positionAbgeschlossenUndNichtVollstaendigFakturiert
    }

    open fun isPaymentSchedulesReached(order: AuftragDO): Boolean {
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
        val counter = orderMap.values.count { it.toBeInvoiced }
        toBeInvoicedCounter = counter
        return counter
    }

    fun setOrderInfo(order: AuftragDO) {
        checkRefresh()
        order.info = getOrderInfo(order)
    }

    open fun getOrderInfo(order: AuftragDO): OrderInfo {
        checkRefresh()
        synchronized(orderMap) {
            orderMap[order.id]?.let {
                return it
            }
        }
        val info = readOrderInfo(order)
        order.id?.let { id -> // id might be null on test cases.
            synchronized(orderMap) {
                orderMap[id] = info
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
        order.info.calculateAll(positions, paymentSchedules)
        return order.info
    }

    fun setExpired(order: AuftragDO) {
        val orderId = order.id ?: return
        synchronized(orderMap) {
            orderMap[orderId] = readOrderInfo(order)
        }
        toBeInvoicedCounter = null
    }

    override fun refresh() {
        log.info("Refreshing AuftragsCache...")
        val map = mutableMapOf<Long, OrderInfo>()
        // Don't use fetch.
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            val orderPositions = persistenceService.executeQuery(
                "SELECT pos FROM AuftragsPositionDO pos WHERE pos.deleted = false",
                AuftragsPositionDO::class.java
            ).groupBy { it.auftragId }
            val paymentSchedules = persistenceService.executeQuery(
                "SELECT pos FROM PaymentScheduleDO pos WHERE pos.deleted = false",
                PaymentScheduleDO::class.java
            ).groupBy { it.auftragId }
            val orders = auftragDao.selectAllNotDeleted(checkAccess = false)
            orders.forEach { order ->
                map[order.id!!] = readOrderInfo(order, orderPositions[order.id], paymentSchedules[order.id])
            }
            orderMap = map
            toBeInvoicedCounter = null
            log.info(
                "AuftragsCache.refresh done. stats=${persistenceService.formatStats(context.savedStats)}, callsStats=${
                    PfPersistenceService.showCallsStatsRecording()
                }"
            )
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
