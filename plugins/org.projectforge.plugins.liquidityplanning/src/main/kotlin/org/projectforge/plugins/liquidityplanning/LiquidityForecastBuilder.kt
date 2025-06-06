/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var liquidityEntryDao: LiquidityEntryDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    /**
     * Calculates expected dates of payments inside the last year (-365 days).
     */
    open fun build(baseDate: LocalDate?): LiquidityForecast {
        val useBaseDate = baseDate ?: LocalDate.now()
        val forecast = LiquidityForecast()
        // Consider only invoices of the last year:
        val historicalForecast = useBaseDate.isBefore(LocalDate.now())
        val fromDate = useBaseDate.minusMonths(12)
        val toDate = useBaseDate.plusMonths(3)
        forecast.baseDate = useBaseDate

        processInvoices(forecast, useBaseDate, fromDate, toDate, historicalForecast)
        processCreditorInvoices(forecast, useBaseDate, fromDate, toDate, historicalForecast)

        val filter = LiquidityFilter()
        filter.baseDate = useBaseDate
        if (!historicalForecast) {
            filter.paymentStatus = PaymentStatus.UNPAID
        }
        val list: MutableList<LiquidityEntryDO> = liquidityEntryDao.select(filter).toMutableList()
        if (historicalForecast) {
            list.removeIf { entry: LiquidityEntryDO -> entry.dateOfPayment!!.isBefore(useBaseDate) }
        }
        forecast.set(list)
        forecast.build()
        return forecast
    }

    private fun processInvoices(forecast: LiquidityForecast, baseDate: LocalDate, fromDate: LocalDate, toDate: LocalDate, historicalForecast: Boolean) {
        val rechnungFilter = createRechnungFilter(baseDate, fromDate, toDate, historicalForecast)
        if (!historicalForecast) {
            rechnungFilter.setShowBezahlt()
            val paidInvoices = rechnungDao.select(rechnungFilter)
            forecast.calculateExpectedTimeOfPayments(paidInvoices)
            rechnungFilter.setShowUnbezahlt() // For next query.
        }
        val invoices = rechnungDao.select(rechnungFilter).toMutableList()
        handleHistoricalInvoices(invoices, baseDate, historicalForecast)
        forecast.setInvoices(invoices)
    }

    private fun processCreditorInvoices(forecast: LiquidityForecast, baseDate: LocalDate, fromDate: LocalDate, toDate: LocalDate, historicalForecast: Boolean) {
        val rechnungFilter = createRechnungFilter(baseDate, fromDate, toDate, historicalForecast)
        if (!historicalForecast) {
            rechnungFilter.setShowBezahlt()
            val paidInvoices = eingangsrechnungDao.select(rechnungFilter)
            forecast.calculateExpectedTimeOfCreditorPayments(paidInvoices)
            rechnungFilter.setShowUnbezahlt() // For next query.
        }
        val invoices = eingangsrechnungDao.select(rechnungFilter).toMutableList()
        handleHistoricalInvoices(invoices, baseDate, historicalForecast)
        forecast.setCreditorInvoices(invoices)
    }

    private fun handleHistoricalInvoices(invoices: MutableList<out AbstractRechnungDO>, baseDate: LocalDate, historicalForecast: Boolean) {
        if (historicalForecast) {
            val historicalPaidDate = baseDate.minusDays(45)
            invoices.removeIf { invoice ->
                invoice.faelligkeit?.let {
                    it.isBefore(baseDate)
                } ?: invoice.datum!!.isBefore(historicalPaidDate)
            }
        }
    }

    private fun createRechnungFilter(baseDate: LocalDate, fromDate: LocalDate, toDate: LocalDate, historicalForecast: Boolean): RechnungFilter {
        val filter = RechnungFilter().setFromDate(fromDate)
        if (historicalForecast) {
            filter.setToDate(baseDate) // Only invoices issued before base date may be handled as paid.
        } else {
            filter.setShowBezahlt()
            filter.toDate = toDate
        }
        return filter
    }
}
