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
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.datev.DatevImportService
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.importer.AbstractImportJob
import org.projectforge.rest.importer.ImportPairEntry

private val log = KotlinLogging.logger {}

/**
 * Persists the selected DATEV chart of accounts import rows. Only the account name is imported: an existing
 * account is loaded and only its name is changed (the legacy import replaced the whole account and wiped its
 * description, status, address and e-invoice data); a new account gets number and name.
 *
 * @author Kai Reinhard
 */
class DatevAccountImportJob(
    private val kontoDao: KontoDao,
    private val selectedEntries: List<ImportPairEntry<DatevAccountImportDTO>>,
    private val importStorage: DatevAccountImportStorage,
) : AbstractImportJob(
    translateMsg("fibu.datev.import.accounts.job.title", selectedEntries.size.toString()),
    area = "DatevAccountImport",
    queueName = "datevAccountImport",
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
        log.info { "Starting import of ${selectedEntries.size} DATEV accounts." }
        val toPersist = mutableListOf<KontoDO>()
        for (entry in selectedEntries) {
            if (!isActive) {
                log.info { "Import cancelled, nothing was saved." }
                return
            }
            val read = entry.read ?: continue // No deletions.
            val nummer = read.nummer
            if (nummer == null || read.bezeichnung.isNullOrBlank()) {
                log.warn { "Skipping incomplete account: $nummer" }
                continue
            }
            val storedId = entry.stored?.id
            val konto = if (storedId != null) {
                kontoDao.find(storedId, checkAccess = false) ?: continue
            } else {
                KontoDO().also { it.nummer = nummer }
            }
            konto.bezeichnung = read.bezeichnung
            if (storedId != null) {
                result.updated += 1
            } else {
                result.inserted += 1
            }
            toPersist.add(konto)
        }
        // One transaction as before (all or nothing), but block by block, so the progress follows the database work.
        totalNumber = toPersist.size
        try {
            kontoDao.persistenceService.runInTransaction { context ->
                toPersist.chunked(INSERT_BLOCK_SIZE).forEach { block ->
                    kontoDao.insertOrUpdate(block, checkAccess = false)
                    context.flush()
                    processedNumber += block.size
                    log.info { "Saved $processedNumber of $totalNumber accounts." }
                }
            }
        } catch (ex: Exception) {
            // Logged here too: the job framework's own failure message doesn't reach the user's import log.
            log.error(ex) { "Import failed, nothing was saved (all or nothing): ${ex.message}" }
            throw ex
        }
        log.info { "Import completed: inserted=${result.inserted}, updated=${result.updated}." }
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
