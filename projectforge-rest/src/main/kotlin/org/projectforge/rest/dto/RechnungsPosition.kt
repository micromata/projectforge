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

import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.KostZuweisungDO
import java.math.BigDecimal

class RechnungsPosition(
        var text: String? = null,
        override var menge: BigDecimal? = null,
        override var einzelNetto: BigDecimal? = null,
        override var vat: BigDecimal? = null

) : BaseDTO<RechnungsPositionDO>(), IRechnungsPosition {

    var kostZuweisungen: MutableList<KostZuweisung>? = null

    override val netSum: BigDecimal
        get() = RechnungCalculator.calculateNetSum(this)

    /**
     * @see copyFrom
     */
    constructor(src: RechnungsPositionDO): this() {
        copyFrom(src)
    }

    override fun copyFrom(src: RechnungsPositionDO) {
        super.copyFrom(src)
        val list = kostZuweisungen ?: mutableListOf()
        src.kostZuweisungen?.forEach {
            val pos = KostZuweisung()
            pos.copyFrom(it)
            list.add(pos)
        }
        kostZuweisungen = list
    }

    override fun copyTo(dest: RechnungsPositionDO) {
        super.copyTo(dest)
        val list = dest.kostZuweisungen ?: mutableListOf()
        kostZuweisungen?.forEach {
            val pos = KostZuweisungDO()
            it.copyTo(pos)
            list.add(pos)
        }
        dest.kostZuweisungen = list
    }
}
