/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.importer.AbstractImportJob
import org.projectforge.rest.importer.ImportPairEntry

private val log = KotlinLogging.logger {}

class EingangsrechnungImportJob(
    private val eingangsrechnungDao: EingangsrechnungDao,
    private val selectedEntries: List<ImportPairEntry<EingangsrechnungPosImportDTO>>,
    private val importStorage: EingangsrechnungImportStorage,
) : AbstractImportJob(
    translateMsg("fibu.eingangsrechnung.import.job.title", selectedEntries.size.toString()),
    area = "EingangsrechnungImport",
    queueName = "eingangsrechnungImport",
    timeoutSeconds = 600,
    importStorage = importStorage,
) {

    init {
        totalNumber = selectedEntries.size
        processedNumber = 0
    }

    override fun onBeforeStart() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
        updateTotals(importStorage)
    }

    override fun onAfterTermination() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
    }

    override suspend fun run() {
        log.info("Starting import of ${selectedEntries.size} incoming invoices.")

        for (entry in selectedEntries) {
            if (!isActive) {
                // Job is being cancelled
                return
            }
            processedNumber += 1

            val storedId = entry.stored?.id
            val readEntry = entry.read

            if (readEntry == null) {
                // Handle deletion
                if (storedId != null) {
                    val existingEntity = eingangsrechnungDao.find(storedId)
                    if (existingEntity != null && !existingEntity.deleted) {
                        eingangsrechnungDao.markAsDeleted(existingEntity)
                        result.deleted += 1
                    }
                }
                continue
            }

            if (storedId != null) {
                // Update existing invoice
                val existingEntity = eingangsrechnungDao.find(storedId)
                if (existingEntity != null) {
                    readEntry.copyTo(existingEntity)
                    val modStatus = eingangsrechnungDao.update(existingEntity)
                    if (modStatus != EntityCopyStatus.NONE) {
                        result.updated += 1
                    } else {
                        result.unmodified += 1
                    }
                } else {
                    // Stored entity not found, insert as new
                    val dbEntity = EingangsrechnungDO()
                    readEntry.copyTo(dbEntity)
                    eingangsrechnungDao.insert(dbEntity)
                    result.inserted += 1
                }
            } else {
                // Insert new invoice
                val dbEntity = EingangsrechnungDO()
                readEntry.copyTo(dbEntity)
                eingangsrechnungDao.insert(dbEntity)
                result.inserted += 1
            }
        }

        log.info("Import completed: inserted=${result.inserted}, updated=${result.updated}, deleted=${result.deleted}, unmodified=${result.unmodified}")
    }

    override fun readAccess(user: PFUserDO?): Boolean {
        user ?: return false
        return isOwner || eingangsrechnungDao.hasUserSelectAccess(user, false)
    }

    override fun writeAccess(user: PFUserDO?): Boolean {
        user ?: return false
        // For import operations, we need general insert/update access to incoming invoices
        // Since this is a batch operation, we check general insert access
        return isOwner || eingangsrechnungDao.hasLoggedInUserInsertAccess()
    }
}
