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
import java.math.BigDecimal
import kotlin.reflect.KProperty

/**
 * Data transfer object for one row of the employee-salary xlsx import (the layout-free, hand-built sibling
 * of the legacy Wicket import). One row is one salary of one employee for one accounting month.
 *
 * The file carries the accounting month itself ([year]/[month], read from the "Abrechnungsmonat" column),
 * the staff number and the gross amount including the employer's SV share ([bruttoMitAgAnteil]) — the only
 * value column imported, exactly as the legacy importer did. [employeeId] is resolved from the staff number
 * while parsing; a row whose staff number matches no employee carries an error and stays FAULTY.
 *
 * @author Kai Reinhard
 */
class EmployeeSalaryImportDTO(
    /** Staff number as read from the file (column "Pers.Nr."). */
    var staffNumber: Int? = null,
    /** The resolved employee's display name, for the preview only. */
    var employee: String? = null,
    /** The resolved employee's id, the reconcile and the import match on it. */
    var employeeId: Long? = null,
    /** Accounting year, read from the "Abrechnungsmonat" column (e.g. 2026). */
    var year: Int? = null,
    /** Accounting month, 1-based, read from the "Abrechnungsmonat" column (e.g. 8 for `2026/08`). */
    var month: Int? = null,
    /** Gross amount including the employer's SV share — the single imported value column. */
    var bruttoMitAgAnteil: BigDecimal? = null,
    /** The id of the matching database salary, set during reconcile; null for a new salary. */
    var id: Long? = null,
) : ImportPairEntry.Modified<EmployeeSalaryImportDTO> {

    /** Parse errors kept on the DTO so they survive reconcile, which rebuilds the pair entries. */
    private val errors = mutableListOf<String>()

    fun addError(errorMessage: String) {
        errors.add(errorMessage)
    }

    fun getErrors(): List<String> = errors.toList()

    /** Only the gross amount decides whether an existing salary is modified. */
    override val properties: Array<KProperty<*>>
        get() = arrayOf(EmployeeSalaryImportDTO::bruttoMitAgAnteil)
}
