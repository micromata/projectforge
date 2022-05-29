/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOChangedListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import javax.annotation.PostConstruct

/**
 * Open needed by Wicket's SpringBean.
 */
@Service
open class AuftragsCache : AbstractCache(8 * TICKS_PER_HOUR), BaseDOChangedListener<RechnungDO> {

  class OrderInfo(
    val netSum: BigDecimal,
    val akquiseSum: BigDecimal,
    val fakturiertSum: BigDecimal,
    val zuFakturierenSum: BigDecimal,
    val beauftragtNettoSumme: BigDecimal,
    val isVollstaendigFakturiert: Boolean,
    val positionAbgeschlossenUndNichtVollstaendigFakturiert: Boolean,
    val paymentSchedulesReached: Boolean,
  )

  @Autowired
  private lateinit var rechnungCache: RechnungCache

  @Autowired
  private lateinit var rechnungDao: RechnungDao

  private var orderMap = mutableMapOf<Int, OrderInfo>()

  @PostConstruct
  private fun init() {
    instance = this
    rechnungDao.register(this)
  }

  open fun getFakturiertSum(order: AuftragDO?): BigDecimal {
    if (order == null) {
      return BigDecimal.ZERO
    }
    return getOrderInfo(order).fakturiertSum
  }

  open fun isVollstaendigFakturiert(order: AuftragDO): Boolean {
    return getOrderInfo(order).isVollstaendigFakturiert
  }

  open fun getPositionAbgeschlossenUndNichtVollstaendigFakturiert(order: AuftragDO): Boolean {
    return getOrderInfo(order).positionAbgeschlossenUndNichtVollstaendigFakturiert
  }

  open fun getPaymentSchedulesReached(order: AuftragDO): Boolean {
    return getOrderInfo(order).paymentSchedulesReached
  }

  open fun getOrderInfo(order: AuftragDO): OrderInfo {
    synchronized(orderMap) {
      orderMap[order.id]?.let {
        return it
      }
    }
    var fakturiertSum = BigDecimal.ZERO
    var positionAbgeschlossenUndNichtVollstaendigFakturiert = false
    order.positionenExcludingDeleted.forEach { pos ->
      rechnungCache.getRechnungsPositionVOSetByAuftragsPositionId(pos.id)?.let { set ->
        fakturiertSum += RechnungDao.getNettoSumme(set)
      }
      if (pos.isAbgeschlossenUndNichtVollstaendigFakturiert) {
        positionAbgeschlossenUndNichtVollstaendigFakturiert = true
      }
    }
    var paymentSchedulesReached = false
    order.paymentSchedules?.let { paymentSchedules ->
      for (schedule in paymentSchedules) {
        if (!schedule.isDeleted && schedule.reached && !schedule.vollstaendigFakturiert) {
          paymentSchedulesReached = true
          break
        }
      }
    }
    var akquiseSum = BigDecimal.ZERO
    val status = order.auftragsStatus ?: AuftragsStatus.IN_ERSTELLUNG
    if (status.isIn(AuftragsStatus.POTENZIAL, AuftragsStatus.IN_ERSTELLUNG, AuftragsStatus.GELEGT)) {
      akquiseSum = order.nettoSumme
    }
    val info = OrderInfo(
      netSum = order.nettoSumme,
      akquiseSum = akquiseSum,
      fakturiertSum = fakturiertSum,
      zuFakturierenSum = order.zuFakturierenSum ?: BigDecimal.ZERO,
      beauftragtNettoSumme = order.beauftragtNettoSumme,
      isVollstaendigFakturiert = order.isVollstaendigFakturiert,
      positionAbgeschlossenUndNichtVollstaendigFakturiert = positionAbgeschlossenUndNichtVollstaendigFakturiert,
      paymentSchedulesReached = paymentSchedulesReached,
    )
    synchronized(orderMap) {
      orderMap[order.id] = info
    }
    return info
  }

  fun setExpired(order: AuftragDO) {
    synchronized(orderMap) {
      orderMap.remove(order.id)
    }
  }

  override fun refresh() {
    synchronized(orderMap) {
      orderMap.clear()
    }
  }

  /**
   * Set order as expired, if any invoice on this order was changed.
   */
  override fun afterSaveOrModifify(changedObject: RechnungDO, operationType: OperationType) {
    changedObject.positionen?.forEach { pos ->
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
