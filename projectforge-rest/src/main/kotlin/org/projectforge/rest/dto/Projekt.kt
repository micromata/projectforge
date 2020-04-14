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
              var status: ProjektStatus? = null)
    : BaseDTODisplayObject<ProjektDO>(id, displayName = displayName) {
    var kunde: Kunde? = Kunde()
    var konto: Konto? = Konto()
    var task: Task? = Task()
    var projektManagerGroup: Group? = null
    var nummernkreis: Int = 0
    var bereich: Int? = 0
    var kost: String? = null
    var kost2Arts: MutableList<Kost2Art>? = ArrayList()
    var kost2Arten: String? = null


    fun initialize(obj: ProjektDO) {
        copyFrom(obj)

        this.kost = obj.kost
        this.nummernkreis = obj.nummernkreis
        this.bereich = obj.bereich

        if(obj.kunde != null){
            this.kunde!!.initialize(obj.kunde!!)
        }

        if(obj.konto != null){
            this.konto!!.initialize(obj.konto!!)
        }

        if(obj.task != null){
            this.task!!.copyFromMinimal(obj.task!!)
        }
    }

    fun getKost2ArtsAsString(): String {
        if (kost2Arts == null) {
            return ""
        }
        val buf = StringBuilder()
        var first = true
        for (value in kost2Arts!!) {
            if (first) {
                first = false
            } else {
                buf.append(", ")
            }
            buf.append(value.getFormattedId())
        }
        return buf.toString()
    }

    fun transformKost2(allKost2Arts: List<org.projectforge.reporting.Kost2Art>?) {
        for (kost2 in allKost2Arts!!){
            val kost2Art = Kost2Art()
            kost2Art.id = kost2.id
            kost2Art.name = kost2.name
            kost2Art.description = kost2.description
            kost2Art.fakturiert = kost2.isFakturiert
            kost2Art.projektStandard = kost2.isProjektStandard
            kost2Art.deleted = kost2.isDeleted
            kost2Art.selected = kost2.isSelected
            kost2Art.existsAlready = kost2.isExistsAlready
            kost2Arts!!.add(kost2Art)
        }
    }
}
