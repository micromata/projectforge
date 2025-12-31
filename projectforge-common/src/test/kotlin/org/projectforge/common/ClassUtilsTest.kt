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

class ClassUtilsTest {
    @Test
    fun getClassOfFieldTest() {
        var fieldInfo = ClassUtils.getFieldInfo(Timesheet::class.java, "employee.mainAddress.street")
        Assertions.assertEquals(Address::class.java, fieldInfo!!.clazz)
        Assertions.assertEquals(String::class.java, fieldInfo.field.type)
        //Assertions.assertEquals(Address::class.java, ClassUtils.getClassOfField(Timesheet::class.java, "employee.addresses.street"))
        fieldInfo = ClassUtils.getFieldInfo(Address::class.java, "street")
        Assertions.assertEquals(Address::class.java, fieldInfo!!.clazz)
    }

    @Test
    fun isKotlinClassTest() {
        Assertions.assertTrue(ClassUtils.isKotlinClass(ClassUtilsTest::class.java))
        Assertions.assertFalse(ClassUtils.isKotlinClass(BeanHelper::class.java))
    }

    @Test
    fun annotationTest() {
        val ann = ClassUtils.getClassAnnotation(Address::class.java, MyAnnotation::class.java)
        Assertions.assertNotNull(ann)
        Assertions.assertEquals("address", ann!!.name)
        Assertions.assertEquals("person", ClassUtils.getClassAnnotation(Employee::class.java, MyAnnotation::class.java)!!.name, "Annotation of super class expected.")
        Assertions.assertEquals("address", ClassUtils.getClassAnnotationOfField(Timesheet::class.java, "employee.mainAddress.street", MyAnnotation::class.java)!!.name, "Annotation of class Address expected.")
    }

    internal inner class Employee(name: String, mainAddress: Address, addresses: List<Address>) : Person(name, mainAddress, addresses)

    @MyAnnotation(name = "person")
    internal open inner class Person(var name: String, var mainAddress: Address, var addresses: List<Address>)

    @MyAnnotation(name = "address")
    internal inner class Address(var street: String)

    internal inner class Timesheet(var employee: Employee)

    internal annotation class MyAnnotation(val name: String)
}
