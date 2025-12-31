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
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.importer.AbstractImportJob
import org.projectforge.rest.importer.ImportPairEntry
import java.math.BigDecimal
import java.math.RoundingMode

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
    selectedEntries = selectedEntries,
) {

    init {
        totalNumber = selectedEntries.size
        processedNumber = 0
    }

    override fun onBeforeStart() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
        // Mark all entries as not reconciled so they show status UNKNOWN until processed
        importStorage.pairEntries.forEach { it.reconciled = false }
    }

    override fun onAfterTermination() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
    }

    override suspend fun run() {
        log.info("Starting import of ${selectedEntries.size} incoming invoice entries (isPositionBasedImport=${importStorage.isPositionBasedImport}).")

        if (importStorage.isPositionBasedImport) {
            runPositionBasedImport()
        } else {
            runHeaderOnlyImport()
        }

        log.info("Import completed: inserted=${result.inserted}, updated=${result.updated}, deleted=${result.deleted}, unmodified=${result.unmodified}")
    }

    /**
     * Positions-based import: Groups entries by invoice and creates positions for each invoice.
     */
    private fun runPositionBasedImport() {
        // Group entries by invoice (same stored.id for updates, same referenz+datum+kreditor for new invoices)
        val invoiceGroups = groupEntriesByInvoice()

        for ((invoiceKey, entries) in invoiceGroups) {
            if (!isActive) {
                return
            }

            // Find the stored invoice ID from any entry that has stored data
            // All entries with stored data should belong to the same invoice
            val storedId = entries.firstNotNullOfOrNull { it.stored?.id }

            // Check if this is a deletion (all entries have read=null)
            if (entries.all { it.read == null }) {
                if (storedId != null) {
                    val existingEntity = eingangsrechnungDao.find(storedId)
                    if (existingEntity != null && !existingEntity.deleted) {
                        eingangsrechnungDao.markAsDeleted(existingEntity)
                        result.deleted += entries.size  // Count positions, not invoices
                        processedNumber += entries.size
                    }
                }
                continue
            }

            // Create or update invoice with positions
            if (storedId != null) {
                updateInvoiceWithPositions(storedId, entries)
            } else {
                insertInvoiceWithPositions(entries)
            }

            processedNumber += entries.size
        }
    }

    /**
     * Groups entries by invoice. Uses invoice-level identification (referenz+datum+kreditor)
     * to ensure all positions of the same invoice are grouped together, regardless of whether
     * some positions exist in DB and others are new.
     */
    private fun groupEntriesByInvoice(): Map<String, List<ImportPairEntry<EingangsrechnungPosImportDTO>>> {
        return selectedEntries.groupBy { entry ->
            // Use read data for grouping to ensure positions of same invoice are together
            // This handles cases where one position exists (has stored.id) and another is new (no stored.id)
            val read = entry.read
            val stored = entry.stored

            // Use read data if available, otherwise fall back to stored data
            val referenz = (read?.referenz ?: stored?.referenz)?.trim() ?: ""
            val datum = (read?.datum ?: stored?.datum)?.toString() ?: ""
            val kreditor = (read?.kreditor ?: stored?.kreditor)?.trim() ?: ""

            // Always group by invoice-level identification (referenz+datum+kreditor)
            // This ensures multiple positions of the same invoice are grouped together
            "$referenz|$datum|$kreditor"
        }
    }

    /**
     * Updates an existing invoice with new positions.
     * Reuses existing position objects by position number to preserve history entries (no orphanRemoval).
     */
    private fun updateInvoiceWithPositions(storedId: Long, entries: List<ImportPairEntry<EingangsrechnungPosImportDTO>>) {
        val existingEntity = eingangsrechnungDao.find(storedId)
        if (existingEntity == null) {
            log.warn { "Stored invoice with id=$storedId not found, inserting as new" }
            insertInvoiceWithPositions(entries)
            return
        }

        // Copy header from first entry
        val firstRead = entries.firstOrNull()?.read
        firstRead?.copyTo(existingEntity)

        // Set betreff (subject) from first position's betreff for position-based imports
        // This maps "Ware/Leistung" column to both invoice.betreff (header) and position.text
        if (firstRead?.betreff != null) {
            existingEntity.betreff = firstRead.betreff
        }

        // Build map of existing positions by position number for ID reuse
        // Include deleted positions so they can be restored
        val existingPositionsById = existingEntity.positionen
            ?.associateBy { it.number }
            ?: emptyMap()

        // Modify the existing collection in-place to maintain Hibernate's PersistentList
        val newPositions = createPositions(entries, existingEntity, existingPositionsById)

        if (log.isDebugEnabled) {
            log.debug { "Updating invoice ${existingEntity.id} with ${newPositions.size} positions" }
            newPositions.forEachIndexed { index, pos ->
                log.debug { "  Position $index: id=${pos.id}, number=${pos.number}, text=${pos.text}, hasKostZuweisungen=${!pos.kostZuweisungen.isNullOrEmpty()}" }
            }
        }

        val currentPositions = existingEntity.positionen
        if (currentPositions != null) {
            log.debug { "Clearing ${currentPositions.size} existing positions from collection" }
            currentPositions.clear()
            log.debug { "Adding ${newPositions.size} new/updated positions to collection" }
            currentPositions.addAll(newPositions)
        } else {
            log.debug { "No existing positions collection, creating new one with ${newPositions.size} positions" }
            existingEntity.positionen = newPositions
        }

        log.debug { "Calling eingangsrechnungDao.update() for invoice ${existingEntity.id}" }
        val modStatus = eingangsrechnungDao.update(existingEntity)
        log.debug { "Update completed with status: $modStatus" }
        if (modStatus != EntityCopyStatus.NONE) {
            result.updated += entries.size  // Count positions, not invoices
        } else {
            result.unmodified += entries.size  // Count positions, not invoices
        }
    }

    /**
     * Creates positions from import data, reusing existing managed position objects.
     * Updates existing positions in-place and creates new ones only when needed.
     * Positions not in the import will be marked as deleted by CollectionHandler (soft-delete).
     */
    private fun createPositions(
        entries: List<ImportPairEntry<EingangsrechnungPosImportDTO>>,
        invoice: EingangsrechnungDO,
        existingPositionsById: Map<Short, EingangsrechnungsPositionDO>
    ): MutableList<EingangsrechnungsPositionDO> {
        val positions = mutableListOf<EingangsrechnungsPositionDO>()

        entries.forEachIndexed { index, entry ->
            val read = entry.read ?: return@forEachIndexed

            // Use position number from import DTO if available (for partial updates where only some positions are selected)
            // Otherwise, use array index for full imports
            val positionNumber = read.positionNummer?.toShort() ?: (index + 1).toShort()

            // Reuse existing managed position object if available, otherwise create new one
            val existingPos = existingPositionsById[positionNumber]
            val position = existingPos ?: EingangsrechnungsPositionDO()

            // Set/update basic position fields
            position.eingangsrechnung = invoice
            position.number = positionNumber
            position.text = read.betreff
            position.menge = BigDecimal.ONE
            position.vat = read.taxRate
            position.deleted = false  // Restore position if it was deleted

            // Use netSum (net amount) from import DTO
            // The netSum is already calculated from grossSum during import to avoid rounding errors
            position.einzelNetto = read.netSum

            // Handle KostZuweisung
            if (read.kost1 != null || read.kost2 != null) {
                // Get or create KostZuweisung
                val existingKostZuweisungen = position.kostZuweisungen
                val kostZuweisung = if (!existingKostZuweisungen.isNullOrEmpty()) {
                    existingKostZuweisungen.first()
                } else {
                    KostZuweisungDO().also {
                        position.ensureAndGetKostzuweisungen().add(it)
                    }
                }

                // Set index (always 0 for imports as there's only one KostZuweisung per position)
                kostZuweisung.index = 0
                kostZuweisung.netto = position.einzelNetto
                // Set bidirectional relationship
                kostZuweisung.eingangsrechnungsPosition = position

                if (read.kost1 != null) {
                    val kost1 = Kost1DO()
                    kost1.id = read.kost1!!.id
                    kostZuweisung.kost1 = kost1
                }

                if (read.kost2 != null) {
                    val kost2 = Kost2DO()
                    kost2.id = read.kost2!!.id
                    kostZuweisung.kost2 = kost2
                }
            } else {
                // Clear KostZuweisung if no kost1/kost2 in import
                position.kostZuweisungen?.clear()
            }

            positions.add(position)
        }

        // Preserve existing positions that are not in the import (for partial updates)
        // This ensures that when only some positions are selected for update, the others remain unchanged
        existingPositionsById.values.forEach { existingPos ->
            if (!positions.any { it.number == existingPos.number }) {
                positions.add(existingPos)
            }
        }

        // Sort by position number to maintain correct order
        positions.sortBy { it.number }

        return positions
    }

    /**
     * Inserts a new invoice with positions.
     */
    private fun insertInvoiceWithPositions(entries: List<ImportPairEntry<EingangsrechnungPosImportDTO>>) {
        val dbEntity = EingangsrechnungDO()

        // Copy header from first entry
        val firstRead = entries.firstOrNull()?.read
        firstRead?.copyTo(dbEntity)

        // Set betreff (subject) from first position's betreff for position-based imports
        // This maps "Ware/Leistung" column to both invoice.betreff (header) and position.text
        if (firstRead?.betreff != null) {
            dbEntity.betreff = firstRead.betreff
        }

        // Create positions (no existing positions for new invoice)
        dbEntity.positionen = createPositions(entries, dbEntity, emptyMap())

        eingangsrechnungDao.insert(dbEntity)
        result.inserted += entries.size  // Count positions, not invoices
    }

    /**
     * Header-only import: Processes each entry independently without positions.
     * New invoices (without stored.id) are skipped as they must be created via position-based import.
     */
    private fun runHeaderOnlyImport() {
        for (entry in selectedEntries) {
            if (!isActive) {
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
                // Update existing invoice (header only, preserve positions)
                val existingEntity = eingangsrechnungDao.find(storedId)
                if (existingEntity != null) {
                    readEntry.copyTo(existingEntity)
                    // Positions remain unchanged (already loaded from DB)
                    val modStatus = eingangsrechnungDao.update(existingEntity)
                    if (modStatus != EntityCopyStatus.NONE) {
                        result.updated += 1
                    } else {
                        result.unmodified += 1
                    }
                } else {
                    // Stored entity not found in DB - skip as new invoice
                    log.warn { "Stored invoice with id=$storedId not found in DB, skipping as new invoice" }
                }
            } else {
                // Skip new invoices - they must be created via position-based import
                log.debug { "Skipping new invoice: referenz=${readEntry.referenz}, datum=${readEntry.datum}, kreditor=${readEntry.kreditor}" }
            }
        }
    }

    /**
     * Adds a fallback position for header-only imports to satisfy the requirement
     * that invoices must have at least one position.
     */
    private fun addFallbackPosition(invoice: EingangsrechnungDO) {
        val position = EingangsrechnungsPositionDO()
        position.eingangsrechnung = invoice
        position.number = 1
        position.text = invoice.betreff ?: "Import"
        position.menge = BigDecimal.ONE

        // Use grossSum from DTO if available (from enrichment calculation)
        val grossSum = invoice.zahlBetrag ?: BigDecimal.ZERO
        position.einzelNetto = grossSum
        position.vat = BigDecimal.ZERO

        invoice.positionen = mutableListOf(position)
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
