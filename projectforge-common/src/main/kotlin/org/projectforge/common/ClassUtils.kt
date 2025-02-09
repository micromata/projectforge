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

package org.projectforge.common

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object ClassUtils {

    fun forNameOrNull(clazz: String?): Class<*>? {
        clazz ?: return null
        return try {
            Class.forName(clazz)
        } catch (e: ClassNotFoundException) {
            log.error(e) { "Class not found: $clazz" }
            null
        }
    }

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
    fun <A : Annotation> getClassAnnotationOfField(
        clazz: Class<*>,
        property: String,
        annotationClass: Class<A>,
        suppressWarning: Boolean = false
    ): A? {
        val cls = getFieldInfo(clazz, property, suppressWarning)?.clazz ?: return null
        return getClassAnnotation(cls, annotationClass)
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
    fun getFieldInfo(clazz: Class<*>, property: String, suppressWarning: Boolean = false): Info? {
        val nestedProps = StringUtils.split(property, '.')
        if (nestedProps.isNullOrEmpty()) {
            if (!suppressWarning) {
                log.warn("Field '" + clazz.name + "." + property + "' not found (no property given).")
            }
            return null
        }
        var currentInfo: Info? = null
        var currentClass = clazz
        for (nestedProp in nestedProps) {
            val declaredFields = BeanHelper.getAllDeclaredFields(currentClass)
            for (field in declaredFields) {
                if (nestedProp == field.name) {
                    currentInfo = Info(currentClass, field, currentInfo)
                    if (Iterable::class.java.isAssignableFrom(field.type) && field.genericType is ParameterizedType) {
                        val type = field.genericType as ParameterizedType
                        currentClass = type.actualTypeArguments[0] as Class<*>
                        currentInfo.genericType = currentClass
                    } else {
                        currentClass = field.type
                    }
                    break
                }
            }
        }
        if (currentInfo == null && !suppressWarning) {
            log.warn("Field '" + clazz.name + "." + property + "' not found.")
        }
        return currentInfo
    }

    fun isKotlinClass(clazz: Class<*>): Boolean {
        return clazz.annotations.any { it.annotationClass.simpleName == "Metadata" }
    }

    /**
     * Sets a private field of an instance.
     *
     * @param instance The instance.
     * @param fieldName The field name.
     * @param value The value to set.
     */
    fun setPrivateField(instance: Any, fieldName: String, value: Any?) {
        val field: Field = instance::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(instance, value)
    }

    class Info(
        /**
         * The parent class of the field.
         */
        val clazz: Class<*>, val field: Field, val parent: Info? = null, var genericType: Class<*>? = null
    )
}
