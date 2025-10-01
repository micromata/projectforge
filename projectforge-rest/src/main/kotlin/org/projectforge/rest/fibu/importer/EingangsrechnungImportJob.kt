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
    private suspend fun runPositionBasedImport() {
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
                        result.deleted += 1
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
     * Reuses existing position IDs by position number to preserve history entries (no orphanRemoval).
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

        // Build map of existing positions by position number for ID reuse
        val existingPositionsById = existingEntity.positionen
            ?.filter { !it.deleted }  // Only consider non-deleted positions for matching
            ?.associateBy { it.number }
            ?: emptyMap()

        // Create new positions, reusing IDs by position number
        existingEntity.positionen = createPositions(entries, existingEntity, existingPositionsById)

        val modStatus = eingangsrechnungDao.update(existingEntity)
        if (modStatus != EntityCopyStatus.NONE) {
            result.updated += 1
        } else {
            result.unmodified += 1
        }
    }

    /**
     * Creates positions from import data, reusing existing position IDs by position number to preserve history.
     * Positions not in the import will be marked as deleted by the CollectionHandler (soft-delete).
     */
    private fun createPositions(
        entries: List<ImportPairEntry<EingangsrechnungPosImportDTO>>,
        invoice: EingangsrechnungDO,
        existingPositionsById: Map<Short, EingangsrechnungsPositionDO>
    ): MutableList<EingangsrechnungsPositionDO> {
        val positions = mutableListOf<EingangsrechnungsPositionDO>()

        entries.forEachIndexed { index, entry ->
            val read = entry.read ?: return@forEachIndexed

            val position = EingangsrechnungsPositionDO()
            val positionNumber = (index + 1).toShort()

            // Set basic position fields first
            position.eingangsrechnung = invoice
            position.number = positionNumber
            position.text = read.betreff
            position.menge = BigDecimal.ONE
            position.vat = read.taxRate

            // Calculate einzelNetto (net amount) from grossSum (gross amount from CSV) BEFORE creating KostZuweisung
            // The CSV contains the gross amount (Brutto), which must be converted to net amount (Netto)
            // Formula: Netto = Brutto / (1 + MwSt-Satz)
            // taxRate is already normalized as decimal (e.g., 0.19 for 19%)
            // This ensures kostZuweisung.netto has the correct calculated value.
            val grossSum = read.grossSum
            val taxRate = read.taxRate
            if (grossSum != null) {
                if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) != 0) {
                    // Convert gross to net: Netto = Brutto / (1 + MwSt)
                    val divisor = BigDecimal.ONE.add(taxRate)
                    position.einzelNetto = grossSum.divide(divisor, 2, RoundingMode.HALF_UP)
                } else {
                    // No VAT, gross = net
                    position.einzelNetto = grossSum
                }
            }

            // Now reuse IDs and create KostZuweisung with correct netto value
            val existingPos = existingPositionsById[positionNumber]
            if (existingPos != null) {
                position.id = existingPos.id

                // Also reuse existing KostZuweisung IDs if present
                val existingKostZuweisungen = existingPos.kostZuweisungen
                if (!existingKostZuweisungen.isNullOrEmpty()) {
                    val existingKostZuweisung = existingKostZuweisungen.first()

                    if (read.kost1 != null || read.kost2 != null) {
                        val kostZuweisung = KostZuweisungDO()
                        kostZuweisung.id = existingKostZuweisung.id // Reuse ID to preserve history
                        kostZuweisung.netto = position.einzelNetto // Now einzelNetto is correctly calculated
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

                        position.ensureAndGetKostzuweisungen().add(kostZuweisung)
                    }
                }
            } else {
                // New position (no existing position with this number), create new KostZuweisung if needed
                if (read.kost1 != null || read.kost2 != null) {
                    val kostZuweisung = KostZuweisungDO()
                    kostZuweisung.netto = position.einzelNetto // Now einzelNetto is correctly calculated
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

                    position.ensureAndGetKostzuweisungen().add(kostZuweisung)
                }
            }

            positions.add(position)
        }

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

        // Create positions (no existing positions for new invoice)
        dbEntity.positionen = createPositions(entries, dbEntity, emptyMap())

        eingangsrechnungDao.insert(dbEntity)
        result.inserted += 1
    }

    /**
     * Header-only import: Processes each entry independently without positions.
     */
    private suspend fun runHeaderOnlyImport() {
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
                // Update existing invoice (header only)
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
                    // Stored entity not found, insert as new (with empty position as fallback)
                    val dbEntity = EingangsrechnungDO()
                    readEntry.copyTo(dbEntity)
                    addFallbackPosition(dbEntity)
                    eingangsrechnungDao.insert(dbEntity)
                    result.inserted += 1
                }
            } else {
                // Insert new invoice (header only, add fallback position)
                val dbEntity = EingangsrechnungDO()
                readEntry.copyTo(dbEntity)
                addFallbackPosition(dbEntity)
                eingangsrechnungDao.insert(dbEntity)
                result.inserted += 1
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
