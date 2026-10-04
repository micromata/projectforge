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
import java.time.LocalDate
import kotlin.reflect.KProperty

/**
 * Data transfer object for one accounting record (Buchungssatz) of the DATEV xlsx import. One row of a month sheet
 * (e.g. `07`) of the tax office's file is one record of the booking batch [year]/[month].
 *
 * The booking batch comes from the sheet, not from the record's date: a record dated in a later month (e.g. an
 * August voucher in the July batch) keeps its real [datum] and only carries the informational [dateHint].
 *
 * Accounts and cost units are resolved while parsing; the DTO carries their ids (for the import) and their
 * display numbers (for the preview and the diff). [betrag] is the signed database representation (negative for
 * debit, see [org.projectforge.business.fibu.kost.BuchungssatzDO.calculate]).
 *
 * @author Kai Reinhard
 */
class DatevRecordImportDTO(
    /** Year of the booking batch. */
    var year: Int? = null,
    /** Month of the booking batch, 1-based. */
    var month: Int? = null,
    /** Record number, unique within the booking batch. */
    var satznr: Int? = null,
    /** The real voucher date, possibly outside the booking month. */
    var datum: LocalDate? = null,
    /** Signed amount (debit is negative). */
    var betrag: BigDecimal? = null,
    /** "S" (Soll, debit) or "H" (Haben, credit). */
    var sh: String? = null,
    var konto: Int? = null,
    var kontoId: Long? = null,
    var gegenKonto: Int? = null,
    var gegenKontoId: Long? = null,
    /** Formatted cost unit 1 (#.###.##.##). */
    var kost1: String? = null,
    var kost1Id: Long? = null,
    /** Preview tooltip of [kost1]: its description. Not part of the diff. */
    var kost1Info: String? = null,
    /** Formatted cost unit 2 (#.###.##.##). */
    var kost2: String? = null,
    var kost2Id: Long? = null,
    /** Preview tooltip of [kost2]: description, customer - project, cost type (see DatevRecordExcelImporter.kost2Tooltip). */
    var kost2Info: String? = null,
    var menge: String? = null,
    var beleg: String? = null,
    var text: String? = null,
    var comment: String? = null,
    /** True for control bookings which are ignored by the business assessment (see BuchungssatzDO.calculate). */
    var ignore: Boolean = false,
    /** Informational hint, e.g. for a record dated after the booking month. Not an error. */
    var dateHint: String? = null,
    /** The id of the matching database record, set during reconcile; null for a new record. */
    var id: Long? = null,
) : ImportPairEntry.Modified<DatevRecordImportDTO> {

    /** Parse errors kept on the DTO so they survive reconcile, which rebuilds the pair entries. */
    private val errors = mutableListOf<String>()

    fun addError(errorMessage: String) {
        errors.add(errorMessage)
    }

    fun getErrors(): List<String> = errors.toList()

    override val properties: Array<KProperty<*>>
        get() = arrayOf(
            DatevRecordImportDTO::datum,
            DatevRecordImportDTO::betrag,
            DatevRecordImportDTO::sh,
            DatevRecordImportDTO::konto,
            DatevRecordImportDTO::gegenKonto,
            DatevRecordImportDTO::kost1,
            DatevRecordImportDTO::kost2,
            DatevRecordImportDTO::menge,
            DatevRecordImportDTO::beleg,
            DatevRecordImportDTO::text,
        )
}
