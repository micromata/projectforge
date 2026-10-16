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

package org.projectforge.rest.address.importer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile

class AddressImportRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressImportRest: AddressImportRest

    @Test
    fun `an uploaded vcf file is matched against the database and kept in the session`() {
        logon(TEST_USER)
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        assertNull(addressImportRest.getState(request).filename)

        val response = addressImportRest.upload(request, MockMultipartFile("file", "contacts.vcf", "text/vcard", VCARD.toByteArray()))
        assertEquals(200, response.statusCode.value(), "Upload refused: ${response.body}")
        val state = addressImportRest.getState(request)
        assertEquals("contacts.vcf", state.filename)
        assertEquals(1, state.entries.size)
        val entry = state.entries.single()
        assertEquals(0, entry.id)
        assertEquals("NEW", entry.status)
        assertNull(entry.addressId)
        assertEquals("Upload-AddressImportRestTest", entry.read?.name)

        val comparison = addressImportRest.getEntry(request, 0).body!!
        assertEquals("Upload-AddressImportRestTest", comparison.vcfComparisonData["name"]?.get("vcf"))
        assertNull(comparison.vcfComparisonData["name"]?.get("db"))
        assertFalse(comparison.hasImage)
        assertEquals(404, addressImportRest.getEntry(request, 1).statusCode.value())

        addressImportRest.cancel(request)
        assertNull(addressImportRest.getState(request).filename)
    }

    @Test
    fun `an upload other than a vcf file is refused with a message`() {
        logon(TEST_USER)
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val response = addressImportRest.upload(request, MockMultipartFile("file", "contacts.csv", "text/csv", "a;b".toByteArray()))
        assertEquals(400, response.statusCode.value())
        @Suppress("UNCHECKED_CAST")
        assertTrue((response.body as Map<String, String>)["error"]!!.isNotBlank())
    }

    companion object {
        private val VCARD = """
            BEGIN:VCARD
            VERSION:3.0
            N:Upload-AddressImportRestTest;Ulla;;;
            FN:Ulla Upload-AddressImportRestTest
            EMAIL;TYPE=WORK:ulla@example.org
            END:VCARD
        """.trimIndent()
    }
}
