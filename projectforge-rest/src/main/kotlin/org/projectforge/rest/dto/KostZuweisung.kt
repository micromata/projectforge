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

import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import java.math.BigDecimal

/**
 * One cost assignment of an invoice position, as the edit form of `/next/invoice` sends and receives it.
 *
 * Never edited on its own: it travels as part of [RechnungsPosition.kostZuweisungen], and [index] is what
 * identifies it there — `RechnungsPositionDO.kostZuweisungen` carries `@OrderColumn(name = "index")`, so a
 * row the client sends without one would be written as a new row.
 *
 * Deleted assignments stay in the list with `deleted = true`. The collection has
 * `autoUpdateCollectionEntries = true` but no `@SoftDeleteCollection` (only `EingangsrechnungDO.positionen`
 * has that), so an assignment missing from the posted collection is deleted **physically**, together with
 * its history — and its index is then free again, which the order column would collide with.
 */
class KostZuweisung(
    var index: Short = 0,
    var netto: BigDecimal? = null,
    var kost1: Kost1? = null,
    var kost2: Kost2? = null,
    var comment: String? = null,
) : BaseDTO<KostZuweisungDO>() {

    /**
     * Also assigns the back reference [KostZuweisungDO.rechnungsPosition], because the collection handler
     * matches by (index, owner) — an assignment without it would look removed and be deleted.
     *
     * Called by [RechnungsPosition.copyTo], not by the framework. The destination has to be a fresh
     * [KostZuweisungDO]: its setter refuses an owner when one of the other two (incoming invoice position,
     * employee salary) is already set.
     */
    fun copyTo(dest: KostZuweisungDO, position: RechnungsPositionDO) {
        copyTo(dest)
        dest.rechnungsPosition = position
    }
}
