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

package org.projectforge.framework.persistence.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.FormOfAddress
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.persistence.api.impl.MatchType

class MagicFilterProcessorTest {
    @Test
    fun mixedEntriesTest() {
        val magicFilter = MagicFilter()
        magicFilter.entries.add(MagicFilterEntry("name", "rein*"))
        magicFilter.entries.add(MagicFilterEntry("zipCode", "12345"))
        magicFilter.entries.add(MagicFilterEntry(stringValue = "fullTextSearch"))
        val queryFilter = MagicFilterProcessor.doIt(AddressDO::class.java, magicFilter)
        val dbFilter = queryFilter.createDBFilter()
        // 0 - deleted, 1 - name, 2 - zipCode, 3 - fullTextSearch
        Assertions.assertEquals(4, dbFilter.allPredicates.size)
        Assertions.assertEquals(4, dbFilter.allPredicates.filter { it.fullTextSupport }.size)
    }

    @Test
    fun filterEntrySearchStringConversionTest() {
        testEntry("12345", "12345",  MatchType.EXACT)
        testEntry("12345", "12345",  MatchType.STARTS_WITH, true) // Numerical

        testEntry("abc", "abc",  MatchType.EXACT)
        testEntry("abc", "abc",  MatchType.STARTS_WITH, true) // Numerical
        testEntry("*abc", "abc",  MatchType.ENDS_WITH)
        testEntry("*abc*", "abc",  MatchType.CONTAINS)
        testEntry("abc*", "abc",  MatchType.STARTS_WITH)
    }

    /**
     * A LIST filter may ask for entries without any value, alone or beside real ones
     * (see [MagicFilterEntry.NULL_VALUE]).
     */
    @Test
    fun enumNullValueTest() {
        // 0 - deleted, 1 - form
        Assertions.assertTrue(enumPredicate(FormOfAddress.MISTER.name) is DBPredicate.IsIn<*>)
        Assertions.assertTrue(enumPredicate(MagicFilterEntry.NULL_VALUE) is DBPredicate.IsNull)
        val both = enumPredicate(MagicFilterEntry.NULL_VALUE, FormOfAddress.MISTER.name)
        Assertions.assertTrue(both is DBPredicate.Or)
        val address = AddressDO()
        Assertions.assertTrue(both.match(address), "form is null: matches the null part.")
        address.form = FormOfAddress.MISTER
        Assertions.assertTrue(both.match(address), "form is one of the given values.")
        address.form = FormOfAddress.COMPANY
        Assertions.assertFalse(both.match(address), "form is set, but not to one of the given values.")
    }

    private fun enumPredicate(vararg values: String): DBPredicate {
        val magicFilter = MagicFilter()
        val entry = MagicFilterEntry("form")
        entry.value.values = arrayOf(*values)
        magicFilter.entries.add(entry)
        return MagicFilterProcessor.doIt(AddressDO::class.java, magicFilter).createDBFilter().allPredicates[1]
    }

    private fun testEntry(value: String, expectedPlainString: String, matchType: MatchType, autoStartWithSearch: Boolean = false) {
        val magicFilter = MagicFilter(autoWildcardSearch = autoStartWithSearch)
        magicFilter.entries.add(MagicFilterEntry("zipCode", value))
        val queryFilter = MagicFilterProcessor.doIt(AddressDO::class.java, magicFilter)
        // 0 - deleted, 1 - zipCode
        val predicate = queryFilter.createDBFilter().allPredicates[1]
        Assertions.assertTrue(predicate is DBPredicate.Like)
        val like = predicate as DBPredicate.Like
        Assertions.assertEquals(value, like.expectedValue)
        Assertions.assertEquals(expectedPlainString, like.plainString)
        Assertions.assertEquals(matchType, like.matchType)
    }
}
