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

import org.projectforge.framework.utils.NumberHelper.add
import java.io.Serializable
import java.math.BigDecimal

class AuftragsStatistik(private val auftragsCache: AuftragsCache) : Serializable {
  /**
   * Sum of all nets.
   */
  var nettoSum: BigDecimal
    private set

  /**
   * Sum of the nets where the order is in POTENZIAL, IN_ERSTELLUNG or GELEGT.
   */
  var akquiseSum: BigDecimal
    private set

  /**
   * Sum of the "beauftragt" nets where the order is in LOI, BEAUFTRAGT or ESKALATION.
   */
  var beauftragtSum: BigDecimal
    private set

  /**
   * Sum of the "fakturiert sums" of all orders.
   */
  var invoicedSum: BigDecimal
    private set

  /**
   * Difference between net sum and invoiced positions.
   */
  var notYetInvoicedSum: BigDecimal
    private set

  /**
   * Sum of the to-be-invoiced-sums of the orders which are ABGESCHLOSSEN and not "vollstaendig fakturiert" or with
   * reached payment schedules.
   */
  var toBeInvoiced: BigDecimal
    private set

  /**
   * Count of orders considered in these statistics.
   */
  var counter: Int
    private set
  var counterAkquise = 0
    private set
  var counterBeauftragt: Int
    private set
  var counterNotYetInvoiced: Int
    private set
  var counterToBeInvoiced: Int
    private set
  var counterInvoiced: Int
    private set

  init {
    notYetInvoicedSum = BigDecimal.ZERO
    invoicedSum = BigDecimal.ZERO
    beauftragtSum = BigDecimal.ZERO
    akquiseSum = BigDecimal.ZERO
    nettoSum = BigDecimal.ZERO
    toBeInvoiced = BigDecimal.ZERO

    counterInvoiced = 0
    counterNotYetInvoiced = 0
    counterBeauftragt = 0
    counter = 0
    counterToBeInvoiced = 0
  }

  fun add(auftrag: AuftragDO) {
    val info = auftragsCache.getOrderInfo(auftrag)
    if (info.akquiseSum > BigDecimal.ZERO) {
      akquiseSum = add(akquiseSum, info.akquiseSum)
      counterAkquise++
    }
    if (info.beauftragtNettoSumme > BigDecimal.ZERO) {
      beauftragtSum = add(beauftragtSum, info.beauftragtNettoSumme)
      counterBeauftragt++
    }
    if (info.notYetInvoicedSum > BigDecimal.ZERO) {
      notYetInvoicedSum = add(notYetInvoicedSum, info.notYetInvoicedSum)
      counterNotYetInvoiced++
    }
    if (info.toBeInvoicedSum > BigDecimal.ZERO) {
      toBeInvoiced = add(toBeInvoiced, info.toBeInvoicedSum)
      counterToBeInvoiced++
    }
    if (info.invoicedSum > BigDecimal.ZERO) {
      invoicedSum = add(invoicedSum, info.invoicedSum)
      counterInvoiced++
    }
    counter++
    nettoSum = add(nettoSum, info.netSum)
  }

  companion object {
    private const val serialVersionUID = -5486964211679100585L
  }
}
