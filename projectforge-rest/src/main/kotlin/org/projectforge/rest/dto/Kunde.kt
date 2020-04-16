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

import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeStatus

class Kunde(id: Int? = null,
            displayName: String? = null,
            var name: String? = null,
            var identifier: String? = null,
            var division: String? = null,
            var status: KundeStatus? = null,
            var description: String? = null
) : BaseDTODisplayObject<KundeDO>(id, displayName = displayName) {
    var nummer: Int? = id
    var kost: String? = null
    var konto: Konto? = null

    fun initialize(obj: KundeDO) {
        copyFrom(obj)

        if(obj.konto != null){
            this.konto!!.copyFrom(obj.konto!!)
        }

    }
}
