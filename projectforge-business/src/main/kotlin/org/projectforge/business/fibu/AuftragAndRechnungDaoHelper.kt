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

import org.projectforge.common.extensions.isZeroOrNull
import org.projectforge.framework.i18n.RequiredFieldIsEmptyException
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.and
import org.projectforge.framework.persistence.api.QueryFilter.Companion.between
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ge
import org.projectforge.framework.persistence.api.QueryFilter.Companion.le
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

object AuftragAndRechnungDaoHelper {
    @JvmStatic
    fun createCriterionForPeriodOfPerformance(myFilter: SearchFilterWithPeriodOfPerformance): Optional<DBPredicate> {
        val popBeginName = "periodOfPerformanceBegin"
        val popEndName = "periodOfPerformanceEnd"
        val startDate = myFilter.periodOfPerformanceStartDate
        val endDate = myFilter.periodOfPerformanceEndDate
        if (startDate != null && endDate != null) {
            return Optional.of(
                    and(ge(popEndName, startDate),
                            le(popBeginName, endDate)
                    )
            )
        }
        if (startDate != null) {
            return Optional.of(
                    ge(popEndName, startDate)
            )
        }
        return if (endDate != null) {
            Optional.of(
                    le(popBeginName, endDate)
            )
        } else Optional.empty()
    }

    @JvmStatic
    fun createQueryFilterWithDateRestriction(myFilter: RechnungFilter): QueryFilter {
        val dateName = "datum"
        val fromDate = myFilter.getFromDate()
        val toDate = myFilter.getToDate()
        val queryFilter = QueryFilter(myFilter)
        if (fromDate != null && toDate != null) {
            queryFilter.add(between(dateName, fromDate, toDate))
        } else if (fromDate != null) {
            queryFilter.add(ge(dateName, fromDate))
        } else if (toDate != null) {
            queryFilter.add(le(dateName, toDate))
        }
        return queryFilter
    }

    @JvmStatic
    fun onSaveOrModify(rechnung: AbstractRechnungDO) {
        checkAndCalculateFaelligkeit(rechnung)
        checkAndCalculateDiscountMaturity(rechnung)
        validateFaelligkeit(rechnung)
        validateBezahlDatumAndZahlBetrag(rechnung)
    }

    private fun checkAndCalculateFaelligkeit(rechnung: AbstractRechnungDO) {
        val zahlungsZiel = rechnung.zahlungsZielInTagen
        if (rechnung.faelligkeit == null && zahlungsZiel != null) {
            val rechnungsDatum: LocalDate? = rechnung.datum
            if (rechnungsDatum != null) {
                var day = PFDateTime.from(rechnungsDatum) // not null
                day = day.plusDays(zahlungsZiel.toLong())
                rechnung.faelligkeit = day.localDate
            }
        }
    }

    private fun checkAndCalculateDiscountMaturity(rechnung: AbstractRechnungDO) {
        if (rechnung.discountPercent.isZeroOrNull()) {
            // No discount, so no discount maturity.
            return
        }
        val discountZahlungsZiel = rechnung.discountZahlungsZielInTagen
        if (rechnung.discountMaturity == null && discountZahlungsZiel != null) {
            val rechnungsDatum: LocalDate? = rechnung.datum
            if (rechnungsDatum != null) {
                var day = PFDay.from(rechnungsDatum)
                day = day.plusDays(discountZahlungsZiel.toLong())
                rechnung.discountMaturity = day.localDate
            }
        }
    }

    private fun validateFaelligkeit(rechnung: AbstractRechnungDO) {
        if (rechnung.faelligkeit == null) {
            throw RequiredFieldIsEmptyException("fibu.rechnung.faelligkeit")
        }
    }

    private fun validateBezahlDatumAndZahlBetrag(rechnung: AbstractRechnungDO) {
        val bezahlDatum: LocalDate? = rechnung.bezahlDatum
        val zahlBetrag = rechnung.zahlBetrag
        val zahlBetragExists = zahlBetrag != null && zahlBetrag.compareTo(BigDecimal.ZERO) != 0
        if (bezahlDatum != null && !zahlBetragExists) {
            throw UserException("fibu.rechnung.error.zahlbetragRequired")
        }
        if (bezahlDatum == null && zahlBetragExists) {
            throw UserException("fibu.rechnung.error.bezahlDatumRequired")
        }
    }
}
