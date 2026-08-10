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

import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.KostentraegerStatus

class Kost1(
    id: Long? = null,
    displayName: String? = null,
    var nummernkreis: Int = 0,
    var bereich: Int = 0,
    var teilbereich: Int = 0,
    var endziffer: Int = 0,
    var kostentraegerStatus: KostentraegerStatus? = null,
    var description: String? = null,
    /**
     * Format `#.###.##.##`, computed by the entity. Read-only: [Kost1DO.formattedNumber] is a getter
     * without a backing field, so [copyTo] can't write it back and the client must not send one.
     */
    var formattedNumber: String? = null,
) : BaseDTODisplayObject<Kost1DO>(id, displayName = displayName) {

    /**
     * @see copyFromMinimal
     */
    constructor(src: Kost1DO) : this() {
        copyFromMinimal(src)
    }

    /**
     * The four number fields, the status and the description are what identifies a cost unit, so even an
     * embedded Kost1 carries them - without them a caller only gets the id and has to look the number up again.
     */
    override fun copyFromMinimal(src: Kost1DO) {
        super.copyFromMinimal(src)
        nummernkreis = src.nummernkreis
        bereich = src.bereich
        teilbereich = src.teilbereich
        endziffer = src.endziffer
        kostentraegerStatus = src.kostentraegerStatus
        description = src.description
        formattedNumber = src.formattedNumber
        displayName = KostFormatter.instance.formatKost1(src, KostFormatter.FormatType.TEXT)
    }

    /**
     * [BaseDTO.copyFrom] copies fields by name, and [Kost1DO.formattedNumber] has none (getter only),
     * so it has to be assigned here - the list and the edit page both show it.
     */
    override fun copyFrom(src: Kost1DO) {
        super.copyFrom(src)
        formattedNumber = src.formattedNumber
        displayName = KostFormatter.instance.formatKost1(src, KostFormatter.FormatType.TEXT)
    }
}
