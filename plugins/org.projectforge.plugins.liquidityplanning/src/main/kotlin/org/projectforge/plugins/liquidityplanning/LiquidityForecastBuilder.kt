/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.liquidityplanning

import org.projectforge.business.fibu.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class LiquidityForecastBuilder {
    @Autowired
    private lateinit var accountCache: KontoCache

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var liquidityEntryDao: LiquidityEntryDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    /**
     * Calculates expected dates of payments inside the last year (-365 days).
     */
    open fun build(nextDays: Int = 60, baseDate: LocalDate?): LiquidityForecast {
        val baseDate = baseDate ?: LocalDate.now()
        val forecast = LiquidityForecast(accountCache)
        // Consider only invoices of the last year:
        val historicalForecast = baseDate.isBefore(LocalDate.now())
        val fromDate = baseDate.minusMonths(12)
        forecast.setBaseDate(baseDate)

        processInvoices(forecast, baseDate, fromDate, nextDays, historicalForecast)

        val filter = LiquidityFilter()
        filter.baseDate = baseDate
        if (!historicalForecast) {
            filter.paymentStatus = PaymentStatus.UNPAID
        }
        val list: MutableList<LiquidityEntryDO> = liquidityEntryDao.getList(filter)
        if (historicalForecast) {
            list.removeIf { entry: LiquidityEntryDO -> entry.dateOfPayment!!.isBefore(baseDate) }
        }
        forecast.set(list)
        forecast.build()
        return forecast
    }

    private fun processInvoices(forecast: LiquidityForecast, baseDate: LocalDate, fromDate: LocalDate, nextDays: Int, historicalForecast: Boolean) {
        val rechnungFilter = createRechnungFilter(baseDate, fromDate, nextDays, historicalForecast)
        if (!historicalForecast) {
            rechnungFilter.setShowBezahlt()
            val paidInvoices: MutableList<RechnungDO> = rechnungDao.getList(rechnungFilter)
            forecast.calculateExpectedTimeOfPayments(paidInvoices)
            rechnungFilter.setShowUnbezahlt() // For next query.
        }
        val invoices: MutableList<RechnungDO> = rechnungDao.getList(rechnungFilter)
        handleHistoricalInvoices(invoices, baseDate, historicalForecast)
        forecast.setInvoices(invoices)
    }

    private fun processCreditorInvoices(forecast: LiquidityForecast, baseDate: LocalDate, fromDate: LocalDate, nextDays: Int, historicalForecast: Boolean) {
        val rechnungFilter = createRechnungFilter(baseDate, fromDate, nextDays, historicalForecast)
        if (!historicalForecast) {
            rechnungFilter.setShowBezahlt()
            val paidInvoices: MutableList<EingangsrechnungDO> = eingangsrechnungDao.getList(rechnungFilter)
            forecast.calculateExpectedTimeOfCreditorPayments(paidInvoices)
            rechnungFilter.setShowUnbezahlt() // For next query.
        }
        val invoices: MutableList<EingangsrechnungDO> = eingangsrechnungDao.getList(rechnungFilter)
        handleHistoricalInvoices(invoices, baseDate, historicalForecast)
        forecast.setCreditorInvoices(invoices)
    }

    private fun handleHistoricalInvoices(invoices: MutableList<out AbstractRechnungDO>, baseDate: LocalDate, historicalForecast: Boolean) {
        if (historicalForecast) {
            val historicalPaidDate = baseDate.minusDays(45)
            invoices.removeIf { invoice ->
                invoice.faelligkeit?.let {
                    it.isAfter(baseDate)
                } ?: invoice.datum!!.isAfter(historicalPaidDate)
            }
        }
    }

    private fun createRechnungFilter(baseDate: LocalDate, fromDate: LocalDate, nextDays: Int, historicalForecast: Boolean): RechnungFilter {
        val filter = RechnungFilter().setFromDate(fromDate)
        if (historicalForecast) {
            filter.setToDate(baseDate) // Only invoices issued before base date may be handled as paid.
        } else {
            filter.setShowBezahlt()
            filter.toDate = baseDate.plusDays(nextDays.toLong())
        }
        return filter
    }
}
