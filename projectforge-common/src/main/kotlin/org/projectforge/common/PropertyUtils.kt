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

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class NestedNullException(message: String) : RuntimeException(message)

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object PropertyUtils {
    /**
     * Retrieves a nested property value from a given bean object using a dot-separated property path.
     * This method supports traversing nested objects, accessing properties within Maps by key, and
     * accessing elements in Lists by index (e.g., "list[0]").
     *
     * @param bean the root object from which to retrieve the property
     * @param name the dot-separated path to the nested property (e.g., "address.city" or "items[0].name")
     * @param throwException whether to throw an exception if any part of the path is null.
     * @return the value of the nested property, or throws an exception if any part of the path is null
     * @throws IllegalArgumentException if the bean is null or if the property name is empty
     * @throws NestedNullException if any nested property in the path is null
     */
    @Throws(NestedNullException::class, IllegalArgumentException::class, NoSuchElementException::class)
    @JvmOverloads
    @JvmStatic
    fun getProperty(bean: Any?, name: String, throwException: Boolean = false): Any? {
        if (bean == null) {
            if (throwException)
                throw IllegalArgumentException("No bean specified")
            else return null
        }
        if (name.isBlank()) {
            if (throwException)
                throw IllegalArgumentException("No name specified for bean class '${bean::class}'")
            else return null
        }

        val resolver = NestedPropertyResolver(name)
        var currentBean = bean

        while (resolver.hasNext()) {
            val next = resolver.next()
            currentBean = resolveProperty(currentBean, next, throwException = throwException)
            if (currentBean == null) {
                if (throwException) {
                    throw NestedNullException("Null property value for '$name' on bean class '${bean::class}'")
                }
                return null
            }
        }

        return currentBean
    }

    /**
     * Resolves a single level of a property name for the given bean. If the bean is a Map,
     * it will attempt to retrieve the value for the provided propertyName as a key. If the
     * bean is a List, it interprets the propertyName as an index (e.g., "items[0]") and
     * returns the element at that index. If it's a regular object, it tries to access the
     * Kotlin property with reflection.
     *
     * @param bean the object, Map, or List from which to retrieve the property
     * @param propertyName the name of the property or the index key for Maps or Lists
     * @return the value of the property, or null if the property doesn't exist or is not accessible
     * @throws IllegalArgumentException if an invalid index format is used
     */
    private fun resolveProperty(bean: Any?, propertyName: String, throwException: Boolean): Any? {
        return when (bean) {
            is Map<*, *> -> bean[propertyName]
            else -> {
                val actualPropertyName = propertyName.removeIndex()
                val index = propertyName.extractIndex()
                val propertyValue = bean?.getKotlinProperty(actualPropertyName)
                if (propertyValue is List<*> && index != null) {
                    // If property is a List, try to access the element by the extracted index
                    if (index >= propertyValue.size) {
                        if (throwException) {
                            throw IllegalArgumentException("Invalid index format for '$propertyName'")
                        }
                        return null
                    }
                    propertyValue.getOrNull(index)
                } else {
                    propertyValue // Return the property itself if not a list
                }
            }
        }
    }

    /**
     * Extension function to retrieve a property from a Kotlin class instance using reflection.
     * The function makes the property accessible if needed and retrieves its value.
     *
     * @receiver the object from which to retrieve the property
     * @param propertyName the name of the property to retrieve
     * @return the value of the specified property, or null if the property does not exist
     */
    private fun Any.getKotlinProperty(propertyName: String): Any? {
        val property = this::class.memberProperties.find { it.name == propertyName } ?: return null
        property.isAccessible = true
        return property.getter.call(this)
    }

    /**
     * Utility function to extract an integer index from a property name formatted as "name[index]".
     * For example, "items[2]" would extract the index 2.
     *
     * @receiver the string containing the index in brackets
     * @return the extracted integer index, or null if no valid index is found
     */
    private fun String.extractIndex(): Int? {
        val match = Regex("""\[(\d+)]$""").find(this)
        return match?.groupValues?.get(1)?.toInt()
    }

    /**
     * Removes the index notation from a property name, e.g., "list[2]" -> "list".
     *
     * @receiver The property name string, potentially containing an index.
     * @return The property name without the index notation.
     */
    private fun String.removeIndex(): String {
        return this.replace(Regex("""\[\d+]$"""), "")
    }
    class NestedPropertyResolver(private val fullName: String) {
        private val parts = fullName.split(".")
        private var currentIndex = 0

        fun hasNext(): Boolean = currentIndex < parts.size
        fun next(): String = parts[currentIndex++]
        fun reset() {
            currentIndex = 0
        }
    }


    data class Address(val city: String)
    data class User(val name: String, val address: Address)

}
