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

package org.projectforge.rest.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReplaceUtilsTest {
    @Test
    fun encodeFilenameTest() {
        assertEquals("file", ReplaceUtils.encodeFilename(null, true))
        assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-",
                ReplaceUtils.encodeFilename("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-", true))
        assertEquals("_", ReplaceUtils.encodeFilename(" ", true))
        assertEquals("__", ReplaceUtils.encodeFilename("  ", true))
        assertEquals("Kai_Oester__Test", ReplaceUtils.encodeFilename("Kai Öster:,Test", true))
        assertEquals("AeOeUeaeoeuess", ReplaceUtils.encodeFilename("ÄÖÜäöüß", true))
        assertEquals("Stephanie", ReplaceUtils.encodeFilename("Stéphanie", true))
        assertEquals("AGOOacae", ReplaceUtils.encodeFilename("ĂĠÒǬåçä", true))

        assertEquals("Ä____.___.__", ReplaceUtils.encodeFilename("Ä\"*/:.<>?.\\|", false))
        var sb = StringBuilder()
        for (ch in 0..31) {
            sb.append(ch.toChar())
        }
        sb.append("xxx").append(127.toChar())
        assertEquals("________________________________xxx_", ReplaceUtils.encodeFilename(sb.toString(), false))
        assertEquals("Ä é", ReplaceUtils.encodeFilename("Ä é", false))
        sb = StringBuilder()
        for (i in 0..99) {
            sb.append("1234567890")
        }
        assertEquals(255, ReplaceUtils.encodeFilename(sb.toString(), false).length)
    }

    @Test
    fun replaceGermanUmlauteAndAccents() {
        assertEquals("untitled", ReplaceUtils.replaceGermanUmlauteAndAccents(null))
        assertEquals("", ReplaceUtils.replaceGermanUmlauteAndAccents(""))
        assertEquals("AGOOacAeaeOeoeUeuessnormal_ .", ReplaceUtils.replaceGermanUmlauteAndAccents("ĂĠÒǬåçÄäÖöÜüßnormal_ ."))
    }
}
