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

class PersonNameParserTest {

    @Test
    fun `test parse simple name without title`() {
        val result = PersonNameParser.parse("Max Mustermann")

        assertEquals("Mustermann", result.name)
        assertEquals("Max", result.firstName)
        assertNull(result.formOfAddress)
        assertNull(result.formOfAddressAsEnum)
        assertTrue(result.titles.isEmpty())
    }

    @Test
    fun `test parse name with Dipl-Phys title`() {
        val result = PersonNameParser.parse("Dipl.-Phys. Max Mustermann")

        assertEquals("Mustermann", result.name)
        assertEquals("Max", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertTrue(result.titles.any { it.contains("Dipl") && it.contains("Phys") })
    }

    @Test
    fun `test parse name with DiplPhys title without hyphen`() {
        val result = PersonNameParser.parse("Dipl.Phys. Max Mustermann")

        assertEquals("Mustermann", result.name)
        assertEquals("Max", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertTrue(result.titles.any { it.contains("Dipl") && it.contains("Phys") })
    }

    @Test
    fun `test parse name with Dipl-Ing title`() {
        val result = PersonNameParser.parse("Dipl.-Ing. Hans Weber")

        assertEquals("Weber", result.name)
        assertEquals("Hans", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertTrue(result.titles.any { it.contains("Dipl") && it.contains("Ing") })
    }

    @Test
    fun `test parse name with Dipl-Kfm title`() {
        val result = PersonNameParser.parse("Dipl.-Kfm. Peter Schmidt")

        assertEquals("Schmidt", result.name)
        assertEquals("Peter", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertTrue(result.titles.any { it.contains("Dipl") && it.contains("Kfm") })
    }

    @Test
    fun `test parse name with multiple titles`() {
        val result = PersonNameParser.parse("Prof. Dr. med. Hans Weber")

        assertEquals("Weber", result.name)
        assertEquals("Hans", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(3, result.titles.size)
        assertTrue(result.titles.any { it.equals("Prof.", ignoreCase = true) })
        assertTrue(result.titles.any { it.equals("Dr.", ignoreCase = true) })
        assertTrue(result.titles.any { it.equals("med.", ignoreCase = true) })
    }

    @Test
    fun `test parse name with Dr rer nat title`() {
        val result = PersonNameParser.parse("Dr. rer. nat. Anna Schmidt")

        assertEquals("Schmidt", result.name)
        assertEquals("Anna", result.firstName)
        assertNull(result.formOfAddress)
        assertTrue(result.titles.size >= 1)
        assertTrue(result.titles.any { it.equals("Dr.", ignoreCase = true) })
        // rer. nat. wird als separater Titel erkannt oder mit Dr. kombiniert
        assertTrue(result.titles.any { it.contains("rer") || it.contains("nat") } || result.titles.size >= 2)
    }

    @Test
    fun `test parse name with form of address Herr`() {
        val result = PersonNameParser.parse("Herr Dr.-Ing. Peter Müller")

        assertEquals("Müller", result.name)
        assertEquals("Peter", result.firstName)
        assertEquals("Herr", result.formOfAddress)
        assertEquals(FormOfAddress.MISTER, result.formOfAddressAsEnum)
        assertTrue(result.titles.isNotEmpty())
        // Dr.-Ing. wird als ein oder zwei Titel erkannt
        assertTrue(result.titles.any { it.contains("Dr") } && result.titles.any { it.contains("Ing") }
            || result.titles.any { it.contains("Dr") && it.contains("Ing") })
    }

    @Test
    fun `test parse name with form of address Frau`() {
        val result = PersonNameParser.parse("Frau Prof. Dr. Lisa Schneider")

        assertEquals("Schneider", result.name)
        assertEquals("Lisa", result.firstName)
        assertEquals("Frau", result.formOfAddress)
        assertEquals(FormOfAddress.MISS, result.formOfAddressAsEnum)
        assertTrue(result.titles.size >= 2)
        assertTrue(result.titles.any { it.equals("Prof.", ignoreCase = true) })
        assertTrue(result.titles.any { it.equals("Dr.", ignoreCase = true) })
    }

    @Test
    fun `test parse name with English form of address Mr`() {
        val result = PersonNameParser.parse("Mr. John Smith")

        assertEquals("Smith", result.name)
        assertEquals("John", result.firstName)
        assertEquals("Mr", result.formOfAddress)
        assertEquals(FormOfAddress.MISTER, result.formOfAddressAsEnum)
        assertTrue(result.titles.isEmpty())
    }

    @Test
    fun `test parse name with English form of address Mrs`() {
        val result = PersonNameParser.parse("Mrs. Jane Doe")

        assertEquals("Doe", result.name)
        assertEquals("Jane", result.firstName)
        assertEquals("Mrs", result.formOfAddress)
        assertEquals(FormOfAddress.MISS, result.formOfAddressAsEnum)
        assertTrue(result.titles.isEmpty())
    }

    @Test
    fun `test parse name with PhD title`() {
        val result = PersonNameParser.parse("PhD Robert Fischer")

        assertEquals("Fischer", result.name)
        assertEquals("Robert", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertEquals("PhD", result.titles[0])
    }

    @Test
    fun `test parse name with MBA title`() {
        val result = PersonNameParser.parse("MBA Sarah Klein")

        assertEquals("Klein", result.name)
        assertEquals("Sarah", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertEquals("MBA", result.titles[0])
    }

    @Test
    fun `test parse name with MSc title`() {
        val result = PersonNameParser.parse("M.Sc. Thomas Wagner")

        assertEquals("Wagner", result.name)
        assertTrue(result.firstName.contains("Thomas"))
        assertNull(result.formOfAddress)
        assertTrue(result.titles.isNotEmpty())
        assertTrue(result.titles.any { it.contains("MSc") || it.contains("M.Sc") })
    }

    @Test
    fun `test parse name with multiple first names`() {
        val result = PersonNameParser.parse("Dr. Hans Peter Weber")

        assertEquals("Weber", result.name)
        assertEquals("Hans Peter", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertEquals("Dr.", result.titles[0])
    }

    @Test
    fun `test parse name with only last name`() {
        val result = PersonNameParser.parse("Mustermann")

        assertEquals("Mustermann", result.name)
        assertEquals("", result.firstName)
        assertNull(result.formOfAddress)
        assertTrue(result.titles.isEmpty())
    }

    @Test
    fun `test parse name with title and only last name`() {
        val result = PersonNameParser.parse("Dr. Mustermann")

        assertEquals("Mustermann", result.name)
        assertEquals("", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertEquals("Dr.", result.titles[0])
    }

    @Test
    fun `test parse complex German name with multiple titles`() {
        val result = PersonNameParser.parse("Herr Prof. Dr. Dr. h.c. Klaus Dieter Müller-Schmidt")

        assertEquals("Müller-Schmidt", result.name)
        assertEquals("Klaus Dieter", result.firstName)
        assertEquals("Herr", result.formOfAddress)
        assertTrue(result.titles.size >= 3)
        assertTrue(result.titles.any { it.equals("Prof.", ignoreCase = true) })
        assertTrue(result.titles.count { it.equals("Dr.", ignoreCase = true) } >= 1)
        assertTrue(result.titles.any { it.contains("h") && it.contains("c") })
    }

    @Test
    fun `test parse name with Dipl space variant`() {
        val result = PersonNameParser.parse("Dipl. Kfm. Max Mustermann")

        assertEquals("Mustermann", result.name)
        assertTrue(result.firstName.contains("Max"))
        assertNull(result.formOfAddress)
        assertTrue(result.titles.isNotEmpty())
        // Dipl. Kfm. kann als ein oder zwei Titel erkannt werden
        assertTrue(result.titles.any { it.contains("Dipl") } || result.titles.any { it.contains("Kfm") })
    }

    @Test
    fun `test parse empty string`() {
        val result = PersonNameParser.parse("")

        assertEquals("", result.name)
        assertEquals("", result.firstName)
        assertNull(result.formOfAddress)
        assertTrue(result.titles.isEmpty())
    }

    @Test
    fun `test parse name with extra whitespace`() {
        val result = PersonNameParser.parse("  Dr.   Max    Mustermann  ")

        assertEquals("Mustermann", result.name)
        assertEquals("Max", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(1, result.titles.size)
        assertEquals("Dr.", result.titles[0])
    }

    @Test
    fun `test parse name with Prof Dr combination`() {
        val result = PersonNameParser.parse("Prof. Dr. Anna Schmidt")

        assertEquals("Schmidt", result.name)
        assertEquals("Anna", result.firstName)
        assertNull(result.formOfAddress)
        assertEquals(2, result.titles.size)
        assertTrue(result.titles.any { it.equals("Prof.", ignoreCase = true) })
        assertTrue(result.titles.any { it.equals("Dr.", ignoreCase = true) })
    }

    @Test
    fun `test parse name with Dr habil title`() {
        val result = PersonNameParser.parse("Dr. habil. Peter Müller")

        assertEquals("Müller", result.name)
        assertEquals("Peter", result.firstName)
        assertNull(result.formOfAddress)
        assertTrue(result.titles.size >= 2)
        assertTrue(result.titles.any { it.equals("Dr.", ignoreCase = true) })
        assertTrue(result.titles.any { it.contains("habil") })
    }

    @Test
    fun `test parse Spanish form of address`() {
        val result = PersonNameParser.parse("Sr. Carlos García")

        assertEquals("García", result.name)
        assertEquals("Carlos", result.firstName)
        assertEquals("Sr", result.formOfAddress)
        assertEquals(FormOfAddress.MISTER, result.formOfAddressAsEnum)
        assertTrue(result.titles.isEmpty())
    }

    @Test
    fun `test parse French form of address`() {
        val result = PersonNameParser.parse("M. Pierre Dupont")

        assertEquals("Dupont", result.name)
        assertEquals("Pierre", result.firstName)
        assertEquals("M", result.formOfAddress)
        assertEquals(FormOfAddress.MISTER, result.formOfAddressAsEnum)
        assertTrue(result.titles.isEmpty())
    }
}
