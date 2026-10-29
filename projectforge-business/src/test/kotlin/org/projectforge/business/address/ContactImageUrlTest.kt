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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContactImageUrlTest {
    private fun create(key: String = "secret-key", publicUrl: String = "", pushUrl: String = "https://gw.test/api/gateway/sync") =
        ContactImageUrl(key, publicUrl, pushUrl)

    @Test
    fun `a signature is only valid for its uid and key`() {
        val url = create()
        val signature = url.sign("uid-1")!!
        assertTrue(url.verify("uid-1", signature))
        assertFalse(url.verify("uid-2", signature))
        assertFalse(url.verify("uid-1", signature.dropLast(1) + (if (signature.last() == 'A') 'B' else 'A')))
        assertFalse(url.verify("uid-1", ""))
        assertFalse(url.verify(null, signature))
        assertFalse(create(key = "other-key").verify("uid-1", signature))
    }

    @Test
    fun `the signing key is not the configured key itself`() {
        // The same key is used for the encryption of tokens: only a derived key signs.
        assertNotEquals(create().sign("uid-1"), create(key = "secret-key$").sign("uid-1"))
        assertEquals(create().sign("uid-1"), create().sign("uid-1"))
    }

    @Test
    fun `the url is built from the public url or the push url`() {
        val signature = create().sign("a b/c")!!
        // The uid is a path segment of its own (base64url), whatever characters it has.
        val url = create().url("a b/c")!!
        assertEquals("https://gw.test/rsPublic/contactImage/YSBiL2M/$signature", url)
        assertEquals("a b/c", ContactImageUrl.decodeUid(url.substringAfter("contactImage/").substringBefore("/")))
        assertEquals(
            "https://pub.test/ctx/rsPublic/contactImage/dWlk/${create().sign("uid")}",
            create(publicUrl = "https://pub.test/ctx/").url("uid"),
        )
        assertNull(ContactImageUrl.decodeUid("not base64!"))
        assertNull(ContactImageUrl.decodeUid(null))
    }

    @Test
    fun `an address without uid is known as pf-id, as pushed to the gateway`() {
        assertEquals("pf-5", ContactImageUrl.addressUid(AddressDO().also { it.id = 5 }))
        assertEquals("u-1", ContactImageUrl.addressUid(AddressDO().also { it.id = 5; it.uid = "u-1" }))
    }

    @Test
    fun `without gateway or key there is no url`() {
        assertNull(create(pushUrl = "").url("uid"))
        assertNull(create(key = "").url("uid"))
        assertNull(create(key = "").sign("uid"))
        assertFalse(create(key = "").verify("uid", "x"))
    }
}
