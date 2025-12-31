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

package org.projectforge.framework.persistence.candh

import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.utils.CollectionUtils
import kotlin.reflect.KMutableProperty1

/**
 * Context of a property to be copied.
 * @param src Source object.
 * @param dest Destination object.
 * @param propertyName Name of the property.
 * @param property Property to be copied.
 * @param srcPropertyValue Value of the property in the source object.
 * @param destPropertyValue Value of the property in the destination object.
 */
class PropertyContext(
    val src: BaseDO<*>,
    val dest: BaseDO<*>,
    val propertyName: String,
    val property: KMutableProperty1<*, *>,
    val srcPropertyValue: Any?,
    val destPropertyValue: Any?,
) {
    /**
     * If the property is a collection, this flag indicates if the entries of the collection are historizable.
     */
    var entriesHistorizable: Boolean? = null

    /**
     * @see CollectionUtils.getTypeClassOfEntries
     */
    val propertyTypeClass: Class<*>
        get() = CollectionUtils.getTypeClassOfEntries(srcPropertyValue as Collection<*>)

    override fun toString(): String {
        return "PropertyContext(propertyName='$propertyName', srcPropertyValue=$srcPropertyValue, destPropertyValue=$destPropertyValue, entriesHistorizable=$entriesHistorizable)"
    }
}
