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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.TestSetup
import org.projectforge.framework.time.PFDateTime.Companion.now

class FilenameUtilsTest {
    @Test
    fun `test of escaping filename`() {
        test(FilenameUtils.escapeFilename("test`it"))
        test(FilenameUtils.escapeFilename("test\\it"))
        test(FilenameUtils.escapeFilename("test/it"))
        test(FilenameUtils.escapeFilename("test:it"))
        test(FilenameUtils.escapeFilename("test*it"))
        test(FilenameUtils.escapeFilename("test?it"))
        test(FilenameUtils.escapeFilename("test\"it"))
        test(FilenameUtils.escapeFilename("test<it"))
        test(FilenameUtils.escapeFilename("test>it"))
        Assertions.assertEquals("Schroedinger", FilenameUtils.escapeFilename("Schrödinger", substituteUmlaute = true))
        Assertions.assertEquals("Schrödinger", FilenameUtils.escapeFilename("Schrödinger", substituteUmlaute = false))
        Assertions.assertEquals(
            "Schr_dinger",
            FilenameUtils.escapeFilename("Schrödinger", strict = true, substituteUmlaute = false)
        )
    }

    private fun test(filename: String) {
        Assertions.assertEquals("test_it", FilenameUtils.escapeFilename(filename, strict = true))
        Assertions.assertEquals("test_it", FilenameUtils.escapeFilename(filename, strict = false))
    }

    @Test
    fun createStrictSafeFilename() {
        TestSetup.init()
        Assertions.assertEquals("http_www.micromata.de", FilenameUtils.createSafeFilename("http://www.micromata.de"))
        Assertions.assertEquals("http_www", FilenameUtils.createSafeFilename("http://www.micromata.de", maxlength = 8))
        Assertions.assertEquals("Schroedinger", FilenameUtils.createSafeFilename("Schrödinger"))
        Assertions.assertEquals(
            "Micromata_is_a_great_software_company.",
            FilenameUtils.createSafeFilename("Micromata is a great software company.")
        )
        Assertions.assertEquals("AeOeUeaeoeuess", FilenameUtils.createSafeFilename("ÄÖÜäöüß"))
        Assertions.assertEquals("AeOeU", FilenameUtils.createSafeFilename("ÄÖÜäöüß", maxlength = 5))
        Assertions.assertEquals("Ae", FilenameUtils.createSafeFilename("ÄÖÜäöüß", maxlength = 2))
        Assertions.assertEquals("AeOe", FilenameUtils.createSafeFilename("ÄÖÜäöüß", maxlength = 4))
        Assertions.assertEquals("Ha", FilenameUtils.createSafeFilename("Hä", maxlength = 2))

        val dateTime = now()
        Assertions.assertEquals(
            ("basename_"
                    + dateTime.year
                    + "-"
                    + StringHelper.format2DigitNumber(dateTime.monthValue)
                    + "-"
                    + StringHelper.format2DigitNumber(dateTime.dayOfMonth)
                    + ".pdf"), FilenameUtils.createSafeFilename("basename", ".pdf", 8, true)
        )
    }
}
