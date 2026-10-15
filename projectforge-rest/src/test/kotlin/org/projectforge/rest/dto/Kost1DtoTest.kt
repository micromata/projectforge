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

package org.projectforge.rest.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.test.AbstractTestBase

/**
 * The Kost1 DTO used to copy only the minimal fields (id, deleted, displayName), so the cost1 list and
 * edit page received four zeros and no formatted number at all. These tests pin down that every field a
 * page shows is copied.
 */
class Kost1DtoTest : AbstractTestBase() {

    @Test
    fun `copyFrom copies every field the list and the edit page show`() {
        val dto = Kost1()
        dto.copyFrom(createKost1())

        assertEquals(42L, dto.id)
        assertEquals(6, dto.nummernkreis)
        assertEquals(100, dto.bereich)
        assertEquals(1, dto.teilbereich)
        assertEquals(2, dto.endziffer)
        assertEquals(KostentraegerStatus.ACTIVE, dto.kostentraegerStatus)
        assertEquals("Test cost unit", dto.description)
        // Computed by the entity's getter, which has no backing field - so copyFrom has to assign it.
        assertEquals("6.100.01.02", dto.formattedNumber)
        assertEquals("6.100.01.02: Test cost unit", dto.displayName)
    }

    @Test
    fun `copyFromMinimal carries the number too, so an embedded Kost1 is readable`() {
        val dto = Kost1()
        dto.copyFromMinimal(createKost1())

        assertEquals(42L, dto.id)
        assertEquals(6, dto.nummernkreis)
        assertEquals(100, dto.bereich)
        assertEquals(1, dto.teilbereich)
        assertEquals(2, dto.endziffer)
        assertEquals(KostentraegerStatus.ACTIVE, dto.kostentraegerStatus)
        assertEquals("Test cost unit", dto.description)
        assertEquals("6.100.01.02", dto.formattedNumber)
        assertEquals("6.100.01.02: Test cost unit", dto.displayName)
    }

    @Test
    fun `the Kost1DO constructor is the minimal copy`() {
        val dto = Kost1(createKost1())
        assertEquals(6, dto.nummernkreis)
        assertEquals("6.100.01.02", dto.formattedNumber)
    }

    @Test
    fun `a non-active status is marked in the display name`() {
        val kost1DO = createKost1()
        kost1DO.kostentraegerStatus = KostentraegerStatus.ENDED
        kost1DO.description = "Ended one"
        val dto = Kost1()
        dto.copyFrom(kost1DO)
        // KostFormatter.FormatType.TEXT wraps the translated status in asterisks and puts it before the
        // description. The status word itself is the test locale's, so only the shape is asserted here —
        // and only up to the description, because the whole thing is then cut at ABBREVIATION_LENGTH (30)
        // and how much of the description survives depends on the length of that locale's status word.
        val displayName = dto.displayName!!
        assertTrue(
            displayName.matches(Regex("""6\.100\.01\.02: \*\S+\* \S+.*""")),
            "Expected the status in asterisks before the description, but got '$displayName'.",
        )
    }

    @Test
    fun `the display name is abbreviated, as the legacy pages show it`() {
        val kost1DO = createKost1()
        kost1DO.description = "A description that is clearly far beyond sixty characters in length"
        val dto = Kost1()
        dto.copyFrom(kost1DO)
        // FormatType.TEXT cuts at KostFormatter.ABBREVIATION_LENGTH (60).
        assertEquals("6.100.01.02: A description that is clearly far beyond six...", dto.displayName)
    }

    @Test
    fun `copyTo does not write the computed number back`() {
        val dto = Kost1()
        dto.copyFrom(createKost1())
        val dest = Kost1DO()
        dto.copyTo(dest)

        assertEquals(6, dest.nummernkreis)
        assertEquals(100, dest.bereich)
        assertEquals(1, dest.teilbereich)
        assertEquals(2, dest.endziffer)
        assertEquals("Test cost unit", dest.description)
        // Derived from the four numbers, never from the dto's field: the entity has no setter for it.
        assertEquals("6.100.01.02", dest.formattedNumber)
    }

    private fun createKost1(): Kost1DO {
        val kost1 = Kost1DO()
        kost1.id = 42L
        kost1.nummernkreis = 6
        kost1.bereich = 100
        kost1.teilbereich = 1
        kost1.endziffer = 2
        kost1.kostentraegerStatus = KostentraegerStatus.ACTIVE
        kost1.description = "Test cost unit"
        return kost1
    }
}
