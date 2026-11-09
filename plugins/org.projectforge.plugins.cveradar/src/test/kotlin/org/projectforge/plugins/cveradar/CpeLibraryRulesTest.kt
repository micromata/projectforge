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

package org.projectforge.plugins.cveradar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CpeLibraryRulesTest {
    private fun library(purl: String) = CpeLibraryRules.libraryOf(purl, null)!!

    @Test
    fun `the platform of an entry admits the libraries of its ecosystem only`() {
        val nodeTar = CpeLibraryRules.targetSoftware("cpe:2.3:a:isaacs:tar:*:*:*:*:*:node.js:*:*")
        assertEquals("node.js", nodeTar)
        assertNull(CpeLibraryRules.targetSoftware("cpe:2.3:a:adobe:connect:*:*:*:*:*:-:*:*"))
        assertNull(CpeLibraryRules.targetSoftware("cpe:2.3:a:gnu:tar:*:*:*:*:*:*:*:*"))
        assertNull(CpeLibraryRules.targetSoftware("EUVD: isaacs tar"))
        assertFalse(CpeLibraryRules.fitsPlatform(nodeTar, library("pkg:deb/debian/tar@1.35+dfsg-3.1?distro=debian-13")))
        assertTrue(CpeLibraryRules.fitsPlatform(nodeTar, library("pkg:npm/tar@7.0.0")))
        assertFalse(CpeLibraryRules.fitsPlatform("rust", library("pkg:deb/debian/coreutils@9.7-3?distro=debian-13")))
        assertTrue(CpeLibraryRules.fitsPlatform("rust", library("pkg:cargo/coreutils@0.1.0")))
        assertTrue(CpeLibraryRules.fitsPlatform(null, library("pkg:deb/debian/tar@1.35")))
        assertTrue(CpeLibraryRules.fitsPlatform("linux", library("pkg:deb/debian/tar@1.35")), "not a runtime")
        assertTrue(CpeLibraryRules.namesPlatform(nodeTar, library("pkg:npm/tar@7.0.0")))
        assertFalse(CpeLibraryRules.namesPlatform(null, library("pkg:npm/tar@7.0.0")))
    }

    @Test
    fun `the vendor of an entry fits the name, the scope or the groupId of a library`() {
        assertFalse(CpeLibraryRules.fitsVendor("adobe", library("pkg:npm/connect@3.7.0")))
        assertFalse(CpeLibraryRules.fitsVendor("apple", library("pkg:npm/xcode@4.0.0")))
        assertTrue(CpeLibraryRules.fitsVendor("lodash", library("pkg:npm/lodash@4.17.20")))
        assertTrue(CpeLibraryRules.fitsVendor("minimist_project", library("pkg:npm/minimist@1.2.0")))
        assertTrue(CpeLibraryRules.fitsVendor("expressjs", library("pkg:npm/express@4.0.0")))
        assertTrue(CpeLibraryRules.fitsVendor("webpack.js", library("pkg:npm/webpack@5.0.0")))
        assertTrue(CpeLibraryRules.fitsVendor("angular", library("pkg:npm/%40angular/core@17.0.0")), "the scope")
        assertTrue(CpeLibraryRules.fitsVendor("fasterxml", library("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.12.0")))
        assertFalse(CpeLibraryRules.fitsVendor("oracle", library("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.12.0")))
        assertTrue(CpeLibraryRules.fitsVendor("python", library("pkg:pypi/pillow@9.0.0")), "the vendor of the ecosystem")
        assertTrue(CpeLibraryRules.fitsVendor(null, library("pkg:npm/connect@3.7.0")))
    }
}
