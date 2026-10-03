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
import org.projectforge.business.fibu.KontoDO
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage

private val log = KotlinLogging.logger {}

/**
 * The session-held storage of the DATEV chart of accounts import. Accounts are matched by their number; a match
 * is MODIFIED if its name differs, otherwise UNMODIFIED. Accounts missing in the file are not deleted.
 *
 * @param loadAccounts Loads all stored accounts (including deleted ones); replaceable for tests.
 * @author Kai Reinhard
 */
class DatevAccountImportStorage(
    private val loadAccounts: (() -> List<KontoDO>)? = null,
) : ImportStorage<DatevAccountImportDTO>(ImportSettings()) {

    override val targetEntityTitle: String
        get() = translate("fibu.datev.import.accounts")

    /** The accounts the parser read from the file, kept so a re-reconcile can rebuild the pairs. */
    val readAccounts = mutableListOf<DatevAccountImportDTO>()

    private var lastReconcileSummary: String? = null

    override fun prepareEntity(): DatevAccountImportDTO {
        return DatevAccountImportDTO()
    }

    override fun commitEntity(obj: DatevAccountImportDTO) {
        readAccounts.add(obj)
        val pairEntry = ImportPairEntry(read = obj)
        obj.getErrors().forEach { pairEntry.addError(it) }
        addEntry(pairEntry)
    }

    override fun commitEntity(pairEntry: ImportPairEntry<DatevAccountImportDTO>) {
        pairEntry.read?.let { dto ->
            readAccounts.add(dto)
            dto.getErrors().forEach { pairEntry.addError(it) }
        }
        addEntry(pairEntry)
    }

    override fun doReconcileImportStorage(rereadDatabaseEntries: Boolean) {
        val accounts = loadAccounts?.invoke() ?: ApplicationContextProvider.getApplicationContext()
            .getBean(PfPersistenceService::class.java)
            .executeQuery("from KontoDO", KontoDO::class.java)
        val byNummer = accounts.mapNotNull { konto -> konto.nummer?.let { it to konto } }.toMap()
        clearEntries()
        readAccounts.forEach { read ->
            val stored = byNummer[read.nummer]?.let { konto ->
                DatevAccountImportDTO(
                    nummer = konto.nummer,
                    bezeichnung = konto.bezeichnung,
                    id = konto.id,
                )
            }
            val pairEntry = ImportPairEntry(read = read, stored = stored)
            read.getErrors().forEach { pairEntry.addError(it) }
            addEntry(pairEntry)
        }
        // Logged on a changed result only: the preview reconciles again on every change of its display options.
        val summary = DatevImportLog.reconcileSummary(pairEntries)
        if (summary != lastReconcileSummary) {
            lastReconcileSummary = summary
            log.info { "Reconciled ${pairEntries.size} accounts of file '$filename' with the database: $summary." }
        }
    }
}
