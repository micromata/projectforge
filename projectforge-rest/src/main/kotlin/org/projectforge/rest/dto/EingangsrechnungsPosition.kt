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

import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.IRechnungsPosition
import org.projectforge.business.fibu.RechnungCalculator
import java.math.BigDecimal

class EingangsrechnungsPosition(
        var text: String? = null,
        override var menge: BigDecimal? = null,
        override var einzelNetto: BigDecimal? = null,
        override var vat: BigDecimal? = null

) : BaseDTO<EingangsrechnungsPositionDO>(), IRechnungsPosition {
    var kostZuweisungen: MutableList<KostZuweisung>? = null

    override val netSum: BigDecimal
        get() = RechnungCalculator.calculateNetSum(this)
}
