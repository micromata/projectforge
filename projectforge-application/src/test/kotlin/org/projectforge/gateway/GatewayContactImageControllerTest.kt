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

package org.projectforge.gateway

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDO
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.ContactImageUrl
import org.projectforge.business.address.ImageType
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class GatewayContactImageControllerTest {
    private val contactImageUrl = ContactImageUrl("secret-key", "https://gw.test", "")
    private val addressDao = mock<AddressDao>()
    private val addressImageDao = mock<AddressImageDao>()
    private val controller = GatewayContactImageController(contactImageUrl, addressDao, addressImageDao)

    private val image = byteArrayOf(1, 2, 3)

    private fun address(deleted: Boolean = false) = AddressDO().also {
        it.id = 7
        it.uid = "uid-7"
        it.deleted = deleted
    }

    private fun stubAddress(address: AddressDO = address(), withImage: Boolean = true) {
        whenever(addressDao.findByUid(eq("uid-7"))).thenReturn(address)
        whenever(addressImageDao.findImage(eq(7L), eq(true), any(), eq(false))).thenReturn(
            if (withImage) AddressImageDO().also { it.image = image; it.imageType = ImageType.JPEG } else null
        )
    }

    private fun get(uid: String = "uid-7", signature: String? = contactImageUrl.sign(uid)) =
        controller.getImage(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(uid.toByteArray()), signature ?: "")

    @Test
    fun `a valid url delivers the image with its content type`() {
        stubAddress()
        val response = get()
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(MediaType.IMAGE_JPEG, response.headers.contentType)
        assertArrayEquals(image, response.body)
        assertEquals("max-age=3600, public", response.headers.cacheControl)
    }

    @Test
    fun `everything else is a plain 404`() {
        stubAddress()
        assertEquals(HttpStatus.NOT_FOUND, get(signature = "wrong").statusCode)
        // A signature of another uid:
        assertEquals(HttpStatus.NOT_FOUND, get(signature = contactImageUrl.sign("uid-8")).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.getImage("not base64!", "x").statusCode)
        // Valid signature, but unknown address:
        assertEquals(HttpStatus.NOT_FOUND, get(uid = "unknown").statusCode)
    }

    @Test
    fun `a deleted address or one without image delivers nothing`() {
        stubAddress(address(deleted = true))
        assertEquals(HttpStatus.NOT_FOUND, get().statusCode)
        stubAddress(withImage = false)
        assertEquals(HttpStatus.NOT_FOUND, get().statusCode)
    }

    @Test
    fun `a key other than the shared one doesn't verify`() {
        stubAddress()
        val foreign = ContactImageUrl("other-key", "https://gw.test", "")
        assertEquals(HttpStatus.NOT_FOUND, get(signature = foreign.sign("uid-7")).statusCode)
    }
}
