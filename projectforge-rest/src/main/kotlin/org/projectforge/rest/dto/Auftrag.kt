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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.framework.utils.NumberFormatter
import java.math.BigDecimal
import java.time.LocalDate

class Auftrag(
        var nummer: Int? = null,
        var customer: Customer? = Customer(),
        var project: Project? = Project(),
        var titel: String? = null,
        var positionen: MutableList<AuftragsPositionDO>? = null,
        var personDays: BigDecimal? = null,
        var referenz: String? = null,
        var assignedPersons: String? = null,
        var erfassungsDatum: LocalDate? = null,
        var entscheidungsDatum: LocalDate? = null,
        var nettoSumme: BigDecimal? = null,
        var beauftragtNettoSumme: BigDecimal? = null,
        var fakturiertSum: BigDecimal? = null,
        var zuFakturierenSum: BigDecimal? = null,
        var periodOfPerformanceBegin: LocalDate? = null,
        var periodOfPerformanceEnd: LocalDate? = null,
        var probabilityOfOccurrence: Int? = null,
        var auftragsStatus: AuftragsStatus? = null
) : BaseDTO<AuftragDO>() {
    var pos: String? = null

    var formattedNettoSumme: String? = null
    var formattedBeauftragtNettoSumme: String? = null
    var formattedFakturiertSum: String? = null
    var formattedZuFakturierenSum: String? = null

    override fun copyFrom(src: AuftragDO) {
        super.copyFrom(src)

        this.customer = src.kunde?.let {
            Customer(it)
        }
        this.project = src.projekt?.let {
            Project(it)
        }

        positionen = src.positionen
        personDays = src.personDays
        assignedPersons = src.assignedPersons
        formattedNettoSumme = NumberFormatter.formatCurrency(src.nettoSumme)
        formattedBeauftragtNettoSumme = NumberFormatter.formatCurrency(src.beauftragtNettoSumme)
        formattedFakturiertSum = NumberFormatter.formatCurrency(src.fakturiertSum)
        formattedZuFakturierenSum = NumberFormatter.formatCurrency(src.zuFakturierenSum)
        pos = "#" + positionen?.size
    }
}
