/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.address

import org.projectforge.business.address.AddressDao.Companion.getNormalizedFullname
import org.projectforge.framework.persistence.api.impl.CustomResultFilter

class DoubletsResultFilter : CustomResultFilter<AddressDO> {
    val fullnames: MutableSet<String> = HashSet()
    val doubletFullnames: MutableSet<String> = HashSet()
    val all: MutableList<AddressDO> = ArrayList() // Already processed addresses to add doublets.
    val addedDoublets: MutableSet<Long?> = HashSet()

    override fun match(list: MutableList<AddressDO>, element: AddressDO): Boolean {
        if (element.deleted) {
            return false
        }
        val fullname = getNormalizedFullname(element)
        if (fullnames.contains(fullname)) {
            doubletFullnames.add(fullname)
            for (adr in all) {
                if (addedDoublets.contains(adr.id)) {
                    continue  // Already added.
                } else if (doubletFullnames.contains(getNormalizedFullname(adr))) {
                    //throw UnsupportedOperationException("Doublet found: $element and $adr")
                    list.add(adr) // TODO: Add doublet to list, if necessary (to be checked).
                    addedDoublets.add(adr.id) // Mark this address as already added.
                }
            }
            return true
        }
        all.add(element)
        fullnames.add(fullname)
        return false
    }
}
