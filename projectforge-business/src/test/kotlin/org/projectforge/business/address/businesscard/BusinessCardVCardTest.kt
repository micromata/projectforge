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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.ImageType

class BusinessCardVCardTest {
    private fun address() = AddressDO().also {
        it.id = 1
        it.firstName = "Kai"
        it.name = "Reinhard"
        it.organization = "ACME GmbH"
        it.email = "kai@acme.test"
        it.mobilePhone = "+49 171 1234"
        it.privatePhone = "+49 561 999"
        it.privateEmail = "private@home.test"
        it.privateAddressText = "Secret street 1"
        it.birthday = java.time.LocalDate.of(1970, 1, 1)
    }

    @Test
    fun `private data is on the vcard only if the card keeps it`() {
        val all = BusinessCardService.createFrom(address())
        assertTrue(all.privateEmail == "private@home.test" && all.birthday == "1970-01-01")
        val vcard = BusinessCardVCard.build(all)
        assertTrue(vcard.contains("EMAIL;TYPE=home:private@home.test"), vcard)
        assertTrue(vcard.contains("Secret street 1"), vcard)
        assertTrue(vcard.contains("BDAY:1970-01-01"), vcard)
        // The editor leaves the private fields out unless ticked: they are null then.
        val business = BusinessCardService.createFrom(address()).also {
            it.privateEmail = null
            it.privatePhone = null
            it.privateStreet = null
            it.birthday = null
        }
        val businessVCard = BusinessCardVCard.build(business)
        assertTrue(businessVCard.contains("kai@acme.test"))
        assertTrue(businessVCard.contains("+49 171 1234"))
        assertFalse(businessVCard.contains("+49 561 999"))
        assertFalse(businessVCard.contains("private@home.test"))
        assertFalse(businessVCard.contains("Secret street"))
        assertFalse(businessVCard.contains("BDAY"))
    }

    @Test
    fun `only the fields of the card are written, empty ones are left out`() {
        val card = BusinessCardService.createFrom(address()).also {
            it.mobilePhone = null
            it.privateStreet = null
        }
        val vcard = BusinessCardVCard.build(card)
        assertFalse(vcard.contains("TEL;TYPE=CELL"))
        // Neither a business address (none in the address) nor the private one (left out):
        assertFalse(vcard.contains("ADR"))
        assertTrue(vcard.contains("ORG:ACME GmbH"))
    }

    @Test
    fun `the photo is a url, never embedded`() {
        val card = BusinessCardService.createFrom(address()).also { it.id = "abc" }
        val vcard = BusinessCardVCard.build(card, "https://gw.test/rsPublic/businesscard/abc/photo", ImageType.JPEG)
        assertTrue(vcard.contains("PHOTO;VALUE=uri;TYPE=JPEG:https://gw.test/rsPublic/businesscard/abc/photo") ||
                vcard.contains("PHOTO;TYPE=JPEG;VALUE=uri:https://gw.test/rsPublic/businesscard/abc/photo"), vcard)
        assertFalse(BusinessCardVCard.build(card).contains("PHOTO"))
    }

    @Test
    fun `texts stay as they are, only the photo type is upper case`() {
        val card = BusinessCardService.createFrom(address()).also { it.id = "abc"; it.comment = "Format TYPE=jpeg" }
        val vcard = BusinessCardVCard.build(card, "https://gw.test/photo", ImageType.JPEG)
        assertTrue(vcard.contains("TYPE=jpeg"), vcard)
        assertTrue(vcard.lines().first { it.startsWith("PHOTO") }.contains("TYPE=JPEG"), vcard)
    }

    @Test
    fun `a card without a name has the organization as formatted name`() {
        val card = BusinessCard().also { it.organization = "ACME GmbH"; it.cardName = "Company" }
        assertTrue(BusinessCardVCard.build(card).contains("FN:ACME GmbH"))
        assertTrue(BusinessCardVCard.build(BusinessCard().also { it.cardName = "Company" }).contains("FN:Company"))
    }
}
