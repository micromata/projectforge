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

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragForecastType
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.core.ListPageCache
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.core.getList
import org.projectforge.rest.core.getListPage
import org.projectforge.rest.dto.Auftrag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpSession
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The forecast period filter of the order book: only orders the forecast expects an invoice for in the period,
 * each with that amount, and the sum of them in the statistics.
 */
class OrderForecastPeriodFilterTest : AbstractTestBase() {
    @Autowired
    private lateinit var orderEntityRest: OrderEntityRest

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var listPageCache: ListPageCache

    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setup() {
        logon(TEST_FINANCE_USER)
        request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getSession(Mockito.anyBoolean())).thenReturn(MockHttpSession())
    }

    @Test
    fun `only orders with a forecast in the period are listed, with their amount`() {
        val nextMonth = PFDay.now().beginOfMonth.plusMonths(1)
        val inPeriod = insertOrder(1000, nextMonth.localDate, nextMonth.endOfMonth.localDate)
        val later = insertOrder(500, nextMonth.plusMonths(3).localDate, nextMonth.plusMonths(3).endOfMonth.localDate)

        val result = list(filter(nextMonth.localDate, nextMonth.endOfMonth.localDate))
        assertRows(result, inPeriod, later)
    }

    @Test
    fun `the paged list gives the same rows and statistics`() {
        val nextMonth = PFDay.now().beginOfMonth.plusMonths(1)
        val inPeriod = insertOrder(1000, nextMonth.localDate, nextMonth.endOfMonth.localDate)
        val later = insertOrder(500, nextMonth.plusMonths(3).localDate, nextMonth.plusMonths(3).endOfMonth.localDate)
        val filter = filter(nextMonth.localDate, nextMonth.endOfMonth.localDate)
        val page = getListPage(request, orderEntityRest, auftragDao, listPageCache, filter.clone(), 0, 50, false)
        assertRows(orderEntityRest.postProcessResultSet(page, request, filter.clone()), inPeriod, later)
    }

    private fun assertRows(result: ResultSet<*>, inPeriod: Long, later: Long) {
        val rows = result.resultSet.filterIsInstance<Auftrag>().associateBy { it.id }
        assertNotNull(rows[inPeriod], "The order with a forecast in the period is listed.")
        assertFalse(later in rows, "An order with a forecast in another month is not listed.")
        assertEquals(0, BigDecimal(1000).compareTo(rows[inPeriod]!!.forecastSumInPeriod))
        assertTrue(rows.values.all { (it.forecastSumInPeriod ?: BigDecimal.ZERO).signum() != 0 })
        val statistics = result.statistics as OrderEntityRest.OrderStatistics
        assertEquals(
            0,
            rows.values.sumOf { it.forecastSumInPeriod!! }.compareTo(statistics.forecastSumInPeriod),
            "The statistics sum the amounts of the listed rows.",
        )
    }

    @Test
    fun `without the filter no forecast amount is given`() {
        val nextMonth = PFDay.now().beginOfMonth.plusMonths(1)
        val id = insertOrder(1000, nextMonth.localDate, nextMonth.endOfMonth.localDate)
        val result = list(MagicFilter())
        val row = result.resultSet.filterIsInstance<Auftrag>().first { it.id == id }
        assertEquals(null, row.forecastSumInPeriod)
        assertEquals(null, (result.statistics as OrderEntityRest.OrderStatistics).forecastSumInPeriod)
    }

    private fun list(filter: MagicFilter): ResultSet<*> =
        orderEntityRest.postProcessResultSet(getList(request, orderEntityRest, auftragDao, filter.clone()), request, filter)

    private fun filter(from: LocalDate, until: LocalDate): MagicFilter {
        val filter = MagicFilter()
        val entry = MagicFilterEntry(OrderEntityRest.FORECAST_PERIOD_FILTER)
        entry.value.fromValue = from.toString()
        entry.value.toValue = until.toString()
        filter.entries.add(entry)
        return filter
    }

    /** One commissioned order with a single position, performed in the given period. */
    private fun insertOrder(netSum: Int, begin: LocalDate, end: LocalDate): Long {
        val order = AuftragDO().also {
            it.nummer = auftragDao.nextNumber
            it.status = AuftragsStatus.BEAUFTRAGT
            // The default (following month) would put the revenue of a one month order into the month after it.
            it.forecastType = AuftragForecastType.CURRENT_MONTH
            it.periodOfPerformanceBegin = begin
            it.periodOfPerformanceEnd = end
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos $netSum"
                pos.nettoSumme = netSum.toBigDecimal()
                pos.status = AuftragsStatus.BEAUFTRAGT
            })
        }
        auftragDao.insert(order, checkAccess = false)
        return order.id!!
    }
}
