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

package org.projectforge.framework.persistence.api.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DBPredicateTest {
    class Company(val title: String, val ceo: Person, val admins: List<Person>, val developers: Array<Person>)
    class Person(val name: String, val address: Address? = null)
    class Address(val city: String)

    @Test
    fun matchTest() {
        val pred = DBPredicate.Equal("name", "Dave")
        Assertions.assertTrue(pred.match(Person("Dave")))
        Assertions.assertFalse(pred.match(Person("Not Dave")))

        val admins = listOf(Person("Sheila", Address("Kassel")), Person("Patrick", Address("Bonn")), Person("Bob"))
        val developers = arrayOf(Person("Dave"), Person("Betty"))
        val company = Company("ACME", Person("Amy"), admins, developers)
        Assertions.assertTrue(DBPredicate.Equal("ceo.name", "Amy").match(company))
        Assertions.assertFalse(DBPredicate.Equal("ceo.name", "Dave").match(company))

        Assertions.assertTrue(DBPredicate.Equal("developers.name", "Dave").match(company))
        Assertions.assertFalse(DBPredicate.Equal("developers.name", "Sheila").match(company))

        Assertions.assertTrue(DBPredicate.Equal("admins.name", "Sheila").match(company))
        Assertions.assertFalse(DBPredicate.Equal("admins.name", "Dave").match(company))

        Assertions.assertTrue(DBPredicate.Equal("admins.address.city", "Kassel").match(company))
        Assertions.assertFalse(DBPredicate.Equal("developers.address.city", "Kassel").match(company))

        Assertions.assertTrue(DBPredicate.NotEqual("admins.address.city", "Hamburg").match(company))
        // Not all cities are Kassel (only one):
        Assertions.assertTrue(DBPredicate.NotEqual("admins.address.city", "Kassel").match(company))

        Assertions.assertTrue(DBPredicate.IsNotNull("admins.address.city").match(company))
        Assertions.assertFalse(DBPredicate.IsNotNull("developers.address.city").match(company))

        Assertions.assertTrue(DBPredicate.IsIn("admins.address.city", "Hamburg", "Kassel").match(company))
        Assertions.assertFalse(DBPredicate.IsIn("admins.address.city", "Hamburg", "Berlin").match(company))

        Assertions.assertTrue(DBPredicate.Like("admins.name", "*hei*").match(company))
        Assertions.assertTrue(DBPredicate.Like("admins.name", "shei*").match(company))
        Assertions.assertTrue(DBPredicate.Like("admins.name", "*eilA").match(company))
        Assertions.assertFalse(DBPredicate.Like("admins.name", "hei*").match(company))

        Assertions.assertTrue(DBPredicate.And(
                DBPredicate.IsIn("admins.address.city", "Hamburg", "Kassel"),
                DBPredicate.Like("admins.name", "*hei*")).match(company))

        Assertions.assertFalse(DBPredicate.And(
                DBPredicate.IsIn("admins.address.city", "Hamburg", "Kassel"),
                DBPredicate.Like("admins.name", "*unknown*")).match(company))
    }

    @Test
    fun likeTest() {
        Assertions.assertEquals("", DBPredicate.Like("name", "").queryString)
        Assertions.assertEquals("%", DBPredicate.Like("name", "*").queryString)
        Assertions.assertEquals("%%", DBPredicate.Like("name", "**").queryString)
        Assertions.assertEquals("%%", DBPredicate.Like("name", "*%").queryString)
        Assertions.assertEquals("%%", DBPredicate.Like("name", "%*").queryString)
        Assertions.assertEquals("test%", DBPredicate.Like("name", "test*").queryString)
        Assertions.assertEquals("%test%", DBPredicate.Like("name", "*test*").queryString)
        Assertions.assertEquals("%test", DBPredicate.Like("name", "*test").queryString)
    }

    @Test
    fun multiFieldFulltextQueryRequiredTest() {
        testMultiField("dfladksj", false)
        testMultiField("dfladksj* dfakl+", false)
        testMultiField("name:rein*", true)
        testMultiField("dfladksj name:rein*", true)
    }

    private fun testMultiField(value: String, expectedResult: Boolean) {
        Assertions.assertEquals(expectedResult, DBPredicate.FullSearch(value).multiFieldFulltextQueryRequired())
    }
}
