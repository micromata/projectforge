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

    override suspend fun run() {
        log.info("Starting import of ${selectedEntries.size} incoming invoices.")

        selectedEntries.forEachIndexed { index, entry ->
            val dbEntity = EingangsrechnungDO()
            entry.read?.copyTo(dbEntity)

            val status = if (entry.stored?.id != null) {
                val existingEntity = eingangsrechnungDao.find(entry.stored!!.id!!)
                if (existingEntity != null) {
                    entry.read?.copyTo(existingEntity)
                    eingangsrechnungDao.update(existingEntity)
                    EntityCopyStatus.MAJOR
                } else {
                    eingangsrechnungDao.insert(dbEntity)
                    EntityCopyStatus.MAJOR
                }
            } else {
                eingangsrechnungDao.insert(dbEntity)
                EntityCopyStatus.MAJOR
            }

            //setProgress((index + 1).toDouble() / selectedEntries.size * 100)
        }

        //setResultMessage(translateMsg("fibu.eingangsrechnung.import.result", selectedEntries.size.toString()))
        log.info("Import completed: ${selectedEntries.size} invoices processed")
    }

    override fun writeAccess(user: PFUserDO?): Boolean {
        TODO("Not yet implemented")
    }
}
