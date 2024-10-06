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

package org.projectforge.framework.persistence.utils

import kotlinx.collections.immutable.toImmutableList
import org.projectforge.framework.persistence.api.IdObject

object CollectionUtils {
    class CompareListResult<T>(val added: Collection<T>? = null, val removed: Collection<T>? = null)

    /**
     * Compares two lists and returns the added and removed entries.
     * @param src Source list.
     * @param dest Destination list.
     */
    fun <T> compareLists(src: Collection<T>?, dest: Collection<T>?): CompareListResult<T> {
        if (src.isNullOrEmpty()) {
            if (dest.isNullOrEmpty()) {
                return CompareListResult()
            }
            return CompareListResult(removed = dest.toImmutableList())
        }
        if (dest.isNullOrEmpty()) {
            return CompareListResult(added = src.toImmutableList())
        }
        val added = addedEntries(src = dest, dest = src)
        val removed = addedEntries(src = src, dest = dest)
        return CompareListResult(added, removed)
    }

    /**
     * Returns all entries of src list which are not contained in dest list.
     */
    private fun <T> addedEntries(src: Collection<T>, dest: Collection<T>): List<T> {
        val foreignFirst = dest.firstOrNull() ?: return emptyList()
        if (foreignFirst is IdObject<*>) {
            val result = mutableListOf<T>()
            dest.forEach { item ->
                if (src.none { (it as IdObject<*>).id == (item as IdObject<*>).id }) {
                    result.add(item)
                }
            }
            return result
        }
        return dest.filterNot { it in src }
    }
}
