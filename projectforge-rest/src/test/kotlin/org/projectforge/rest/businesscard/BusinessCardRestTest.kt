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

package org.projectforge.rest.businesscard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.address.businesscard.BusinessCard
import org.projectforge.business.address.businesscard.BusinessCardService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.PostData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

/**
 * Who reaches which card through the page's endpoints, and how refusals are answered. The rules themselves are
 * BusinessCardService's (see its test).
 */
class BusinessCardRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var businessCardRest: BusinessCardRest

    private fun createUser(name: String): PFUserDO {
        val user = PFUserDO()
        user.firstname = "$name.firstname"
        user.lastname = "$name.lastname"
        user.username = "${this.javaClass.simpleName}.$name.username"
        return initTestDB.addUser(user)
    }

    private fun post(card: BusinessCard) = PostData(card, watchFieldsTriggered = null, serverData = null)

    private fun card(name: String?, id: String? = null) = BusinessCard().also {
        it.id = id
        it.cardName = name
        it.lastName = "Reinhard"
        it.email = "kai@acme.test"
    }

    @Test
    fun `the owner saves, lists and downloads a card with its qr code`() {
        logon(createUser("rest-owner"))
        val response = businessCardRest.save(post(card("With mail")))
        assertEquals(HttpStatus.OK, response.statusCode)
        val init = businessCardRest.getInitialData()
        assertEquals(1, init.cards.size)
        val info = init.cards[0]
        assertTrue(info.preview.qrCodeUrl.startsWith("data:image/png;base64,"))
        assertFalse(info.preview.withPhoto)
        assertEquals(BusinessCardService.MAX_CARDS_PER_USER, init.maxCards)
        val vcf = businessCardRest.downloadVCard(info.card.id!!)
        assertEquals(HttpStatus.OK, vcf.statusCode)
        assertTrue((vcf.body as String).contains("kai@acme.test"))
    }

    @Test
    fun `another user, the admin included, reaches none of the cards`() {
        logon(createUser("rest-private"))
        businessCardRest.save(post(card("Private")))
        val id = businessCardRest.getInitialData().cards[0].card.id!!
        for (user in listOf(createUser("rest-other"), getUser(TEST_ADMIN_USER))) {
            logon(user)
            assertTrue(businessCardRest.getInitialData().cards.none { it.card.id == id })
            assertEquals(HttpStatus.NOT_FOUND, businessCardRest.downloadVCard(id).statusCode)
            assertEquals(HttpStatus.NOT_FOUND, businessCardRest.delete(id).statusCode)
            // Posting the id of somebody else's card doesn't update it:
            assertEquals(HttpStatus.NOT_ACCEPTABLE, businessCardRest.save(post(card("Hijacked", id))).statusCode)
        }
    }

    @Test
    fun `a refusal is a 406 with the translated error`() {
        logon(createUser("rest-refusal"))
        val response = businessCardRest.save(post(card(null)))
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        val error = response.body!!.validationErrors!!.single()
        assertEquals("cardName", error.fieldId)
        assertNotNull(error.message)
        // The photo of an address that isn't the own one:
        val photo = card("Photo").also { it.addressId = -1; it.includePhoto = true }
        assertEquals(HttpStatus.NOT_ACCEPTABLE, businessCardRest.save(post(photo)).statusCode)
        // The preview never fails, it just has no photo:
        assertFalse(businessCardRest.getPreview(photo).withPhoto)
        assertFalse(businessCardRest.getPreview(photo).photoAllowed)
    }
}
