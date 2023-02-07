/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import org.junit.jupiter.api.Test


class SipgateContactTest {
  @Test
  fun getCompareTest() {
    Assertions.assertTrue(SipgateNumber.compare(null, null))
    Assertions.assertTrue(SipgateNumber.compare(arrayOf(), arrayOf()))
    Assertions.assertTrue(SipgateNumber.compare(arrayOf("work"), arrayOf("work")))
    Assertions.assertTrue(SipgateNumber.compare(arrayOf("work", "cell"), arrayOf("work", "cell")))
    Assertions.assertTrue(SipgateNumber.compare(arrayOf("cell", "work"), arrayOf("work", "cell")))

    Assertions.assertFalse(SipgateNumber.compare(null, arrayOf()))
    Assertions.assertFalse(SipgateNumber.compare(arrayOf(), null))
    Assertions.assertFalse(SipgateNumber.compare(arrayOf(), arrayOf("")))
    Assertions.assertFalse(SipgateNumber.compare(arrayOf("work"), arrayOf("home")))
    Assertions.assertFalse(SipgateNumber.compare(arrayOf("work", "cell"), arrayOf("home", "cell")))

    Assertions.assertTrue(SipgateNumber().setHomeType().isHomeType())
    Assertions.assertTrue(SipgateNumber().setWorkType().isWorkType())
    Assertions.assertTrue(SipgateNumber().setCellType().isCellType())
    Assertions.assertTrue(SipgateNumber().setFaxWorkType().isFaxWorkType())
    Assertions.assertTrue(SipgateNumber().setFaxHomeType().isFaxHomeType())
    Assertions.assertTrue(SipgateNumber().setOtherType().isOtherType())
    Assertions.assertTrue(SipgateNumber().setPagerType().isPagerType())
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
    contact.work = "#work"
    Assertions.assertEquals(1, contact.numbers?.size)
    Assertions.assertEquals("#work", contact.work)
    contact.home = "#home"
    Assertions.assertEquals(2, contact.numbers?.size)
    Assertions.assertEquals("#home", contact.home)
    contact.cell = "#cell"
    Assertions.assertEquals(3, contact.numbers?.size)
    Assertions.assertEquals("#cell", contact.cell)
    contact.other = "#other"
    Assertions.assertEquals(4, contact.numbers?.size)
    Assertions.assertEquals("#other", contact.other)
    contact.faxWork = "#faxWork"
    Assertions.assertEquals(5, contact.numbers?.size)
    Assertions.assertEquals("#faxWork", contact.faxWork)
    contact.faxHome = "#faxHome"
    Assertions.assertEquals(6, contact.numbers?.size)
    Assertions.assertEquals("#faxHome", contact.faxHome)
    contact.pager = "#pager"
    Assertions.assertEquals(7, contact.numbers?.size)
    Assertions.assertEquals("#pager", contact.pager)
    Assertions.assertEquals("#work", contact.work)
  }
}
