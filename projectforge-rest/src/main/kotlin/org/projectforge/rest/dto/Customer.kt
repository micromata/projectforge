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

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeStatus
import org.projectforge.framework.i18n.translate

class Customer(id: Long? = null,
               displayName: String? = null,
               var nummer: Int? = null,
               var name: String? = null,
               var identifier: String? = null,
               var division: String? = null,
               var status: KundeStatus? = null,
               var description: String? = null,
               var konto: Konto? = null,
               var kost: String? = null
) : BaseDTODisplayObject<KundeDO>(id, displayName = displayName) {

    @get:JsonProperty
    val statusAsString: String?
        get() {
            status?.let { return translate(it.i18nKey) }
            return null
        }

    /**
     * @see copyFromMinimal
     */
    constructor(src: KundeDO) : this() {
        copyFromMinimal(src)
    }

    override fun copyFrom(src: KundeDO) {
        super.copyFrom(src)
        this.id = src.nummer
        this.kost = src.kost
        this.konto = src.konto?.let {
            val konto = Konto()
            konto.copyFromMinimal(it)
            konto
        }
    }
}
