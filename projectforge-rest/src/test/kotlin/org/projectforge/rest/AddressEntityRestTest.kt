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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.address.FormOfAddress
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.address.importer.AddressImportStorage
import org.projectforge.rest.address.importer.AddressImportSupport
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.importer.ImportEntry
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class AddressEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressEntityRest: AddressEntityRest

    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @Autowired
    private lateinit var addressImageServicesRest: AddressImageServicesRest

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Test
    fun `a new address is up to date, active and in the global address book`() {
        logon(ADMIN)
        val dto = addressEntityRest.newBaseDTO(null)
        assertEquals(AddressStatus.UPTODATE, dto.addressStatus)
        assertEquals(ContactStatus.ACTIVE, dto.contactStatus)
        assertEquals(listOf(addressbookDao.globalAddressbook.id), dto.addressbookList?.map { it.id })
    }

    @Test
    fun `an address needs a name, first name or organization and an address book`() {
        logon(ADMIN)
        val errors = mutableListOf<ValidationError>()
        addressEntityRest.validate(errors, Address())
        assertEquals(setOf("name", "addressbookList"), errors.map { it.fieldId }.toSet())
        errors.clear()
        addressEntityRest.validate(errors, addressEntityRest.newBaseDTO(null).also { it.organization = "Acme" })
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `rows and search hits lead to the view, the form is a sub route of it`() {
        assertEquals("next/address/:id", addressEntityRest.getStandardEditPage())
    }

    @Test
    fun `the favorite flag is set without a save of the address`() {
        logon(ADMIN)
        val id = insertAddress("Favorite")
        addressEntityRest.setFavorite(id, true)
        assertEquals(true, personalAddressDao.getByAddressId(id)?.isFavoriteCard)
        assertTrue(view(id).address.isFavoriteCard)
        addressEntityRest.setFavorite(id, false)
        assertEquals(false, personalAddressDao.getByAddressId(id)?.isFavoriteCard)
    }

    @Test
    fun `the view carries the address with its access flags`() {
        logon(ADMIN)
        val id = insertAddress("View")
        val view = view(id)
        assertEquals("View", view.address.name)
        assertEquals(true, view.address.writeAccess)
        assertFalse(view.address.hasImage)
    }

    @Test
    fun `the address book of the app lists the visible addresses, a sync only the changed ones`() {
        logon(ADMIN)
        val id = insertAddress("AppList-First", "Anna")
        val first = addressEntityRest.getAppList(null)
        assertTrue(first.ids.contains(id))
        val entry = first.addresses.single { it.id == id }
        assertEquals("AppList-First", entry.name)
        assertEquals(true, entry.writeAccess)
        assertFalse(entry.hasImage)
        assertFalse(first.favoriteIds.contains(id))
        addressEntityRest.setFavorite(id, true)
        assertTrue(addressEntityRest.getAppList(first.timestamp + 10_000).favoriteIds.contains(id))
        // Nothing changed since: the ids are still all there, the addresses none.
        val none = addressEntityRest.getAppList(first.timestamp + 10_000)
        assertTrue(none.ids.contains(id))
        assertTrue(none.addresses.none { it.id == id })
        // A change is delivered.
        val second = insertAddress("AppList-Second")
        val changed = addressEntityRest.getAppList(entry.lastUpdate)
        assertTrue(changed.addresses.any { it.id == second })
        assertTrue(changed.addresses.none { it.id == id })
    }

    @Test
    fun `the address book of the app has only current addresses`() {
        logon(ADMIN)
        val outdated = addressDao.insert(AddressDO().also {
            it.name = "AppList-Outdated"
            it.addressStatus = AddressStatus.OUTDATED
            it.add(addressbookDao.globalAddressbook)
        })
        val inactive = addressDao.insert(AddressDO().also {
            it.name = "AppList-Inactive"
            it.contactStatus = ContactStatus.NON_ACTIVE
            it.add(addressbookDao.globalAddressbook)
        })
        val current = insertAddress("AppList-Current")
        val ids = addressEntityRest.getAppList(null).ids
        assertTrue(ids.contains(current))
        assertFalse(ids.contains(outdated))
        assertFalse(ids.contains(inactive))
    }

    @Test
    fun `the address book of the app lacks addresses of address books without access`() {
        logon(ADMIN)
        val closedBook = AddressbookDO().also {
            it.title = "AppList-closed"
            it.fullAccessUserIds = ThreadLocalUserContext.requiredLoggedInUserId.toString()
        }
        addressbookDao.insert(closedBook, checkAccess = false)
        val closed = addressDao.insert(AddressDO().also {
            it.name = "AppList-Closed"
            it.add(closedBook)
        }, checkAccess = false)
        val open = insertAddress("AppList-Open")
        assertTrue(addressEntityRest.getAppList(null).ids.containsAll(listOf(closed, open)))
        logon(TEST_USER)
        val ids = addressEntityRest.getAppList(null).ids
        assertFalse(ids.contains(closed))
    }

    @Test
    fun `duplicates are found by name and first name`() {
        logon(ADMIN)
        val id = insertAddress("Duplicate", "Dora")
        val duplicates = addressEntityRest.getDuplicates("Duplicate", "Dora")
        assertEquals(listOf(id), duplicates.map { it.id })
        assertTrue(addressEntityRest.getDuplicates("Nobody-like-this", "Dora").isEmpty())
    }

    @Test
    fun `an uploaded image is stored by the save, a removed one deleted`() {
        logon(ADMIN)
        val request = newRequest()
        val png = MockMultipartFile("file", "image.png", "image/png", createPng())
        assertEquals(200, addressImageServicesRest.uploadFile(-1, png, request).statusCode.value())
        val id = save(request, addressEntityRest.newBaseDTO(null).also {
            it.name = "Image-AddressEntityRestTest"
            it.form = FormOfAddress.UNKNOWN
        })
        assertNotNull(addressImageDao.findImage(id, fetchImage = true)?.image)
        assertTrue(addressEntityRest.transformFromDB(addressDao.find(id)!!).hasImage)

        val dto = addressEntityRest.transformFromDB(addressDao.find(id)!!, editMode = true)
        dto.imageDeleted = true
        save(request, dto)
        assertNull(addressImageDao.findImage(id, fetchImage = true)?.image)
    }

    @Test
    fun `saving the form of an import entry marks the entry as imported`() {
        logon(ADMIN)
        val request = newRequest()
        val storage = AddressImportStorage()
        storage.filename = "test.vcf"
        storage.parseVcfData(VCARD.toByteArray())
        storage.reconcileImportStorage()
        AddressImportSupport.storeStorage(request, storage)
        assertEquals(ImportEntry.Status.NEW, storage.getImportEntryByIndex(0)!!.status)

        save(request, addressEntityRest.newBaseDTO(null).also {
            it.name = "Importer-AddressEntityRestTest"
            it.firstName = "Ina"
            it.form = FormOfAddress.UNKNOWN
            it.importIndex = 0
        })
        val reconciled = AddressImportSupport.getStorage(request)!!
        assertEquals(ImportEntry.Status.IMPORTED, reconciled.getImportEntryByIndex(0)!!.status)
    }

    private fun view(id: Long): AddressEntityRest.AddressView {
        return addressEntityRest.getView(id).body!!
    }

    private fun insertAddress(name: String, firstName: String? = null): Long {
        return addressDao.insert(AddressDO().also {
            it.name = name
            it.firstName = firstName
            it.add(addressbookDao.globalAddressbook)
        })
    }

    private fun newRequest(): MockHttpServletRequest {
        return MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
    }

    private fun save(request: MockHttpServletRequest, dto: Address): Long {
        val postData = PostData(
            data = dto,
            watchFieldsTriggered = null,
            serverData = sessionCsrfService.createServerData(request),
        )
        val response = addressEntityRest.saveOrUpdate(request, postData)
        assertEquals(
            200,
            response.statusCode.value(),
            "Save refused: ${response.body?.validationErrors?.joinToString { "${it.fieldId}: ${it.message}" }}",
        )
        // The answer carries the id of a new address only as a variable for the client; look it up instead.
        return dto.id ?: addressDao.findByNameAndFirstName(dto.name, dto.firstName).single().id!!
    }

    private fun createPng(): ByteArray {
        val image = BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    companion object {
        private val VCARD = """
            BEGIN:VCARD
            VERSION:3.0
            N:Importer-AddressEntityRestTest;Ina;;;
            FN:Ina Importer-AddressEntityRestTest
            ORG:Import GmbH
            EMAIL;TYPE=WORK:ina@example.org
            END:VCARD
        """.trimIndent()
    }
}
