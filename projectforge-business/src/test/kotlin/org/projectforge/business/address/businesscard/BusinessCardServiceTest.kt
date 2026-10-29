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

package org.projectforge.business.address.businesscard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.UserPrefDao
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

class BusinessCardServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var businessCardService: BusinessCardService

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    private fun createUser(name: String, email: String? = null): PFUserDO {
        val user = PFUserDO()
        user.firstname = "$name.firstname"
        user.lastname = "$name.lastname"
        user.email = email
        user.username = "${this.javaClass.simpleName}.$name.username"
        return initTestDB.addUser(user)
    }

    private fun card(name: String, id: String? = null) = BusinessCard().also {
        it.cardName = name
        it.id = id
        it.lastName = "Reinhard"
        it.mobilePhone = "0123"
    }

    @Test
    fun `a new card gets an id from the server, whatever was posted`() {
        logon(createUser("card-new"))
        val stored = businessCardService.save(card("With mobile"))
        assertEquals(36, stored.id!!.length) // A uuid.
        assertEquals(stored.id, businessCardService.get(stored.id)?.id)
        // Posting an unknown id doesn't create a card with that id:
        assertThrows<UserException> { businessCardService.save(card("Chosen", id = "chosen-by-client")) }
        assertEquals(1, businessCardService.list().size)
    }

    @Test
    fun `an update keeps the id and changes the fields`() {
        logon(createUser("card-update"))
        val stored = businessCardService.save(card("Update"))
        val updated = businessCardService.save(card("Updated", stored.id).also { it.mobilePhone = null })
        assertEquals(stored.id, updated.id)
        val list = businessCardService.list()
        assertEquals(1, list.size)
        assertEquals("Updated", list[0].cardName)
        assertNull(list[0].mobilePhone)
    }

    @Test
    fun `nobody reaches the cards of another user, admins included`() {
        val owner = createUser("card-private-owner")
        logon(owner)
        val stored = businessCardService.save(card("Private"))
        for (user in listOf(createUser("card-private-other"), getUser(TEST_ADMIN_USER))) {
            logon(user)
            assertTrue(businessCardService.list().none { it.id == stored.id })
            assertNull(businessCardService.get(stored.id))
            assertThrows<UserException> { businessCardService.save(card("Hijacked", stored.id)) }
            assertFalse(businessCardService.delete(stored.id))
        }
        logon(owner)
        assertEquals("Private", businessCardService.get(stored.id)?.cardName)
    }

    @Test
    fun `delete, name and limit`() {
        logon(createUser("card-limit"))
        assertThrows<UserException> { businessCardService.save(BusinessCard()) }
        val first = businessCardService.save(card("First"))
        repeat(BusinessCardService.MAX_CARDS_PER_USER - 1) { businessCardService.save(card("Card $it")) }
        assertThrows<UserException> { businessCardService.save(card("One too many")) }
        assertTrue(businessCardService.delete(first.id))
        assertFalse(businessCardService.delete(first.id))
        businessCardService.save(card("Fits again"))
    }

    @Test
    fun `the photo is only published from the own address`() {
        val user = createUser("card-photo", email = "card-photo@acme.test")
        logon(TEST_ADMIN_USER)
        val book = AddressbookDO().also { it.title = "card-photo book"; it.readonlyAccessUserIds = "${user.id}" }
        addressbookDao.insert(book, checkAccess = false)
        val own = AddressDO().also {
            it.firstName = user.firstname; it.name = user.lastname; it.email = "card-photo@acme.test"; it.add(book)
        }
        addressDao.insert(own, checkAccess = false)
        val foreign = AddressDO().also { it.firstName = "Colleague"; it.name = user.lastname; it.add(book) }
        // Same name as the user (a namesake, or a name changed in My account), but not the user's e-mail address:
        val namesake = AddressDO().also {
            it.firstName = user.firstname; it.name = user.lastname; it.email = "other@acme.test"; it.add(book)
        }
        addressDao.insert(namesake, checkAccess = false)
        addressDao.insert(foreign, checkAccess = false)
        // Own address, but not readable for the user (global address book only): no photo either.
        val unreadable = AddressDO().also {
            it.firstName = user.firstname; it.name = user.lastname; it.email = "card-photo@acme.test"
        }
        addressDao.insert(unreadable, checkAccess = false)
        logon(user)
        assertThrows<UserException> {
            businessCardService.save(card("Unreadable").also { it.addressId = unreadable.id; it.includePhoto = true })
        }
        businessCardService.save(card("Own").also { it.addressId = own.id; it.includePhoto = true })
        assertThrows<UserException> {
            businessCardService.save(card("Namesake").also { it.addressId = namesake.id; it.includePhoto = true })
        }
        // The suggestion finds the addresses of the user's name, the one with the user's e-mail address first:
        val suggested = businessCardService.suggestAddresses().map { it.id }
        assertEquals(own.id, suggested.first())
        assertTrue(namesake.id in suggested)
        assertFalse(foreign.id in suggested)
        assertFalse(unreadable.id in suggested)
        // The photo of a colleague's address isn't published, the data may be taken over without it:
        assertThrows<UserException> {
            businessCardService.save(card("Foreign").also { it.addressId = foreign.id; it.includePhoto = true })
        }
        businessCardService.save(card("Foreign").also { it.addressId = foreign.id })
        // Unknown address: no photo, no error.
        val unknown = card("Unknown").also { it.addressId = -1; it.includePhoto = true }
        assertThrows<UserException> { businessCardService.save(unknown) }
        assertFalse(businessCardService.buildVCard(unknown).contains("PHOTO"))
        // No gateway configured in the tests: no photo url, but the vCard works.
        val vcard = businessCardService.buildVCard(card("Own").also { it.addressId = own.id; it.includePhoto = true })
        assertFalse(vcard.contains("PHOTO"))
        assertTrue(vcard.contains("FN:"))
    }

    @Test
    fun `too long fields and an invalid birthday are refused`() {
        logon(createUser("card-invalid"))
        assertThrows<UserException> { businessCardService.save(card("Long").also { it.email = "x".repeat(256) }) }
        assertThrows<UserException> { businessCardService.save(card("Note").also { it.comment = "x".repeat(1001) }) }
        assertThrows<UserException> { businessCardService.save(card("Birthday").also { it.birthday = "31.12.1980" }) }
        businessCardService.save(card("Fine").also { it.comment = "x".repeat(1000); it.birthday = "1980-12-31" })
    }

    @Test
    fun `the stored copy is not changed by editing the returned card`() {
        logon(createUser("card-copy"))
        val stored = businessCardService.save(card("Copy"))
        stored.cardName = "Changed without saving"
        assertEquals("Copy", businessCardService.get(stored.id)?.cardName)
    }

    @Test
    fun `the cards survive the json round trip of the user preferences`() {
        val data = BusinessCards().also { it.cards.add(card("Json", "t1").also { c -> c.includePhoto = true }) }
        val json = UserPrefDao.serialize(data)
        val read = UserPrefDao.fromJson(json, BusinessCards::class.java)!!
        assertEquals(1, read.cards.size)
        assertEquals("t1", read.cards[0].id)
        assertEquals("Json", read.cards[0].cardName)
        assertTrue(read.cards[0].includePhoto)
        assertNotEquals(null, read.cards[0].mobilePhone)
    }
}
