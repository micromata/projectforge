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

import mu.KotlinLogging
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.datev.DatevImportService
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.SHType
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.importer.AbstractImportJob
import org.projectforge.rest.importer.ImportPairEntry

private val log = KotlinLogging.logger {}

/**
 * Persists the selected DATEV accounting record import rows. A row with a matching database record
 * ([ImportPairEntry.stored]) is updated by its id, an unmatched row is inserted; records are never deleted.
 * The voucher date is stored as read, even if it lies outside the booking batch (year/month).
 *
 * @author Kai Reinhard
 */
class DatevRecordImportJob(
    private val buchungssatzDao: BuchungssatzDao,
    private val kontoCache: KontoCache,
    private val kostCache: KostCache,
    private val selectedEntries: List<ImportPairEntry<DatevRecordImportDTO>>,
    private val importStorage: DatevRecordImportStorage,
) : AbstractImportJob(
    translateMsg("fibu.datev.import.records.job.title", selectedEntries.size.toString()),
    area = "DatevRecordImport",
    queueName = "datevRecordImport",
    timeoutSeconds = 600,
    importStorage = importStorage,
    selectedEntries = selectedEntries,
) {

    init {
        totalNumber = selectedEntries.size
        processedNumber = 0
    }

    override fun onBeforeStart() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
        // Mark all entries as not reconciled so they show status UNKNOWN until processed.
        importStorage.pairEntries.forEach { it.reconciled = false }
    }

    override fun onAfterTermination() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
    }

    override suspend fun run() {
        log.info("Starting import of ${selectedEntries.size} DATEV accounting records.")
        val toPersist = mutableListOf<BuchungssatzDO>()
        for (entry in selectedEntries) {
            if (!isActive) {
                return
            }
            val read = entry.read ?: continue // No deletions.
            val satz = buildRecord(read)
            if (satz == null) {
                // Faulty rows are filtered out before commit, this is a last-resort guard.
                log.warn { "Skipping incomplete accounting record: ${read.year}/${read.month} #${read.satznr}" }
                continue
            }
            val stored = entry.stored
            if (stored?.id != null) {
                satz.id = stored.id
                // The comment is editable in ProjectForge, keep it if the file has none.
                if (satz.comment.isNullOrBlank()) {
                    satz.comment = stored.comment
                }
                result.updated += 1
            } else {
                result.inserted += 1
            }
            toPersist.add(satz)
        }
        // One transaction as before (all or nothing), but block by block, so the progress follows the database work.
        totalNumber = toPersist.size
        buchungssatzDao.persistenceService.runInTransaction { context ->
            toPersist.chunked(INSERT_BLOCK_SIZE).forEach { block ->
                buchungssatzDao.insertOrUpdate(block, checkAccess = false)
                context.flush()
                processedNumber += block.size
            }
        }
        log.info("Import completed: inserted=${result.inserted}, updated=${result.updated}.")
    }

    private fun buildRecord(read: DatevRecordImportDTO): BuchungssatzDO? {
        val konto = kontoCache.getKonto(read.kontoId)
        val gegenKonto = kontoCache.getKonto(read.gegenKontoId)
        val kost1 = kostCache.getKost1(read.kost1Id)
        val kost2 = kostCache.getKost2(read.kost2Id)
        val sh = when (read.sh) {
            "S" -> SHType.SOLL
            "H" -> SHType.HABEN
            else -> null
        }
        if (read.year == null || read.month == null || read.satznr == null || read.datum == null || read.betrag == null
            || sh == null || konto == null || gegenKonto == null || kost1 == null || kost2 == null
        ) {
            return null
        }
        return BuchungssatzDO().also { satz ->
            satz.year = read.year
            satz.month = read.month
            satz.satznr = read.satznr
            satz.datum = read.datum
            satz.betrag = read.betrag // Already signed by BuchungssatzDO.calculate while parsing.
            satz.sh = sh
            satz.konto = konto
            satz.gegenKonto = gegenKonto
            satz.kost1 = kost1
            satz.kost2 = kost2
            satz.menge = read.menge
            satz.beleg = read.beleg
            satz.text = read.text
            satz.comment = read.comment
        }
    }

    override fun readAccess(user: PFUserDO?): Boolean {
        user ?: return false
        return isOwner || hasRight(user)
    }

    override fun writeAccess(user: PFUserDO?): Boolean {
        user ?: return false
        return isOwner || hasRight(user)
    }

    private fun hasRight(user: PFUserDO): Boolean {
        return accessChecker.hasRight(user, DatevImportService.USER_RIGHT_ID, UserRightValue.TRUE)
    }

    companion object {
        private const val INSERT_BLOCK_SIZE = 50
    }
}
