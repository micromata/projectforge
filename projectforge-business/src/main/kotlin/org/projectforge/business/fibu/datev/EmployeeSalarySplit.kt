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

package org.projectforge.business.fibu.datev

import org.projectforge.business.fibu.MonthlyEmployeeReport
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.framework.utils.CurrencyHelper
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The split of the gross salary of an employee and month to the cost units (kost2) by the hours of the time sheets,
 * as the salary export ([EmployeeSalaryExportDao]) writes it for DATEV: one record per kost2 on [KONTO] with
 * the counter account [GEGENKONTO]. Also used by the Kost2 reallocation to check the stored salary records.
 */
object EmployeeSalarySplit {
    /** The account of the salary records. */
    const val KONTO = 6000

    /** The counter account of the salary records. */
    const val GEGENKONTO = 3791

    /** One kost2 of the split: [amount] includes the [correction] (only given for the last part). */
    class Part(val kost2: Kost2DO, val millis: Long, val amount: BigDecimal, val correction: BigDecimal?)

    /**
     * The split of [brutto] (gross salary incl. employer's share) by the kost2 durations of the report: amount =
     * brutto × millis of the kost2 / total net duration of the month. The last part takes the difference to [brutto]
     * (rounding and the time without kost2).
     */
    @JvmStatic
    fun split(brutto: BigDecimal, report: MonthlyEmployeeReport): List<Part> {
        val netDuration = BigDecimal(report.totalNetDuration)
        val rows = report.kost2Rows.values.toList()
        var sum = BigDecimal.ZERO
        return rows.mapIndexed { index, row ->
            val kost2 = row.kost2!!
            val millis = report.kost2Durations[kost2.id]?.millis ?: 0L
            val amount = if (netDuration.signum() != 0) {
                CurrencyHelper.multiply(brutto, BigDecimal(millis).divide(netDuration, 8, RoundingMode.HALF_UP))
            } else {
                BigDecimal.ZERO
            }
            sum += amount
            if (index == rows.size - 1) {
                val correction = brutto - sum
                Part(kost2, millis, amount + correction, correction)
            } else {
                Part(kost2, millis, amount, null)
            }
        }
    }

    /** Are these the accounts of a salary record (account [KONTO], counter account [GEGENKONTO])? */
    @JvmStatic
    fun isSalaryRecord(kontoNummer: Int?, gegenKontoNummer: Int?): Boolean {
        return kontoNummer == KONTO && gegenKontoNummer == GEGENKONTO
    }
}
