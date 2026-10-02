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

import de.micromata.merlin.excel.ExcelColumnDef
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.SHType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val log = KotlinLogging.logger {}

/**
 * Parses the accounting records (Buchungssätze) of the tax office's original DATEV xlsx into the
 * [DatevRecordImportStorage].
 *
 * In contrast to the legacy [org.projectforge.business.fibu.datev.BuchungssatzExcelImporter] the file needn't be
 * prepared by hand:
 *  - Only sheets named by a month number (e.g. `07`) with the record columns are read; the report sheets of the
 *    tax office (`07_BWA`, `07_SuSa`, `07_USt`, `07_Kontenplan` ...) are ignored. If no such sheet exists, a sheet
 *    with the record columns is used and its month is taken from the file name (`2026-07_...`).
 *  - The month sheet is the booking batch: every record gets its year/month, but keeps its real voucher date. A
 *    record dated in a later month (frequent for vouchers of the following month) is accepted with a hint. Only a
 *    date more than [MAX_MONTH_DISTANCE] months away from the booking month is an error (guard against typos).
 *  - The booking year is the most frequent year of the records dated in the booking month, so a January batch
 *    with December vouchers gets the right year.
 *
 * Cells are read leniently: numbers may be numeric or (German formatted) text cells, dates may be date cells or
 * text (`dd.MM.yyyy`), cost units may be numeric or text.
 *
 * Accounts and cost units are resolved via the given lookups (memoized per distinct value); an unresolvable one
 * makes the record FAULTY, as in the legacy import.
 *
 * @author Kai Reinhard
 */
