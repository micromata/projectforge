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

package org.projectforge.rest.pub

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.address.ImageType
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * The public url of a contact image: the random token is the only authorization, nothing else is guessable.
 */
class ContactImagePublicRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var contactImagePublicRest: ContactImagePublicRest

    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    private fun png(): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", out)
        return out.toByteArray()
    }

    /** An address in an address book the logged-in user may modify. */
    private fun newAddress(name: String): AddressDO {
        val book = AddressbookDO().also {
            it.title = "token book $name"
            it.fullAccessUserIds = "${ThreadLocalUserContext.requiredLoggedInUser.id}"
        }
        addressbookDao.insert(book, checkAccess = false)
        val address = AddressDO().also { it.firstName = "Token"; it.name = name; it.add(book) }
        addressDao.insert(address, checkAccess = false)
        return address
    }

    private fun request(ip: String) = MockHttpServletRequest().also { it.remoteAddr = ip }

    private fun addressWithImage(name: String): AddressDO {
        logon(TEST_ADMIN_USER)
        val address = newAddress(name)
        assertEquals(true, addressImageDao.saveOrUpdate(address.id!!, png(), ImageType.PNG))
        return address
    }

    @Test
    fun `the image is delivered under its token without login, replacing or deleting the image keeps the token`() {
        val address = addressWithImage("Delivered")
        val token = addressImageDao.getOrCreatePublicToken(address.id!!)
        assertNotNull(token)
        assertEquals(token, addressImageDao.getOrCreatePublicToken(address.id!!)) // Stable.
        val response = contactImagePublicRest.getImage(request("10.1.0.1"), token!!)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("image", response.headers.contentType?.type)
        // A replaced image keeps its url, which delivers the new image.
        assertEquals(true, addressImageDao.saveOrUpdate(address.id!!, png(), ImageType.PNG))
        assertEquals(token, addressImageDao.getOrCreatePublicToken(address.id!!))
        assertEquals(HttpStatus.OK, contactImagePublicRest.getImage(request("10.1.0.2"), token).statusCode)
        // Without image the url answers 404, with a new image it works again: the token belongs to the address.
        addressImageDao.delete(address.id!!)
        assertEquals(HttpStatus.NOT_FOUND, contactImagePublicRest.getImage(request("10.1.0.8"), token).statusCode)
        assertNull(addressImageDao.getOrCreatePublicToken(address.id!!))
        assertEquals(true, addressImageDao.saveOrUpdate(address.id!!, png(), ImageType.PNG))
        assertEquals(token, addressImageDao.getOrCreatePublicToken(address.id!!))
        assertEquals(HttpStatus.OK, contactImagePublicRest.getImage(request("10.1.0.2"), token).statusCode)
    }

    @Test
    fun `renewing revokes the old url and creates a new one`() {
        val address = addressWithImage("Renewed")
        val token = addressImageDao.getOrCreatePublicToken(address.id!!)!!
        val newToken = addressImageDao.renewPublicToken(address.id!!)
        assertNotNull(newToken)
        assertNotEquals(token, newToken)
        assertEquals(HttpStatus.NOT_FOUND, contactImagePublicRest.getImage(request("10.1.0.4"), token).statusCode)
        assertEquals(HttpStatus.OK, contactImagePublicRest.getImage(request("10.1.0.5"), newToken!!).statusCode)
        assertEquals(newToken, addressImageDao.getOrCreatePublicToken(address.id!!))
    }

    @Test
    fun `an edited address keeps its token, a posted one can't set it`() {
        val address = addressWithImage("Edited")
        val token = addressImageDao.getOrCreatePublicToken(address.id!!)!!
        val posted = AddressDO().also {
            it.id = address.id; it.firstName = "Token"; it.name = "Edited"; it.title = "Dr."
            it.imagePublicToken = "chosen-by-the-poster"
            it.addressbookList = address.addressbookList
        }
        addressDao.update(posted, checkAccess = false)
        assertEquals(token, addressImageDao.getOrCreatePublicToken(address.id!!))
        assertEquals(HttpStatus.OK, contactImagePublicRest.getImage(request("10.1.0.6"), token).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, contactImagePublicRest.getImage(request("10.1.0.7"), "chosen-by-the-poster").statusCode)
    }

    @Test
    fun `an address without image has no token, unknown tokens and deleted images are not found`() {
        logon(TEST_ADMIN_USER)
        val address = newAddress("NoImage")
        assertNull(addressImageDao.getOrCreatePublicToken(address.id!!))
        assertNull(addressImageDao.findByPublicToken("nope"))
        assertNull(addressImageDao.findByPublicToken(null))
        assertNull(addressImageDao.findByPublicToken("x".repeat(100)))
        val withImage = addressWithImage("Deleted")
        val token = addressImageDao.getOrCreatePublicToken(withImage.id!!)!!
        addressImageDao.delete(withImage.id!!)
        assertEquals(HttpStatus.NOT_FOUND, contactImagePublicRest.getImage(request("10.1.0.3"), token).statusCode)
    }

    @Test
    fun `guessing is throttled per client ip`() {
        val address = addressWithImage("Throttled")
        val token = addressImageDao.getOrCreatePublicToken(address.id!!)!!
        val ip = "10.1.0.99"
        repeat(3) {
            assertEquals(HttpStatus.NOT_FOUND, contactImagePublicRest.getImage(request(ip), "wrong-$it").statusCode)
        }
        // Even the right token is refused for a while after failed attempts.
        assertEquals(HttpStatus.NOT_FOUND, contactImagePublicRest.getImage(request(ip), token).statusCode)
        assertArrayEquals(
            addressImageDao.findByPublicToken(token)?.image,
            contactImagePublicRest.getImage(request("10.1.0.100"), token).body,
        )
    }
}
