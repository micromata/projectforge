/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.persistence.api.IdObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

object CollectionUtils {
    class CompareCollectionsResult<T>(
        /** New entries in src, not yet part of dest. */
        val added: Collection<T>? = null,
        /** Removed entries in src, to be removed in dest. */
        val removed: Collection<T>? = null,
        /** Entries in src and dest. */
        val kept: Collection<T>? = null,
    ) {
        /**
         * Returns the first entry of added, removed or kept entries. Is needed to check weather the entries should
         * be historized or not.
         */
        val anyOrNull: T?
            get() = added?.firstOrNull() ?: removed?.firstOrNull() ?: kept?.firstOrNull()
    }

    fun getTypeClassOfEntries(col: Collection<*>?): Class<*> {
        return getTypeClassOfEntriesOrNull(col)!!
    }

    fun getTypeClassOfEntriesOrNull(col: Collection<*>?): Class<*>? {
        col ?: return null
        return col.firstOrNull()?.javaClass
    }

    fun isCollection(property: kotlin.reflect.KProperty1<*, *>): Boolean {
        return property.returnType.jvmErasure.isSubclassOf(Collection::class)
    }

    /**
     * Joins the id's of the given collection to a csv string. The entries are sorted by id.
     * Used for example in history entries, representing the removed and added entries of a collection.
     */
    fun joinToStringOfIds(col: Collection<IdObject<Long>>?): String? {
        col ?: return null
        return joinToString(col.map { it.id })
    }

    /**
     * Joins the given collection to a csv string. The entries are sorted.
     */
    fun <T : Comparable<T>> joinToString(col: Collection<T?>?): String? {
        col ?: return null
        return col.filterNotNull().sorted().joinToString(separator = ",")
    }

    /**
     * Compares two collection and returns the added, removed entries and kept entries.
     * @param src Source collection.
     * @param dest Destination collection.
     * @param withKept If true, the kept entries (part of both collections) are returned as well.
     */
    fun <T> compareCollections(
        src: Collection<T?>?,
        dest: Collection<T?>?,
        withKept: Boolean = false,
    ): CompareCollectionsResult<T> {
        val useSrc = src?.filterNotNull()
        val useDest = dest?.filterNotNull()
        if (useSrc.isNullOrEmpty()) {
            if (useDest.isNullOrEmpty()) {
                return CompareCollectionsResult()
            }
            return CompareCollectionsResult(removed = useDest.toList())
        }
        if (useDest.isNullOrEmpty()) {
            return CompareCollectionsResult(added = useSrc.toList())
        }
        val added = getAddedEntries(src = useSrc, dest = useDest)
        val removed = getAddedEntries(src = useDest, dest = useSrc)
        val kept = if (withKept) getKeptEntries(src = useSrc, dest = useDest) else null
        return CompareCollectionsResult(added, removed, kept)
    }

    /**
     * Returns all entries of src collection which are not contained in dest collection.
     */
    private fun <T> getAddedEntries(src: Collection<T>, dest: Collection<T>): Collection<T> {
        val srcFirst = dest.firstOrNull() ?: return emptyList()
        if (srcFirst !is IdObject<*>) {
            return src.filterNot { it in dest }
        }
        val result = mutableListOf<T>()
        src.forEach { item ->
            if (dest.none { idObjectsEqual(it as IdObject<*>, item as IdObject<*>) }) {
                result.add(item)
            }
        }
        return result
    }

    /**
     * Returns all entries of src collection which are part of both collections.
     */
    private fun <T> getKeptEntries(src: Collection<T>, dest: Collection<T>): List<T> {
        val srcFirst = dest.firstOrNull() ?: return emptyList()
        if (srcFirst !is IdObject<*>) {
            return src.filter { it in dest }
        }
        val result = mutableListOf<T>()
        src.forEach { item ->
            if (dest.any { idObjectsEqual(it as IdObject<*>, item as IdObject<*>) }) {
                result.add(item)
            }
        }
        return result
    }

    /**
     * Compares two IdObjects by their id's, if given, otherwise by equals.
     */
    fun idObjectsEqual(src: IdObject<*>, dest: IdObject<*>): Boolean {
        return if (src.id == null || dest.id == null) {
            src == dest
        } else {
            src.id == dest.id
        }
    }
}
