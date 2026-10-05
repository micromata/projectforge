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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.SHType
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage

private val log = KotlinLogging.logger {}

/**
 * The session-held storage of the DATEV accounting record import. It keeps the records the
 * [DatevRecordExcelImporter] parsed and reconciles them against the database.
 *
 * A record is unique per booking batch and record number (year, month, satznr), so the reconcile loads each booking
 * batch once ([BuchungssatzDao.selectByMonth]) and matches on the record number. A match becomes the `stored` side
 * (→ MODIFIED/UNMODIFIED), no match is a NEW record. Records missing in the file are not deleted (as in the legacy
 * import).
 *
 * @param loadBatch Loads all stored records of a booking batch (year, month); replaceable for tests.
 * @author Kai Reinhard
 */
class DatevRecordImportStorage(
    private val loadBatch: ((year: Int, month: Int) -> List<BuchungssatzDO>)? = null,
) : ImportStorage<DatevRecordImportDTO>(ImportSettings()) {

    override val targetEntityTitle: String
        get() = translate("fibu.datev.import.records")

    /** The records the parser read from the file, kept so a re-reconcile can rebuild the pairs. */
    val readRecords = mutableListOf<DatevRecordImportDTO>()

    private var lastReconcileSummary: String? = null

    override fun prepareEntity(): DatevRecordImportDTO {
        return DatevRecordImportDTO()
    }

    override fun commitEntity(obj: DatevRecordImportDTO) {
        readRecords.add(obj)
        val pairEntry = ImportPairEntry(read = obj)
        obj.getErrors().forEach { pairEntry.addError(it) }
        addEntry(pairEntry)
    }

    override fun commitEntity(pairEntry: ImportPairEntry<DatevRecordImportDTO>) {
        pairEntry.read?.let { dto ->
            readRecords.add(dto)
            dto.getErrors().forEach { pairEntry.addError(it) }
        }
        addEntry(pairEntry)
    }

    override fun doReconcileImportStorage(rereadDatabaseEntries: Boolean) {
        val loader = loadBatch ?: run {
            val dao = ApplicationContextProvider.getApplicationContext().getBean(BuchungssatzDao::class.java)
            val fn: (Int, Int) -> List<BuchungssatzDO> = { year, month -> dao.selectByMonth(year, month) }
            fn
        }
        val dbByBatch = mutableMapOf<Pair<Int, Int>, Map<Int, BuchungssatzDO>>()
        clearEntries()
        readRecords.forEach { read ->
            val year = read.year
            val month = read.month
            val satznr = read.satznr
            val stored = if (year != null && month != null && satznr != null) {
                val bySatznr = dbByBatch.getOrPut(year to month) {
                    loader(year, month).mapNotNull { record -> record.satznr?.let { it to record } }.toMap()
                }
                bySatznr[satznr]?.let { toStoredDTO(it) }
            } else {
                null
            }
            val pairEntry = ImportPairEntry(read = read, stored = stored)
            read.getErrors().forEach { pairEntry.addError(it) }
            addEntry(pairEntry)
        }
        // Logged on a changed result only: the preview reconciles again on every change of its display options.
        val summary = DatevImportLog.reconcileSummary(pairEntries)
        if (summary != lastReconcileSummary) {
            lastReconcileSummary = summary
            log.info { "Reconciled ${pairEntries.size} accounting records of file '$filename' with the database: $summary." }
        }
    }

    /**
     * The read records which are importable (no errors), as transient [BuchungssatzDO]s for the business
     * assessment (BWA) of the preview. Accounts and cost units come from the caches.
     */
    fun buildImportableRecords(): List<BuchungssatzDO> {
        val context = ApplicationContextProvider.getApplicationContext()
        val kontoCache = context.getBean(KontoCache::class.java)
        val kostCache = context.getBean(KostCache::class.java)
        return readRecords.filter { it.getErrors().isEmpty() }.map { dto ->
            BuchungssatzDO().also { satz ->
                satz.year = dto.year
                satz.month = dto.month
                satz.satznr = dto.satznr
                satz.datum = dto.datum
                satz.betrag = dto.betrag // Already signed.
                satz.konto = kontoCache.getKonto(dto.kontoId)
                satz.gegenKonto = kontoCache.getKonto(dto.gegenKontoId)
                satz.kost1 = kostCache.getKost1(dto.kost1Id)
                satz.kost2 = kostCache.getKost2(dto.kost2Id)
                satz.isIgnore = dto.ignore
            }
        }
    }

    /** Uses only the foreign keys and the caches, never the lazy associations of the stored record. */
    private fun toStoredDTO(record: BuchungssatzDO): DatevRecordImportDTO {
        val context = ApplicationContextProvider.getApplicationContext()
        val kontoCache = context.getBean(KontoCache::class.java)
        val kostCache = context.getBean(KostCache::class.java)
        val kontoId = record.konto?.id
        val gegenKontoId = record.gegenKonto?.id
        val kost1Id = record.kost1?.id
        val kost2Id = record.kost2?.id
        return DatevRecordImportDTO(
            year = record.year,
            month = record.month,
            satznr = record.satznr,
            datum = record.datum,
            betrag = record.betrag,
            sh = record.sh?.let { if (it == SHType.SOLL) "S" else "H" },
            konto = kontoCache.getKonto(kontoId)?.nummer,
            kontoId = kontoId,
            gegenKonto = kontoCache.getKonto(gegenKontoId)?.nummer,
            gegenKontoId = gegenKontoId,
            kost1 = kostCache.getKost1(kost1Id)?.let { DatevRecordExcelImporter.formatKost1(it) },
            kost1Id = kost1Id,
            kost2 = kostCache.getKost2(kost2Id)?.let { DatevRecordExcelImporter.formatKost2(it) },
            kost2Id = kost2Id,
            menge = record.menge,
            beleg = record.beleg,
            text = record.text,
            comment = record.comment,
            id = record.id,
        )
    }
}
