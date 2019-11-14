/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils

import java.lang.reflect.Field

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object ClassUtils {
    private val log = org.slf4j.LoggerFactory.getLogger(ClassUtils::class.java)

    fun getProxiedClass(clazz: Class<*>): Class<*> {
        if (clazz.name.contains("$$")) {
            val superclass = clazz.superclass
            if (superclass != null && superclass != Any::class.java) {
                return getProxiedClass(superclass)
            }
        }
        return clazz
    }

    /**
     * @param clazz           The class of the specified property.
     * @param property        Nested properties are supported: task.project.title.
     * @param annotationClass The annotation to look for.
     * @param suppressWarning If true, no warning message will be logged, if property not found.
     * @return Annotation if found or null.
     */
    @JvmOverloads
    fun <A : Annotation> getClassAnnotationOfField(clazz: Class<*>, property: String, annotationClass: Class<A>, suppressWarning: Boolean = false): A? {
        val clazz = getClassOfField(clazz, property, suppressWarning)
        if (clazz == null) return null
        return getClassAnnotation(clazz, annotationClass)
    }

    /**
     * @param clazz           The class of the specified property.
     * @param annotationClass The annotation to look for.
     * @param suppressWarning If true, no warning message will be logged, if property not found.
     * @return
     */
    fun <A : Annotation> getClassAnnotation(clazz: Class<*>, annotationClass: Class<A>): A? {
        val annotation = clazz.getAnnotation(annotationClass)
        if (annotation != null) {
            return annotation
        }
        if (clazz.superclass == null) {
            return null
        }
        return getClassAnnotation(clazz.superclass, annotationClass)
    }

    /**
     * @param clazz           The class of the specified property.
     * @param property        Nested properties are supported: task.project.title.
     * @param suppressWarning If true, no warning message will be logged, if property not found.
     * @return
     */
    @JvmOverloads
    fun getClassOfField(clazz: Class<*>, property: String, suppressWarning: Boolean = false): Class<*>? {
        val nestedProps = StringUtils.split(property, '.')
        if (nestedProps.isNullOrEmpty()) {
            if (!suppressWarning) {
                log.warn("Field '" + clazz.name + "." + property + "' not found (no property given).")
            }
            return null
        }
        if (nestedProps.size == 1) {
            return clazz
        }
        var cls = clazz
        var field: Field? = null
        var pos = 0
        for (nestedProp in nestedProps) {
            if (++pos >= nestedProps.size) {
                // Ignore field itself to get parent.
                break
            }
            field = null // Reset field from previous loops.
            val declaredFields = BeanHelper.getAllDeclaredFields(cls)
            for (declaredField in declaredFields) {
                if (nestedProp == declaredField.name == true) {
                    field = declaredField
                    cls = field!!.type
                    break
                }
            }
        }
        if (field == null && !suppressWarning) {
            log.warn("Field '" + clazz.name + "." + property + "' not found.")
        }
        return field?.type
    }
}
