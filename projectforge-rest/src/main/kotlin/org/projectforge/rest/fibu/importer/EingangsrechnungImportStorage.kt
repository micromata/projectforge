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
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage
import java.time.LocalDate

private val log = KotlinLogging.logger {}

class EingangsrechnungImportStorage(importSettings: String? = null) :
    ImportStorage<EingangsrechnungPosImportDTO>(
        ImportSettings()
            .parseSettings(
                importSettings,
                EingangsrechnungDO::class.java,
                EingangsrechnungDO::kreditor.name
            )
    ) {

    var readInvoices = mutableListOf<EingangsrechnungPosImportDTO>()

    /**
     * Map of consolidated invoices grouped by RENR (invoice number).
     * Key: RENR (invoice number)
     * Value: List of positions belonging to that invoice
     */
    var consolidatedInvoices = mapOf<String, List<EingangsrechnungPosImportDTO>>()

    /**
     * Database invoices within the import date range for reconciliation matching.
     */
    var databaseInvoices: List<EingangsrechnungDO>? = null

    private var fromDate: LocalDate? = null
    private var untilDate: LocalDate? = null

    override fun prepareEntity(): EingangsrechnungPosImportDTO {
        return EingangsrechnungPosImportDTO()
    }

    override fun commitEntity(obj: EingangsrechnungPosImportDTO) {
        readInvoices.add(obj)
        val pairEntry = ImportPairEntry(read = obj)
        addEntry(pairEntry)
    }

    /**
     * Commits an ImportPairEntry that was prepared using prepareImportPairEntry().
     * This method is preferred for new parsing implementations as it allows
     * setting errors directly during parsing.
     */
    override fun commitEntity(pairEntry: ImportPairEntry<EingangsrechnungPosImportDTO>) {
        pairEntry.read?.let { dto ->
            readInvoices.add(dto)
        }
        addEntry(pairEntry)
    }

    /**
     * Reconciles imported invoice data with existing database entries to identify matches.
     *
     * This method implements a sophisticated matching algorithm similar to BankingImportStorage:
     * 1. Analyzes imported invoices to determine date range
     * 2. Loads database invoices within that date range
     * 3. Groups invoices by date for efficient processing
     * 4. Uses score-based matching algorithm to find best matches
     * 5. Creates ImportPairEntry objects with status: NEW, MATCHED, or DELETED
     *
     * @param rereadDatabaseEntries If true, reloads database entries; if false, uses cached data
     */
    override fun reconcileImportStorage(rereadDatabaseEntries: Boolean) {
        // Determine date range from imported invoices
        analyzeReadInvoices()
        val from = fromDate
        val until = untilDate
        if (from == null || until == null || from > until) {
            return // No date range available for reconciliation
        }

        // Load database invoices within import date range
        if (rereadDatabaseEntries) {
            val eingangsrechnungDao = ApplicationContextProvider.getApplicationContext().getBean(EingangsrechnungDao::class.java)
            databaseInvoices = eingangsrechnungDao.getByDateRange(from, until)
        }

        // Clear existing pair entries before rebuilding
        clearEntries()

        // Process all invoices together to allow cross-date matching
        // The date matching score will handle date proximity properly
        val readInvoicesWithDate = readInvoices.filter { it.datum != null }
        val readInvoicesWithoutDate = readInvoices.filter { it.datum == null }
        val dbInvoicesAll = databaseInvoices ?: emptyList()

        if (readInvoicesWithDate.isNotEmpty() || dbInvoicesAll.isNotEmpty()) {
            if (isPositionBasedImport) {
                buildMatchingPairs(readInvoicesWithDate, dbInvoicesAll)
            } else {
                buildHeaderOnlyMatchingPairs(readInvoicesWithDate, dbInvoicesAll)
            }
        }

        // Handle imported invoices without date separately (mark as NEW)
        if (readInvoicesWithoutDate.isNotEmpty()) {
            if (isPositionBasedImport) {
                buildMatchingPairs(readInvoicesWithoutDate, emptyList())
            } else {
                buildHeaderOnlyMatchingPairs(readInvoicesWithoutDate, emptyList())
            }
        }
    }

    private fun analyzeReadInvoices() {
        readInvoices.removeIf { it.datum == null }
        readInvoices.sortBy { it.datum }
        if (readInvoices.isEmpty()) {
            return
        }
        fromDate = readInvoices.first().datum!!
        untilDate = readInvoices.last().datum!!
    }

    private fun buildMatchingPairs(
        readByDate: List<EingangsrechnungPosImportDTO>,
        dbInvoicesByDate: List<EingangsrechnungDO>
    ) {
        if (readByDate.isEmpty()) {
            // Only database entries exist - mark as deleted
            dbInvoicesByDate.forEach { dbInvoice ->
                addEntry(ImportPairEntry(null, createImportDTO(dbInvoice)))
            }
            return
        }

        val scoreMatrix = Array(readByDate.size) { IntArray(dbInvoicesByDate.size) }
        for (i in readByDate.indices) {
            for (j in dbInvoicesByDate.indices) {
                scoreMatrix[i][j] = readByDate[i].matchScore(dbInvoicesByDate[j])
            }
        }

        val takenReadEntries = mutableSetOf<Int>()
        val takenDbEntries = mutableSetOf<Int>()

        // Match highest scoring pairs first
        for (iteration in 0..(readByDate.size + dbInvoicesByDate.size)) {
            var maxScore = 0
            var maxReadIndex = -1
            var maxDbIndex = -1

            for (i in readByDate.indices) {
                if (takenReadEntries.contains(i)) continue
                for (j in dbInvoicesByDate.indices) {
                    if (takenDbEntries.contains(j)) continue
                    if (scoreMatrix[i][j] > maxScore) {
                        maxScore = scoreMatrix[i][j]
                        maxReadIndex = i
                        maxDbIndex = j
                    }
                }
            }

            if (maxScore < 25) break // Require meaningful match (creditor similarity + additional factors)

            takenReadEntries.add(maxReadIndex)
            takenDbEntries.add(maxDbIndex)
            addEntry(ImportPairEntry(readByDate[maxReadIndex], createImportDTO(dbInvoicesByDate[maxDbIndex])))
        }

        // Add unmatched read entries as new
        for (i in readByDate.indices) {
            if (!takenReadEntries.contains(i)) {
                addEntry(ImportPairEntry(readByDate[i], null))
            }
        }

        // Add unmatched database entries as deleted
        for (j in dbInvoicesByDate.indices) {
            if (!takenDbEntries.contains(j)) {
                addEntry(ImportPairEntry(null, createImportDTO(dbInvoicesByDate[j])))
            }
        }
    }

    /**
     * Builds matching pairs for header-only imports.
     * Only matches based on header fields, preserves existing positions.
     */
    private fun buildHeaderOnlyMatchingPairs(
        readByDate: List<EingangsrechnungPosImportDTO>,
        dbInvoicesByDate: List<EingangsrechnungDO>
    ) {
        if (readByDate.isEmpty()) {
            // Only database entries exist - mark as deleted (header-only, so just the header)
            dbInvoicesByDate.forEach { dbInvoice ->
                addEntry(ImportPairEntry(null, createImportDTO(dbInvoice)))
            }
            return
        }

        // For header-only import, match based on invoice header data only
        // Each read entry represents a complete invoice header
        val scoreMatrix = Array(readByDate.size) { IntArray(dbInvoicesByDate.size) }
        for (i in readByDate.indices) {
            for (j in dbInvoicesByDate.indices) {
                // Use header-only matching score (higher weight on header fields)
                scoreMatrix[i][j] = readByDate[i].headerMatchScore(dbInvoicesByDate[j])
            }
        }

        val takenReadEntries = mutableSetOf<Int>()
        val takenDbEntries = mutableSetOf<Int>()

        // Match highest scoring pairs first
        for (iteration in 0..(readByDate.size + dbInvoicesByDate.size)) {
            var maxScore = 0
            var maxReadIndex = -1
            var maxDbIndex = -1

            for (i in readByDate.indices) {
                if (takenReadEntries.contains(i)) continue
                for (j in dbInvoicesByDate.indices) {
                    if (takenDbEntries.contains(j)) continue
                    if (scoreMatrix[i][j] > maxScore) {
                        maxScore = scoreMatrix[i][j]
                        maxReadIndex = i
                        maxDbIndex = j
                    }
                }
            }

            if (maxScore < 25) break // Require meaningful match for header-only (same as position-based)

            takenReadEntries.add(maxReadIndex)
            takenDbEntries.add(maxDbIndex)
            // For header-only import, the matched entry updates header data only
            val dbDto = createImportDTO(dbInvoicesByDate[maxDbIndex])
            addEntry(ImportPairEntry(readByDate[maxReadIndex], dbDto))
        }

        // Add unmatched read entries as new
        for (i in readByDate.indices) {
            if (!takenReadEntries.contains(i)) {
                addEntry(ImportPairEntry(readByDate[i], null))
            }
        }

        // Add unmatched database entries as deleted (header-only)
        for (j in dbInvoicesByDate.indices) {
            if (!takenDbEntries.contains(j)) {
                addEntry(ImportPairEntry(null, createImportDTO(dbInvoicesByDate[j])))
            }
        }
    }

    private fun createImportDTO(eingangsrechnungDO: EingangsrechnungDO): EingangsrechnungPosImportDTO {
        val dto = EingangsrechnungPosImportDTO()
        dto.copyFrom(eingangsrechnungDO)

        // Calculate grossSum from invoice positions
        try {
            // Always calculate to ensure we have current values
            dto.grossSum = eingangsrechnungDO.ensuredInfo.grossSum
        } catch (e: Exception) {
            log.error("Could not calculate grossSum for invoice ${eingangsrechnungDO.id}, using zahlBetrag as fallback", e)
        }

        return dto
    }
}
