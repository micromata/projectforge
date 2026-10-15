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

package org.projectforge.framework.persistence.search

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SearchStringTokenizerTest {
    /**
     * The terms of the index: the hyphen separates, the dot inside a word or number doesn't.
     */
    @Test
    fun tokenizeTest() {
        Assertions.assertEquals(listOf("dhl", "pop"), SearchStringTokenizer.tokenize("dhl-pop"))
        Assertions.assertEquals(listOf("dhl"), SearchStringTokenizer.tokenize("dhl"))
        Assertions.assertEquals(listOf("test.de"), SearchStringTokenizer.tokenize("test.de"))
        Assertions.assertEquals(listOf("kai", "acme.de"), SearchStringTokenizer.tokenize("kai@acme.de"))
        Assertions.assertEquals(listOf("5.100.01.02"), SearchStringTokenizer.tokenize("5.100.01.02"))
        Assertions.assertEquals(listOf("dhl", "pop"), SearchStringTokenizer.tokenize("DHL-POP"), "Lower cased.")
        Assertions.assertEquals(listOf("2008", "11", "21"), SearchStringTokenizer.tokenize("2008-11-21"))
        Assertions.assertEquals(emptyList<String>(), SearchStringTokenizer.tokenize("-"), "No word at all.")
        Assertions.assertEquals(emptyList<String>(), SearchStringTokenizer.tokenize(""))
    }

    @Test
    fun expandWordTest() {
        Assertions.assertEquals("dhl*", SearchStringTokenizer.expandWord("dhl"))
        Assertions.assertEquals("+dhl*", SearchStringTokenizer.expandWord("dhl", required = true))
        Assertions.assertEquals(
            "+(dhl-pop* (+dhl* +pop*))",
            SearchStringTokenizer.expandWord("dhl-pop", required = true),
            "Either the whole word (keyword analyzed fields) or all of the terms the index holds for it.",
        )
        Assertions.assertEquals("(dhl-pop* (+dhl* +pop*))", SearchStringTokenizer.expandWord("dhl-pop"))
        Assertions.assertEquals(
            "+(kai@acme.de* (+kai* +acme.de*))",
            SearchStringTokenizer.expandWord("kai@acme.de", required = true),
        )
        Assertions.assertEquals(
            "+(dhl-* (+dhl*))",
            SearchStringTokenizer.expandWord("dhl-", required = true),
            "The index holds 'dhl' for 'dhl-', so the wildcard term 'dhl-*' alone would match nothing.",
        )
        Assertions.assertEquals(
            "+(kai@* (+kai*))",
            SearchStringTokenizer.expandWord("kai@", required = true),
            "The user is still typing: the separator at the end is not a term of the index.",
        )
        Assertions.assertEquals("-", SearchStringTokenizer.expandWord("-"), "Nothing to search for: unchanged.")
        Assertions.assertEquals(
            "+(c++* (+c*))",
            SearchStringTokenizer.expandWord("c++", required = true),
            "The '++' is no part of the term the index holds.",
        )
        Assertions.assertEquals(
            "+(+dhl*)",
            SearchStringTokenizer.expandWord("-dhl", required = true),
            "A leading '-' would be the prohibited operator of the parser, so only the term is asked for.",
        )
        Assertions.assertEquals(
            "+dhl-pop%",
            SearchStringTokenizer.expandWord("dhl-pop", "%", required = true),
            "The criteria search (like '%') has no query syntax for alternatives.",
        )
    }

    @Test
    fun expandSplitWordTest() {
        Assertions.assertEquals("(dhl-pop* (+dhl* +pop*))", SearchStringTokenizer.expandSplitWord("dhl-pop"))
        Assertions.assertEquals("(dhl-* (+dhl*))", SearchStringTokenizer.expandSplitWord("dhl-"))
        Assertions.assertNull(SearchStringTokenizer.expandSplitWord("dhl"), "One term as typed: nothing to expand.")
        Assertions.assertNull(SearchStringTokenizer.expandSplitWord("DHL"), "One term as typed, but upper case.")
        Assertions.assertNull(SearchStringTokenizer.expandSplitWord("-"))
        Assertions.assertNull(SearchStringTokenizer.expandSplitWord("dhl-pop", "%"))
    }
}
