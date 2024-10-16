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

package org.projectforge.common

import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object KClassUtils {
    /**
     * Filters all mutable properties of the given class.
     * Ignores all properties that do not have a public getter and setter.
     * This method doesn't work for Java classes, because the visibility of the fields is never 'public'.
     * @param kClass The class to filter the mutable properties from.
     * @return A list of all mutable properties of the given class.
     */
    fun filterPublicMutableProperties(kClass: KClass<*>): MutableList<KMutableProperty1<*, *>> {
        val result = mutableListOf<KMutableProperty1<*, *>>()
        kClass.members.filterIsInstance<KMutableProperty1<*, *>>().forEach { property ->
            // The following check does not work for Java classes, because the visibility of the fields is never 'public'.
            // For support of Java classes, the visibility check must be modified.
            if (property.setter.visibility != KVisibility.PUBLIC || property.getter.visibility != KVisibility.PUBLIC) {
                log.debug { "copyProperties: Getter and/or setter of property $kClass.${property.name} has not visibility 'public', ignoring it." }
                return@forEach
            }
            result.add(property)
        }
        return result
    }
}
