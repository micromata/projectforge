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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

class AnnotationsUtilsTest {
    @Test
    fun getAnnotationsTest() {
        assertAnnotations( "age", "Age")
        assertAnnotations( "firstName", "First name")
        assertAnnotations( "name", "Name")
        assertAnnotations( "staffNumber", "Number")
    }

    private fun assertAnnotations(propertyName: String, expectedValue: String) {
        val annotations = AnnotationsUtils.getAnnotations(Employee::class.java, propertyName)
        Assertions.assertNotNull(annotations)
        Assertions.assertEquals(1, annotations.size)
        val annotation = annotations.first() as MyAnnotation
        Assertions.assertEquals(expectedValue, annotation.name)
        Assertions.assertNotNull(AnnotationsUtils.getAnnotation(Employee::class.memberProperties.filter { it.name == propertyName }.first(), MyAnnotation::class.java))
    }

    internal inner class Employee : Person() {
        @get:MyAnnotation(name = "Number")
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        var staffNumber: Integer? = null
    }

    internal open inner class Person {
        @get:MyAnnotation(name = "Name")
        var name: String? = null

        @MyAnnotation(name = "First name")
        var firstName: String? = null

        @set:MyAnnotation(name = "Age")
        var age: Int? = null
    }

    @Retention(AnnotationRetention.RUNTIME)
    internal annotation class MyAnnotation(val name: String)
}
