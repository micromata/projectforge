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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.time.PFDay
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class ForecastOrderPosInfoTest {
    @Test
    fun `test time and materials pos`() {
        OrderInfo().also { it.status = AuftragsStatus.BEAUFTRAGT }.let { orderInfo ->
            val pos = JsonUtils.fromJson(pos1Json, OrderPositionInfo::class.java)!!
            // it.paymentScheduleEntries = emptyList()}
            val forecastInfo = ForecastOrderPosInfo(orderInfo, pos, baseDate = PFDay.of(2025, Month.JANUARY, 8))
            forecastInfo.calculate()
            Assertions.assertEquals(6, forecastInfo.months.size)
            for (i in 0..1) {
                Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.months[i].toBeInvoicedSum)
            }
            if (ForecastOrderPosInfo.DISTRIBUTE_UNUSED_BUDGET) {
                for (i in 2..4) {
                    Assertions.assertEquals("13891.25", forecastInfo.months[i].toBeInvoicedSum.toString())
                }
                Assertions.assertEquals("20857.54", forecastInfo.months[5].toBeInvoicedSum.toString())
                Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.difference)
            } else {
                for (i in 2..5) {
                    Assertions.assertEquals("13891.25", forecastInfo.months[i].toBeInvoicedSum.toString())
                }
                Assertions.assertEquals("-6966.29", forecastInfo.difference.toString())
            }
        }
        OrderInfo().also {
            it.status = AuftragsStatus.GELEGT
            it.probabilityOfOccurrence = 50
            it.periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1)
            it.periodOfPerformanceEnd = LocalDate.of(2025, Month.DECEMBER, 31)
        }.let { orderInfo ->
            val pos = JsonUtils.fromJson(pos2Json, OrderPositionInfo::class.java)!!
            // it.paymentScheduleEntries = emptyList()}
            val forecastInfo = ForecastOrderPosInfo(orderInfo, pos, baseDate = PFDay.of(2025, Month.JANUARY, 8))
            forecastInfo.calculate()
            Assertions.assertEquals(13, forecastInfo.months.size)
            Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.difference)
            Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.months[0].toBeInvoicedSum)
            for (i in 1..12) {
                Assertions.assertEquals("41666.6667", forecastInfo.months[i].toBeInvoicedSum.toString())
            }
        }
    }

    companion object {
        val pos1Json = """
              |{
              |  "id": 44474374,
              |  "number": 1,
              |  "auftragId": 44474373,
              |  "auftragNummer": 6308,
              |  "titel": "title",
              |  "status": "BEAUFTRAGT",
              |  "paymentType": "TIME_AND_MATERIALS",
              |  "art": "NEUENTWICKLUNG",
              |  "personDays": 63.00,
              |  "modeOfPaymentType": null,
              |  "dbNetSum": 69456.24,
              |  "vollstaendigFakturiert": false,
              |  "periodOfPerformanceType": "OWN",
              |  "periodOfPerformanceBegin": "2024-11-13",
              |  "periodOfPerformanceEnd": "2025-03-31",
              |  "toBeInvoiced": false,
              |  "commissionedNetSum": 69456.24,
              |  "netSum": 69456.24,
              |  "akquiseSum": 0,
              |  "invoicedSum": 6924.95,
              |  "toBeInvoicedSum": 0,
              |  "notYetInvoiced": 62531.29
              |}
        """.trimMargin()

        val pos2Json = """
              |{
              |  "id": 61817701,
              |  "number": 1,
              |  "auftragId": 61817651,
              |  "auftragNummer": 6395,
              |  "titel": "Submitted Position with 50% probability",
              |  "status": "GELEGT",
              |  "paymentType": "TIME_AND_MATERIALS",
              |  "art": "NEUENTWICKLUNG",
              |  "personDays": 1000.00,
              |  "modeOfPaymentType": null,
              |  "dbNetSum": 1000000.00,
              |  "vollstaendigFakturiert": false,
              |  "periodOfPerformanceType": "SEEABOVE",
              |  "periodOfPerformanceBegin": null,
              |  "periodOfPerformanceEnd": null,
              |  "taskId": 61815017,
              |  "toBeInvoiced": false,
              |  "commissionedNetSum": 0,
              |  "netSum": 1000000.00,
              |  "akquiseSum": 1000000.00,
              |  "invoicedSum": 0,
              |  "toBeInvoicedSum": 0,
              |  "notYetInvoiced": 0,
              |  "snapshotVersion": false
              |}
        """.trimMargin()
    }
}
