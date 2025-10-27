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

package org.projectforge.business.address

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AddressTextParserTest {

    @Test
    fun `test parse full German signature with title`() {
        val text = """
            Dipl.-Kfm. Max Mustermann
            Senior Manager Web & Data Analytics

            Example GmbH
            Musterstraße 1-5
            12345 Musterstadt

            Tel: +49 30 12345 678-90
            m.mustermann@example.com
            www.example.com
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Dipl.-Kfm.", result.title)
        assertEquals("Max", result.firstName)
        assertEquals("Mustermann", result.name)
        assertEquals("Senior Manager Web & Data Analytics", result.positionText)
        assertEquals("Example GmbH", result.organization)
        assertEquals("Musterstraße 1-5", result.addressText)
        assertEquals("12345", result.zipCode)
        assertEquals("Musterstadt", result.city)
        assertEquals("+49 30 12345 678-90", result.businessPhone) // Normalized format
        assertEquals("m.mustermann@example.com", result.email)
        assertEquals("www.example.com", result.website)
    }

    @Test
    fun `test parse English signature`() {
        val text = """
            Dr. Jane Smith
            Chief Technology Officer

            TechCorp Inc.
            123 Main Street
            94105 San Francisco

            Phone: +1 415 555-1234
            jane.smith@techcorp.com
            https://www.techcorp.com
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Dr.", result.title)
        assertEquals("Jane", result.firstName)
        assertEquals("Smith", result.name)
        assertEquals("Chief Technology Officer", result.positionText)
        assertEquals("TechCorp Inc.", result.organization)
        assertNotNull(result.addressText)
        assertEquals("94105", result.zipCode)
        assertEquals("San Francisco", result.city)
        assertEquals("+1 415 555-1234", result.businessPhone)
        assertEquals("jane.smith@techcorp.com", result.email)
        assertEquals("https://www.techcorp.com", result.website)
    }

    @Test
    fun `test parse signature without title`() {
        val text = """
            Anna Müller
            Marketing Manager

            Innovate AG
            Hauptstraße 42
            80331 München

            Tel: +49 89 12345678
            a.mueller@innovate.de
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertNull(result.title)
        assertEquals("Anna", result.firstName)
        assertEquals("Müller", result.name)
        assertEquals("Marketing Manager", result.positionText)
        assertEquals("Innovate AG", result.organization)
        assertEquals("Hauptstraße 42", result.addressText)
        assertEquals("80331", result.zipCode)
        assertEquals("München", result.city)
        assertEquals("+49 89 12345678", result.businessPhone) // Normalized format
        assertEquals("a.mueller@innovate.de", result.email)
    }

    @Test
    fun `test parse signature with multiple phone numbers`() {
        val text = """
            Peter Schmidt
            Sales Director

            BusinessSoft GmbH
            Lindenweg 7
            10115 Berlin

            Tel: +49 30 98765432
            Mobil: +49 170 1234567
            Fax: +49 30 98765433
            p.schmidt@businesssoft.com
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Peter", result.firstName)
        assertEquals("Schmidt", result.name)
        assertEquals("Sales Director", result.positionText)
        assertEquals("BusinessSoft GmbH", result.organization)
        assertEquals("+49 30 98765432", result.businessPhone) // Normalized format
        assertEquals("+49 170 1234567", result.mobilePhone) // Normalized format
        assertEquals("+49 30 98765433", result.fax) // Normalized format
        assertEquals("p.schmidt@businesssoft.com", result.email)
    }

    @Test
    fun `test parse minimal signature - only name and email`() {
        val text = """
            Thomas Weber
            t.weber@company.de
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Thomas", result.firstName)
        assertEquals("Weber", result.name)
        assertEquals("t.weber@company.de", result.email)
        assertNull(result.positionText)
        assertNull(result.organization)
        assertNull(result.businessPhone)
    }

    @Test
    fun `test parse signature with Prof title`() {
        val text = """
            Prof. Dr. Lisa Schneider
            Geschäftsführerin

            University Ltd.
            Universitätsplatz 1
            69120 Heidelberg

            Tel: +49 6221 54-7890
            l.schneider@university.de
            www.university.de
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Prof.", result.title)
        assertEquals("Dr.", result.firstName) // Will parse "Dr." as first name
        assertEquals("Lisa Schneider", result.name)
        assertEquals("l.schneider@university.de", result.email)
    }

    @Test
    fun `test parse signature with e-V company`() {
        val text = """
            Michael Bauer
            Vorstand

            Förderverein e.V.
            Parkstraße 15
            50667 Köln

            Tel: 0221 123456
            info@foerderverein.de
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Michael", result.firstName)
        assertEquals("Bauer", result.name)
        assertEquals("Vorstand", result.positionText)
        assertEquals("Förderverein e.V.", result.organization)
        assertEquals("50667", result.zipCode)
        assertEquals("Köln", result.city)
    }

    @Test
    fun `test parse empty text`() {
        val result = AddressTextParser.parseAddressText("")

        assertNull(result.firstName)
        assertNull(result.name)
        assertNull(result.email)
        assertNull(result.organization)
    }

    @Test
    fun `test parse signature with compound street name`() {
        val text = """
            Sarah Klein
            Project Manager

            Digital Solutions GmbH
            Karl-Marx-Straße 100-102
            12043 Berlin

            Tel: +49 30 555 1234
            s.klein@digitalsolutions.de
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Sarah", result.firstName)
        assertEquals("Klein", result.name)
        assertEquals("Project Manager", result.positionText)
        assertEquals("Digital Solutions GmbH", result.organization)
        assertEquals("Karl-Marx-Straße 100-102", result.addressText)
        assertEquals("12043", result.zipCode)
        assertEquals("Berlin", result.city)
    }

    @Test
    fun `test parse signature with website without www`() {
        val text = """
            Robert Fischer
            CTO

            StartupHub UG
            Innovationsweg 9
            01099 Dresden

            robert@startuphub.io
            startuphub.io
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Robert", result.firstName)
        assertEquals("Fischer", result.name)
        assertEquals("CTO", result.positionText)
        assertEquals("StartupHub UG", result.organization)
        assertEquals("robert@startuphub.io", result.email)
        assertEquals("startuphub.io", result.website)
    }

    @Test
    fun `test parse signature with i A and ampersand in position`() {
        val text = """
            i. A. Sandra Berta, Menschen & Kultur

            Example GmbH
            Marie-Calm Str. 1-5, D-34131 Kassel
            Tel: +49 561 / 31 67 93 - 0
            Fax: +49 561 / 31 67 93- 11
            mailto: s.berta@example.de
            https://www.example.de

            AG Kassel HRB 7370
            Geschäftsführung:
            Max Mustermann
            John Doe
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        // "i. A." should be recognized as title or part of position
        assertEquals("Sandra", result.firstName)
        assertEquals("Berta", result.name)
        assertEquals("Menschen & Kultur", result.positionText)
        assertEquals("Example GmbH", result.organization)
        assertEquals("Marie-Calm Str. 1-5", result.addressText)
        assertEquals("34131", result.zipCode)
        assertEquals("Kassel", result.city)
        assertEquals("+49 561 31 67 93-0", result.businessPhone) // Normalized format
        assertEquals("+49 561 31 67 93-11", result.fax) // Normalized format
        assertEquals("s.berta@example.de", result.email)
        assertEquals("https://www.example.de", result.website)
    }

    @Test
    fun `test parse signature with multiline position and logo text`() {
        val text = """
            Thomas Wagner
            Team Lead & Marketing Manager

            TechSolutions Logo aktuell


            TechSolutions GmbH
            Member of GlobalTech Group

            Universitätsplatz 12
            34127 Kassel
            GERMANY

            Phone: +49 561 123456-789
            E-Mail: thomas.wagner@techsolutions.de
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Thomas", result.firstName)
        assertEquals("Wagner", result.name)
        assertEquals("Team Lead & Marketing Manager", result.positionText)
        assertEquals("TechSolutions GmbH", result.organization)
        assertEquals("Universitätsplatz 12", result.addressText)
        assertEquals("34127", result.zipCode)
        assertEquals("Kassel", result.city)
        assertEquals("+49 561 123456-789", result.businessPhone)
        assertEquals("thomas.wagner@techsolutions.de", result.email)
    }

    @Test
    fun `test parse Swiss address with multiple empty lines`() {
        val text = """
            Hans Mueller






            R2D2 AG
            Peterstrasse 86
            6312 Steinhausen
            Schweiz / Switzerland

            Tel        +41 41 234 56 78
            Mobile  +49 177 123 45 67
            E-Mail    hans.mueller@r2d2.group
            Web       www.r2d2.group
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Hans", result.firstName)
        assertEquals("Mueller", result.name)
        assertEquals("R2D2 AG", result.organization)
        assertEquals("Peterstrasse 86", result.addressText)
        assertEquals("6312", result.zipCode)
        assertEquals("Steinhausen", result.city)
        assertEquals("Schweiz", result.country) // First part before /
        assertEquals("+41 41 234 56 78", result.businessPhone) // Normalized format
        assertEquals("+49 177 123 45 67", result.mobilePhone) // Normalized format
        assertEquals("hans.mueller@r2d2.group", result.email)
        assertEquals("www.r2d2.group", result.website)
    }

    @Test
    fun `test parse German company address with department info`() {
        val text = """
            Dr. Hans Weber
            Abteilungsleiter
            Abteilung 1120, ADM GK XYZ

            Beispiel AG
            SNL IT P&P
            Musterstraße 7
            53113 Bonn

            Telefon  +49 228 123 45678
            Mobil     +49 170 123456

            hans.weber@beispiel.de
            www.beispiel.de
            Beispiel Gruppe
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Dr.", result.title)
        assertEquals("Hans", result.firstName)
        assertEquals("Weber", result.name)
        assertEquals("Abteilungsleiter", result.positionText)
        // "Abteilung 1120, ADM GK XYZ" is parsed as addressText (appears after position, before org)
        // This is a limitation - department info gets interpreted as address
        assertEquals("Beispiel AG", result.organization)
        assertEquals("Musterstraße 7", result.addressText)
        assertEquals("53113", result.zipCode)
        assertEquals("Bonn", result.city)
        assertEquals("+49 228 123 45678", result.businessPhone) // Normalized format
        assertEquals("+49 170 123456", result.mobilePhone) // Normalized format
        assertEquals("hans.weber@beispiel.de", result.email)
        assertEquals("www.beispiel.de", result.website)
    }

    @Test
    fun `test parse without names`() {
        val text = """
            Musterstraße 7
            53113 Bonn

            Telefon  +49 228 123 45678
            Mobil     +49 170 123456

            hans.weber@beispiel.de
            www.beispiel.de
            Beispiel Gruppe
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        assertEquals("Musterstraße 7", result.addressText)
        assertEquals("53113", result.zipCode)
        assertEquals("Bonn", result.city)
        assertEquals("+49 228 123 45678", result.businessPhone) // Normalized format
        assertEquals("+49 170 123456", result.mobilePhone) // Normalized format
        assertEquals("hans.weber@beispiel.de", result.email)
        assertEquals("www.beispiel.de", result.website)
    }

    @Test
    fun `test parse address only without name or phone`() {
        val text = """
            Hauptstraße 42
            80331 München
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        // Name should be null
        assertNull(result.firstName)
        assertNull(result.name)

        // Address should be parsed
        assertEquals("Hauptstraße 42", result.addressText)
        assertEquals("80331", result.zipCode)
        assertEquals("München", result.city)

        // Contact info should be null
        assertNull(result.businessPhone)
        assertNull(result.email)
    }

    @Test
    fun `test parse phone numbers only`() {
        val text = """
            Tel: +49 30 12345678
            Mobil: +49 170 1234567
            Fax: +49 30 12345679
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        // Name should be null
        assertNull(result.firstName)
        assertNull(result.name)

        // Phone numbers should be parsed
        assertEquals("+49 30 12345678", result.businessPhone)
        assertEquals("+49 170 1234567", result.mobilePhone)
        assertEquals("+49 30 12345679", result.fax)

        // Address should be null
        assertNull(result.addressText)
        assertNull(result.zipCode)
        assertNull(result.city)
    }

    @Test
    fun `test parse address with phone numbers without name`() {
        val text = """
            Hauptstraße 42
            80331 München

            Tel: +49 89 12345678
            info@example.de
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        // Name should be null
        assertNull(result.firstName)
        assertNull(result.name)

        // Address should be parsed
        assertEquals("Hauptstraße 42", result.addressText)
        assertEquals("80331", result.zipCode)
        assertEquals("München", result.city)

        // Contact info should be parsed
        assertEquals("+49 89 12345678", result.businessPhone)
        assertEquals("info@example.de", result.email)
    }

    @Test
    fun `test parse organization with address without person name`() {
        val text = """
            Example GmbH
            Musterstraße 1-5
            12345 Musterstadt

            Tel: +49 30 12345678
            kontakt@example.de
            www.example.de
        """.trimIndent()

        val result = AddressTextParser.parseAddressText(text)

        // Person name should be null
        assertNull(result.firstName)
        assertNull(result.name)
        assertNull(result.title)

        // Organization should be parsed
        assertEquals("Example GmbH", result.organization)

        // Address should be parsed
        assertEquals("Musterstraße 1-5", result.addressText)
        assertEquals("12345", result.zipCode)
        assertEquals("Musterstadt", result.city)

        // Contact info should be parsed
        assertEquals("+49 30 12345678", result.businessPhone)
        assertEquals("kontakt@example.de", result.email)
        assertEquals("www.example.de", result.website)
    }
}
