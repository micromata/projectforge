/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.projectforge.caldav.controller

import io.milton.annotations.*
import org.projectforge.caldav.model.*
import org.projectforge.caldav.service.AddressService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

open class ProjectForgeCarddavController : BaseDAVController() {
    @Autowired
    private lateinit var addressService: AddressService

    @get:Root
    val root: ProjectForgeCarddavController
        get() = this

    @ChildrenOf
    fun getUsersHome(root: ProjectForgeCarddavController?): UsersHome {
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
        return AddressBook(cons.user)
    }

    @ChildrenOf
    fun getContact(ab: AddressBook): List<Contact> {
        return addressService.getContactList(ab)
    }

    @Get
    @ContactData
    fun getContactData(c: Contact): ByteArray? {
        return c.vcardData
    }

    @PutChild
    fun createContact(ab: AddressBook?, vcardBytearray: ByteArray?, newName: String): Contact {
        log.info("CreateContact: $newName")
        return addressService.createContact(ab, vcardBytearray)
    }

    @PutChild
    fun updateContact(c: Contact, vcardBytearray: ByteArray?): Contact {
        var c = c
        log.info("updateContact: " + c.name)
        c = addressService.updateContact(c, vcardBytearray)
        return c
    }

    @Delete
    fun deleteContact(c: Contact) {
        log.info("deleteContact: " + c.name)
        addressService.deleteContact(c)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProjectForgeCarddavController::class.java)
    }

    init {
        log.info("init")
    }
}
