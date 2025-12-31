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

package org.projectforge.framework.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SourcesUtilsTest {
    @Test
    fun `test removing comments`() {
        val source = """
            // This is a comment
            /* This is a block comment
            val a = 1 // This is a comment */
            val b = 2 /* This is a block comment */
            val c = "This is a // /* no comment */ string"
        """.trimIndent()

        val expected = """
            
            
            val b = 2 
            val c = "This is a // /* no comment */ string"
        """.trimIndent()

        val actual = SourcesUtils.removeComments(source)
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `test removing comments with strings`() {
        val source = """
            val a = "This is a string with // a comment"
            val b = \"\"\"This is a multiline 
               string with /* a block comment */
            \"\"\"
            val c = "This is a string"
        """.trimIndent()

        val expected = """
            val a = "This is a string with // a comment"
            val b = \"\"\"This is a multiline 
               string with /* a block comment */
            \"\"\"
            val c = "This is a string"
        """.trimIndent()

        val actual = SourcesUtils.removeComments(source)
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `test removing strings`() {
        val tripleQuote = "\"\"\""
        val source = """
            val a = "This is a string"
            val b = "This is a string with \"escaped quotes\""
            val c = "This is a string with // a comment"
            val d = "This is a string with /* a block comment */"
            val e = ${tripleQuote}This is a multiline
                string with // a comment
            $tripleQuote
            val f = true
        """.trimIndent()
        val expected = """
            val a = 
            val b = 
            val c = 
            val d = 
            val e = 
            val f = true
        """.trimIndent()
        val actual = SourcesUtils.removeStrings(source)
        Assertions.assertEquals(expected, actual)
    }
}
