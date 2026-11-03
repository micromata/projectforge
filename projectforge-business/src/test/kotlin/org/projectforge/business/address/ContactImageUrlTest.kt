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

package org.projectforge.business.address

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContactImageUrlTest {
    private fun create(
        photoBaseUrl: String = "",
        publicUrl: String = "",
        pushUrl: String = "",
        domain: String = "",
        contextPath: String = "",
    ) = ContactImageUrl(photoBaseUrl, publicUrl, pushUrl, domain, contextPath)

    @Test
    fun `the url is the base url plus the token`() {
        assertEquals(
            "https://pub.test/rsPublic/contactImage/tok-1",
            create(publicUrl = "https://pub.test/").url("tok-1"),
        )
        assertNull(create(publicUrl = "https://pub.test").url(null))
        assertNull(create(publicUrl = "https://pub.test").url(" "))
    }

    @Test
    fun `the photo base url overrides gateway and domain`() {
        val url = create(
            photoBaseUrl = "https://photos.test/pf/",
            publicUrl = "https://pub.test",
            pushUrl = "https://gw.test/api/gateway/sync",
            domain = "https://pf.test",
        )
        assertEquals("https://photos.test/pf", url.baseUrl)
        assertEquals("https://photos.test/pf/rsPublic/contactImage/t", url.url("t"))
    }

    @Test
    fun `the gateway comes before the domain`() {
        assertEquals("https://gw.test", create(pushUrl = "https://gw.test/api/gateway/sync", domain = "https://pf.test").baseUrl)
        assertEquals("https://pub.test", create(publicUrl = "https://pub.test", pushUrl = "https://gw.test/api/gateway/sync").baseUrl)
    }

    @Test
    fun `without gateway the domain with context path is the base`() {
        assertEquals("https://pf.test/demo", create(domain = "https://pf.test/", contextPath = "/demo").baseUrl)
        assertEquals("https://pf.test", create(domain = "https://pf.test").baseUrl)
    }

    @Test
    fun `a local domain is no base url`() {
        assertEquals("", create(domain = "http://localhost:8080").baseUrl)
        assertEquals("", create(domain = "http://127.0.0.1:8080").baseUrl)
        assertEquals("", create().baseUrl)
        assertNull(create(domain = "http://localhost:8080").url("t"))
    }

    @Test
    fun `an address without uid is known as pf-id, as pushed to the gateway`() {
        assertEquals("pf-5", ContactImageUrl.addressUid(AddressDO().also { it.id = 5 }))
        assertEquals("u-1", ContactImageUrl.addressUid(AddressDO().also { it.id = 5; it.uid = "u-1" }))
    }

    @Test
    fun `public tokens are random and url safe`() {
        val a = AddressImageDao.newPublicToken()
        val b = AddressImageDao.newPublicToken()
        assertNotEquals(a, b)
        assertEquals(43, a.length)
        assertTrue(a.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }
}
