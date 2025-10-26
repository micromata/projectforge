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

package org.projectforge.business.address.vcard

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.address.*
import org.projectforge.business.test.TestSetup
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Month
import java.util.*
import java.util.Base64

class VCardUtilsTest {
    @Test
    fun `test converting of vcards from and to AddressDO`() {
        TestSetup.init()
        AddressDO().also { address ->
            address.birthday = LocalDate.of(1992, Month.JULY, 11)
            address.firstName = "Joe"
            address.name = "Hill"
            val vcardString = VCardUtils.buildVCardString(address, VCardVersion.V_3_0, "https://www.projectforge.org/carddav/users/kai/photos/contact-1234.png", ImageType.JPEG)
            assertTrue(vcardString.contains("BDAY:1992-07-11"))
        }
        AddressDO().also { address ->
            address.birthday = LocalDate.of(1992, Month.JULY, 11)
            address.firstName = "Joe"
            address.name = "Hill"
            val vcardString = VCardUtils.buildVCardString(address, VCardVersion.V_4_0)
            assertTrue(vcardString.contains("BDAY:19920711"))
        }
        val vcard = VCardUtils.parseVCardsFromByteArray(EXAMPLE_VCF.toByteArray(StandardCharsets.UTF_8))
        assertEquals(1, vcard.size)
        VCardUtils.buildAddressDO(vcard[0]).also { address ->
            assertEquals("John", address.firstName)
            assertEquals("Doe", address.name)
            assertEquals(LocalDate.of(1992, Month.JULY, 11), address.birthday)
        }
    }

    @Test
    fun `test full address round-trip with V3_0`() {
        TestSetup.init()
        val original = createFullyPopulatedAddressDO()

        // Export to VCard 3.0
        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_3_0)

        // Parse back
        val vcards = VCardUtils.parseVCardsFromString(vcardString)
        assertEquals(1, vcards.size)
        val imported = VCardUtils.buildAddressDO(vcards[0])

