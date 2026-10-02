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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsCache
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsPositionsPaymentType
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.ForecastChartData
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.fibu.orderbooksnapshots.OrderbookSnapshotsService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The forecast charts compute over the orders the list shows: every list criterion (customer, payment types, ...)
 * selects the orders, IST and the previous years count the invoices of these orders only, and the plan of a
 * planning date is restricted to the same orders.
 */
class ForecastChartFilterTest : AbstractTestBase() {
    @Autowired
    private lateinit var orderEntityRest: OrderEntityRest

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var orderbookSnapshotsService: OrderbookSnapshotsService

    private val today = PFDay.now()

    private val baseDate = today.plusMonths(-4)

    @Test
    fun `the charts apply the list's customer and payment type criteria, with and without planning date`() {
        logon(TEST_FINANCE_USER)
        createInvoicedOrder(4911L, "Forecastalpha", 11, 1000.0, AuftragsPositionsPaymentType.TIME_AND_MATERIALS)
        createInvoicedOrder(4912L, "Forecastbeta", 12, 2000.0, AuftragsPositionsPaymentType.FESTPREISPAKET)
        createInvoicedOrder(4913L, "Forecastgamma", 13, 4000.0, AuftragsPositionsPaymentType.PAUSCHALE)
        auftragsCache.setExpired()
        auftragsCache.forceReload()
        orderbookSnapshotsService.storeOrderbookSnapshot(date = today.localDate)

        assertSelection(1000.0, entry("kunde.name", "Forecastalpha"))
        assertSelection(3000.0, paymentTypes(AuftragsPositionsPaymentType.TIME_AND_MATERIALS, AuftragsPositionsPaymentType.FESTPREISPAKET))
        // All three, as a cross-check that the criteria above really selected:
        assertSelection(7000.0, paymentTypes(*AuftragsPositionsPaymentType.entries.toTypedArray()))
    }

    /**
     * IST of the selection is [expectedIst]; the planning date changes neither IST nor the previous years, and the
     * plan of today's snapshot is today's forecast of the selected orders.
     */
    private fun assertSelection(expectedIst: Double, vararg entries: MagicFilterEntry) {
        val withoutPlan = chart(null, *entries)
        val withPlan = chart(today, *entries)
        val name = entries.joinToString { "${it.field}=${it.value.value ?: it.value.values?.joinToString()}" }
        assertEquals(scaled(BigDecimal(expectedIst)), scaled(withoutPlan.ist.fold(BigDecimal.ZERO, BigDecimal::add)), "IST of $name")
        assertAmounts(withoutPlan.ist, withPlan.ist, "IST with planning date of $name")
        assertAmounts(withoutPlan.prevYear, withPlan.prevYear, "Previous year with planning date of $name")
        assertAmounts(withoutPlan.total, withPlan.total, "Total with planning date of $name")
        assertAmounts(withoutPlan.total, withPlan.plan!!, "Plan of $name")
    }

    private fun chart(planningDate: PFDay?, vararg entries: MagicFilterEntry): ForecastChartData {
        val filter = MagicFilter()
        filter.entries.addAll(entries)
        return orderEntityRest.forecastChart(
            OrderEntityRest.ForecastChartRequest(
                filter = filter,
                startDate = baseDate.beginOfMonth.localDate,
                planningDate = planningDate?.localDate,
            )
        )
    }

    private fun createInvoicedOrder(
        customerId: Long,
        customerName: String,
        projectNumber: Int,
        amount: Double,
        paymentType: AuftragsPositionsPaymentType,
    ) {
        val kunde = KundeDO()
        kunde.id = customerId
        kunde.name = customerName
        kundeDao.insert(kunde, checkAccess = false)
        val projekt = ProjektDO()
        projekt.nummer = projectNumber
        projekt.name = "ForecastChartFilterTest - $customerName"
        projekt.kunde = kunde
        val projektId = projektDao.insert(projekt, checkAccess = false)
        val order = AuftragDO()
        order.nummer = auftragDao.nextNumber
        order.titel = "ForecastChartFilterTest order"
        order.status = AuftragsStatus.BEAUFTRAGT
        order.angebotsDatum = baseDate.localDate
        order.periodOfPerformanceBegin = baseDate.localDate
        order.periodOfPerformanceEnd = baseDate.plusMonths(4).localDate
        order.kunde = kunde
        order.projekt = projektDao.find(projektId, checkAccess = false, attached = true)
        order.addPosition(AuftragsPositionDO().also {
            it.number = 1
            it.status = AuftragsStatus.BEAUFTRAGT
            it.paymentType = paymentType
            it.nettoSumme = BigDecimal(5 * amount)
        })
        val orderId = auftragDao.insert(order, checkAccess = false)
        val invoice = RechnungDO()
        invoice.nummer = rechnungDao.nextNumber
        invoice.datum = baseDate.plusMonths(1).localDate
        invoice.faelligkeit = baseDate.plusMonths(2).localDate
        invoice.status = RechnungStatus.GESTELLT
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.kunde = kunde
        invoice.addPosition(RechnungsPositionDO().also {
            it.auftragsPosition = auftragDao.find(orderId, checkAccess = false)!!.getPosition(1)
            it.einzelNetto = BigDecimal(amount)
        })
        rechnungDao.insert(invoice, checkAccess = false)
    }

    private fun entry(field: String, value: String? = null) = MagicFilterEntry(field, value)

    private fun paymentTypes(vararg types: AuftragsPositionsPaymentType) =
        entry("positionsPaymentType").also { entry -> entry.value.values = types.map { it.name }.toTypedArray() }

    private fun assertAmounts(expected: List<BigDecimal>, actual: List<BigDecimal>, name: String) {
        assertEquals(expected.map { scaled(it) }, actual.map { scaled(it) }, name)
    }

    private fun scaled(value: BigDecimal) = value.setScale(2, RoundingMode.HALF_UP)
}
