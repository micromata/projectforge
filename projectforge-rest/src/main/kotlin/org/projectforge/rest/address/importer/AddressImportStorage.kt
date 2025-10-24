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

package org.projectforge.rest.address.importer

import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDO
import org.projectforge.business.address.vcard.VCardUtils
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage

private val log = KotlinLogging.logger {}

class AddressImportStorage : ImportStorage<AddressImportDTO>(
    ImportSettings()
) {
    override val targetEntityTitle: String
        get() = translate("address.import.title")

    var readAddresses = mutableListOf<AddressImportDTO>()

    /**
     * Database addresses for reconciliation matching.
     */
    var databaseAddresses: List<AddressDO>? = null

    override fun prepareEntity(): AddressImportDTO {
        return AddressImportDTO()
    }

    override fun commitEntity(obj: AddressImportDTO) {
        readAddresses.add(obj)
        val pairEntry = ImportPairEntry(read = obj)
        // Transfer errors from DTO to PairEntry
        obj.getErrors().forEach { error ->
            pairEntry.addError(error)
        }
        addEntry(pairEntry)
    }

    /**
     * Commits an ImportPairEntry that was prepared using prepareImportPairEntry().
     * This method is preferred for new parsing implementations as it allows
     * setting errors directly during parsing.
     */
    override fun commitEntity(pairEntry: ImportPairEntry<AddressImportDTO>) {
        pairEntry.read?.let { dto ->
            readAddresses.add(dto)
            // Transfer errors from DTO to PairEntry
            dto.getErrors().forEach { error ->
                pairEntry.addError(error)
            }
        }
        addEntry(pairEntry)
    }

    /**
     * Parse VCF data and populate import storage.
     * @param vcfBytes VCF file content as byte array
     */
    fun parseVcfData(vcfBytes: ByteArray) {
        try {
            log.info("Parsing VCF data (${vcfBytes.size} bytes)")
            val addresses = VCardUtils.parseFromByteArray(vcfBytes)
            log.info("Parsed ${addresses.size} addresses from VCF")

            addresses.forEach { addressDO ->
                val dto = AddressImportDTO()
                dto.copyFrom(addressDO)

                // Handle image from VCard (stored as transient attribute)
                val imageData = addressDO.getTransientAttribute("image") as? AddressImageDO
                if (imageData != null) {
                    dto.setTransientAttribute("image", imageData)
                }

                commitEntity(dto)
            }
        } catch (e: Exception) {
            log.error("Error parsing VCF data: ${e.message}", e)
            addError("Error parsing VCF data: ${e.message}")
        }
    }

    /**
     * Reconciles imported address data with existing database entries to identify matches.
     *
     * This method implements a score-based matching algorithm:
     * 1. Loads all addresses from database
     * 2. For each import address, calculates match scores against all DB addresses
     * 3. Selects best match if score >= 50 (threshold)
     * 4. Creates ImportPairEntry objects with status: NEW, MATCHED, or DELETED
     *
     * @param rereadDatabaseEntries If true, reloads database entries; if false, uses cached data
     */
    override fun doReconcileImportStorage(rereadDatabaseEntries: Boolean) {
        // Load database addresses for matching
        if (rereadDatabaseEntries || databaseAddresses == null) {
            val addressDao = ApplicationContextProvider.getApplicationContext().getBean(AddressDao::class.java)
            databaseAddresses = addressDao.selectAll(checkAccess = false)
            log.info("=== DATABASE ADDRESSES LOADED: ${databaseAddresses?.size ?: 0} addresses ===")
        }

        // Clear existing pair entries before rebuilding
        clearEntries()

        val dbAddresses = databaseAddresses ?: emptyList()
        val matchedReadIndices = mutableSetOf<Int>()
        val matchedDbIndices = mutableSetOf<Int>()
        val matches = mutableListOf<Pair<Int, Int>>()

        // Stage 1: Find exact name matches first (optimization)
        log.debug { "Stage 1: Looking for exact name matches..." }
        findExactNameMatches(readAddresses, dbAddresses, matchedReadIndices, matchedDbIndices, matches)

        // Stage 2: Score-based matching for remaining addresses
        log.debug { "Stage 2: Score-based matching for remaining addresses..." }
        findScoreBasedMatches(readAddresses, dbAddresses, matchedReadIndices, matchedDbIndices, matches)

        // Create ImportPairEntry objects from matches
        matches.forEach { (readIndex, dbIndex) ->
            val readAddress = readAddresses[readIndex]
            val dbAddress = dbAddresses[dbIndex]
            val dbDto = createImportDTO(dbAddress)
            addEntry(createPairEntryWithErrors(readAddress, dbDto))
        }

        // Add unmatched read entries as new
        readAddresses.forEachIndexed { index, address ->
            if (!matchedReadIndices.contains(index)) {
                addEntry(createPairEntryWithErrors(address, null))
            }
        }

        // Note: We do NOT add unmatched database entries as deleted.
        // VCF imports contain only selected addresses, not the entire database.
        // Addresses not in the VCF file should remain unchanged in the database.

        // Sort entries by name for better UI presentation
        sortPairEntriesByName()

        log.info("Reconciliation completed: ${matches.size} matches, ${readAddresses.size - matchedReadIndices.size} new")
    }

    /**
     * Stage 1: Find exact name matches using hash-based lookups for maximum performance.
     */
    private fun findExactNameMatches(
        readAddresses: List<AddressImportDTO>,
        dbAddresses: List<AddressDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        // Create hash map for fast lookups
        val dbByFullName = mutableMapOf<String, MutableList<Pair<Int, AddressDO>>>()

        // Build hash map for database addresses
        dbAddresses.forEachIndexed { index, dbAddress ->
            if (matchedDbIndices.contains(index)) return@forEachIndexed

            val name = dbAddress.name?.trim()?.lowercase()
            val firstName = dbAddress.firstName?.trim()?.lowercase()

            if (!name.isNullOrBlank() && !firstName.isNullOrBlank()) {
                val fullName = "$name|$firstName"
                dbByFullName.getOrPut(fullName) { mutableListOf() }.add(Pair(index, dbAddress))
            }
        }

        // Find matches for read addresses
        readAddresses.forEachIndexed { readIndex, readAddress ->
            if (matchedReadIndices.contains(readIndex)) return@forEachIndexed

            val name = readAddress.name?.trim()?.lowercase()
            val firstName = readAddress.firstName?.trim()?.lowercase()

            if (!name.isNullOrBlank() && !firstName.isNullOrBlank()) {
                val fullName = "$name|$firstName"
                val candidates = dbByFullName[fullName]

                if (candidates != null && candidates.size == 1) {
                    // Exactly one match with same name - accept as exact match
                    val (dbIndex, dbAddress) = candidates.first()
                    if (!matchedDbIndices.contains(dbIndex)) {
                        matches.add(Pair(readIndex, dbIndex))
                        matchedReadIndices.add(readIndex)
                        matchedDbIndices.add(dbIndex)
                        log.debug { "EXACT NAME MATCH: '${readAddress.name}/${readAddress.firstName}' -> DB ID ${dbAddress.id}" }
                    }
                } else if (candidates != null && candidates.size > 1) {
                    // Multiple candidates with same name - use scoring to disambiguate
                    var bestScore = 0
                    var bestDbIndex: Int? = null

                    candidates.forEach { (dbIndex, dbAddress) ->
                        if (!matchedDbIndices.contains(dbIndex)) {
                            val score = readAddress.matchScore(dbAddress)
                            if (score > bestScore) {
                                bestScore = score
                                bestDbIndex = dbIndex
                            }
                        }
                    }

                    if (bestScore >= 50 && bestDbIndex != null) {
                        matches.add(Pair(readIndex, bestDbIndex!!))
                        matchedReadIndices.add(readIndex)
                        matchedDbIndices.add(bestDbIndex!!)
                        log.debug { "DISAMBIGUATED MATCH (score $bestScore): '${readAddress.name}/${readAddress.firstName}' -> DB ID ${dbAddresses[bestDbIndex!!].id}" }
                    }
                }
            }
        }

        log.debug { "Stage 1 completed: ${matches.size} exact matches found" }
    }

    /**
     * Stage 2: Score-based matching for remaining addresses.
     */
    private fun findScoreBasedMatches(
        readAddresses: List<AddressImportDTO>,
        dbAddresses: List<AddressDO>,
        matchedReadIndices: MutableSet<Int>,
        matchedDbIndices: MutableSet<Int>,
        matches: MutableList<Pair<Int, Int>>
    ) {
        val initialMatches = matches.size
        val unmatchedRead = readAddresses.filterIndexed { index, _ -> !matchedReadIndices.contains(index) }
        val unmatchedDb = dbAddresses.filterIndexed { index, _ -> !matchedDbIndices.contains(index) }

        if (unmatchedRead.isEmpty() || unmatchedDb.isEmpty()) {
            return
        }

        val readToOriginalIndex = readAddresses.mapIndexed { index, address -> address to index }.toMap()
        val dbToOriginalIndex = dbAddresses.mapIndexed { index, address -> address to index }.toMap()

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

            // Require minimum score of 50 to avoid false-positive matches
            if (maxScore < 50) break

            takenReadIndices.add(maxReadIndex)
            takenDbIndices.add(maxDbIndex)

            val originalReadIndex = readToOriginalIndex[unmatchedRead[maxReadIndex]]!!
            val originalDbIndex = dbToOriginalIndex[unmatchedDb[maxDbIndex]]!!

            val readAddr = unmatchedRead[maxReadIndex]
            val dbAddr = unmatchedDb[maxDbIndex]
            log.debug { "SCORE-BASED MATCH (score: $maxScore): Import='${readAddr.name}/${readAddr.firstName}' vs DB='${dbAddr.name}/${dbAddr.firstName}'" }

            matches.add(Pair(originalReadIndex, originalDbIndex))
            matchedReadIndices.add(originalReadIndex)
            matchedDbIndices.add(originalDbIndex)
        }

        log.debug { "Stage 2 completed: ${matches.size - initialMatches} score-based matches found" }
    }

    /**
     * Creates an import DTO from a DB address.
     */
    private fun createImportDTO(addressDO: AddressDO): AddressImportDTO {
        val dto = AddressImportDTO()
        dto.copyFrom(addressDO)
        return dto
    }

    /**
     * Sort all pair entries by name after matching is complete.
     */
    private fun sortPairEntriesByName() {
        pairEntries.sortWith(compareBy<ImportPairEntry<AddressImportDTO>> { pairEntry ->
            // Primary sort: Name (case-insensitive, nulls last)
            pairEntry.read?.name?.trim()?.lowercase()
                ?: pairEntry.stored?.name?.trim()?.lowercase()
        }.thenBy { pairEntry ->
            // Secondary sort: FirstName (case-insensitive, nulls last)
            pairEntry.read?.firstName?.trim()?.lowercase()
                ?: pairEntry.stored?.firstName?.trim()?.lowercase()
        }.thenBy { pairEntry ->
            // Tertiary sort: Organization (case-insensitive, nulls last)
            pairEntry.read?.organization?.trim()?.lowercase()
                ?: pairEntry.stored?.organization?.trim()?.lowercase()
        })
    }

    /**
     * Creates an ImportPairEntry and transfers errors from DTO to PairEntry.
     * This ensures errors survive the reconcile process (which recreates PairEntries).
     */
    private fun createPairEntryWithErrors(
        read: AddressImportDTO?,
        stored: AddressImportDTO?
    ): ImportPairEntry<AddressImportDTO> {
        val pairEntry = ImportPairEntry(read, stored)
        read?.getErrors()?.forEach { error ->
            pairEntry.addError(error)
        }
        return pairEntry
    }
}
