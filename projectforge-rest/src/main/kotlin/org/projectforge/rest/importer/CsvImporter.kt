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

package org.projectforge.rest.importer

import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset

object CsvImporter {

  /**
   * @param charset to use, if UTF-8 encoding or UTF-16-encoding doesn't fit.
   */
  fun <O : ImportPairEntry.Modified<O>> parse(
    inputStream: InputStream,
    importStorage: ImportStorage<O>,
    defaultCharset: Charset? = null,
  ) {
    val importer = DefaultCsvImporter<O>()
    importer.parse(inputStream, importStorage, defaultCharset)
  }

  fun <O : ImportPairEntry.Modified<O>> parse(reader: Reader, importStorage: ImportStorage<O>) {
    val importer = DefaultCsvImporter<O>()
    importer.parse(reader, importStorage)
  }

}
