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

class Projekt(id: Int? = null,
              displayName: String? = null,
              var nummer: Int = 0,
              var name: String? = null,
              var identifier: String? = null,
              var status: ProjektStatus? = null,
              var kunde: Kunde? = null,
              var konto: Konto? = null,
              var task: Task? = null,
              var projektManagerGroup: Group? = null,
              var nummernkreis: Int? = null,
              var bereich: Int? = null,
              var kost2Arts: List<Kost2Art>? = null)
    : BaseDTODisplayObject<ProjektDO>(id, displayName = displayName) {

    /**
     * @see copyFromMinimal
     */
    constructor(src: ProjektDO): this() {
        copyFromMinimal(src)
    }

    override fun copyFrom(src: ProjektDO) {
        super.copyFrom(src)
        src.kunde?.let {
            this.kunde = Kunde(it)
        }
        src.konto?.let {
            this.konto = Konto(it)
        }
        src.task?.let {
            this.task = Task(it)
        }
    }

    val kost2ArtsAsString: String
        get() = kost2Arts?.joinToString { it.getFormattedId() } ?: ""

    fun transformKost2(allKost2Arts: List<org.projectforge.reporting.Kost2Art>?) {
        for (kost2 in allKost2Arts!!) {
            val kost2Art = Kost2Art()
            kost2Art.id = kost2.id
            kost2Art.name = kost2.name
            kost2Art.description = kost2.description
            kost2Art.fakturiert = kost2.isFakturiert
            kost2Art.projektStandard = kost2.isProjektStandard
            kost2Art.deleted = kost2.isDeleted
            kost2Art.selected = kost2.isSelected
            kost2Art.existsAlready = kost2.isExistsAlready
        }
    }
}
