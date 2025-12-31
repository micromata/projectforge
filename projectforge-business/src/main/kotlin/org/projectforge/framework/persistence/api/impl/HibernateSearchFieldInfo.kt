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

package org.projectforge.framework.persistence.api.impl

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.projectforge.common.KClassUtils
import org.projectforge.framework.persistence.search.ClassBridge
import kotlin.reflect.jvm.javaType

private val log = KotlinLogging.logger {}

class HibernateSearchFieldInfo(val javaProp: String, val type: Class<*>) {
    @JsonIgnore
    private var annotations: MutableList<Annotation>? = null
    var idProperty: Boolean = false
        internal set

    /**
     * The type of the value bridge.
     * For example:@GenericField(valueBridge = ValueBridgeRef(type = YearValueBridge::class))
     * The valueBridgeType is Int, because YearValueBridge.toIndexedValue returns an Int.
     */
    /*@JsonIgnore
    var valueBridgeType: Class<*>? = null*/

    var persistentField = false

    var luceneField: String = javaProp
        internal set

    fun add(annotation: Annotation?) {
        if (annotation == null) {
            return
        }
        if (annotations == null) {
            annotations = mutableListOf()
        }
        annotations!!.add(annotation)
        if (annotation is FullTextField && annotation.name.isNotBlank()) {
            luceneField = annotation.name
        }
        if (annotation is GenericField) {
            if (annotation.name.isNotBlank()) {
                luceneField = annotation.name
            }
            /*
            val valueBridgeRef = annotation.valueBridge
            val type = valueBridgeRef.type
            valueBridgeType = KClassUtils.getReturnTypeOfMethod(type, "toIndexedValue")?.javaType as? Class<*>*/
        }
    }

    fun hasAnnotations(): Boolean {
        return !annotations.isNullOrEmpty()
    }

    /**
     * If field is of type string or field is a class bridge.
     */
    fun isStringSearchSupported(): Boolean {
        return String::class.java.isAssignableFrom(type)
                || isClassBridge()
    }

    /**
     * @return true if field is an Int/Integer field or idProperty and has no FieldBridge with NumberBridge impl.
     */
    fun isNumericSearchSupported(): Boolean {
        /*if (valueBridgeType != null) {
            return numberTypes.any { it.isAssignableFrom(valueBridgeType) }
        }*/
        return numberTypes.any { it.isAssignableFrom(type) }
    }

    fun isClassBridge(): Boolean {
        return ClassBridge::class.java.isAssignableFrom(type)
    }

    companion object {
        private val numberTypes = listOf(
            java.lang.Integer::class.java,
            Int::class.java,
            Long::class.java,
            java.lang.Long::class.java,
            Short::class.java,
            java.lang.Short::class.java,
        )
    }
}
