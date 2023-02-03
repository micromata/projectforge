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

package org.projectforge.rest.sipgate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO


class SipgateSyncServiceTest {
  @Test
  fun getNameTest() {
    Assertions.assertEquals("", SipgateContactSyncService.getName(AddressDO()))
    Assertions.assertEquals("Reinhard", SipgateContactSyncService.getName(createAddress(name = "Reinhard")))
    Assertions.assertEquals("Kai", SipgateContactSyncService.getName(createAddress(firstName = "Kai")))
    Assertions.assertEquals(
      "Kai Reinhard",
      SipgateContactSyncService.getName(createAddress(firstName = "Kai Reinhard"))
    )

    assertName(null, null, null)
    assertName(null, null, "")
    assertName("", "Reinhard", "Reinhard")
    assertName("Kai", "Reinhard", "Kai Reinhard")
    assertName("Anna-Isabell", "Meier", "Anna-Isabell Meier")
    assertName("Anna Isabell", "Meier", "Anna Isabell Meier")
    assertName("Anna Isabell", "Meier-Müller", "Anna Isabell Meier-Müller")
  }

  @Test
  fun matchTest() {
    SipgateContactSyncService.countryPrefixForTestcases = "+49"
    val address1 = createAddress(name = "Reinhard", firstName = "Kai")
    val contact = SipgateContact()
    contact.name = SipgateContactSyncService.getName(address1)
    Assertions.assertEquals(1, SipgateContactSyncService.matchScore(contact, address1))
    val address2 = createAddress(
      name = "Reinhard",
      firstName = "Kai",
      mobilePhone = "+49 1234 567890",
      fax = "+49 222 33333",
    )
    contact.numbers = arrayOf(SipgateNumber("+49 1234 567890"))
    Assertions.assertEquals(2, SipgateContactSyncService.matchScore(contact, address2))
    contact.numbers = arrayOf(SipgateNumber("+49 1234 567890"), SipgateNumber("022233333"))
    Assertions.assertEquals(3, SipgateContactSyncService.matchScore(contact, address2))
    address1.firstName = "Karl"
    Assertions.assertEquals(-1, SipgateContactSyncService.matchScore(contact, address1))

    val contactList = mutableListOf(
      createContact(name = "Reinhard", firstName = "Kai", mobilePhone = "+49 23456789"),
      createContact(name = "Reinhard", firstName = "Kai", mobilePhone = "+49 1234 56789"),
      createContact(name = "Reinhard", firstName = "Karl", fax = "+49 23456789"),
    )
    val match = SipgateContactSyncService.findBestMatch(
      contactList,
      createAddress("Reinhard", "Kai", "Dipl.-Phys.", "0123456789")
    )
    Assertions.assertEquals("+49 1234 56789", match!!.numbers!!.first().number)
  }

  private fun assertName(expectedFirstName: String?, expectedName: String?, fullname: String?) {
    val address = SipgateContactSyncService.extractName(fullname)
    Assertions.assertEquals(expectedFirstName, address.firstName)
    Assertions.assertEquals(expectedName, address.name)
  }

  private fun createAddress(
    name: String? = null,
    firstName: String? = null,
    mobilePhone: String? = null,
    fax: String? = null,
  ): AddressDO {
    val address = AddressDO()
    address.name = name
    address.firstName = firstName
    address.mobilePhone = mobilePhone
    address.fax = fax
    return address
  }

  private fun createContact(
    name: String,
    firstName: String,
    mobilePhone: String? = null,
    fax: String? = null,
  ): SipgateContact {
    val address = createAddress(name = name, firstName = firstName)
    val contact = SipgateContact()
    contact.name = SipgateContactSyncService.getName(address)
    val numbers = mutableListOf<SipgateNumber>()
    mobilePhone?.let {
      numbers.add(SipgateNumber(it).setCell())
    }
    fax?.let {
      numbers.add(SipgateNumber(it).setFaxWork())
    }
    if (numbers.isNotEmpty()) {
      contact.numbers = numbers.toTypedArray()
    }
    return contact
  }
}
