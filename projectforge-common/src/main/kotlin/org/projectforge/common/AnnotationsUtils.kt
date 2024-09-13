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

import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object AnnotationsUtils {
    /**
     * Get all annotations of field, getter and setter method.
     */
    fun getAnnotations(clazz: Class<*>, propertyName: String): Set<Annotation> {
        val set = mutableSetOf<Annotation>()
        addAnnotations(clazz, propertyName, set)
        val kotlinClass: KClass<*> = clazz.kotlin
        addAnnotations(kotlinClass, propertyName, set)
        return set
    }

    private fun addAnnotations(clazz: Class<*>, propertyName: String, annotations: MutableSet<Annotation>) {
        clazz.declaredFields.find { it.name == propertyName }?.let { field ->
            annotations.addAll(field.annotations)
        }
        clazz.declaredMethods.find { it.name == "get${propertyName.capitalize()}" }?.let { method ->
            annotations.addAll(method.annotations)
        }
        clazz.declaredMethods.find { it.name == "set${propertyName.capitalize()}" }?.let { method ->
            annotations.addAll(method.annotations)
        }
        clazz.superclass?.let { superclass ->
            addAnnotations(superclass, propertyName, annotations)
        }
    }

    private fun addAnnotations(clazz: KClass<*>, propertyName: String, annotations: MutableSet<Annotation>) {
        clazz.members.find { it.name == propertyName }?.annotations?.let {
            annotations.addAll(it)
        }
        clazz.superclasses.forEach { superclass ->
            addAnnotations(superclass, propertyName, annotations)
        }
    }
}
