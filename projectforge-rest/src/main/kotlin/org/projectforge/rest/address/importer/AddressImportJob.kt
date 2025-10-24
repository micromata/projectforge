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
import org.projectforge.business.address.*
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.importer.AbstractImportJob
import org.projectforge.rest.importer.ImportPairEntry

private val log = KotlinLogging.logger {}

class AddressImportJob(
    private val selectedEntries: List<ImportPairEntry<AddressImportDTO>>,
    private val importStorage: AddressImportStorage,
) : AbstractImportJob(
    translateMsg("address.import.job.title", selectedEntries.size.toString()),
    area = "AddressImport",
    queueName = "addressImport",
    timeoutSeconds = 600,
    importStorage = importStorage,
    selectedEntries = selectedEntries,
) {

    private lateinit var addressDao: AddressDao
    private lateinit var addressbookDao: AddressbookDao
    private lateinit var addressImageDao: AddressImageDao

    init {
        totalNumber = selectedEntries.size
        processedNumber = 0
    }

    override fun onBeforeStart() {
        // Initialize DAOs
        val context = ApplicationContextProvider.getApplicationContext()
        addressDao = context.getBean(AddressDao::class.java)
        addressbookDao = context.getBean(AddressbookDao::class.java)
        addressImageDao = context.getBean(AddressImageDao::class.java)

        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
        // Mark all entries as not reconciled so they show status UNKNOWN until processed
        importStorage.pairEntries.forEach { it.reconciled = false }
    }

    override fun onAfterTermination() {
        importStorage.reconcileImportStorage(rereadDatabaseEntries = true)
    }

    override suspend fun run() {
        log.info("Starting import of ${selectedEntries.size} address entries.")

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
                    val existingEntity = addressDao.find(storedId)
                    if (existingEntity != null && !existingEntity.deleted) {
                        addressDao.markAsDeleted(existingEntity)
                        result.deleted += 1
                    }
                }
                continue
            }

            if (storedId != null) {
                // Update existing address
                updateAddress(storedId, readEntry)
            } else {
                // Insert new address
                insertAddress(readEntry)
            }
        }

        log.info("Import completed: inserted=${result.inserted}, updated=${result.updated}, deleted=${result.deleted}, unmodified=${result.unmodified}")
    }

    /**
     * Updates an existing address with new data.
     */
    private fun updateAddress(storedId: Long, readEntry: AddressImportDTO) {
        val existingEntity = addressDao.find(storedId)
        if (existingEntity == null) {
            log.warn { "Stored address with id=$storedId not found, inserting as new" }
            insertAddress(readEntry)
            return
        }

        // Copy data from DTO to entity
        readEntry.copyTo(existingEntity)

        // Update address
        val modStatus = addressDao.update(existingEntity)
        if (modStatus != EntityCopyStatus.NONE) {
            result.updated += 1
        } else {
            result.unmodified += 1
        }

        // Handle image update
        val imageData = readEntry.getTransientAttribute("image") as? AddressImageDO
        if (imageData != null && imageData.image != null && imageData.imageType != null) {
            addressImageDao.saveOrUpdate(storedId, imageData.image!!, imageData.imageType!!)
            log.debug { "Updated image for address ID $storedId" }
        }
    }

    /**
     * Inserts a new address.
     */
    private fun insertAddress(readEntry: AddressImportDTO) {
        val dbEntity = AddressDO()

        // Copy data from DTO to entity
        readEntry.copyTo(dbEntity)

        // Set default status
        dbEntity.addressStatus = AddressStatus.UPTODATE
        dbEntity.contactStatus = ContactStatus.ACTIVE

        // Add to global addressbook
        val globalAddressbook = addressbookDao.globalAddressbook
        if (globalAddressbook != null) {
            dbEntity.add(globalAddressbook)
            log.debug { "Added address '${dbEntity.fullName}' to global addressbook" }
        } else {
            log.warn { "Global addressbook not found, address will not be added to any addressbook" }
        }

        // Insert address
        addressDao.insert(dbEntity)
        result.inserted += 1

        // Handle image insert
        val imageData = readEntry.getTransientAttribute("image") as? AddressImageDO
        if (imageData != null && dbEntity.id != null && imageData.image != null && imageData.imageType != null) {
            addressImageDao.saveOrUpdate(dbEntity.id!!, imageData.image!!, imageData.imageType!!)
            log.debug { "Inserted image for new address ID ${dbEntity.id}" }
        }
    }

    override fun readAccess(user: PFUserDO?): Boolean {
        user ?: return false
        return isOwner || addressDao.hasUserSelectAccess(user, false)
    }

    override fun writeAccess(user: PFUserDO?): Boolean {
        user ?: return false
        // For import operations, we need general insert/update access to addresses
        return isOwner || addressDao.hasLoggedInUserInsertAccess()
    }
}
