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
import org.projectforge.common.extensions.capitalize
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object AnnotationsUtils {
    fun getClassAnnotations(clazz: Class<*>): Array<Annotation> {
        return clazz.annotations
    }

    fun getClassAnnotation(clazz: Class<*>, annotationClass: Class<out Annotation>): Annotation? {
        return clazz.annotations.filterIsInstance(annotationClass).firstOrNull()
    }

    fun hasClassAnnotation(clazz: Class<*>, annotationClass: Class<out Annotation>): Boolean {
        return getClassAnnotation(clazz, annotationClass) != null
    }

    fun <Ann : Annotation> getAnnotation(property: KProperty1<*, *>, annotationClass: Class<Ann>): Ann? {
        property.annotations.filterIsInstance(annotationClass).firstOrNull()?.let { return it }
        property.javaField?.let { field ->
            field.annotations.filterIsInstance(annotationClass).firstOrNull()?.let { return it }
        }
        property.javaGetter?.let { getter ->
            getter.annotations.filterIsInstance(annotationClass).firstOrNull()?.let { return it }
        }
        if (property is KMutableProperty1) {
            property.javaSetter?.let { setter ->
                setter.annotations.filterIsInstance(annotationClass).firstOrNull()?.let { return it }
            }
        }
        return null
    }

    fun getAnnotations(property: KProperty1<*, *>): List<Annotation> {
        val annotations = mutableListOf<Annotation>()
        annotations.addAll(property.annotations)
        property.javaField?.let { field ->
            annotations.addAll(field.annotations)
        }
        property.javaGetter?.let { getter ->
            annotations.addAll(getter.annotations)
        }
        if (property is KMutableProperty1) {
            property.javaSetter?.let { setter ->
                annotations.addAll(setter.annotations)
            }
        }
        return annotations
    }

    fun <T : Annotation> getAnnotation(clazz: Class<*>, propertyName: String, annotationClass: Class<T>): T? {
        return getAnnotations(clazz, propertyName).find { it.annotationClass == annotationClass } as? T
    }

    fun hasAnnotation(property: KProperty1<*, *>, annotationClass: Class<out Annotation>): Boolean {
        return getAnnotation(property, annotationClass) != null
    }

    /**
     * @param property The property to check. Must be of type KProperty1, otherwise false is returned.
     */
    fun hasAnnotation(property: KCallable<*>, annotationClass: Class<out Annotation>): Boolean {
        if (property is KProperty1<*, *>) {
            return hasAnnotation(property, annotationClass)
        }
        log.warn { "hasAnnotation is called, but property is not of type KProperty1: $property" }
        return false
    }

    fun hasAnnotation(clazz: Class<*>, propertyName: String, annotationClass: Class<out Annotation>): Boolean {
        return getAnnotation(clazz, propertyName, annotationClass) != null
    }

    /**
     * Get all annotations of field, getter and setter method.
     */
    fun getAnnotations(kClass: KClass<*>, propertyName: String): Set<Annotation> {
        val set = mutableSetOf<Annotation>()
        addAnnotations(kClass, propertyName, set)
        return set
    }

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

    /* private fun addAnnotations(clazz: Class<*>, propertyName: String, annotations: MutableSet<Annotation>) {
         clazz.declaredFields.find { it.name == propertyName }?.let { field ->
             annotations.addAll(field.annotations)
         }
         clazz.declaredMethods.find { it.name == "get${propertyName.capitalize()}" }
             ?.let { method ->
                 annotations.addAll(method.annotations)
             }
         clazz.declaredMethods.find { it.name == "set${propertyName.capitalize()}" }
             ?.let { method ->
                 annotations.addAll(method.annotations)
             }
         clazz.superclass?.let { superclass ->
             addAnnotations(superclass, propertyName, annotations)
         }
     }*/

    private fun addAnnotations(clazz: Class<*>, propertyName: String, annotations: MutableSet<Annotation>) {
        // Split the propertyName at each point to detect the nested levels
        val propertyParts = propertyName.split(".")

        var currentClass: Class<*> = clazz

        // Loop over all parts of the property name except the last one, as this is the deepest level
        for (i in 0 until propertyParts.size - 1) {
            val part = propertyParts[i]

            // Find the field for the current property level
            val field = currentClass.declaredFields.find { it.name == part }
            if (field != null) {
                currentClass = field.type
            } else {
                // If the field is not found, we try to find the getter method
                val getterMethod = currentClass.declaredMethods.find { it.name == "get${part.capitalize()}" }
                if (getterMethod != null) {
                    currentClass = getterMethod.returnType
                } else {
                    // If neither field nor method is found, the property cannot be traced further
                    return
                }
            }
        }

        // Process the final property level
        val finalProperty = propertyParts.last()

        // Add annotations of the field if it exists
        currentClass.declaredFields.find { it.name == finalProperty }?.let { field ->
            annotations.addAll(field.annotations)
        }

        // Add annotations to the getter method if it exists
        currentClass.declaredMethods.find { it.name == "get${finalProperty.capitalize()}" }?.let { method ->
            annotations.addAll(method.annotations)
        }

        // Add annotations to the setter method if it exists
        currentClass.declaredMethods.find { it.name == "set${finalProperty.capitalize()}" }?.let { method ->
            annotations.addAll(method.annotations)
        }

        // If the superclass exists, apply recursively to the superclass
        currentClass.superclass?.let { superclass ->
            addAnnotations(superclass, finalProperty, annotations)
        }
    }

    private fun addAnnotations(clazz: KClass<*>, propertyName: String, annotations: MutableSet<Annotation>) {
        clazz.members.find { it.name == propertyName }?.annotations?.let {
            annotations.addAll(it)
        }
        clazz.members.find { it.name == propertyName }?.let { member ->
            member.annotations.let {
                annotations.addAll(it)
            }
            if (member is KMutableProperty1<*, *>) {
                member.setter.annotations.let {
                    annotations.addAll(it)
                }
                member.getter.annotations.let {
                    annotations.addAll(it)
                }
            } else if (member is KProperty1<*, *>) {
                member.getter.annotations.let {
                    annotations.addAll(it)
                }
            } else {
                // Can't access getter/setter (OK).
            }
        }
        clazz.superclasses.forEach { superclass ->
            addAnnotations(superclass, propertyName, annotations)
        }
    }
}
