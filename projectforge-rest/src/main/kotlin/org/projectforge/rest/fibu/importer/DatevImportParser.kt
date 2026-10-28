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

package org.projectforge.rest.fibu.importer

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.KostCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.InputStream

/**
 * Parses the tax office's original DATEV file for both tabs of the DATEV import, the accounting records
 * ([DatevRecordImportRest]) and the chart of accounts ([DatevAccountImportRest]). Both read the same file, so an
 * upload on one tab also fills the other one, if the file has its part (see the controllers' `afterUpload`).
 */
@Component
class DatevImportParser {
    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var caches: PfCaches

    /** Reads the month sheets, throws an exception with a message for the user if there is none. */
    fun parseRecords(inputStream: InputStream, filename: String): DatevRecordImportStorage {
        val storage = DatevRecordImportStorage()
        storage.filename = filename
        DatevRecordExcelImporter(kontoCache, kostCache, caches).parse(inputStream, storage)
        return storage
    }

    /** Reads the chart of accounts, throws an exception with a message for the user if there is none. */
    fun parseAccounts(inputStream: InputStream, filename: String): DatevAccountImportStorage {
        val storage = DatevAccountImportStorage()
        storage.filename = filename
        DatevAccountExcelImporter().parse(inputStream, storage)
        // The tooltips are there right after the upload, as in the record preview; reconcile refreshes them.
        storage.readAccounts.forEach { dto ->
            dto.kontoInfo = kontoCache.findKontoByNumber(dto.nummer)?.let { DatevAccountImportStorage.kontoTooltip(it) }
        }
        return storage
    }
}
