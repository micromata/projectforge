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
  sipgateClient.postConstruct()

  val addressService = SipgateAddressService()
  addressService.sipgateClient = sipgateClient
  addressService.debugConsoleOutForTesting = true
  addressService.postConstruct()

  val contactService = SipgateContactService()
  contactService.sipgateClient = sipgateClient
  contactService.debugConsoleOutForTesting = true
  contactService.postConstruct()

  val list = addressService.getList()
  println("#${list.size} addresses.")

  var contacts = contactService.getList()
  println("#${contacts.size} contacts.")

  var contact = SipgateContact()
  // IDs given by Sipgate! contact.id = "077EE02A-9AEF-11ED-BFEA-BEA196FC4242"
  contact.name = "Hurzel Meier"
  contact.scope = SipgateContact.Scope.SHARED
  contact.organization = "Micromata GmbH"
  contact.division = "DevOps"
  contact.emails = arrayOf(SipgateEmail("kai@acme.com", type = SipgateContact.EmailType.HOME))
  contactService.create(contact)
  contacts = contactService.getList()
  contact = contacts.find { it.name == "Hurzel Meier" }!!
  Assertions.assertEquals(SipgateContact.Scope.SHARED, contact.scope)
  Assertions.assertEquals("Micromata GmbH", contact.organization)
  Assertions.assertEquals("DevOps", contact.division)
  Assertions.assertEquals("kai@acme.com", contact.emails?.firstOrNull()?.email)

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
  Assertions.assertEquals(size -1, contacts.size)
  println("#${contacts.size} contacts.")
}