        // Verify all supported fields
        assertAddressFieldsEqual(original, imported)
    }

    @Test
    fun `test full address round-trip with V4_0`() {
        TestSetup.init()
        val original = createFullyPopulatedAddressDO()

        // Export to VCard 4.0
        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)

        // Parse back
        val vcards = VCardUtils.parseVCardsFromString(vcardString)
        assertEquals(1, vcards.size)
        val imported = VCardUtils.buildAddressDO(vcards[0])

        // Verify all supported fields
        assertAddressFieldsEqual(original, imported)
    }

    @Test
    fun `test minimal address round-trip`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "Smith"
            firstName = "Jane"
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        val vcards = VCardUtils.parseVCardsFromString(vcardString)
        val imported = VCardUtils.buildAddressDO(vcards[0])

        assertEquals(original.name, imported.name)
        assertEquals(original.firstName, imported.firstName)
    }

    @Test
    fun `test parse complex VCard with all field types`() {
        TestSetup.init()
        val vcards = VCardUtils.parseVCardsFromString(COMPLEX_VCARD)
        assertEquals(1, vcards.size)

        val address = VCardUtils.buildAddressDO(vcards[0])

        // Verify parsed fields
        assertEquals("Dr.", address.title)
        assertEquals("Schmidt", address.name)
        assertEquals("Maria", address.firstName)
        assertEquals("+49-89-1234567", address.businessPhone)
        assertEquals("+49-171-9876543", address.mobilePhone)
        assertEquals("+49-89-1234568", address.fax)
        assertEquals("Hauptstraße 123", address.addressText)
        assertEquals("80333", address.zipCode)
        assertEquals("München", address.city)
        assertEquals("Bayern", address.state)
        assertEquals("Deutschland", address.country)
        assertEquals("maria.schmidt@example.com", address.email)
        assertEquals("Acme Corp", address.organization)
        assertEquals("Engineering", address.division)
        assertEquals("https://www.example.com", address.website)
        assertEquals(LocalDate.of(1985, 5, 15), address.birthday)
    }

    @Test
    fun `test parse multiple addresses HOME WORK POSTAL`() {
        TestSetup.init()
        val vcards = VCardUtils.parseVCardsFromString(VCARD_MULTIPLE_ADDRESSES)
        assertEquals(1, vcards.size)

        val address = VCardUtils.buildAddressDO(vcards[0])

        // Business address
        assertEquals("Business Street 1", address.addressText)
        assertEquals("10115", address.zipCode)
        assertEquals("Berlin", address.city)
        assertEquals("Berlin", address.state)
        assertEquals("Germany", address.country)

        // Private address
        assertEquals("Home Street 42", address.privateAddressText)
        assertEquals("80331", address.privateZipCode)
        assertEquals("München", address.privateCity)
        assertEquals("Bayern", address.privateState)
        assertEquals("Germany", address.privateCountry)

        // Postal address
        assertEquals("PO Box 5678", address.postalAddressText)
        assertEquals("80333", address.postalZipCode)
        assertEquals("München", address.postalCity)
        assertEquals("Bayern", address.postalState)
        assertEquals("Germany", address.postalCountry)
    }

    @Test
    fun `test parse multiple phone numbers`() {
        TestSetup.init()
        val vcards = VCardUtils.parseVCardsFromString(VCARD_MULTIPLE_PHONES)
        assertEquals(1, vcards.size)

        val address = VCardUtils.buildAddressDO(vcards[0])

        assertEquals("+49-89-1111111", address.businessPhone)
        assertEquals("+49-171-2222222", address.mobilePhone)
        assertEquals("+49-89-3333333", address.fax)
        assertEquals("+49-89-4444444", address.privatePhone)
        assertEquals("+49-171-5555555", address.privateMobilePhone)
    }

    @Test
    fun `test parse multiple emails`() {
        TestSetup.init()
        val vcards = VCardUtils.parseVCardsFromString(VCARD_MULTIPLE_EMAILS)
        assertEquals(1, vcards.size)

        val address = VCardUtils.buildAddressDO(vcards[0])

        assertEquals("work@example.com", address.email)
        assertEquals("private@example.com", address.privateEmail)
    }

    @Test
    fun `test parse organization with multiple values`() {
        TestSetup.init()

        // Test with 3 values (organization, division, position)
        val vcard3 = """
            BEGIN:VCARD
            VERSION:4.0
            FN:John Doe
            N:Doe;John;;;
            ORG:Company;Department;Position
            END:VCARD
        """.trimIndent()

        val address3 = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcard3)[0])
        assertEquals("Company", address3.organization)
        assertEquals("Department", address3.division)
        assertEquals("Position", address3.positionText)

        // Test with 2 values (organization, division)
        val vcard2 = """
            BEGIN:VCARD
            VERSION:4.0
            FN:John Doe
            N:Doe;John;;;
            ORG:Company;Department
            END:VCARD
        """.trimIndent()

        val address2 = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcard2)[0])
        assertEquals("Company", address2.organization)
        assertEquals("Department", address2.division)
        assertNull(address2.positionText)

        // Test with 1 value (organization only)
        val vcard1 = """
            BEGIN:VCARD
            VERSION:4.0
            FN:John Doe
            N:Doe;John;;;
            ORG:Company
            END:VCARD
        """.trimIndent()

        val address1 = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcard1)[0])
        assertEquals("Company", address1.organization)
        assertNull(address1.division)
        assertNull(address1.positionText)
    }

    @Test
    fun `test parse birthday formats`() {
        TestSetup.init()

        // Test V3.0 format (YYYY-MM-DD)
        val vcardV3 = """
            BEGIN:VCARD
            VERSION:3.0
            FN:John Doe
            N:Doe;John;;;
            BDAY:1990-12-25
            END:VCARD
        """.trimIndent()

        val addressV3 = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardV3)[0])
        assertEquals(LocalDate.of(1990, 12, 25), addressV3.birthday)

        // Test V4.0 format (YYYYMMDD)
        val vcardV4 = """
            BEGIN:VCARD
            VERSION:4.0
            FN:Jane Doe
            N:Doe;Jane;;;
            BDAY:19851231
            END:VCARD
        """.trimIndent()

        val addressV4 = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardV4)[0])
        assertEquals(LocalDate.of(1985, 12, 31), addressV4.birthday)
    }

    @Test
    fun `test parse special characters and umlauts`() {
        TestSetup.init()
        val vcard = """
            BEGIN:VCARD
            VERSION:4.0
            FN:Müller Ößwald
            N:Müller;Jürgen;Franz;Dr.;Jr.
            ADR;TYPE=WORK:;;Straße äöü 123;Göttingen;;37073;Deutschland
            EMAIL:jürgen@müller.de
            NOTE:Special chars: äöüÄÖÜß @#$%&*()
            END:VCARD
        """.trimIndent()

        val address = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcard)[0])

        assertEquals("Müller", address.name)
        assertEquals("Jürgen", address.firstName)
        assertEquals("Dr.", address.title)
        assertTrue(address.addressText?.contains("äöü") == true)
        assertEquals("Göttingen", address.city)
        assertEquals("jürgen@müller.de", address.email)
        assertTrue(address.comment?.contains("äöüÄÖÜß") == true)
    }

    @Test
    fun `test image round-trip with embedded photo`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "PhotoTest"
            firstName = "John"
        }

        // Create test image (1x1 PNG)
        val testImageBytes = Base64.getDecoder().decode(TINY_PNG_BASE64)
        original.setTransientImage(AddressImageDO().apply {
            image = testImageBytes
            imageType = ImageType.PNG
        })

        // Export VCard
        val vcard = VCardUtils.buildVCard(original, null, null)

        // Import back
        val imported = VCardUtils.buildAddressDO(vcard)

        // Verify image was imported
        val importedImage = imported.transientImage
        assertNotNull(importedImage)
        assertNotNull(importedImage?.image)
        assertArrayEquals(testImageBytes, importedImage?.image)
        assertEquals(ImageType.PNG, importedImage?.imageType)
    }

    @Test
    fun `test image type preservation`() {
        TestSetup.init()

        // Test JPEG
        testImageTypeRoundTrip(ImageType.JPEG)

        // Test PNG
        testImageTypeRoundTrip(ImageType.PNG)

        // Test GIF
        testImageTypeRoundTrip(ImageType.GIF)
    }

    @Test
    fun `test edge case - empty fields`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = ""
            firstName = ""
            email = ""
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])

        // Empty strings should be handled gracefully
        assertNotNull(imported)
    }

    @Test
    fun `test edge case - null fields`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = null
            firstName = null
            email = null
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])

        assertNotNull(imported)
        assertNull(imported.name)
        assertNull(imported.firstName)
    }

    @Test
    fun `test edge case - very long comment`() {
        TestSetup.init()
        val longComment = "x".repeat(4000)
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "LongComment"
            firstName = "Test"
            comment = longComment
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])

        assertEquals(longComment, imported.comment?.trim())
    }

    @Test
    fun `test edge case - very long public key`() {
        TestSetup.init()
        val longKey = "-----BEGIN PUBLIC KEY-----\n" + "A".repeat(5000) + "\n-----END PUBLIC KEY-----"
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "KeyTest"
            firstName = "Test"
            publicKey = longKey
        }

        // Note: publicKey is not mapped in VCard, so it won't survive round-trip
        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])

        // Verify other fields work despite long key
        assertEquals("KeyTest", imported.name)
        assertEquals("Test", imported.firstName)
    }

    @Test
    fun `test form and title in prefixes`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "Müller"
            firstName = "Hans"
            form = FormOfAddress.MISTER
            title = "Dr."
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])

        assertEquals(FormOfAddress.MISTER, imported.form)
        assertEquals("Dr.", imported.title)
    }

    @Test
    fun `test birthName mapping`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "Müller"
            firstName = "Anna"
            birthName = "Schmidt"
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        assertTrue(vcardString.contains("X-BIRTHNAME:Schmidt"))

        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])
        assertEquals("Schmidt", imported.birthName)
    }

    @Test
    fun `test addressText2 fields mapping`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "Test"
            firstName = "User"
            addressText = "Hauptstraße 1"
            addressText2 = "Gebäude A"
            privateAddressText = "Nebenstraße 2"
            privateAddressText2 = "Apartment 5"
            postalAddressText = "Postfach 123"
            postalAddressText2 = "c/o Firma"
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])

        assertEquals("Gebäude A", imported.addressText2)
        assertEquals("Apartment 5", imported.privateAddressText2)
        assertEquals("c/o Firma", imported.postalAddressText2)
    }

    @Test
    fun `test communicationLanguage mapping`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "Test"
            firstName = "User"
            communicationLanguage = Locale.GERMAN
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])

        assertEquals(Locale.GERMAN, imported.communicationLanguage)
    }

    @Test
    fun `test positionText as TITLE property`() {
        TestSetup.init()
        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "Developer"
            firstName = "Senior"
            positionText = "Lead Software Engineer"
            organization = "Tech Corp"
            division = "Engineering"
        }

        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        assertTrue(vcardString.contains("TITLE:Lead Software Engineer"))
        // Should NOT be in ORG anymore (only organization and division)
        assertFalse(vcardString.contains("ORG:Tech Corp;Engineering;Lead Software Engineer"))

        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])
        assertEquals("Lead Software Engineer", imported.positionText)
    }

    @Test
    fun `test publicKey and fingerprint mapping`() {
        TestSetup.init()
        val testPublicKey = """
            -----BEGIN PGP PUBLIC KEY BLOCK-----

            mQENBGRzH8wBCADJqQj8Y8xvN8wKZ9nHqLNVJW1F5HtP9L+xC/dF8wX2Z9wY5vL3
            -----END PGP PUBLIC KEY BLOCK-----
        """.trimIndent()
        val testFingerprint = "ABAF 11C6 5A29 70B1 30AB E3C4 79BE 3E43 0041 1886"

        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "PGP User"
            firstName = "Test"
            publicKey = testPublicKey
            fingerprint = testFingerprint
        }

        // Test V4.0
        val vcardString = VCardUtils.buildVCardString(original, VCardVersion.V_4_0)
        assertTrue(vcardString.contains("KEY") && vcardString.contains("PGP"), "VCard should contain KEY with PGP type")
        assertTrue(vcardString.contains("X-PGP-FPR"), "VCard should contain X-PGP-FPR property")

        val imported = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardString)[0])
        assertEquals(testPublicKey, imported.publicKey)
        assertEquals(testFingerprint, imported.fingerprint)

        // Test V3.0
        val vcardStringV3 = VCardUtils.buildVCardString(original, VCardVersion.V_3_0)
        val importedV3 = VCardUtils.buildAddressDO(VCardUtils.parseVCardsFromString(vcardStringV3)[0])
        assertEquals(testPublicKey, importedV3.publicKey)
        assertEquals(testFingerprint, importedV3.fingerprint)
    }

    @Test
    fun `test parsing multiple contacts from single VCard string`() {
        TestSetup.init()
        val multipleVCards = """
            BEGIN:VCARD
            VERSION:4.0
            FN:John Doe
            N:Doe;John;;;
            EMAIL:john@example.com
            END:VCARD
            BEGIN:VCARD
            VERSION:4.0
            FN:Jane Smith
            N:Smith;Jane;;;
            EMAIL:jane@example.com
            END:VCARD
        """.trimIndent()

        val vcards = VCardUtils.parseVCardsFromString(multipleVCards)
        assertEquals(2, vcards.size)

        val address1 = VCardUtils.buildAddressDO(vcards[0])
        assertEquals("Doe", address1.name)
        assertEquals("John", address1.firstName)
        assertEquals("john@example.com", address1.email)

        val address2 = VCardUtils.buildAddressDO(vcards[1])
        assertEquals("Smith", address2.name)
        assertEquals("Jane", address2.firstName)
        assertEquals("jane@example.com", address2.email)
    }

    // Helper functions

    private fun createFullyPopulatedAddressDO(): AddressDO {
        return AddressDO().apply {
            // Basic fields
            uid = UUID.randomUUID().toString()
            contactStatus = ContactStatus.ACTIVE
            addressStatus = AddressStatus.UPTODATE
            name = "Müller"
            birthName = "Schmidt"
            firstName = "Hans"
            form = FormOfAddress.MISTER
            title = "Dr."

            // Business address
            addressText = "Hauptstraße 123"
            addressText2 = "Gebäude A"
            zipCode = "80333"
            city = "München"
            state = "Bayern"
            country = "Deutschland"

            // Private address
            privateAddressText = "Nebenstraße 456"
            privateAddressText2 = "Apartment 5"
            privateZipCode = "80335"
            privateCity = "München"
            privateState = "Bayern"
            privateCountry = "Deutschland"

            // Postal address
            postalAddressText = "Postfach 789"
            postalAddressText2 = "c/o Company"
            postalZipCode = "80331"
            postalCity = "München"
            postalState = "Bayern"
            postalCountry = "Deutschland"

            // Phone numbers
            businessPhone = "+49-89-12345678"
            mobilePhone = "+49-171-1234567"
            fax = "+49-89-12345679"
            privatePhone = "+49-89-98765432"
            privateMobilePhone = "+49-171-9876543"

            // Emails
            email = "hans.mueller@example.com"
            privateEmail = "hans.private@example.com"

            // Organization
            organization = "Example GmbH"
            division = "IT Department"
            positionText = "Senior Developer"

            // Other fields
            website = "https://www.example.com"
            birthday = LocalDate.of(1980, 6, 15)
            comment = "This is a test comment with special chars: äöüÄÖÜß"
            communicationLanguage = Locale.GERMAN

            // Note: publicKey and fingerprint are not mapped in VCard
            publicKey = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n-----END PUBLIC KEY-----"
            fingerprint = "AB:CD:EF:12:34:56"
        }
    }

    private fun assertAddressFieldsEqual(expected: AddressDO, actual: AddressDO) {
        // Basic fields that are mapped
        assertEquals(expected.name, actual.name, "name mismatch")
        assertEquals(expected.firstName, actual.firstName, "firstName mismatch")
        assertEquals(expected.birthName, actual.birthName, "birthName mismatch")
        assertEquals(expected.title, actual.title, "title mismatch")
        assertEquals(expected.form, actual.form, "form mismatch")

        // Business address
        assertEquals(expected.addressText, actual.addressText, "addressText mismatch")
        assertEquals(expected.addressText2, actual.addressText2, "addressText2 mismatch")
        assertEquals(expected.zipCode, actual.zipCode, "zipCode mismatch")
        assertEquals(expected.city, actual.city, "city mismatch")
        assertEquals(expected.state, actual.state, "state mismatch")
        assertEquals(expected.country, actual.country, "country mismatch")

        // Private address
        assertEquals(expected.privateAddressText, actual.privateAddressText, "privateAddressText mismatch")
        assertEquals(expected.privateAddressText2, actual.privateAddressText2, "privateAddressText2 mismatch")
        assertEquals(expected.privateZipCode, actual.privateZipCode, "privateZipCode mismatch")
        assertEquals(expected.privateCity, actual.privateCity, "privateCity mismatch")
        assertEquals(expected.privateState, actual.privateState, "privateState mismatch")
        assertEquals(expected.privateCountry, actual.privateCountry, "privateCountry mismatch")

        // Postal address
        assertEquals(expected.postalAddressText, actual.postalAddressText, "postalAddressText mismatch")
        assertEquals(expected.postalAddressText2, actual.postalAddressText2, "postalAddressText2 mismatch")
        assertEquals(expected.postalZipCode, actual.postalZipCode, "postalZipCode mismatch")
        assertEquals(expected.postalCity, actual.postalCity, "postalCity mismatch")
        assertEquals(expected.postalState, actual.postalState, "postalState mismatch")
        assertEquals(expected.postalCountry, actual.postalCountry, "postalCountry mismatch")

        // Phones
        assertEquals(expected.businessPhone, actual.businessPhone, "businessPhone mismatch")
        assertEquals(expected.mobilePhone, actual.mobilePhone, "mobilePhone mismatch")
        assertEquals(expected.fax, actual.fax, "fax mismatch")
        assertEquals(expected.privatePhone, actual.privatePhone, "privatePhone mismatch")
        assertEquals(expected.privateMobilePhone, actual.privateMobilePhone, "privateMobilePhone mismatch")

        // Emails
        assertEquals(expected.email, actual.email, "email mismatch")
        assertEquals(expected.privateEmail, actual.privateEmail, "privateEmail mismatch")

        // Organization
        assertEquals(expected.organization, actual.organization, "organization mismatch")
        assertEquals(expected.division, actual.division, "division mismatch")
        assertEquals(expected.positionText, actual.positionText, "positionText mismatch")

        // Other
        assertEquals(expected.website, actual.website, "website mismatch")
        assertEquals(expected.birthday, actual.birthday, "birthday mismatch")
        assertEquals(expected.comment?.trim(), actual.comment?.trim(), "comment mismatch")
        assertEquals(expected.communicationLanguage, actual.communicationLanguage, "communicationLanguage mismatch")

        // PGP fields
        assertEquals(expected.publicKey, actual.publicKey, "publicKey mismatch")
        assertEquals(expected.fingerprint, actual.fingerprint, "fingerprint mismatch")

        // Note: The following fields are NOT mapped in VCard and won't survive round-trip:
        // - contactStatus
        // - addressStatus
    }

    private fun testImageTypeRoundTrip(imageType: ImageType) {
        val testImageBytes = Base64.getDecoder().decode(TINY_PNG_BASE64)

        val original = AddressDO().apply {
            uid = UUID.randomUUID().toString()
            name = "ImageTest"
            firstName = "Test"
        }

        original.setTransientImage(AddressImageDO().apply {
            image = testImageBytes
            this.imageType = imageType
        })

        val vcard = VCardUtils.buildVCard(original, null, null)
        val imported = VCardUtils.buildAddressDO(vcard)

        val importedImage = imported.transientImage
        assertNotNull(importedImage, "Image should be imported for type $imageType")
        assertEquals(imageType, importedImage?.imageType, "Image type should be preserved")
    }

    // Test VCard strings

    private val EXAMPLE_VCF = """
        BEGIN:VCARD
        VERSION:3.0
        FN:John Doe
        N:Doe;John;;;
        ADR;TYPE=HOME:;;123 Main Street;Anytown;CA;12345;USA
        TEL;TYPE=CELL:+1-123-456-7890
        EMAIL:john.doe@example.com
        BDAY:1992-07-11
        END:VCARD""".trimIndent()

    private val COMPLEX_VCARD = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Dr. Maria Schmidt
        N:Schmidt;Maria;;Dr.;
        ORG:Acme Corp;Engineering;Senior Engineer
        ADR;TYPE=WORK:;;Hauptstraße 123;München;Bayern;80333;Deutschland
        TEL;TYPE=WORK:+49-89-1234567
        TEL;TYPE=CELL:+49-171-9876543
        TEL;TYPE=FAX,WORK:+49-89-1234568
        EMAIL;TYPE=WORK:maria.schmidt@example.com
        URL:https://www.example.com
        BDAY:19850515
        NOTE:Senior software engineer
        END:VCARD
    """.trimIndent()

    private val VCARD_MULTIPLE_ADDRESSES = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Max Mustermann
        N:Mustermann;Max;;;
        ADR;TYPE=WORK:;;Business Street 1;Berlin;Berlin;10115;Germany
        ADR;TYPE=HOME:;;Home Street 42;München;Bayern;80331;Germany
        ADR;TYPE=POSTAL:;;PO Box 5678;München;Bayern;80333;Germany
        END:VCARD
    """.trimIndent()

    private val VCARD_MULTIPLE_PHONES = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Phone Test
        N:Test;Phone;;;
        TEL;TYPE=WORK:+49-89-1111111
        TEL;TYPE=CELL:+49-171-2222222
        TEL;TYPE=FAX,WORK:+49-89-3333333
        TEL;TYPE=HOME:+49-89-4444444
        TEL;TYPE=CELL,HOME:+49-171-5555555
        END:VCARD
    """.trimIndent()

    private val VCARD_MULTIPLE_EMAILS = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Email Test
        N:Test;Email;;;
        EMAIL;TYPE=WORK:work@example.com
        EMAIL;TYPE=HOME:private@example.com
        END:VCARD
    """.trimIndent()

    // 1x1 transparent PNG in Base64 (smallest valid PNG)
    private val TINY_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
}
