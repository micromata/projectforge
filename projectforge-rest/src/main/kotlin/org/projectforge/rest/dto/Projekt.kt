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

import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.reporting.Kost2Art

class Projekt(id: Int? = null,
              displayName: String? = null,
              var nummer: Int = 0,
              var name: String? = null,
              var identifier: String? = null,
              var status: ProjektStatus? = null)
    : BaseDTODisplayObject<ProjektDO>(id, displayName = displayName) {
    var kunde: Kunde? = null
    var konto: Konto? = null
    var task: Task? = null
    var projektManagerGroup: Group? = null
    var nummernkreis: Int = 0
    var bereich: Int? = 0
    var kost: String? = null
    var kost2Arts: List<Kost2Art>? = null


    fun initialize(obj: ProjektDO) {
        copyFrom(obj)

        if(obj.kunde != null){
            this.kunde!!.initialize(obj.kunde!!)
        }
    }
}