class DatevRecordExcelImporter(
    private val findKonto: (Int) -> KontoDO?,
    private val findKost1: (String) -> Kost1DO?,
    private val findKost2: (String) -> Kost2DO?,
) {
    constructor(kontoCache: KontoCache, kostCache: KostCache) : this(
        findKonto = { kontoCache.findKontoByNumber(it) },
        findKost1 = { kostCache.getKost1(it) },
        findKost2 = { kostCache.getKost2(it) },
    )

    private val kontoMemo = mutableMapOf<Int, KontoDO?>()
    private val kost1Memo = mutableMapOf<String, Kost1DO?>()
    private val kost2Memo = mutableMapOf<String, Kost2DO?>()

    /** One record as read from the file, before the booking year is known. */
    private class RawRecord(
        val rowNum: Int,
        val satznr: Int?,
        val betrag: BigDecimal?,
        val sh: String?,
        val konto: Int?,
        val gegenKonto: Int?,
        val kost1: String?,
        val kost2: String?,
        val datum: LocalDate?,
        val dateText: String?,
        val menge: String?,
        val beleg: String?,
        val text: String?,
        val comment: String?,
    )

    private class Columns(
        val satznr: ExcelColumnDef,
        val betrag: ExcelColumnDef,
        val sh: ExcelColumnDef,
        val konto: ExcelColumnDef,
        val gegenKonto: ExcelColumnDef,
        val kost1: ExcelColumnDef,
        val kost2: ExcelColumnDef,
        val datum: ExcelColumnDef,
        val menge: ExcelColumnDef,
        val beleg: ExcelColumnDef,
        val text: ExcelColumnDef,
        val comment: ExcelColumnDef,
    ) {
        val required: Boolean
            get() = listOf(satznr, betrag, sh, konto, gegenKonto, kost1, kost2, datum).all { it.found() }
    }

    fun parse(inputStream: InputStream, storage: DatevRecordImportStorage) {
        val filename = storage.filename ?: "unknown"
        val fileMonth = parseFileMonth(filename)
        ExcelWorkbook(inputStream, filename, ThreadLocalUserContext.locale).use { workbook ->
            val batches = mutableListOf<Triple<ExcelSheet, Columns, Int>>()
            for (idx in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheet(idx)
                val month = SHEET_MONTH_REGEX.matchEntire(sheet.sheetName.trim())?.groupValues?.get(1)?.toIntOrNull()
                if (month == null || month !in 1..12) {
                    continue
                }
                val cols = registerColumns(sheet)
                if (sheet.headRow != null && cols.required) {
                    batches.add(Triple(sheet, cols, month))
                } else {
                    log.info { "Ignoring sheet '${sheet.sheetName}', no accounting record columns found." }
                }
            }
            if (batches.isEmpty() && fileMonth != null) {
                // Fallback: a prepared file with a renamed sheet, the month is given by the file name.
                for (idx in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheet(idx)
                    val cols = registerColumns(sheet)
                    if (sheet.headRow != null && cols.required) {
                        batches.add(Triple(sheet, cols, fileMonth.second))
                        break
                    }
                }
            }
            if (batches.isEmpty()) {
                throw IllegalArgumentException(translate("fibu.datev.import.records.error.noSheet"))
            }
            batches.forEach { (sheet, cols, month) ->
                log.info { "Reading accounting records of sheet '${sheet.sheetName}' (month $month)." }
                parseSheet(sheet, cols, month, fileMonth, storage)
            }
        }
    }

    private fun registerColumns(sheet: ExcelSheet): Columns {
        sheet.autotrimCellValues = true
        return Columns(
            satznr = sheet.registerColumn(Cols.SATZNR),
            betrag = sheet.registerColumn(Cols.BETRAG),
            sh = sheet.registerColumn(Cols.SH),
            konto = sheet.registerColumn(Cols.KONTO),
            gegenKonto = sheet.registerColumn(Cols.GEGENKONTO),
            kost1 = sheet.registerColumn(Cols.KOST1),
            kost2 = sheet.registerColumn(Cols.KOST2),
            datum = sheet.registerColumn(Cols.DATUM),
            menge = sheet.registerColumn(Cols.MENGE),
            beleg = sheet.registerColumn(Cols.BELEG),
            text = sheet.registerColumn(Cols.TEXT),
            comment = sheet.registerColumn(Cols.KOMMENTAR),
        )
    }

    private fun parseSheet(
        sheet: ExcelSheet,
        cols: Columns,
        month: Int,
        fileMonth: Pair<Int, Int>?,
        storage: DatevRecordImportStorage,
    ) {
        val raws = mutableListOf<RawRecord>()
        val it = sheet.dataRowIterator
        while (it.hasNext()) {
            val row = it.next()
            fun cell(col: ExcelColumnDef): Cell? = sheet.getCell(row, col, ensureCell = false)
            val raw = RawRecord(
                rowNum = row.rowNum,
                satznr = intValue(cell(cols.satznr)),
                betrag = numberValue(cell(cols.betrag)),
                sh = stringValue(cell(cols.sh))?.uppercase(),
                konto = intValue(cell(cols.konto)),
                gegenKonto = intValue(cell(cols.gegenKonto)),
                kost1 = stringValue(cell(cols.kost1)),
                kost2 = stringValue(cell(cols.kost2)),
                datum = dateValue(cell(cols.datum)),
                dateText = stringValue(cell(cols.datum)),
                menge = mengeValue(cell(cols.menge)),
                beleg = stringValue(cell(cols.beleg)),
                text = stringValue(cell(cols.text)),
                comment = stringValue(cell(cols.comment)),
            )
            if (raw.satznr == null && raw.betrag == null && raw.konto == null && raw.gegenKonto == null) {
                continue // Empty or sum row.
            }
            raws.add(raw)
        }
        val year = detectYear(raws, month, fileMonth)
        val satznrs = mutableSetOf<Int>()
        raws.forEach { raw ->
            val dto = buildRecord(raw, year, month)
            raw.satznr?.let { satznr ->
                if (!satznrs.add(satznr)) {
                    dto.addError(translateMsg("fibu.datev.import.error.satznrDuplicate", "$satznr"))
                }
            }
            storage.commitEntity(dto)
        }
        log.info { "Read ${raws.size} accounting records of sheet '${sheet.sheetName}' for $year/$month." }
    }

    private fun buildRecord(raw: RawRecord, year: Int?, month: Int): DatevRecordImportDTO {
        val dto = DatevRecordImportDTO(
            year = year,
            month = month,
            satznr = raw.satznr,
            datum = raw.datum,
            sh = raw.sh,
            konto = raw.konto,
            gegenKonto = raw.gegenKonto,
            menge = raw.menge,
            beleg = raw.beleg,
            text = raw.text,
            comment = raw.comment,
        )
        if (year == null) {
            dto.addError(translate("fibu.datev.import.error.year"))
        }
        if (raw.satznr == null) {
            dto.addError(translate("fibu.datev.import.error.satznrMissing"))
        }
        val datum = raw.datum
        if (datum == null) {
            dto.addError(translateMsg("fibu.datev.import.error.dateMissing", raw.dateText ?: ""))
        } else if (year != null) {
            val distance = (datum.year * 12 + datum.monthValue) - (year * 12 + month)
            val batch = formatMonth(year, month)
            when {
                distance > MAX_MONTH_DISTANCE || distance < -MAX_MONTH_DISTANCE ->
                    dto.addError(translateMsg("fibu.datev.import.error.dateOutOfRange", datum.toString(), batch))

                distance > 0 -> dto.dateHint = translateMsg("fibu.datev.import.hint.futureDate", batch)
                distance < 0 -> dto.dateHint = translateMsg("fibu.datev.import.hint.pastDate", batch)
            }
        }
        val shType = when (raw.sh) {
            "S" -> SHType.SOLL
            "H" -> SHType.HABEN
            else -> null
        }
        if (shType == null) {
            dto.addError(translateMsg("fibu.datev.import.error.shInvalid", raw.sh ?: ""))
        }
        if (raw.betrag == null) {
            dto.addError(translate("fibu.datev.import.error.betragMissing"))
        }
        val konto = raw.konto?.let { nr -> kontoMemo.getOrPut(nr) { findKonto(nr) } }
        if (konto == null) {
            dto.addError(translateMsg("fibu.datev.import.error.kontoNotFound", "${raw.konto ?: ""}"))
        }
        val gegenKonto = raw.gegenKonto?.let { nr -> kontoMemo.getOrPut(nr) { findKonto(nr) } }
        if (gegenKonto == null) {
            dto.addError(translateMsg("fibu.datev.import.error.gegenKontoNotFound", "${raw.gegenKonto ?: ""}"))
        }
        val kost1 = raw.kost1?.let { str -> kost1Memo.getOrPut(str) { findKost1(str) } }
        if (kost1 == null) {
            dto.addError(translateMsg("fibu.datev.import.error.kost1NotFound", raw.kost1 ?: ""))
        }
        val kost2 = raw.kost2?.let { str -> kost2Memo.getOrPut(str) { findKost2(str) } }
        if (kost2 == null) {
            dto.addError(translateMsg("fibu.datev.import.error.kost2NotFound", raw.kost2 ?: ""))
        }
        dto.kontoId = konto?.id
        dto.gegenKontoId = gegenKonto?.id
        dto.kost1Id = kost1?.id
        dto.kost1 = kost1?.let { formatKost1(it) } ?: raw.kost1
        dto.kost2Id = kost2?.id
        dto.kost2 = kost2?.let { formatKost2(it) } ?: raw.kost2
        val betrag = raw.betrag?.setScale(2, RoundingMode.HALF_UP)
        dto.betrag = betrag
        if (betrag != null && shType != null && konto != null && gegenKonto != null && kost2 != null) {
            // Same Datev logic as the legacy import: the sign follows S/H, control bookings are ignored.
            val satz = BuchungssatzDO()
            satz.betrag = betrag
            satz.sh = shType
            satz.konto = konto
            satz.gegenKonto = gegenKonto
            satz.kost2 = kost2
            satz.calculate(true)
            dto.betrag = satz.betrag
            dto.ignore = satz.isIgnore
        }
        return dto
    }

    /**
     * The most frequent year of the records dated in the booking month. Fallbacks: the year of the file name
     * (`2026-07_...`), then the most frequent year of all records.
     */
    private fun detectYear(raws: List<RawRecord>, month: Int, fileMonth: Pair<Int, Int>?): Int? {
        mostFrequentYear(raws.mapNotNull { it.datum }.filter { it.monthValue == month })?.let { return it }
        fileMonth?.let { return it.first }
        return mostFrequentYear(raws.mapNotNull { it.datum })
    }

    private fun mostFrequentYear(dates: List<LocalDate>): Int? {
        return dates.groupingBy { it.year }.eachCount().maxByOrNull { it.value }?.key
    }

    private enum class Cols(
        override val head: String,
        override vararg val aliases: String,
    ) : de.micromata.merlin.excel.ExcelColumnName {
        // Same heads and aliases as the legacy BuchungssatzExcelImporter.
        SATZNR("SatzNr.", "Satz-Nr."),
        BETRAG("Betrag"),
        SH("SH", "S/H"),
        KOST1("Alt.-Kst.", "Kost1"),
        KOST2("Kostenstelle/-träger", "Kost2", "Kst."),
        KONTO("Konto"),
        GEGENKONTO("Gegenkonto"),
        MENGE("Menge"),
        BELEG("Beleg"),
        DATUM("Datum"),
        TEXT("Text"),
        KOMMENTAR("Kommentar", "Bemerkung"),
    }

    companion object {
        /** A date further away from the booking month is most likely a typo. */
        const val MAX_MONTH_DISTANCE = 12

        private val SHEET_MONTH_REGEX = Regex("""(\d{1,2})""")

        /** The tax office's file name starts with the booking month: `2026-07_-_FiBu_Auswertungen...`. */
        private val FILE_MONTH_REGEX = Regex("""^(\d{4})[-_ ](\d{1,2})(\D|$)""")

        private val DATE_FORMATS = listOf("d.M.yyyy", "d.M.yy", "yyyy-MM-dd").map { DateTimeFormatter.ofPattern(it) }

        internal fun parseFileMonth(filename: String): Pair<Int, Int>? {
            val name = filename.substringAfterLast('/').substringAfterLast('\\')
            val match = FILE_MONTH_REGEX.find(name) ?: return null
            val year = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            return if (month in 1..12) year to month else null
        }

        /**
         * #.###.##.##, as KostFormatter's FORMATTED_NUMBER, but without its cache lookups (the DOs here are
         * already cache instances).
         */
        internal fun formatKost1(kost1: Kost1DO): String =
            "%d.%03d.%02d.%02d".format(kost1.nummernkreis, kost1.bereich, kost1.teilbereich, kost1.endziffer)

        /** #.###.##.##, see [formatKost1]. */
        internal fun formatKost2(kost2: Kost2DO): String {
            val art = kost2.kost2Art?.id?.let { "%02d".format(it) } ?: "--"
            return "%d.%03d.%02d.%s".format(kost2.nummernkreis, kost2.bereich, kost2.teilbereich, art)
        }

        private fun formatMonth(year: Int, month: Int): String = "%02d/%d".format(month, year)

        private fun effectiveType(cell: Cell): CellType {
            return if (cell.cellType == CellType.FORMULA) cell.cachedFormulaResultType else cell.cellType
        }

        internal fun stringValue(cell: Cell?): String? {
            cell ?: return null
            val str = when (effectiveType(cell)) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> {
                    val value = BigDecimal.valueOf(cell.numericCellValue)
                    // Account and cost unit numbers are often numeric cells: 59991002.0 -> "59991002".
                    value.stripTrailingZeros().toPlainString()
                }

                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                else -> null
            }
            return str?.trim()?.takeIf { it.isNotEmpty() }
        }

        /**
         * The quantity is a free text in the database. A numeric cell is formatted as the legacy import (Merlin's
         * PoiHelper.getValueAsString) stored it, German: 88.25 -> "88,25". Otherwise every record with a fractional
         * quantity would be shown as modified.
         */
        internal fun mengeValue(cell: Cell?): String? {
            cell ?: return null
            if (effectiveType(cell) != CellType.NUMERIC) {
                return stringValue(cell)
            }
            return NumberFormat.getInstance(Locale.GERMAN).format(cell.numericCellValue)
        }

        /** Numeric cells or text cells in German (`1.234,56`) or plain (`1234.56`) format. */
        internal fun numberValue(cell: Cell?): BigDecimal? {
            cell ?: return null
            return when (effectiveType(cell)) {
                CellType.NUMERIC -> BigDecimal.valueOf(cell.numericCellValue)
                CellType.STRING -> parseNumber(cell.stringCellValue)
                else -> null
            }
        }

        internal fun parseNumber(text: String?): BigDecimal? {
            val str = text?.trim()?.replace(" ", "")?.takeIf { it.isNotEmpty() } ?: return null
            val normalized = if (str.contains(',')) {
                str.replace(".", "").replace(',', '.') // German: 1.234,56
            } else {
                str
            }
            return normalized.toBigDecimalOrNull()
        }

        internal fun intValue(cell: Cell?): Int? {
            val value = numberValue(cell) ?: return null
            return try {
                value.stripTrailingZeros().intValueExact()
            } catch (ex: ArithmeticException) {
                null
            }
        }

        /** Date cells or text cells (`dd.MM.yyyy`, `dd.MM.yy`, `yyyy-MM-dd`). */
        internal fun dateValue(cell: Cell?): LocalDate? {
            cell ?: return null
            return when (effectiveType(cell)) {
                CellType.NUMERIC -> {
                    if (DateUtil.isValidExcelDate(cell.numericCellValue)) {
                        cell.localDateTimeCellValue?.toLocalDate()
                    } else {
                        null
                    }
                }

                CellType.STRING -> parseDate(cell.stringCellValue)
                else -> null
            }
        }

        internal fun parseDate(text: String?): LocalDate? {
            val str = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            for (formatter in DATE_FORMATS) {
                try {
                    return LocalDate.parse(str, formatter)
                } catch (ex: DateTimeParseException) {
                    // Try next format.
                }
            }
            return null
        }
    }
}
