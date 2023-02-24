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
import org.projectforge.business.address.AddressDO
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.framework.configuration.ConfigXmlTest
import java.util.*
import kotlin.io.path.Path


fun main(args: Array<String>) {
  // From documentation:
  // println("{    \"contactType\": {        \"value\": \"COMPANY\"    },    \"organization\": {        \"id\": \"\"    },    \"company\": \"d.velop AG\",    \"type\": {        \"value\": \"PARTNER\"    },    \"number\": \"0004\",    \"street\": \"Schildarpstraße 6\",    \"zip\": \"48712\",    \"city\": \"Gescher\",    \"country\": \"Deutschland\",    \"addressAdditional\": \"d.velop Campus Gebäude 3\",    \"website\": \"www.d-velop.de\",    \"active\": {        \"value\": \"TRUE\"    }}")
  val props = Properties()
  val path = Path(System.getProperty("user.home"), "ProjectForge", "projectforge.properties")
  props.load(path.toFile().bufferedReader())
  val baseUri = props.getProperty("projectforge.sipgate.baseUri")
  val tokenId = props.getProperty("projectforge.sipgate.tokenId")
  val token = props.getProperty("projectforge.sipgate.token")
  if (baseUri.isNullOrBlank() || tokenId.isNullOrBlank() || token.isNullOrBlank()) {
    println("projectforge.sipgate.baseUri and/or projectforge.sipgate.token/id is not given in file: ${path.toFile().absolutePath}")
    System.exit(0)
  }
  val sipgateClient = SipgateClient()
  val config = SipgateConfiguration()
  config.baseUri = baseUri
  config.tokenId = tokenId
  config.token = token
  sipgateClient.sipgateConfiguration = config
  sipgateClient.postConstruct(useMenuCreator = false)
  readLists(sipgateClient)
  // contactService(sipgateClient)
  // getLists(sipgateClient)
  // contactTest(sipgateClient)
}

private fun readLists(sipgateClient: SipgateClient) {
  val sipgateService = SipgateService()
  sipgateService.sipgateClient = sipgateClient
  sipgateService.postConstruct()
  val sipgateSyncService = SipgateSyncService()
  sipgateSyncService.sipgateService = sipgateService
  ConfigXmlTest.createTestConfiguration().workingDirectory = "/tmp"
  println(sipgateSyncService.storage)
}

private fun getLists(sipgateClient: SipgateClient) {
  val sipgateService = SipgateService()
  sipgateService.sipgateClient = sipgateClient
  sipgateService.postConstruct()
  /*val logs = sipgateService.getLogs()
  println("Logs: [${logs?.joinToString { it.toString() }}]")
  val users = sipgateService.getUsers()
  println("Users: [${users?.joinToString { it.toString() }}]")
  users?.find { it.firstname == "Kai" && it.lastname == "Reinhard" }?.let { user ->
    val devices = sipgateService.getDevices(user)
    println("Devices: [${devices?.joinToString { it.toString() }}]")
  }
  val numbers = sipgateService.getNumbers()
  println("Numbers: [${numbers?.joinToString { it.toString() }}]")
   */
  // sipgateService.initCall(deviceId = "x1", callerId = "p1", callee = "01234567", caller = "p1")
  // sipgateService.sendSms(smsId = "s0", message = "Test sipgate", recipient = "012345")
}

private fun contactTest(sipgateClient: SipgateClient) {
  val contactService = SipgateContactService()
  contactService.sipgateClient = sipgateClient
  contactService.debugConsoleOutForTesting = true
  contactService.postConstruct()

  val contact = SipgateContact()
  contact.family = "Hurzelfamily"
  contact.given = "Hurzelfirst"
  contact.scope = SipgateContact.Scope.SHARED
  contact.numbers = mutableListOf(SipgateContact.Number("0111111").setCellHomeType())

  contactService.create(contact)
}

private fun contactService(sipgateClient: SipgateClient) {
  val contactService = SipgateContactService()
  contactService.sipgateClient = sipgateClient
  contactService.debugConsoleOutForTesting = true
  contactService.postConstruct()

  var contacts = contactService.getList()
  println("#${contacts.size} contacts.")

  val addressDO = AddressDO()
  addressDO.name = "Meier"
  addressDO.firstName = "Hurzel"
  addressDO.organization = "Micromata GmbH"
  addressDO.division = "DevOps"
  addressDO.privateEmail = "kai@acme.com"
  addressDO.email = "business@acme.com"
  addressDO.addressText = "Marie-Calm-Str. 1-5"
  addressDO.zipCode = "34131"
  addressDO.city = "Kassel"
  addressDO.businessPhone = "0123"
  addressDO.privatePhone = "1234"
  addressDO.mobilePhone = "2345"
  addressDO.fax = "3456"

  contactService.create(SipgateContactSyncService.from(addressDO))
  contacts = contactService.getList()
  var contact = contacts.find { it.name == "Hurzel Meier" }!!
  Assertions.assertEquals(SipgateContact.Scope.SHARED, contact.scope)
  Assertions.assertEquals("Micromata GmbH", contact.organization)
  Assertions.assertEquals("DevOps", contact.division)
  val emails = contact.emails
  Assertions.assertEquals(2, emails!!.size)
  Assertions.assertEquals("kai@acme.com", emails.find { it.type == SipgateContact.EmailType.HOME }!!.email)
  Assertions.assertEquals("business@acme.com", emails.find { it.type == SipgateContact.EmailType.WORK }!!.email)
  Assertions.assertEquals(1, contact.addresses!!.size)
  val address = contact.addresses!!.first()
  Assertions.assertEquals("Marie-Calm-Str. 1-5", address.streetAddress)
  Assertions.assertEquals("34131", address.postalCode)
  Assertions.assertEquals("Kassel", address.locality)


  println("#${contacts.size} contacts.")
  val size = contacts.size

  contact.division = "Sysop"
  contact.emails?.firstOrNull()?.email = "reinhard@acme.com"
  Assertions.assertTrue(contactService.update(contact.id!!, contact))
  contact.division = null
  contact.emails = null
  contacts = contactService.getList()
  contact = contacts.find { it.name == "Hurzel Meier" }!!

  Assertions.assertEquals("Sysop", contact.division)
  Assertions.assertEquals("reinhard@acme.com", contact.emails?.firstOrNull()?.email)

  contactService.delete(contact.id, contact)
  contacts = contactService.getList()
  Assertions.assertEquals(size - 1, contacts.size)
  println("#${contacts.size} contacts.")
}
