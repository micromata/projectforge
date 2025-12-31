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

package org.projectforge.business.sipgate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.utils.NumberHelper


class SipgateContactTest {
  @Test
  fun getCompareTest() {
    Assertions.assertTrue(SipgateContact.Number.compare(null, null))
    Assertions.assertTrue(SipgateContact.Number.compare(arrayOf(), arrayOf()))
    Assertions.assertTrue(SipgateContact.Number.compare(arrayOf("work"), arrayOf("work")))
    Assertions.assertTrue(SipgateContact.Number.compare(arrayOf("work", "cell"), arrayOf("work", "cell")))
    Assertions.assertTrue(SipgateContact.Number.compare(arrayOf("cell", "work"), arrayOf("work", "cell")))

    Assertions.assertFalse(SipgateContact.Number.compare(null, arrayOf()))
    Assertions.assertFalse(SipgateContact.Number.compare(arrayOf(), null))
    Assertions.assertFalse(SipgateContact.Number.compare(arrayOf(), arrayOf("")))
    Assertions.assertFalse(SipgateContact.Number.compare(arrayOf("work"), arrayOf("home")))
    Assertions.assertFalse(SipgateContact.Number.compare(arrayOf("work", "cell"), arrayOf("home", "cell")))

    Assertions.assertTrue(SipgateContact.Number().setHomeType().isHomeType())
    Assertions.assertTrue(SipgateContact.Number().setWorkType().isWorkType())
    Assertions.assertTrue(SipgateContact.Number().setCellType().isCellType())
    Assertions.assertTrue(SipgateContact.Number().setCellHomeType().isCellHomeType())
    Assertions.assertTrue(SipgateContact.Number().setFaxWorkType().isFaxWorkType())
    Assertions.assertTrue(SipgateContact.Number().setFaxHomeType().isFaxHomeType())
    Assertions.assertTrue(SipgateContact.Number().setOtherType().isOtherType())
    Assertions.assertTrue(SipgateContact.Number().setPagerType().isPagerType())
  }

  @Test
  fun emailTest() {
    val contact = SipgateContact()
    Assertions.assertNull(contact.email)
    contact.email = "test@ace.com"
    Assertions.assertEquals(1, contact.emails?.size)
    Assertions.assertEquals("test@ace.com", contact.email)
    contact.email = "business@ace.com"
    Assertions.assertEquals(1, contact.emails?.size)
    Assertions.assertEquals("business@ace.com", contact.email)
    contact.privateEmail = "private@ace.com"
    Assertions.assertEquals(2, contact.emails?.size)
    Assertions.assertEquals("business@ace.com", contact.email)
    Assertions.assertEquals("private@ace.com", contact.privateEmail)
  }

  @Test
  fun numberTest() {
    val contact = SipgateContact()
    Assertions.assertNull(contact.work)
    contact.work = "1111"
    Assertions.assertEquals(1, contact.numbers?.size)
    Assertions.assertEquals("1111", contact.work)
    contact.home = "2222"
    Assertions.assertEquals(2, contact.numbers?.size)
    Assertions.assertEquals("2222", contact.home)
    contact.cell = "3333"
    Assertions.assertEquals(3, contact.numbers?.size)
    Assertions.assertEquals("3333", contact.cell)
    contact.other = "5555"
    Assertions.assertEquals(4, contact.numbers?.size)
    Assertions.assertEquals("5555", contact.other)
    contact.faxWork = "6666"
    Assertions.assertEquals(5, contact.numbers?.size)
    Assertions.assertEquals("6666", contact.faxWork)
    contact.faxHome = "7777"
    Assertions.assertEquals(6, contact.numbers?.size)
    Assertions.assertEquals("7777", contact.faxHome)
    contact.pager = "8888"
    Assertions.assertEquals(7, contact.numbers?.size)
    Assertions.assertEquals("8888", contact.pager)
    Assertions.assertEquals("1111", contact.work)
    contact.cellHome = "9999"
    Assertions.assertEquals(8, contact.numbers?.size)
    Assertions.assertEquals("9999", contact.cellHome)
  }

  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      NumberHelper.TEST_COUNTRY_PREFIX_USAGE_IN_TESTCASES_ONLY = "+49"
    }
  }
}
