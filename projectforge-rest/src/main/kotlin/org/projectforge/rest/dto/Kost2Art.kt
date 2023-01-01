/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.common.StringHelper
import java.math.BigDecimal

class Kost2Art(
        id: Int? = null,
        displayName: String? = null,
        var name: String? = null,
        var fakturiert: Boolean = false,
        var workFraction: BigDecimal? = null,
        var projektStandard: Boolean = false,
        var description: String? = null
) : BaseDTODisplayObject<Kost2ArtDO>(id, displayName = displayName) {
    var selected: Boolean = false
    var existsAlready: Boolean = false

    fun getFormattedId(): String {
        return StringHelper.format2DigitNumber(id!!)
    }

    operator fun compareTo(o: Kost2Art): Int {
        return this.compareTo(o)
    }


}
