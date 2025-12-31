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

package org.projectforge.framework.persistence.api

import java.io.Serializable

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
interface BaseDO<I : Serializable>
    : IdObject<I> {

    /**
     * Can be used for marking changes in a data object as minor changes. This means for example, that after minor changes
     * all dependent objects will not be re-indexed.
     */
    var isMinorChange: Boolean

    /**
     * Free use-able multi purpose attributes.
     *
     * @param key
     * @return
     */
    fun getTransientAttribute(key: String): Any?

    fun removeTransientAttribute(key: String): Any?

    fun setTransientAttribute(key: String, value: Any?)

    /**
     * Copies all values from the given src object excluding the values created and lastUpdate. Do not overwrite created
     * and lastUpdate from the original database object. Null values will be excluded therefore for such null properties
     * the original properties will be preserved. If you want to delete such properties, please overwrite them manually.
     * <br></br>
     * This method is required by BaseDao for example for updating DOs.
     *
     * @param src
     * @return true, if any modifications are detected, otherwise false;
     */
    fun copyValuesFrom(src: BaseDO<out Serializable>, vararg ignoreFields: String): EntityCopyStatus

    fun copyFrom(
        iface: Class<out BaseDO<I>>?, orig: BaseDO<I>,
        vararg ignoreCopyFields: String
    ): EntityCopyStatus {
        val mds = copyValuesFrom(orig, *ignoreCopyFields)
        return mds
    }
}
