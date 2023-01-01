/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.importer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.Charset

class CsvImporterTest {
  @Test
  fun detectCharsetTest() {
    check("UTF-16", "UTF-16", "ISO-8859-1")

    check("ISO-8859-15", "ISO-8859-1", "ISO-8859-15")
    check("UTF-8", "ISO-8859-1")
    check("UTF-8", "UTF-8", "ISO-8859-15")

    val i18nDe = File("../projectforge-business/src/main/resources/I18nResources_de.properties").readBytes()
    check("UTF-8", i18nDe, "ISO-8859-15")
  }

  private fun check(expected: String, encodeWith: String, defaultCharsetName: String? = null) {
    val str = "Hallo öäüß, éáàc"
    val bytes = str.toByteArray(Charset.forName(encodeWith))
    check(expected, bytes, defaultCharsetName)
  }

  private fun check(expected: String, bytes: ByteArray, defaultCharsetName: String? = null) {
    var defaultCharset: Charset? = null
    if (defaultCharsetName != null) {
      defaultCharset = Charset.forName(defaultCharsetName)
    }
    Assertions.assertEquals(expected, CsvImporter.detectCharset(bytes, defaultCharset).name())
  }
}
