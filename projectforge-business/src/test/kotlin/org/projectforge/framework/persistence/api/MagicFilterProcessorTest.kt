/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.framework.persistence.api.impl.MatchType

class MagicFilterProcessorTest {
    @Test
    fun mixedEntriesTest() {
        val magicFilter = MagicFilter()
        magicFilter.entries.add(MagicFilterEntry("name", "rein*"))
        magicFilter.entries.add(MagicFilterEntry("zipCode", "12345"))
        magicFilter.entries.add(MagicFilterEntry(value  = "fullTextSearch"))
        val dbFilter = MagicFilterProcessor.doIt(AddressDO::class.java, magicFilter)
        Assertions.assertEquals(2, dbFilter.criteriaSearchEntries.size)
        Assertions.assertEquals(1, dbFilter.fulltextSearchEntries.size)
        Assertions.assertEquals("fullTextSearch", dbFilter.fulltextSearchEntries[0].value)
    }

    @Test
    fun filterEntrySearchStringConversionTest() {
        testEntry("12345", "12345", "12345", MatchType.EXACT)
        testEntry("*12345", "12345", "%12345", MatchType.STARTS_WITH)
        testEntry("*12345*", "12345", "%12345%", MatchType.CONTAINS)
        testEntry("12345*", "12345", "12345%", MatchType.ENDS_WITH)
    }

    private fun testEntry(value: String, expectedPlainString: String, expectedDBString: String, matchType: MatchType) {
        val magicEntry = MagicFilterEntry("zipCode", value)
        val dbEntry = MagicFilterProcessor.createFieldSearchEntry(AddressDO::class.java, magicEntry)
        Assertions.assertEquals(value, dbEntry.value)
        Assertions.assertEquals(expectedPlainString, dbEntry.plainSearchString)
        Assertions.assertEquals(expectedDBString, dbEntry.dbSearchString)
        Assertions.assertFalse(dbEntry.fulltextSearch)
    }
}
