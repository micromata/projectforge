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
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.common.StringMatchUtils
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.rest.dto.Konto
import org.projectforge.rest.dto.Kost1
import org.projectforge.rest.dto.Kost2
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage
import java.math.BigDecimal
import java.math.RoundingMode
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

    /**
     * Flag indicating whether this import contains individual positions (true) or only header data (false).
     * For invoice imports: true if "Periode" column exists, false otherwise.
     */
    var isPositionBasedImport: Boolean = true

    var readInvoices = mutableListOf<EingangsrechnungPosImportDTO>()

    /**
     * Map of consolidated invoices grouped by RENR (invoice number).
     * Key: RENR (invoice number)
     * Value: List of positions belonging to that invoice
     * Used by IncomingInvoiceCsvImporter for backward compatibility.
     */
    var consolidatedInvoices = mapOf<String, List<EingangsrechnungPosImportDTO>>()

    /**
     * Internal data structure for header-based matching in position-based imports.
     * Groups positions by (referenz, datum, kreditor) for accurate matching.
     */
    data class ConsolidatedInvoice(
        val header: EingangsrechnungPosImportDTO,
        val positions: List<EingangsrechnungPosImportDTO>
    )

    private var consolidatedInvoicesByHeader = mapOf<String, ConsolidatedInvoice>()

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
    override fun doReconcileImportStorage(rereadDatabaseEntries: Boolean) {
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
            log.debug { "=== DATABASE INVOICES LOADED (Date range: $from to $until) ===" }
            if (log.isDebugEnabled) {
                databaseInvoices?.forEachIndexed { index, invoice ->
                    log.debug { "  DB[$index]: referenz='${invoice.referenz}', kreditor='${invoice.kreditor}', datum=${invoice.datum}, grossSum=${try { invoice.ensuredInfo.grossSum } catch (e: Exception) { "ERROR" }}" }
                }
            }
            log.info { "=== TOTAL: ${databaseInvoices?.size ?: 0} DB invoices loaded ===" }
        }

        // Clear existing pair entries and consolidated invoices before rebuilding
        clearEntries()
        consolidatedInvoicesByHeader = emptyMap()

        // Process all invoices together to allow cross-date matching
        // The date matching score will handle date proximity properly
        val readInvoicesWithDate = readInvoices.filter { it.datum != null }
        val readInvoicesWithoutDate = readInvoices.filter { it.datum == null }
        val dbInvoicesAll = databaseInvoices ?: emptyList()

        if (readInvoicesWithDate.isNotEmpty() || dbInvoicesAll.isNotEmpty()) {
            if (isPositionBasedImport) {
                // Use consolidated matching for position-based imports
                // This prevents incorrect matches when number of positions differs between import and DB
                buildConsolidatedMatchingPairs(readInvoicesWithDate, dbInvoicesAll)
            } else {
                // Use header-only matching for header-only imports
                buildHeaderOnlyMatchingPairs(readInvoicesWithDate, dbInvoicesAll)
            }
        }

        // Handle imported invoices without date separately (mark as NEW)
        if (readInvoicesWithoutDate.isNotEmpty()) {
            if (isPositionBasedImport) {
                // Consolidate and mark as new
                buildConsolidatedMatchingPairs(readInvoicesWithoutDate, emptyList())
            } else {
                buildHeaderOnlyMatchingPairs(readInvoicesWithoutDate, emptyList())
            }
        }

        // Sort all pair entries after matching is complete
        sortPairEntriesAfterMatching()

        // Enrich read objects with stored values that are not in import data
        enrichReadWithStoredValues()
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

    /**
     * Consolidates import positions by invoice header (referenz, datum, kreditor).
     * Groups positions that belong to the same invoice and creates header representatives
     * with aggregated grossSum.
     *
     * This is essential for position-based imports to prevent incorrect matching when
     * the number of positions differs between import and database.
     *
     * @return Map with key="referenz|datum|kreditor" and value=ConsolidatedInvoice
     */
    private fun consolidateInvoicesByHeader(positions: List<EingangsrechnungPosImportDTO>): Map<String, ConsolidatedInvoice> {
        val grouped = positions.groupBy { pos ->
            val referenz = pos.referenz?.trim() ?: ""
            val datum = pos.datum?.toString() ?: ""
            val kreditor = pos.kreditor?.trim() ?: ""
            "$referenz|$datum|$kreditor"
        }

        return grouped.mapValues { (key, positionList) ->
            // Create header representative from first position (manual copy of header fields only)
            val first = positionList.first()
            val header = EingangsrechnungPosImportDTO(
                kreditor = first.kreditor,
                konto = first.konto,
                referenz = first.referenz,
                betreff = first.betreff,
                datum = first.datum,
                faelligkeit = first.faelligkeit,
                bezahlDatum = first.bezahlDatum,
                taxRate = first.taxRate,
                currency = first.currency,
                zahlBetrag = first.zahlBetrag,
                discountMaturity = first.discountMaturity,
                discountPercent = first.discountPercent,
                iban = first.iban,
                bic = first.bic,
                receiver = first.receiver,
                paymentType = first.paymentType,
                customernr = first.customernr,
                bemerkung = first.bemerkung
                // Note: kost1, kost2, positionNummer are position-specific and not copied
            )
            header.id = first.id

            // Aggregate grossSum from all positions
            val totalGrossSum = positionList.mapNotNull { it.grossSum }.reduceOrNull { acc, sum -> acc.add(sum) }
            header.grossSum = totalGrossSum

            log.debug { "Consolidated invoice: key='$key', positions=${positionList.size}, totalGrossSum=$totalGrossSum" }

            ConsolidatedInvoice(header, positionList)
        }
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
     * Builds matching pairs for position-based imports using consolidated invoice matching.
     *
     * This method addresses the problem where invoices have different numbers of positions
     * in the import vs. database, which can cause incorrect matching of leftover positions.
     *
     * Process:
     * 1. Consolidate import positions by (referenz, datum, kreditor) into invoices
     * 2. Match consolidated invoices against DB invoices using header fields only
     * 3. Expand invoice matches back to position-level pair entries
     *
     * @param readPositions All imported positions (may belong to different invoices)
     * @param dbInvoices All database invoices in date range
     */
    private fun buildConsolidatedMatchingPairs(
        readPositions: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>
    ) {
        if (readPositions.isEmpty()) {
            // Only database entries exist - mark as deleted
            dbInvoices.forEach { dbInvoice ->
                addEntry(ImportPairEntry(null, createImportDTO(dbInvoice)))
            }
            return
        }

        log.debug { "=== CONSOLIDATING IMPORT POSITIONS ===" }
        log.debug { "  Total positions to consolidate: ${readPositions.size}" }

        // Step 1: Consolidate import positions by invoice header
        consolidatedInvoicesByHeader = consolidateInvoicesByHeader(readPositions)
        val consolidatedHeaders = consolidatedInvoicesByHeader.values.map { it.header }

        log.info { "  Consolidated into ${consolidatedInvoicesByHeader.size} invoices" }

        // Step 2: Match consolidated invoices against DB invoices (header-only)
        log.debug { "=== MATCHING CONSOLIDATED INVOICES (HEADER-ONLY) ===" }
        buildOptimizedHeaderOnlyMatchingPairs(consolidatedHeaders, dbInvoices)
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
        log.debug { "Stage 1: Looking for exact matches..." }
        findExactMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)
        log.info { "=== AFTER STAGE 1: ${matches.size} matches found ===" }
        log.info { "  Unmatched Import: ${readInvoices.size - matchedReadIndices.size} invoices" }
        log.info { "  Unmatched DB: ${dbInvoices.size - matchedDbIndices.size} invoices" }

        // Stage 2: Grouped matches - O(n log n)
        log.debug { "Stage 2: Looking for grouped matches..." }
        findGroupedMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)
        log.info { "=== AFTER STAGE 2: ${matches.size} total matches found ===" }
        log.info { "  Unmatched Import: ${readInvoices.size - matchedReadIndices.size} invoices" }
        log.info { "  Unmatched DB: ${dbInvoices.size - matchedDbIndices.size} invoices" }

        // Stage 3: Fallback matches for remaining invoices
        log.debug { "Stage 3: Fallback matching for remaining invoices..." }
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
     * When used for consolidated position-based imports, expands matches to all positions.
     */
    private fun buildOptimizedHeaderOnlyMatchingPairs(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>
    ) {
        val matchedReadIndices = mutableSetOf<Int>()
        val matchedDbIndices = mutableSetOf<Int>()
        val matches = mutableListOf<Pair<Int, Int>>()

        // Stage 1: Exact matches - O(n)
        log.debug { "Header-only Stage 1: Looking for exact matches..." }
        findExactMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Stage 2: Grouped matches - O(n log n)
        log.debug { "Header-only Stage 2: Looking for grouped matches..." }
        findGroupedHeaderMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Stage 3: Fallback matches for remaining invoices
        log.debug { "Header-only Stage 3: Fallback matching for remaining invoices..." }
        findFallbackHeaderMatches(readInvoices, dbInvoices, matchedReadIndices, matchedDbIndices, matches)

        // Check if we're in consolidated position-based mode
        val isConsolidatedMode = consolidatedInvoicesByHeader.isNotEmpty()

        if (isConsolidatedMode) {
            // Expand header matches to position-level pair entries
            log.debug { "=== EXPANDING MATCHES TO POSITIONS ===" }
            expandHeaderMatchesToPositions(readInvoices, dbInvoices, matches, matchedReadIndices, matchedDbIndices)
        } else {
            // Normal header-only mode
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
    }

    /**
     * Expands invoice-level matches to position-level pair entries.
     * This is used after consolidating and matching positions as invoices.
     *
     * For each matched invoice:
     * - Creates pair entries for ALL import positions of that invoice with the matched DB invoice
     *
     * @param readHeaders List of consolidated invoice headers
     * @param dbInvoices List of database invoices
     * @param matches List of (readIndex, dbIndex) pairs from matching
     * @param matchedReadIndices Set of matched read indices
     * @param matchedDbIndices Set of matched DB indices
     */
    private fun expandHeaderMatchesToPositions(
        readHeaders: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matches: List<Pair<Int, Int>>,
        matchedReadIndices: Set<Int>,
        matchedDbIndices: Set<Int>
    ) {
        // Create a map: header -> consolidated invoice
        val headerToConsolidated = mutableMapOf<EingangsrechnungPosImportDTO, ConsolidatedInvoice>()
        consolidatedInvoicesByHeader.values.forEach { consolidated ->
            headerToConsolidated[consolidated.header] = consolidated
        }

        // Process matches: expand to positions and pair by index
        matches.forEach { (readIndex, dbIndex) ->
            val header = readHeaders[readIndex]
            val dbInvoice = dbInvoices[dbIndex]
            val consolidated = headerToConsolidated[header]

            if (consolidated != null) {
                val importPositions = consolidated.positions
                val dbPositionCount = dbInvoice.positionen?.size ?: 0
                val maxPositions = maxOf(importPositions.size, dbPositionCount)

                log.debug { "  Match: Import invoice '${header.referenz}' (${importPositions.size} positions) → DB invoice '${dbInvoice.referenz}' (${dbPositionCount} positions)" }

                // Pair positions by index: Import[0]↔DB[0], Import[1]↔DB[1], etc.
                for (i in 0 until maxPositions) {
                    val importPos = importPositions.getOrNull(i)
                    val dbDto = if (i < dbPositionCount) {
                        createImportDTOForPosition(dbInvoice, i)
                    } else {
                        null  // DB position doesn't exist (import has more positions)
                    }

                    if (importPos != null) {
                        // Import position exists
                        addEntry(ImportPairEntry(importPos, dbDto))
                    } else if (dbDto != null) {
                        // Only DB position exists (import has fewer positions)
                        addEntry(ImportPairEntry(null, dbDto))
                    }
                }
            } else {
                log.warn { "  No consolidated invoice found for header: ${header.referenz}" }
            }
        }

        // Add unmatched import invoices as new (expand to all positions)
        readHeaders.forEachIndexed { index, header ->
            if (!matchedReadIndices.contains(index)) {
                val consolidated = headerToConsolidated[header]
                if (consolidated != null) {
                    log.debug { "  New: Import invoice '${header.referenz}' (${consolidated.positions.size} positions) → NEW" }
                    consolidated.positions.forEach { position ->
                        addEntry(ImportPairEntry(position, null))
                    }
                }
            }
        }

        // Add unmatched database invoices as deleted
        dbInvoices.forEachIndexed { index, invoice ->
            if (!matchedDbIndices.contains(index)) {
                log.debug { "  Deleted: DB invoice '${invoice.referenz}' → DELETED" }
                addEntry(ImportPairEntry(null, createImportDTO(invoice)))
            }
        }

        log.debug { "=== EXPANSION COMPLETED ===" }
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

        log.debug { "Header-only Stage 2 completed: ${matches.size - initialMatches} grouped matches found" }
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

        val readToOriginalIndex = readInvoices.mapIndexed { index, invoice -> invoice to index }.toMap()
        val dbToOriginalIndex = dbInvoices.mapIndexed { index, invoice -> invoice to index }.toMap()

        val scoreMatrix = Array(unmatchedRead.size) { IntArray(unmatchedDb.size) }

        // Calculate scores
        for (i in unmatchedRead.indices) {
            for (j in unmatchedDb.indices) {
                val score = unmatchedRead[i].matchScore(unmatchedDb[j], logErrors = true)
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

            // Require minimum score of 50 to avoid false-positive matches based on substring similarity alone
            // Score 50 = exact invoice number OR strong combination (e.g., similar kreditor + exact date + some more)
            if (maxScore < 50) break

            takenReadIndices.add(maxReadIndex)
            takenDbIndices.add(maxDbIndex)

            val originalReadIndex = readToOriginalIndex[unmatchedRead[maxReadIndex]]!!
            val originalDbIndex = dbToOriginalIndex[unmatchedDb[maxDbIndex]]!!

            val readInv = unmatchedRead[maxReadIndex]
            val dbInv = unmatchedDb[maxDbIndex]
            log.debug { "STAGE 3 HEADER FALLBACK MATCH (score: $maxScore): Import='${readInv.referenz}' vs DB='${dbInv.referenz}' | ImportKreditor='${readInv.kreditor}' vs DBKreditor='${dbInv.kreditor}' | ImportDate=${readInv.datum} vs DBDate=${dbInv.datum}" }

            matches.add(Pair(originalReadIndex, originalDbIndex))
            matchedReadIndices.add(originalReadIndex)
            matchedDbIndices.add(originalDbIndex)
        }

        log.debug { "Header-only Stage 3 completed: ${matches.size - initialMatches} fallback matches found" }
    }

    /**
     * Creates an import DTO from a DB invoice header (without position-specific fields).
     * Used for header-only imports and deleted invoice entries.
     */
    private fun createImportDTO(eingangsrechnungDO: EingangsrechnungDO): EingangsrechnungPosImportDTO {
        val dto = EingangsrechnungPosImportDTO()
        dto.copyFrom(eingangsrechnungDO)

        // Calculate grossSum from invoice positions
        try {
            dto.grossSum = eingangsrechnungDO.ensuredInfo.grossSum
        } catch (e: Exception) {
            log.error(e) { "Could not calculate grossSum for invoice ${eingangsrechnungDO.id}" }
        }

        // Copy konto from header using cache to handle lazy loading
        val initializedKonto = PfCaches.instance.getKontoIfNotInitialized(eingangsrechnungDO.konto)
        initializedKonto?.let { dto.konto = Konto(it.id, description = it.nummer?.toString()) }

        return dto
    }

    /**
     * Creates an import DTO for a specific position within a DB invoice.
     * Pairs import positions with DB positions by index for position-based imports.
     *
     * @param eingangsrechnungDO The database invoice
     * @param positionIndex The 0-based position index
     * @return DTO with position-specific fields from the specified DB position
     */
    private fun createImportDTOForPosition(
        eingangsrechnungDO: EingangsrechnungDO,
        positionIndex: Int
    ): EingangsrechnungPosImportDTO {
        val dto = EingangsrechnungPosImportDTO()
        dto.copyFrom(eingangsrechnungDO)

        // Calculate grossSum from invoice positions
        try {
            dto.grossSum = eingangsrechnungDO.ensuredInfo.grossSum
        } catch (e: Exception) {
            log.error(e) { "Could not calculate grossSum for invoice ${eingangsrechnungDO.id}" }
        }

        // Copy konto from header using cache to handle lazy loading
        val initializedKonto = PfCaches.instance.getKontoIfNotInitialized(eingangsrechnungDO.konto)
        initializedKonto?.let { dto.konto = Konto(it.id, description = it.nummer?.toString()) }

        // Get position by index (in order)
        val dbPosition = eingangsrechnungDO.positionen?.getOrNull(positionIndex)

        if (dbPosition != null) {
            // Copy position-specific fields
            dto.taxRate = dbPosition.vat
            dto.positionNummer = dbPosition.number.toInt()

            // Copy kost1/kost2 from first cost assignment using cache to handle lazy loading
            val firstKostZuweisung = dbPosition.kostZuweisungen?.firstOrNull()
            if (firstKostZuweisung != null) {
                val initializedKost1 = PfCaches.instance.getKost1IfNotInitialized(firstKostZuweisung.kost1)
                initializedKost1?.let { dto.kost1 = Kost1(it.id, description = it.formattedNumber) }

                val initializedKost2 = PfCaches.instance.getKost2IfNotInitialized(firstKostZuweisung.kost2)
                initializedKost2?.let { dto.kost2 = Kost2(it) }
                initializedKost2?.let { dto.kost2 = Kost2(it.id, description = it.formattedNumber) }
            }
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
        // Create hash map for fast lookups - O(n)
        val dbByReferenzDateAmount = mutableMapOf<String, MutableList<Pair<Int, EingangsrechnungDO>>>()

        // Build hash map for database invoices using exact match key (referenz + datum + grossSum)
        dbInvoices.forEachIndexed { index, dbInvoice ->
            if (matchedDbIndices.contains(index)) return@forEachIndexed

            val referenz = dbInvoice.referenz?.trim()?.lowercase()
            val datum = dbInvoice.datum
            val grossSum = try { dbInvoice.ensuredInfo.grossSum } catch (e: Exception) { null }

            if (!referenz.isNullOrBlank() && datum != null && grossSum != null) {
                val primaryKey = "$referenz|$datum|$grossSum"
                dbByReferenzDateAmount.getOrPut(primaryKey) { mutableListOf() }.add(Pair(index, dbInvoice))
            }
        }

        // Find matches for read invoices - O(n)
        readInvoices.forEachIndexed { readIndex, readInvoice ->
            if (matchedReadIndices.contains(readIndex)) return@forEachIndexed

            val referenz = readInvoice.referenz?.trim()?.lowercase()
            val datum = readInvoice.datum
            val grossSum = readInvoice.grossSum

            // Only match if referenz + datum + grossSum are all identical
            if (!referenz.isNullOrBlank() && datum != null && grossSum != null) {
                val primaryKey = "$referenz|$datum|$grossSum"
                dbByReferenzDateAmount[primaryKey]?.firstOrNull { (dbIndex, _) ->
                    !matchedDbIndices.contains(dbIndex)
                }?.let { (dbIndex, dbInvoice) ->
                    matches.add(Pair(readIndex, dbIndex))
                    matchedReadIndices.add(readIndex)
                    matchedDbIndices.add(dbIndex)
                    log.debug { "STAGE 1 EXACT MATCH: Import='${readInvoice.referenz}' vs DB='${dbInvoice.referenz}' | ImportKreditor='${readInvoice.kreditor}' vs DBKreditor='${dbInvoice.kreditor}' | ImportDate=${readInvoice.datum} vs DBDate=${dbInvoice.datum} | ImportAmount=${readInvoice.grossSum} vs DBAmount=${try { dbInvoice.ensuredInfo.grossSum } catch (e: Exception) { null }}" }
                    return@forEachIndexed
                }
            }
        }

        log.debug { "Stage 1 completed: ${matches.size} exact matches found" }
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

        log.debug { "Stage 2 completed: ${matches.size - initialMatches} grouped matches found" }
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

        log.debug { "=== STAGE 3: Starting fallback matching ===" }
        if (log.isDebugEnabled) {
            log.debug { "  Unmatched Import invoices (${unmatchedRead.size}):" }
            unmatchedRead.forEach { inv ->
                log.debug { "    Import: referenz='${inv.referenz}', kreditor='${inv.kreditor}', datum=${inv.datum}" }
            }
            log.debug { "  Unmatched DB invoices (${unmatchedDb.size}):" }
            unmatchedDb.forEach { inv ->
                log.debug { "    DB: referenz='${inv.referenz}', kreditor='${inv.kreditor}', datum=${inv.datum}" }
            }
        }

        if (unmatchedRead.isEmpty() || unmatchedDb.isEmpty()) {
            return
        }

        val readToOriginalIndex = readInvoices.mapIndexed { index, invoice -> invoice to index }.toMap()
        val dbToOriginalIndex = dbInvoices.mapIndexed { index, invoice -> invoice to index }.toMap()

        val scoreMatrix = Array(unmatchedRead.size) { IntArray(unmatchedDb.size) }

        // Calculate scores
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

            // Require minimum score of 50 to avoid false-positive matches based on substring similarity alone
            // Score 50 = exact invoice number OR strong combination (e.g., similar kreditor + exact date + some more)
            if (maxScore < 50) break

            takenReadIndices.add(maxReadIndex)
            takenDbIndices.add(maxDbIndex)

            val originalReadIndex = readToOriginalIndex[unmatchedRead[maxReadIndex]]!!
            val originalDbIndex = dbToOriginalIndex[unmatchedDb[maxDbIndex]]!!

            val readInv = unmatchedRead[maxReadIndex]
            val dbInv = unmatchedDb[maxDbIndex]
            log.debug { "STAGE 3 FALLBACK MATCH (score: $maxScore): Import='${readInv.referenz}' vs DB='${dbInv.referenz}' | ImportKreditor='${readInv.kreditor}' vs DBKreditor='${dbInv.kreditor}' | ImportDate=${readInv.datum} vs DBDate=${dbInv.datum} | ImportAmount=${readInv.grossSum} vs DBAmount=${try { dbInv.ensuredInfo.grossSum } catch (e: Exception) { null }}" }

            matches.add(Pair(originalReadIndex, originalDbIndex))
            matchedReadIndices.add(originalReadIndex)
            matchedDbIndices.add(originalDbIndex)
        }

        log.debug { "Stage 3 completed: ${matches.size - initialMatches} fallback matches found" }
    }

    /**
     * Group by invoice number (referenz) and find matches within each group.
     * Uses prefix-based grouping to catch variations like "325124610" matching "3251246-10 / Az.: IS-0017-10/KSR".
     */
    private fun findMatchesInReferenzGroups(
        readInvoices: List<EingangsrechnungPosImportDTO>,
        dbInvoices: List<EingangsrechnungDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        // Create multiple grouping strategies to improve matching
        // Strategy 1: Full normalized string (exact match)
        val dbByExactReferenz = dbInvoices.mapIndexed { index, invoice ->
            index to invoice
        }.filter { (index, _) -> !matchedDbIndices.contains(index) }
         .groupBy { (_, invoice) ->
             invoice.referenz?.let { StringMatchUtils.normalizeString(it) }?.takeIf { it.isNotBlank() }
         }
         .filterKeys { !it.isNullOrBlank() }

        // Strategy 2: Prefix-based grouping (first 8 chars of normalized string)
        // This catches variations like "325124610" matching "32512461010is001710ksr"
    val dbByReferenzPrefix = dbInvoices.mapIndexed { index, invoice ->
            index to invoice
        }.filter { (index, _) -> !matchedDbIndices.contains(index) }
         .groupBy { (_, invoice) ->
         invoice.referenz?.let {
                 val normalized = StringMatchUtils.normalizeString(it)
                 if (normalized.length >= 8) normalized.substring(0, 8) else normalized
             }?.takeIf { it.isNotBlank() }
         }
         .filterKeys { !it.isNullOrBlank() }

        // Find matches for each read invoice
        readInvoices.forEachIndexed { readIndex, readInvoice ->
            if (matchedReadIndices.contains(readIndex)) return@forEachIndexed

            val normalizedReferenz = readInvoice.referenz?.let { StringMatchUtils.normalizeString(it) }
            if (normalizedReferenz.isNullOrBlank()) return@forEachIndexed

            // Try exact match first
            var candidateGroup = dbByExactReferenz[normalizedReferenz]

            // If no exact match, try prefix-based match
            if (candidateGroup == null && normalizedReferenz.length >= 8) {
                val prefix = normalizedReferenz.substring(0, 8)
                candidateGroup = dbByReferenzPrefix[prefix]
            }

            if (candidateGroup == null) return@forEachIndexed

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
                val dbInvoice = candidateGroup.first { it.first == dbIdx }.second
                matches.add(Pair(readIdx, dbIdx))
                matchedReadIndices.add(readIdx)
                matchedDbIndices.add(dbIdx)
                log.debug { "STAGE 2 REFERENZ GROUP MATCH (score: $bestScore): Import='${readInvoice.referenz}' vs DB='${dbInvoice.referenz}' | ImportKreditor='${readInvoice.kreditor}' vs DBKreditor='${dbInvoice.kreditor}' | ImportDate=${readInvoice.datum} vs DBDate=${dbInvoice.datum} | ImportAmount=${readInvoice.grossSum} vs DBAmount=${try { dbInvoice.ensuredInfo.grossSum } catch (e: Exception) { null }}" }
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
                val dbInvoice = candidateGroup.first { it.first == dbIdx }.second
                matches.add(Pair(readIdx, dbIdx))
                matchedReadIndices.add(readIdx)
                matchedDbIndices.add(dbIdx)
                log.debug { "STAGE 2 KREDITOR GROUP MATCH (score: $bestScore): Import='${readInvoice.referenz}' vs DB='${dbInvoice.referenz}' | ImportKreditor='${readInvoice.kreditor}' vs DBKreditor='${dbInvoice.kreditor}' | ImportDate=${readInvoice.datum} vs DBDate=${dbInvoice.datum} | ImportAmount=${readInvoice.grossSum} vs DBAmount=${try { dbInvoice.ensuredInfo.grossSum } catch (e: Exception) { null }}" }
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

                val score = readInvoice.matchScore(dbInvoice)
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = Pair(readIndex, dbIndex)
                }
            }

            bestMatch?.let { (readIdx, dbIdx) ->
                val dbInvoice = candidateGroup.first { it.first == dbIdx }.second
                matches.add(Pair(readIdx, dbIdx))
                matchedReadIndices.add(readIdx)
                matchedDbIndices.add(dbIdx)
                log.debug { "STAGE 2 HEADER REFERENZ GROUP MATCH (score: $bestScore): Import='${readInvoice.referenz}' vs DB='${dbInvoice.referenz}' | ImportKreditor='${readInvoice.kreditor}' vs DBKreditor='${dbInvoice.kreditor}' | ImportDate=${readInvoice.datum} vs DBDate=${dbInvoice.datum}" }
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

                val score = readInvoice.matchScore(dbInvoice)
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = Pair(readIndex, dbIndex)
                }
            }

            bestMatch?.let { (readIdx, dbIdx) ->
                val dbInvoice = candidateGroup.first { it.first == dbIdx }.second
                matches.add(Pair(readIdx, dbIdx))
                matchedReadIndices.add(readIdx)
                matchedDbIndices.add(dbIdx)
                log.debug { "STAGE 2 HEADER KREDITOR GROUP MATCH (score: $bestScore): Import='${readInvoice.referenz}' vs DB='${dbInvoice.referenz}' | ImportKreditor='${readInvoice.kreditor}' vs DBKreditor='${dbInvoice.kreditor}' | ImportDate=${readInvoice.datum} vs DBDate=${dbInvoice.datum}" }
            }
        }
    }

    /**
     * Enriches read objects with stored values that are not present in the import data.
     * This prevents the frontend from showing deletions for fields that are simply not
     * included in the import (e.g., bezahlDatum and zahlBetrag in position-based imports).
     * Also calculates zahlBetrag if bezahlDatum is set but zahlBetrag is missing.
     */
    private fun enrichReadWithStoredValues() {
        pairEntries.forEach { pairEntry ->
            val read = pairEntry.read
            val stored = pairEntry.stored

            if (read != null && stored != null) {
                // Preserve bezahlDatum if not in import
                if (read.bezahlDatum == null && stored.bezahlDatum != null) {
                    read.bezahlDatum = stored.bezahlDatum
                }

                // Preserve zahlBetrag if not in import
                if (read.zahlBetrag == null && stored.zahlBetrag != null) {
                    read.zahlBetrag = stored.zahlBetrag
                }
            }

            // Calculate zahlBetrag if bezahlDatum is set but zahlBetrag is missing
            if (read != null && read.bezahlDatum != null && read.zahlBetrag == null) {
                read.zahlBetrag = calculateZahlBetrag(read)
            }
        }

        log.debug { "Enriched ${pairEntries.size} pair entries with stored values" }
    }

    /**
     * Calculates zahlBetrag based on bezahlDatum, grossSum, and discount settings.
     * If bezahlDatum <= discountMaturity: zahlBetrag = grossSum - discount
     * Otherwise: zahlBetrag = grossSum
     */
    private fun calculateZahlBetrag(dto: EingangsrechnungPosImportDTO): BigDecimal? {
        val grossSum = dto.grossSum ?: return null
        val bezahlDatum = dto.bezahlDatum ?: return null

        val shouldApplyDiscount = dto.discountMaturity != null &&
                dto.discountPercent != null &&
                dto.discountPercent!!.compareTo(BigDecimal.ZERO) > 0 &&
                !bezahlDatum.isAfter(dto.discountMaturity!!)

        return if (shouldApplyDiscount) {
            val discountPercent = dto.discountPercent!!
            val discountAmount = grossSum.multiply(discountPercent).divide(
                BigDecimal(100),
                2,
                RoundingMode.HALF_UP
            )
            grossSum.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP)
        } else {
            grossSum
        }
    }

    /**
     * Sort all pair entries after matching is complete.
     * Sort order: Date, Creditor, Invoice Number, Position Number
     */
    private fun sortPairEntriesAfterMatching() {
        log.debug { "Sorting ${pairEntries.size} pair entries by date, creditor, invoice number, and position" }

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

        log.debug { "Sorting completed" }
    }
}
