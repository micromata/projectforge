/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.caldav.controller

import io.milton.annotations.*
import mu.KotlinLogging
import org.projectforge.caldav.model.*
import org.projectforge.caldav.service.AddressService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

// curl -A "CoreDAV" --user "kai:xxxx" -H "Content-Type: text/xml" -H "Depth: 1" --data "<propfind xmlns='DAV:'><prop><address-data xmlns='urn:ietf:params:xml:ns:carddav'/></prop></propfind>" -X PROPFIND "http://localhost:8080/users/kai/addressBooks/default"

private val log = KotlinLogging.logger {}

@ResourceController
open class ProjectForgeCardDAVController : BaseDAVController() {
    @Autowired
    private lateinit var addressService_: AddressService

    private val addressService: AddressService
        get() {
            ensureAutowire()
            return addressService_
        }

    @get:Root
    val root: ProjectForgeCardDAVController
        get() = this

    @ChildrenOf
    fun getUsersHome(root: ProjectForgeCardDAVController?): UsersHome {
        if (usersHome == null) {
            log.info("Create new UsersHome")
            usersHome = UsersHome()
        }
        return usersHome!!
    }

    @ChildrenOf
    fun getContactsHome(user: User): ContactsHome {
        log.info("Creating ContactsHome for user:$user")
        return ContactsHome(user)
    }

    @ChildrenOf
    @AddressBooks
    fun getAddressBook(cons: ContactsHome): AddressBook {
        log.info("getAddressBook: '${cons.name}' for user '${cons.user.username}'.")
        return AddressBook(cons.user)
    }

    @ChildrenOf
    fun getContacts(ab: AddressBook): List<Contact> {
        log.info("getContacts for address book '${ab.name}' and user '${ab.user.username}'.")
        return addressService.getContactList(ab)
    }

    @Get
    @ContactData
    fun getContactData(c: Contact): ByteArray? {
        if (log.isDebugEnabled) {
            log.debug("getContactData: '${c.name}' with id ${c.id}.")
        }
        return c.vcardData
    }

    @PutChild
    fun createContact(ab: AddressBook, vcardBytearray: ByteArray, newName: String): Contact {
        log.info("CreateContact: $newName")
        return addressService.createContact(ab, vcardBytearray)
    }

    @PutChild
    fun updateContact(c: Contact, vcardBytearray: ByteArray?): Contact {
        log.info("updateContact: '${c.name}' with id ${c.id}.")
        return addressService.updateContact(c, vcardBytearray)
    }

    @Delete
    fun deleteContact(c: Contact) {
        log.info("deleteContact: '${c.name}' with id ${c.id}.")
        addressService.deleteContact(c)
    }

    init {
        log.info("init")
    }
}
