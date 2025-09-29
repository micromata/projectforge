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

        // Sort all pair entries after matching is complete
        sortPairEntriesAfterMatching()
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

        // Use optimized multi-stage matching algorithm
        buildOptimizedMatchingPairs(readByDate, dbInvoicesByDate)
    }

    /**
     * Optimized multi-stage matching algorithm for better performance with large datasets.
     *
     * Stage 1: Exact matches (O(n)) - immediate matches without score calculation
     * Stage 2: Grouped matching (O(n log n)) - group by key fields and match within groups
     * Stage 3: Fallback matching - remaining invoices with limited comparisons
     */
    private fun buildOptimizedMatchingPairs(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>
    ) {
        val matchedReadIndices = mutableSetOf<Int>()
        val matchedDbIndices = mutableSetOf<Int>()
        val matches = mutableListOf<Pair<Int, Int>>()

        // Stage 1: Exact matches - O(n)
        log.debug("Stage 1: Looking for exact matches...")
        findExactMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Stage 2: Grouped matches - O(n log n)
        log.debug("Stage 2: Looking for grouped matches...")
        findGroupedMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Stage 3: Fallback matches for remaining invoices
        log.debug("Stage 3: Fallback matching for remaining invoices...")
        findFallbackMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Create ImportPairEntry objects from matches
        matches.forEach { (readIndex, dbIndex) ->
            addEntry(ImportPairEntry(readInvoices[readIndex], createImportDTO(dbInvoices[dbIndex])))
        }

        // Add unmatched read entries as new
        readInvoices.forEachIndexed { index, invoice ->
            if (!matchedReadIndices.contains(index)) {
                addEntry(ImportPairEntry(invoice, null))
            }
        }

        // Add unmatched database entries as deleted
        dbInvoices.forEachIndexed { index, invoice ->
            if (!matchedDbIndices.contains(index)) {
                addEntry(ImportPairEntry(null, createImportDTO(invoice)))
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

        // Use optimized multi-stage matching algorithm for header-only imports too
        buildOptimizedHeaderOnlyMatchingPairs(readByDate, dbInvoicesByDate)
    }

    /**
     * Optimized header-only matching using the same multi-stage approach.
     */
    private fun buildOptimizedHeaderOnlyMatchingPairs(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>
    ) {
        val matchedReadIndices = mutableSetOf<Int>()
        val matchedDbIndices = mutableSetOf<Int>()
        val matches = mutableListOf<Pair<Int, Int>>()

        // Stage 1: Exact matches - O(n)
        log.debug("Header-only Stage 1: Looking for exact matches...")
        findExactMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Stage 2: Grouped matches - O(n log n)
        log.debug("Header-only Stage 2: Looking for grouped matches...")
        findGroupedHeaderMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Stage 3: Fallback matches for remaining invoices
        log.debug("Header-only Stage 3: Fallback matching for remaining invoices...")
        findFallbackHeaderMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Create ImportPairEntry objects from matches (header-only style)
        matches.forEach { (readIndex, dbIndex) ->
            val dbDto = createImportDTO(dbInvoices[dbIndex])
            addEntry(ImportPairEntry(readInvoices[readIndex], dbDto))
        }

        // Add unmatched read entries as new
        readInvoices.forEachIndexed { index, invoice ->
            if (!matchedReadIndices.contains(index)) {
                addEntry(ImportPairEntry(invoice, null))
            }
        }

        // Add unmatched database entries as deleted (header-only)
        dbInvoices.forEachIndexed { index, invoice ->
            if (!matchedDbIndices.contains(index)) {
                addEntry(ImportPairEntry(null, createImportDTO(invoice)))
            }
        }
    }

    /**
     * Grouped matches for header-only imports using headerMatchScore.
     */
    private fun findGroupedHeaderMatches(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        val initialMatches = matches.size

        // Use header-specific matching for grouped searches
        findHeaderMatchesInReferenzGroups(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)
        findHeaderMatchesInCreditorGroups(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        log.debug("Header-only Stage 2 completed: ${matches.size - initialMatches} grouped matches found")
    }

    /**
     * Fallback header-only matches using headerMatchScore.
     */
    private fun findFallbackHeaderMatches(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        val initialMatches = matches.size
        val unmatchedRead = readInvoices.filterIndexed { index, _ -> !matchedReadIndices.contains(index) }
        val unmatchedDb = dbInvoices.filterIndexed { index, _ -> !matchedDbIndices.contains(index) }

        if (unmatchedRead.isEmpty() || unmatchedDb.isEmpty()) {
            return
        }

        // Use headerMatchScore for fallback matching
        if (unmatchedRead.size * unmatchedDb.size <= 1000) {
            val readToOriginalIndex = readInvoices.mapIndexed { index, invoice -> invoice to index }.toMap()
            val dbToOriginalIndex = dbInvoices.mapIndexed { index, invoice -> invoice to index }.toMap()

            val scoreMatrix = Array(unmatchedRead.size) { IntArray(unmatchedDb.size) }

            // Calculate header scores
            for (i in unmatchedRead.indices) {
                for (j in unmatchedDb.indices) {
                    val score = unmatchedRead[i].headerMatchScore(unmatchedDb[j])
                    scoreMatrix[i][j] = score
                }
            }

            val takenReadIndices = mutableSetOf<Int>()
            val takenDbIndices = mutableSetOf<Int>()

            // Find best matches iteratively
            while (true) {
                var maxScore = 0
                var maxReadIndex = -1
                var maxDbIndex = -1

                for (i in unmatchedRead.indices) {
                    if (takenReadIndices.contains(i)) continue
                    for (j in unmatchedDb.indices) {
                        if (takenDbIndices.contains(j)) continue
                        if (scoreMatrix[i][j] > maxScore) {
                            maxScore = scoreMatrix[i][j]
                            maxReadIndex = i
                            maxDbIndex = j
                        }
                    }
                }

                if (maxScore < 25) break // Same threshold for header-only

                takenReadIndices.add(maxReadIndex)
                takenDbIndices.add(maxDbIndex)

                val originalReadIndex = readToOriginalIndex[unmatchedRead[maxReadIndex]]!!
                val originalDbIndex = dbToOriginalIndex[unmatchedDb[maxDbIndex]]!!

                matches.add(Pair(originalReadIndex, originalDbIndex))
                matchedReadIndices.add(originalReadIndex)
                matchedDbIndices.add(originalDbIndex)
            }
        }

        log.debug("Header-only Stage 3 completed: ${matches.size - initialMatches} fallback matches found")
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

    /**
     * Stage 1: Find exact matches using hash-based lookups for maximum performance.
     * Creates hash maps for fast O(1) lookups of exact matches.
     */
    private fun findExactMatches(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        // Create hash maps for fast lookups - O(n)
        val dbByReferenzDateAmount = mutableMapOf<String, MutableList<Pair<Int, EingangsrechnungDO>>>()
        val dbByReferenzCreditor = mutableMapOf<String, MutableList<Pair<Int, EingangsrechnungDO>>>()

        // Build hash maps for database invoices
        dbInvoices.forEachIndexed { index, dbInvoice ->
            if (matchedDbIndices.contains(index)) return@forEachIndexed

            // Primary key: referenz + datum + grossSum
            val referenz = dbInvoice.referenz?.trim()?.lowercase()
            val datum = dbInvoice.datum
            val grossSum = try { dbInvoice.ensuredInfo.grossSum } catch (e: Exception) { null }

            if (!referenz.isNullOrBlank() && datum != null && grossSum != null) {
                val primaryKey = "$referenz|$datum|$grossSum"
                dbByReferenzDateAmount.getOrPut(primaryKey) { mutableListOf() }.add(Pair(index, dbInvoice))
            }

            // Secondary key: referenz + kreditor
            val kreditor = dbInvoice.kreditor?.trim()?.lowercase()
            if (!referenz.isNullOrBlank() && !kreditor.isNullOrBlank()) {
                val secondaryKey = "$referenz|$kreditor"
                dbByReferenzCreditor.getOrPut(secondaryKey) { mutableListOf() }.add(Pair(index, dbInvoice))
            }
        }

        // Find matches for read invoices - O(n)
        readInvoices.forEachIndexed { readIndex, readInvoice ->
            if (matchedReadIndices.contains(readIndex)) return@forEachIndexed

            val referenz = readInvoice.referenz?.trim()?.lowercase()
            val datum = readInvoice.datum
            val grossSum = readInvoice.grossSum

            // Try primary key match first (referenz + datum + grossSum)
            if (!referenz.isNullOrBlank() && datum != null && grossSum != null) {
                val primaryKey = "$referenz|$datum|$grossSum"
                dbByReferenzDateAmount[primaryKey]?.firstOrNull { (dbIndex, _) ->
                    !matchedDbIndices.contains(dbIndex)
                }?.let { (dbIndex, _) ->
                    matches.add(Pair(readIndex, dbIndex))
                    matchedReadIndices.add(readIndex)
                    matchedDbIndices.add(dbIndex)
                    log.debug("Exact match (primary): ${readInvoice.referenz}")
                    return@forEachIndexed
                }
            }

            // Try secondary key match (referenz + kreditor)
            val kreditor = readInvoice.kreditor?.trim()?.lowercase()
            if (!referenz.isNullOrBlank() && !kreditor.isNullOrBlank()) {
                val secondaryKey = "$referenz|$kreditor"
                dbByReferenzCreditor[secondaryKey]?.firstOrNull { (dbIndex, _) ->
                    !matchedDbIndices.contains(dbIndex)
                }?.let { (dbIndex, _) ->
                    matches.add(Pair(readIndex, dbIndex))
                    matchedReadIndices.add(readIndex)
                    matchedDbIndices.add(dbIndex)
                    log.debug("Exact match (secondary): ${readInvoice.referenz}")
                    return@forEachIndexed
                }
            }
        }

        log.debug("Stage 1 completed: ${matches.size} exact matches found")
    }

    /**
     * Stage 2: Find grouped matches by organizing invoices into relevant groups
     * and only comparing within those groups.
     */
    private fun findGroupedMatches(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        val initialMatches = matches.size

        // Group by invoice number (referenz)
        findMatchesInReferenzGroups(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Group by creditor for remaining unmatched invoices
        findMatchesInCreditorGroups(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        log.debug("Stage 2 completed: ${matches.size - initialMatches} grouped matches found")
    }

    /**
     * Stage 3: Fallback matching for remaining invoices with limited comparisons.
     * Uses the traditional scoring approach but with optimizations.
     */
    private fun findFallbackMatches(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        val initialMatches = matches.size
        val unmatchedRead = readInvoices.filterIndexed { index, _ -> !matchedReadIndices.contains(index) }
        val unmatchedDb = dbInvoices.filterIndexed { index, _ -> !matchedDbIndices.contains(index) }

        if (unmatchedRead.isEmpty() || unmatchedDb.isEmpty()) {
            return
        }

        // For smaller remaining sets, use the original algorithm with optimizations
        if (unmatchedRead.size * unmatchedDb.size <= 1000) { // Limit to reasonable comparison count
            val readToOriginalIndex = readInvoices.mapIndexed { index, invoice -> invoice to index }.toMap()
            val dbToOriginalIndex = dbInvoices.mapIndexed { index, invoice -> invoice to index }.toMap()

            val scoreMatrix = Array(unmatchedRead.size) { IntArray(unmatchedDb.size) }

            // Calculate scores with early exit optimization
            for (i in unmatchedRead.indices) {
                for (j in unmatchedDb.indices) {
                    val score = unmatchedRead[i].matchScore(unmatchedDb[j])
                    scoreMatrix[i][j] = score
                }
            }

            val takenReadIndices = mutableSetOf<Int>()
            val takenDbIndices = mutableSetOf<Int>()

            // Find best matches iteratively
            while (true) {
                var maxScore = 0
                var maxReadIndex = -1
                var maxDbIndex = -1

                for (i in unmatchedRead.indices) {
                    if (takenReadIndices.contains(i)) continue
                    for (j in unmatchedDb.indices) {
                        if (takenDbIndices.contains(j)) continue
                        if (scoreMatrix[i][j] > maxScore) {
                            maxScore = scoreMatrix[i][j]
                            maxReadIndex = i
                            maxDbIndex = j
                        }
                    }
                }

                if (maxScore < 25) break // Require meaningful match

                takenReadIndices.add(maxReadIndex)
                takenDbIndices.add(maxDbIndex)

                // Map back to original indices
                val originalReadIndex = readToOriginalIndex[unmatchedRead[maxReadIndex]]!!
                val originalDbIndex = dbToOriginalIndex[unmatchedDb[maxDbIndex]]!!

                matches.add(Pair(originalReadIndex, originalDbIndex))
                matchedReadIndices.add(originalReadIndex)
                matchedDbIndices.add(originalDbIndex)
            }
        }

        log.debug("Stage 3 completed: ${matches.size - initialMatches} fallback matches found")
    }

    /**
     * Group by invoice number (referenz) and find matches within each group.
     */
    private fun findMatchesInReferenzGroups(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        // Group database invoices by referenz
        val dbByReferenz = dbInvoices.mapIndexed { index, invoice ->
            index to invoice
        }.filter { (index, _) -> !matchedDbIndices.contains(index) }
         .groupBy { (_, invoice) -> invoice.referenz?.trim()?.lowercase() }
         .filterKeys { !it.isNullOrBlank() }

        // Find matches for each read invoice within its referenz group
        readInvoices.forEachIndexed { readIndex, readInvoice ->
            if (matchedReadIndices.contains(readIndex)) return@forEachIndexed

            val referenz = readInvoice.referenz?.trim()?.lowercase()
            if (referenz.isNullOrBlank()) return@forEachIndexed

            val candidateGroup = dbByReferenz[referenz] ?: return@forEachIndexed
            var bestMatch: Pair<Int, Int>? = null
            var bestScore = 24 // Just below our minimum threshold of 25

            // Score all candidates in this referenz group
            candidateGroup.forEach { (dbIndex, dbInvoice) ->
                if (matchedDbIndices.contains(dbIndex)) return@forEach

                val score = readInvoice.matchScore(dbInvoice)
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = Pair(readIndex, dbIndex)
                }
            }

            // Add the best match if it meets our threshold
            bestMatch?.let { (readIdx, dbIdx) ->
                matches.add(Pair(readIdx, dbIdx))
                matchedReadIndices.add(readIdx)
                matchedDbIndices.add(dbIdx)
                log.debug("Referenz group match: ${readInvoice.referenz} (score: $bestScore)")
            }
        }
    }

    /**
     * Group by creditor and find matches within each group for remaining invoices.
     */
    private fun findMatchesInCreditorGroups(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        // Group database invoices by kreditor
        val dbByKreditor = dbInvoices.mapIndexed { index, invoice ->
            index to invoice
        }.filter { (index, _) -> !matchedDbIndices.contains(index) }
         .groupBy { (_, invoice) -> invoice.kreditor?.trim()?.lowercase() }
         .filterKeys { !it.isNullOrBlank() }

        // Find matches for each read invoice within its kreditor group
        readInvoices.forEachIndexed { readIndex, readInvoice ->
            if (matchedReadIndices.contains(readIndex)) return@forEachIndexed

            val kreditor = readInvoice.kreditor?.trim()?.lowercase()
            if (kreditor.isNullOrBlank()) return@forEachIndexed

            val candidateGroup = dbByKreditor[kreditor] ?: return@forEachIndexed
            var bestMatch: Pair<Int, Int>? = null
            var bestScore = 24 // Just below our minimum threshold of 25

            // Score all candidates in this kreditor group
            candidateGroup.forEach { (dbIndex, dbInvoice) ->
                if (matchedDbIndices.contains(dbIndex)) return@forEach

                val score = readInvoice.matchScore(dbInvoice)
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = Pair(readIndex, dbIndex)
                }
            }

            // Add the best match if it meets our threshold
            bestMatch?.let { (readIdx, dbIdx) ->
                matches.add(Pair(readIdx, dbIdx))
                matchedReadIndices.add(readIdx)
                matchedDbIndices.add(dbIdx)
                log.debug("Kreditor group match: ${readInvoice.kreditor} (score: $bestScore)")
            }
        }
    }

    /**
     * Group by invoice number (referenz) and find header matches within each group.
     */
    private fun findHeaderMatchesInReferenzGroups(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        val dbByReferenz = dbInvoices.mapIndexed { index, invoice ->
            index to invoice
        }.filter { (index, _) -> !matchedDbIndices.contains(index) }
         .groupBy { (_, invoice) -> invoice.referenz?.trim()?.lowercase() }
         .filterKeys { !it.isNullOrBlank() }

        readInvoices.forEachIndexed { readIndex, readInvoice ->
            if (matchedReadIndices.contains(readIndex)) return@forEachIndexed

            val referenz = readInvoice.referenz?.trim()?.lowercase()
            if (referenz.isNullOrBlank()) return@forEachIndexed

            val candidateGroup = dbByReferenz[referenz] ?: return@forEachIndexed
            var bestMatch: Pair<Int, Int>? = null
            var bestScore = 24

            candidateGroup.forEach { (dbIndex, dbInvoice) ->
                if (matchedDbIndices.contains(dbIndex)) return@forEach

                val score = readInvoice.headerMatchScore(dbInvoice)
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = Pair(readIndex, dbIndex)
                }
            }

            bestMatch?.let { (readIdx, dbIdx) ->
                matches.add(Pair(readIdx, dbIdx))
                matchedReadIndices.add(readIdx)
                matchedDbIndices.add(dbIdx)
                log.debug("Header referenz group match: ${readInvoice.referenz} (score: $bestScore)")
            }
        }
    }

    /**
     * Group by creditor and find header matches within each group.
     */
    private fun findHeaderMatchesInCreditorGroups(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        val dbByKreditor = dbInvoices.mapIndexed { index, invoice ->
            index to invoice
        }.filter { (index, _) -> !matchedDbIndices.contains(index) }
         .groupBy { (_, invoice) -> invoice.kreditor?.trim()?.lowercase() }
         .filterKeys { !it.isNullOrBlank() }

        readInvoices.forEachIndexed { readIndex, readInvoice ->
            if (matchedReadIndices.contains(readIndex)) return@forEachIndexed

            val kreditor = readInvoice.kreditor?.trim()?.lowercase()
            if (kreditor.isNullOrBlank()) return@forEachIndexed

            val candidateGroup = dbByKreditor[kreditor] ?: return@forEachIndexed
            var bestMatch: Pair<Int, Int>? = null
            var bestScore = 24

            candidateGroup.forEach { (dbIndex, dbInvoice) ->
                if (matchedDbIndices.contains(dbIndex)) return@forEach

                val score = readInvoice.headerMatchScore(dbInvoice)
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = Pair(readIndex, dbIndex)
                }
            }

            bestMatch?.let { (readIdx, dbIdx) ->
                matches.add(Pair(readIdx, dbIdx))
                matchedReadIndices.add(readIdx)
                matchedDbIndices.add(dbIdx)
                log.debug("Header kreditor group match: ${readInvoice.kreditor} (score: $bestScore)")
            }
        }
    }

    /**
     * Sort all pair entries after matching is complete.
     * Sort order: Date, Creditor, Invoice Number, Position Number
     */
    private fun sortPairEntriesAfterMatching() {
        log.debug("Sorting ${pairEntries.size} pair entries by date, creditor, invoice number, and position")

        pairEntries.sortWith(compareBy<ImportPairEntry<EingangsrechnungPosImportDTO>> { pairEntry ->
            // Primary sort: Date (nulls last)
            pairEntry.read?.datum ?: pairEntry.stored?.datum
        }.thenBy { pairEntry ->
            // Secondary sort: Creditor (case-insensitive, nulls last)
            pairEntry.read?.kreditor?.trim()?.lowercase()
                ?: pairEntry.stored?.kreditor?.trim()?.lowercase()
        }.thenBy { pairEntry ->
            // Tertiary sort: Invoice Number (case-insensitive, nulls last)
            pairEntry.read?.referenz?.trim()?.lowercase()
                ?: pairEntry.stored?.referenz?.trim()?.lowercase()
        }.thenBy { pairEntry ->
            // Quaternary sort: Position Number (nulls last)
            pairEntry.read?.positionNummer ?: Int.MAX_VALUE
        })

        log.debug("Sorting completed")
    }
}
