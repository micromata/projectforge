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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.common.PropertyUtils.getProperty

data class Address(val city: String?)
data class User(val name: String, val address: Address?, val attributes: Map<String, Any>?, val friends: List<User>?)

class PropertyUtilsTest {
    @Test
    fun `should retrieve a simple property`() {
        val user = User("Alice", Address("Wonderland"), null, null)
        val result = getProperty(user, "name")
        assertEquals("Alice", result)
    }

    @Test
    fun `should retrieve a nested property`() {
        val user = User("Alice", Address("Wonderland"), null, null)
        val result = getProperty(user, "address.city")
        assertEquals("Wonderland", result)
    }

    @Test
    fun `should retrieve a property within a map`() {
        val user = User("Alice", null, mapOf("age" to 30), null)
        val result = getProperty(user, "attributes.age")
        assertEquals(30, result)
    }

    @Test
    fun `should retrieve a property from a list by index`() {
        val friend = User("Bob", Address("Springfield"), null, null)
        val user = User("Alice", null, null, listOf(friend))
        val result = getProperty(user, "friends[0].name")
        assertEquals("Bob", result)
    }

    @Test
    fun `should throw NestedNullException when intermediate property is null`() {
        val user = User("Alice", null, null, null)
        val exception = assertThrows<NestedNullException> {
            getProperty(user, "address.city", true)
        }
        assertEquals("Null property value for 'address.city' on bean class 'class org.projectforge.common.User'", exception.message)
    }

    @Test
    fun `should throw IllegalArgumentException for invalid index in list`() {
        val user = User("Alice", null, null, listOf())
        val exception = assertThrows<IllegalArgumentException> {
            getProperty(user, "friends[1].name", true)
        }
        assertEquals("Invalid index format for 'friends[1]'", exception.message)
    }

    @Test
    fun `should throw IllegalArgumentException when bean is null`() {
        val exception = assertThrows<IllegalArgumentException> {
            getProperty(null, "name", true)
        }
        assertEquals("No bean specified", exception.message)
    }

    @Test
    fun `should throw IllegalArgumentException when name is blank`() {
        val user = User("Alice", Address("Wonderland"), null, null)
        val exception = assertThrows<IllegalArgumentException> {
            getProperty(user, "", true)
        }
        assertEquals("No name specified for bean class 'class org.projectforge.common.User'", exception.message)
    }

    @Test
    fun `should return null for non-existent property`() {
        val user = User("Alice", Address("Wonderland"), null, null)
        val result = getProperty(user, "nonExistentProperty")
        assertNull(result)
    }

    @Test
    fun `should return null for non-existent nested property`() {
        val user = User("Alice", Address("Wonderland"), null, null)
        val result = getProperty(user, "address.nonExistentProperty")
        assertNull(result)
    }
}
