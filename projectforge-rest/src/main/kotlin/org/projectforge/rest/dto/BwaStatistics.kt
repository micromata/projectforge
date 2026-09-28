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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.kost.AccountingConfig
import org.projectforge.business.fibu.kost.BusinessAssessment
import org.projectforge.business.fibu.kost.BuchungssatzDO
import java.math.BigDecimal

/**
 * The "Betriebswirtschaftliche Auswertung" (business assessment) of a set of accounting records, sent to the
 * next frontend as raw amounts (formatted client-side via lib/format.ts) rather than the legacy server-rendered
 * HTML of [BusinessAssessment.asHtml].
 *
 * Mirrors what the Wicket accounting-record list shows above its table: the three headline figures plus the full
 * row table.
 */
class BwaStatistics(
    /** Overall performance (Gesamtleistung) headline amount. */
    var overallPerformance: BigDecimal? = null,
    /** Merchandise purchase (Wareneinkauf) headline amount. */
    var merchandisePurchase: BigDecimal? = null,
    /** Preliminary result (vorläufiges Ergebnis) headline amount. */
    var preliminaryResult: BigDecimal? = null,
    /** All assessment rows, in configured order, for the expandable full table. */
    var rows: List<BwaRow> = emptyList(),
) {
    companion object {
        /**
         * Builds the assessment over the given records, exactly as the Wicket page does
         * (AccountingConfig business-assessment config). Returns null for an empty record set (no assessment).
         */
        fun from(records: List<BuchungssatzDO>?): BwaStatistics? {
            if (records.isNullOrEmpty()) {
                return null
            }
            val assessment = BusinessAssessment(AccountingConfig.getInstance().businessAssessmentConfig)
            assessment.setAccountRecords(records)
            return BwaStatistics(
                overallPerformance = assessment.overallPerformanceRowAmount,
                merchandisePurchase = assessment.merchandisePurchaseRowAmount,
                preliminaryResult = assessment.preliminaryResultRowAmount,
                rows = assessment.rows.map { row ->
                    BwaRow(
                        no = row.no,
                        id = row.id,
                        title = row.title,
                        amount = row.amount,
                        indent = row.indent,
                        scale = row.scale,
                        unit = row.unit,
                    )
                },
            )
        }
    }
}

/** One row of a [BwaStatistics] assessment table. */
class BwaRow(
    var no: String? = null,
    var id: String? = null,
    var title: String? = null,
    var amount: BigDecimal? = null,
    var indent: Int = 0,
    var scale: Int = 2,
    var unit: String? = null,
)
