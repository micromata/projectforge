/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.UIFieldset
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Comprehensive tests for AddressMultiSelectedPageRest covering form generation,
 * mass update logic, and field processing.
 */
class AddressMultiSelectedPageRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressMultiSelectedPageRest: AddressMultiSelectedPageRest

    @Autowired
    private lateinit var addressDao: AddressDao

    private lateinit var mockRequest: HttpServletRequest

    @BeforeEach
    fun setup() {
        logon(ADMIN)
        mockRequest = mock(HttpServletRequest::class.java)
    }

    // =========================================================================
    // Form Generation Tests
    // =========================================================================

    @Test
    fun `fillForm should create layout with all expected fields`() {
        logon(ADMIN)
        val layout = UILayout("test")
        val massUpdateData = mutableMapOf<String, MassUpdateParameter>()
        val selectedIds = listOf<Long>()

        addressMultiSelectedPageRest.fillForm(mockRequest, layout, massUpdateData, selectedIds, mutableMapOf())

        assertNotNull(layout)
        assertTrue(layout.layout.isNotEmpty(), "Layout should contain elements")
    }

    @Test
    fun `fillForm should create addressStatus field`() {
        logon(ADMIN)
        val layout = UILayout("test")
        val massUpdateData = mutableMapOf<String, MassUpdateParameter>()

        addressMultiSelectedPageRest.fillForm(mockRequest, layout, massUpdateData, listOf(), mutableMapOf())

        // Verify addressStatus is created
        assertNotNull(massUpdateData["addressStatus"])
    }

    @Test
    fun `fillForm should create contactStatus field`() {
        logon(ADMIN)
        val layout = UILayout("test")
        val massUpdateData = mutableMapOf<String, MassUpdateParameter>()

        addressMultiSelectedPageRest.fillForm(mockRequest, layout, massUpdateData, listOf(), mutableMapOf())

        // Verify contactStatus is created
        assertNotNull(massUpdateData["contactStatus"])
    }

    @Test
    fun `fillForm should create collapsible fieldsets for business and postal addresses`() {
        logon(ADMIN)
        val layout = UILayout("test")
        val massUpdateData = mutableMapOf<String, MassUpdateParameter>()

        addressMultiSelectedPageRest.fillForm(mockRequest, layout, massUpdateData, listOf(), mutableMapOf())

        // Find fieldsets in layout
        val fieldsets = layout.getAllElements().filterIsInstance<UIFieldset>()

        // Verify at least 2 fieldsets exist (business and postal)
        assertTrue(fieldsets.size >= 2, "Should have at least business and postal address fieldsets")

        // Verify fieldsets have collapsed=true
        val collapsibleFieldsets = fieldsets.filter { it.collapsed == true }
        assertTrue(collapsibleFieldsets.isNotEmpty(), "Should have collapsible fieldsets")
    }

    @Test
    fun `getTitleKey should return correct i18n key`() {
        assertEquals("address.multiselected.title", addressMultiSelectedPageRest.getTitleKey())
    }

    // =========================================================================
    // Integration Tests - Mass Update Operations
    // =========================================================================

    @Test
    fun `mass update should update addressStatus to OUTDATED`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        address.addressStatus = AddressStatus.UPTODATE
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "addressStatus" to MassUpdateParameter().apply {
                textValue = AddressStatus.OUTDATED.name
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals(AddressStatus.OUTDATED, updated.addressStatus)
    }

    @Test
    fun `mass update should set addressStatus to default when deleted`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        address.addressStatus = AddressStatus.OUTDATED
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "addressStatus" to MassUpdateParameter().apply {
                delete = true
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals(AddressStatus.UPTODATE, updated.addressStatus, "Should reset to UPTODATE default")
    }

    @Test
    fun `mass update should update contactStatus to NON_ACTIVE`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        address.contactStatus = ContactStatus.ACTIVE
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "contactStatus" to MassUpdateParameter().apply {
                textValue = ContactStatus.NON_ACTIVE.name
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals(ContactStatus.NON_ACTIVE, updated.contactStatus)
    }

    @Test
    fun `mass update should set contactStatus to default when deleted`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        address.contactStatus = ContactStatus.NON_ACTIVE
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "contactStatus" to MassUpdateParameter().apply {
                delete = true
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals(ContactStatus.ACTIVE, updated.contactStatus, "Should reset to ACTIVE default")
    }

    // =========================================================================
    // Communication Language Tests
    // =========================================================================

    @Test
    fun `mass update should set communicationLanguage`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "communicationLanguage" to MassUpdateParameter().apply {
                textValue = "de"
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertNotNull(updated.communicationLanguage)
        assertEquals("de", updated.communicationLanguage?.language)
    }

    @Test
    fun `mass update should delete communicationLanguage`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        address.communicationLanguage = Locale.GERMAN
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "communicationLanguage" to MassUpdateParameter().apply {
                delete = true
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertNull(updated.communicationLanguage)
    }

    // =========================================================================
    // Text Field Tests
    // =========================================================================

    @Test
    fun `mass update should update organization field`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        address.organization = "Old Org"
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "organization" to MassUpdateParameter().apply {
                textValue = "New Organization"
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals("New Organization", updated.organization)
    }

    @Test
    fun `mass update should update business address fields`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "addressText" to MassUpdateParameter().apply { textValue = "Main Street 1" },
            "zipCode" to MassUpdateParameter().apply { textValue = "12345" },
            "city" to MassUpdateParameter().apply { textValue = "Berlin" },
            "country" to MassUpdateParameter().apply { textValue = "Germany" }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals("Main Street 1", updated.addressText)
        assertEquals("12345", updated.zipCode)
        assertEquals("Berlin", updated.city)
        assertEquals("Germany", updated.country)
    }

    @Test
    fun `mass update should update postal address fields`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "postalAddressText" to MassUpdateParameter().apply { textValue = "PO Box 123" },
            "postalZipCode" to MassUpdateParameter().apply { textValue = "54321" },
            "postalCity" to MassUpdateParameter().apply { textValue = "Munich" }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals("PO Box 123", updated.postalAddressText)
        assertEquals("54321", updated.postalZipCode)
        assertEquals("Munich", updated.postalCity)
    }

    @Test
    fun `mass update should update comment field`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "comment" to MassUpdateParameter().apply {
                textValue = "This is a test comment"
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals("This is a test comment", updated.comment)
    }

    // =========================================================================
    // Batch Processing Tests
    // =========================================================================

    @Test
    fun `mass update should update multiple addresses`() {
        logon(ADMIN)
        val address1 = createTestAddress("Address 1")
        val address2 = createTestAddress("Address 2")
        val address3 = createTestAddress("Address 3")
        addressDao.insert(address1, checkAccess = false)
        addressDao.insert(address2, checkAccess = false)
        addressDao.insert(address3, checkAccess = false)

        val params = mutableMapOf(
            "organization" to MassUpdateParameter().apply {
                textValue = "Batch Updated Org"
            }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address1.id!!, address2.id!!, address3.id!!), massUpdateContext)

        val updated1 = addressDao.find(address1.id!!)!!
        val updated2 = addressDao.find(address2.id!!)!!
        val updated3 = addressDao.find(address3.id!!)!!

        assertEquals("Batch Updated Org", updated1.organization)
        assertEquals("Batch Updated Org", updated2.organization)
        assertEquals("Batch Updated Org", updated3.organization)
    }

    @Test
    fun `mass update should handle empty selection`() {
        logon(ADMIN)
        val params = mutableMapOf<String, MassUpdateParameter>()
        val massUpdateContext = createMassUpdateContext(params)

        val result = callProceedMassUpdate(emptyList(), massUpdateContext)

        assertNull(result)
    }

    @Test
    fun `mass update should handle mixed field updates`() {
        logon(ADMIN)
        val address = createTestAddress("Test Address")
        addressDao.insert(address, checkAccess = false)

        val params = mutableMapOf(
            "organization" to MassUpdateParameter().apply { textValue = "New Org" },
            "addressStatus" to MassUpdateParameter().apply { textValue = AddressStatus.OUTDATED.name },
            "city" to MassUpdateParameter().apply { textValue = "Hamburg" },
            "communicationLanguage" to MassUpdateParameter().apply { textValue = "en" }
        )
        val massUpdateContext = createMassUpdateContext(params)

        callProceedMassUpdate(listOf(address.id!!), massUpdateContext)

        val updated = addressDao.find(address.id!!)!!
        assertEquals("New Org", updated.organization)
        assertEquals(AddressStatus.OUTDATED, updated.addressStatus)
        assertEquals("Hamburg", updated.city)
        assertEquals("en", updated.communicationLanguage?.language)
    }

    // =========================================================================
    // Utility Tests
    // =========================================================================

    @Test
    fun `customizeExcelIdentifierHeadCells should return correct column headers`() {
        val headers = addressMultiSelectedPageRest.customizeExcelIdentifierHeadCells()

        assertEquals(3, headers.size)
        assertTrue(headers[0].contains("name") || headers[0].contains("Name"))
        assertTrue(headers[1].contains("firstName") || headers[1].contains("First"))
        assertTrue(headers[2].contains("organization") || headers[2].contains("Organization"))
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private fun createTestAddress(name: String): AddressDO {
        return AddressDO().apply {
            this.name = name
            this.addressStatus = AddressStatus.UPTODATE
            this.contactStatus = ContactStatus.ACTIVE
        }
    }

    private fun createMassUpdateContext(params: MutableMap<String, MassUpdateParameter>): MassUpdateContext<AddressDO> {
        return object : MassUpdateContext<AddressDO>(params) {
            override fun getId(obj: AddressDO): Long {
                return obj.id!!
            }
        }
    }

    private fun callProceedMassUpdate(
        selectedIds: Collection<Long>,
        massUpdateContext: MassUpdateContext<AddressDO>
    ): org.springframework.http.ResponseEntity<*>? {
        // Use reflection to call protected method
        val method = AddressMultiSelectedPageRest::class.java.getDeclaredMethod(
            "proceedMassUpdate",
            HttpServletRequest::class.java,
            Collection::class.java,
            MassUpdateContext::class.java
        )
        method.isAccessible = true
        return method.invoke(addressMultiSelectedPageRest, mockRequest, selectedIds, massUpdateContext)
            as org.springframework.http.ResponseEntity<*>?
    }
}
