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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.Addressbook
import org.springframework.beans.factory.annotation.Autowired

class AddressbookEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var addressbookEntityRest: AddressbookEntityRest

    @Autowired
    private lateinit var userService: UserService

    /** A new address book belongs to the logged-in user. */
    @Test
    fun `a new address book is owned by the logged-in user`() {
        val user = logon(TEST_USER)
        val dto = addressbookEntityRest.newBaseDTO(null)
        assertEquals(user.id, dto.owner?.id)
    }

    /** The access lists travel as users and groups and come back as the csv lists of ids of the DO. */
    @Test
    fun `access lists survive the round trip through the form`() {
        val owner = logon(TEST_USER)
        val other = getUser(TEST_USER2)
        val group = getGroup(TEST_GROUP)
        val abId = addressbookDao.insert(AddressbookDO().also {
            it.title = "Round trip test"
            it.owner = owner
            it.fullAccessUserIds = "${other.id}"
            it.readonlyAccessGroupIds = "${group.id}"
        })
        val dto = addressbookEntityRest.transformFromDB(addressbookDao.find(abId)!!, editMode = true)
        assertEquals(listOf(other.id), dto.fullAccessUsers?.map { it.id })
        assertNotNull(dto.fullAccessUsers?.single()?.displayName)
        assertEquals(listOf(group.id), dto.readonlyAccessGroups?.map { it.id })
        assertNotNull(dto.readonlyAccessGroups?.single()?.displayName)
        assertNotNull(dto.owner?.displayName)
        val saved = addressbookEntityRest.transformForDB(dto)
        assertEquals("${other.id}", saved.fullAccessUserIds)
        assertEquals("${group.id}", saved.readonlyAccessGroupIds)
        assertEquals(owner.id, saved.owner?.id)
    }

    /** Users with full access to the addresses of an address book may still not save it: only its owner may. */
    @Test
    fun `the form offers save and delete to the owner only`() {
        val owner = logon(TEST_USER)
        val fullAccessUser = getUser(TEST_USER2)
        val abId = addressbookDao.insert(AddressbookDO().also {
            it.title = "Write access test"
            it.owner = owner
            it.fullAccessUserIds = "${fullAccessUser.id}"
        })
        (addressbookEntityRest.getItem(abId).body as Addressbook).let {
            assertEquals(true, it.writeAccess)
            assertEquals(true, it.deleteAccess)
        }
        logon(fullAccessUser)
        (addressbookEntityRest.getItem(abId).body as Addressbook).let {
            assertEquals(false, it.writeAccess)
            assertEquals(false, it.deleteAccess)
        }
    }

    /** A deactivated user is struck through in the access lists, so the form must learn it, also as a non-admin. */
    @Test
    fun `access lists tell a deactivated user from an active one`() {
        logon(TEST_ADMIN_USER)
        val deactivatedId = userService.insert(PFUserDO().also {
            it.username = "AddressbookEntityRestTest-deactivated"
            it.deactivated = true
        }, false)
        val owner = logon(TEST_USER)
        val active = getUser(TEST_USER2)
        val abId = addressbookDao.insert(AddressbookDO().also {
            it.title = "Deactivated user test"
            it.owner = owner
            it.fullAccessUserIds = "${active.id},$deactivatedId"
        })
        val dto = addressbookEntityRest.transformFromDB(addressbookDao.find(abId)!!, editMode = true)
        val users = dto.fullAccessUsers!!.associateBy { it.id }
        assertTrue(users[deactivatedId]!!.deactivated)
        assertFalse(users[active.id]!!.deactivated)
        assertFalse(dto.owner!!.deactivated)
    }
}
