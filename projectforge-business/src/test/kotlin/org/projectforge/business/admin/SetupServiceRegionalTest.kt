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

package org.projectforge.business.admin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetupServiceRegionalTest {
    private val service = SetupService()

    @Test
    fun `valid and blank regional settings are accepted`() {
        assertTrue(service.validateRegional(RegionalSettings()).isEmpty())
        assertTrue(
            service.validateRegional(
                RegionalSettings("chf", "CHF", "de", "H24", "SUNDAY")
            ).isEmpty()
        )
        assertTrue(service.validateRegional(RegionalSettings(currencySymbol = "€")).isEmpty())
    }

    @Test
    fun `invalid regional settings are refused with their field`() {
        fun field(settings: RegionalSettings) = service.validateRegional(settings).map { it.field }
        assertEquals(listOf("currency"), field(RegionalSettings(currency = "XXXX")))
        assertEquals(listOf("currencySymbol"), field(RegionalSettings(currencySymbol = "€\nserver.port=1")))
        assertEquals(listOf("currencySymbol"), field(RegionalSettings(currencySymbol = "a\\u0041")))
        assertEquals(listOf("currencySymbol"), field(RegionalSettings(currencySymbol = "123456")))
        assertEquals(listOf("defaultLocale"), field(RegionalSettings(defaultLocale = "fr")))
        assertEquals(listOf("defaultTimeNotation"), field(RegionalSettings(defaultTimeNotation = "H25")))
        assertEquals(listOf("defaultFirstDayOfWeek"), field(RegionalSettings(defaultFirstDayOfWeek = "Monday")))
    }
}
