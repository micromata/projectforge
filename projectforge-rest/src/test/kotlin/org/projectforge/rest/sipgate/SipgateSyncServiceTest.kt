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
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.business.sipgate.SipgateContactSyncDO
import org.projectforge.business.sipgate.SipgateNumber
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.utils.NumberHelper


class SipgateSyncServiceTest {
  @Test
  fun getNameTest() {
    Assertions.assertEquals("", SipgateContactSyncDO.getName(AddressDO()))
    Assertions.assertEquals("Reinhard", SipgateContactSyncDO.getName(createAddress(name = "Reinhard")))
    Assertions.assertEquals("Kai", SipgateContactSyncDO.getName(createAddress(firstName = "Kai")))
    Assertions.assertEquals(
      "Kai Reinhard",
      SipgateContactSyncDO.getName(createAddress(firstName = "Kai Reinhard"))
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
    address1.addressStatus = AddressStatus.UPTODATE
    address1.contactStatus = ContactStatus.ACTIVE
    val contact = SipgateContact()
    contact.name = SipgateContactSyncDO.getName(address1)
    Assertions.assertEquals(3, SipgateContactSyncService.matchScore(contact, address1))
    address1.contactStatus = ContactStatus.DEPARTED
    address1.addressStatus = AddressStatus.LEAVED
    Assertions.assertEquals(2, SipgateContactSyncService.matchScore(contact, address1))
    address1.isDeleted = true
    Assertions.assertEquals(1, SipgateContactSyncService.matchScore(contact, address1))
    val address2 = createAddress(
      name = "Reinhard",
      firstName = "Kai",
      mobilePhone = "+49 1234 567890",
      fax = "+49 222 33333",
    )
    contact.numbers = mutableListOf(SipgateNumber("+49 1234 567890"))
    Assertions.assertEquals(4, SipgateContactSyncService.matchScore(contact, address2))
    contact.numbers = mutableListOf(SipgateNumber("+49 1234 567890"), SipgateNumber("022233333"))
    Assertions.assertEquals(5, SipgateContactSyncService.matchScore(contact, address2))
    address1.firstName = "Karl"
    Assertions.assertEquals(-1, SipgateContactSyncService.matchScore(contact, address1))

    val addressList = listOf(
      createAddress(name = "Reinhard", firstName = "Kai", mobilePhone = "+49 23456789", id = 10),
      createAddress(name = "Reinhard", firstName = "Kai", mobilePhone = "+49 1234 56789", id = 11),
      createAddress(name = "Reinhard", firstName = "Kai", mobilePhone = "+49 987654321", id = 12), // synced
      createAddress(name = "Reinhard", firstName = "Karl", fax = "+49 23456789", id = 20),
      createAddress(name = "Reinhard", firstName = "Karl", fax = "+49 238328", id = 21),
    )
    val contactList = mutableListOf(
      createContact("Reinhard", "Kai", "0123456789", id = "contact-10"),
      createContact("Reinhard", "Kai", id = "contact-11"),
      createContact("Reinhard", "Karl", id = "contact-20"), // synced
      createContact("Reinhard", "Karl", id = "contact-21"),
    )
    val syncList = mutableListOf(
      createSyncDO("other-contact-id", 12),
      createSyncDO("contact-20", 42),
    )
    val syncContext = SipgateContactSyncService.SyncContext()
    syncContext.syncDOList = syncList
    syncContext.addressList = addressList
    syncContext.remoteContacts = contactList
    val list = SipgateContactSyncService.findMatches(syncContext)
    Assertions.assertTrue(
      list.none { it.contactId == "contact-20" },
      "contact-20 without any match (is already synced)"
    )
    assertScore(list, "contact-10", 10, 3, "only name is matching")
    assertScore(list, "contact-10", 11, 4, "cell phone is matching")
    assertScore(list, "contact-11", 10, 3, "only name is matching")
    assertScore(list, "contact-11", 11, 3, "only name is matching")
    Assertions.assertTrue(
      list.none { it.contactId == "contact-20" },
      "contact-20 without any match (is already synced)"
    )
    assertScore(list, "contact-21", 20, 3, "only name is matching")
    assertScore(list, "contact-21", 21, 3, "only name is matching")
    Assertions.assertEquals(6, list.size)
  }

  private fun assertScore(
    matchScores: List<SipgateContactSyncService.MatchScore>,
    contactId: String,
    addressId: Int,
    expectedScore: Int,
    msg: String,
  ) {
    val score = matchScores.find { it.contactId == contactId && it.addressId == addressId }
    Assertions.assertNotNull(score, msg)
    Assertions.assertNotNull(score!!.score, msg)
    Assertions.assertEquals(expectedScore, score.score, msg)
  }


  @Test
  fun syncTest() {
    syncTest("mail", "mail", "mail", false, false)
    syncTest("mail", "mail2", "mail", true, false)
    syncTest("mail", "mail", "mail2", false, true)
    syncTest("mail", "mail", "mail2", false, true)

    var address = createAddress("Lastname", "Firstname", "+49 11111 1111", "+49 222222222", "Micromata GmbH", 2)
    address.email = "f.lastname@devnull.com"
    address.privateEmail = "f.lastname@google.com"

    var contact = SipgateContactSyncService.from(address)
    contact.id = "abcdef-ghijkl-1234"

    var syncDO = SipgateContactSyncDO()
    syncDO.sipgateContactId = contact.id
    syncDO.address = address
    syncDO.updateJson(contact) // Store remote fields as last sync state for modification detection.

    contact.privateEmail = "f.lastname@yahoo.com" // Modification on remote.
    contact.faxWork = "04444444"
    address.businessPhone = "+49 3333 33333"
    address.privateMobilePhone = "+49 5555555"
    address.email = "business@devnull.com"
    NumberHelper.TEST_COUNTRY_PREFIX_USAGE_IN_TESTCASES_ONLY = "+49"
    var result = SipgateContactSyncService.sync(contact, address, syncDO.syncInfo)
    Assertions.assertTrue(result.contactOutdated)
    Assertions.assertTrue(result.addressDOOutdated)
    assertEquals("business@devnull.com", contact.email, address.email)
    assertEquals("f.lastname@yahoo.com", contact.privateEmail, address.privateEmail)
    assertEquals("+49 11111 1111", contact.cell, address.mobilePhone)
    assertEquals("+49 4444444", contact.faxWork, address.fax)
    assertEquals("+49 3333 33333", contact.work, address.businessPhone)
    assertEquals("+49 5555555", contact.other, address.privateMobilePhone)

    address = createAddress("Lastname", "Firstname", "+49 11111 1111", "+49 222222222", organization ="Micromata GmbH", 2)
    address.businessPhone = "02222222222222"
    contact = SipgateContactSyncService.from(address)
    contact.id = "12345678"
    syncDO = SipgateContactSyncDO()
    syncDO.sipgateContactId = contact.id
    syncDO.address = address
    syncDO.updateJson(contact) // Store remote fields as last sync state for modification detection.

    val json = """{"id":"59519076","name":"Firstname Lastname","emails":[{"email":"f.lastname@yahoo.de","type":["work"]},{"email":"f.lastname@google.com","type":["home"]}],"numbers":[{"number":"+49888888","type":["other"]},{"number":"+49111111111","type":["cell"]},{"number":"+49222222222","type":["work"]},{"number":"+555555","type":["home"]}],"addresses":[],"scope":"SHARED","organization":[["Micromata GmbH"]]}"""
    contact = JsonUtils.fromJson(json, SipgateContact::class.java)!!
    println(contact)
    result = SipgateContactSyncService.sync(contact, address, syncDO.syncInfo)
    println(result)
    Assertions.assertFalse(result.contactOutdated)
    Assertions.assertTrue(result.addressDOOutdated)
    assertEquals("+49 11111 1111", contact.cell, address.mobilePhone)
    assertEquals("+49 222222222", contact.faxWork, address.fax)
    assertEquals("02222222222222", contact.work, address.businessPhone)
    assertEquals("+49 8888888888888", contact.home, address.privatePhone)
  }

  private fun assertEquals(expected: String, str1: String?, str2: String?) {
    Assertions.assertEquals(expected, str1)
    Assertions.assertEquals(expected, str2)
  }

  private fun syncTest(
    lastSyncEmail: String?,
    contactEmail: String?,
    addressEmail: String?,
    expectedAddressOutdated: Boolean,
    expectedContactOutdated: Boolean,
  ) {
    val syncInfo = SipgateContactSyncDO.SyncInfo()
    val contact = SipgateContact()
    val address = AddressDO()
    address.email = addressEmail
    val result = SipgateContactSyncService.SyncResult()
    syncInfo.setFieldsInfo(AddressDO::email.name, lastSyncEmail)
    contact.email = contactEmail
    SipgateContactSyncService.sync(contact, SipgateContact::email, address, AddressDO::email, syncInfo, result)
    Assertions.assertEquals(expectedContactOutdated, result.contactOutdated)
    Assertions.assertEquals(expectedAddressOutdated, result.addressDOOutdated)
    Assertions.assertEquals(contact.email, address.email)
  }

  private fun assertName(expectedFirstName: String?, expectedName: String?, fullname: String?) {
    val address = SipgateContactSyncService.extractName(fullname)
    Assertions.assertEquals(expectedFirstName, address.firstName)
    Assertions.assertEquals(expectedName, address.name)
  }

  companion object {
    internal fun createAddress(
      name: String? = null,
      firstName: String? = null,
      mobilePhone: String? = null,
      fax: String? = null,
      organization: String? = null,
      id: Int? = null,
    ): AddressDO {
      val address = AddressDO()
      address.name = name
      address.firstName = firstName
      address.mobilePhone = mobilePhone
      address.fax = fax
      address.id = id
      address.organization = organization
      return address
    }

    internal fun createContact(
      name: String,
      firstName: String,
      mobilePhone: String? = null,
      fax: String? = null,
      organization: String? = null,
      id: String? = null,
    ): SipgateContact {
      val address = createAddress(name = name, firstName = firstName)
      val contact = SipgateContact()
      contact.name = SipgateContactSyncDO.getName(address)
      val numbers = mutableListOf<SipgateNumber>()
      mobilePhone?.let {
        numbers.add(SipgateNumber(it).setCellType())
      }
      fax?.let {
        numbers.add(SipgateNumber(it).setFaxWorkType())
      }
      contact.numbers = numbers
      contact.organization = organization
      contact.id = id
      contact.scope = SipgateContact.Scope.SHARED
      return contact
    }

    internal fun createSyncDO(
      contactId: String,
      addressId: Int,
    ): SipgateContactSyncDO {
      val syncDO = SipgateContactSyncDO()
      syncDO.sipgateContactId = contactId
      val address = AddressDO()
      address.id = addressId
      syncDO.address = address
      return syncDO
    }
  }
}
