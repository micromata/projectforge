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

import org.projectforge.rest.importer.ImportPairEntry
import kotlin.reflect.KProperty

/**
 * Data transfer object for one account (Konto) of the DATEV chart of accounts (Kontenplan) import.
 *
 * @author Kai Reinhard
 */
class DatevAccountImportDTO(
    var nummer: Int? = null,
    var bezeichnung: String? = null,
    /** The id of the matching database account, set during reconcile; null for a new account. */
    var id: Long? = null,
) : ImportPairEntry.Modified<DatevAccountImportDTO> {

    /** Parse errors kept on the DTO so they survive reconcile, which rebuilds the pair entries. */
    private val errors = mutableListOf<String>()

    fun addError(errorMessage: String) {
        errors.add(errorMessage)
    }

    fun getErrors(): List<String> = errors.toList()

    /** Only the name is imported, all other account properties are maintained in ProjectForge. */
    override val properties: Array<KProperty<*>>
        get() = arrayOf(DatevAccountImportDTO::bezeichnung)
}
