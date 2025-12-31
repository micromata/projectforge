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

package org.projectforge.plugins.marketing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AddressCampaignDOTest {

    @Test
    fun `getValuesArray should handle various input formats correctly`() {
        // Null and blank inputs
        assertNull(AddressCampaignDO.getValuesArray(null))
        assertNull(AddressCampaignDO.getValuesArray(""))
        assertNull(AddressCampaignDO.getValuesArray("   "))
        assertNull(AddressCampaignDO.getValuesArray("\t"))

        // Simple split by semicolon
        assertArrayEquals(arrayOf("eins", "zwei", "drei"), AddressCampaignDO.getValuesArray("eins;zwei;drei"))

        // Trim whitespace around values
        assertArrayEquals(arrayOf("eins", "zwei", "drei"), AddressCampaignDO.getValuesArray("eins; zwei ; drei"))

        // Preserve spaces within values
        assertArrayEquals(arrayOf("eins", "zwei Kirschen", "drei"), AddressCampaignDO.getValuesArray("eins; zwei Kirschen;drei"))

        // Filter out empty entries
        assertArrayEquals(arrayOf("eins", "zwei", "drei"), AddressCampaignDO.getValuesArray("eins;;zwei;;;drei"))

        // Filter out whitespace-only entries
        assertArrayEquals(arrayOf("eins", "zwei", "drei"), AddressCampaignDO.getValuesArray("eins; ;zwei;  ;drei"))

        // Single value
        assertArrayEquals(arrayOf("eins"), AddressCampaignDO.getValuesArray("eins"))

        // Leading and trailing semicolons
        assertArrayEquals(arrayOf("eins", "zwei"), AddressCampaignDO.getValuesArray(";eins;zwei;"))

        // Only semicolons
        assertNull(AddressCampaignDO.getValuesArray(";;;"))

        // Complex real-world example
        assertArrayEquals(
            arrayOf("premium", "normal", "zwei Kirschen", "no"),
            AddressCampaignDO.getValuesArray("premium; normal; zwei Kirschen;no;;")
        )

        // Test via property
        val campaign = AddressCampaignDO()
        campaign.values = "Ja;Nein;Vielleicht"
        assertArrayEquals(arrayOf("Ja", "Nein", "Vielleicht"), campaign.valuesArray)

        campaign.values = null
        assertNull(campaign.valuesArray)
    }
}
