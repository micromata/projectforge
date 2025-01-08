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
        val pos1 = JsonUtils.fromJson(pos1Json, OrderPositionInfo::class.java)!!
        val orderInfo = OrderInfo().also { it.status = AuftragsStatus.BEAUFTRAGT }
        // it.paymentScheduleEntries = emptyList()}
        val forecastInfo = ForecastOrderPosInfo(orderInfo, pos1, baseDate = PFDay.of(2025, Month.JANUARY, 8))
        forecastInfo.calculate()
        Assertions.assertEquals(6, forecastInfo.months.size)
        Assertions.assertEquals("-6966.29", forecastInfo.difference.toString())
        for (i in 0..1) {
            Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.months[i].toBeInvoicedSum)
        }
        for (i in 2..5) {
            Assertions.assertEquals("13891.25", forecastInfo.months[i].toBeInvoicedSum.toString())
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
    }
}
