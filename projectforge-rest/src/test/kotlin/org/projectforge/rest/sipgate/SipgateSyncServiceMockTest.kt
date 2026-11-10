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

package org.projectforge.rest.sipgate

import jakarta.persistence.EntityManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.business.sipgate.SipgateContactSyncDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.business.test.AbstractTestBase

private val log = KotlinLogging.logger {}

class SipgateSyncServiceMockTest : AbstractTestBase() {
    private var addressCounter = 0L
    var contactIdCounter = 0

    private inner class Mocks {
        val addressDao = MockAddressDao()
        val syncService = MockSyncService()
        val contactService = MockContactService()

        init {
            syncService.configuration = SipgateConfiguration()
            syncService.configuration.updateLocalAddresses = true
            syncService.persistenceService = persistenceService
            syncService.addressDao = addressDao
            syncService.sipgateContactService = contactService
        }

        /**
         * Simulates AddressDao.forceDelete: the address is gone, its sync entry is kept as tombstone
         * (see SipgateDeleteContactService).
         */
        fun forceDelete(address: AddressDO): String {
            addressDao.addressList.remove(address)
            val syncDO = syncService.syncDOList.first { it.address?.id == address.id }
            syncDO.address = null
            syncDO.remoteStatus = SipgateContactSyncDO.RemoteStatus.DELETED_BY_LOCAL
            return syncDO.sipgateContactId!!
        }
    }

    private inner class MockAddressDao : AddressDao() {
        val addressList = createAddressList()
        override fun selectAll(checkAccess: Boolean): MutableList<AddressDO> {
            return addressList
        }

        override fun insert(obj: AddressDO, checkAccess: Boolean): Long {
            obj.id = ++addressCounter
            addressList.add(obj)
            return obj.id!!
        }

        override fun update(obj: AddressDO, checkAccess: Boolean): EntityCopyStatus {
            // Nothing to do, address values are already updated.
            return EntityCopyStatus.MAJOR
        }
    }

    private class MockSyncService : SipgateContactSyncService() {
        val syncDOList = mutableListOf<SipgateContactSyncDO>()
        override fun loadAll(): List<SipgateContactSyncDO> {
            return syncDOList
        }

        override fun upsert(entry: SipgateContactSyncDO) {
            val dbObj =
                syncDOList.find { it.sipgateContactId == entry.sipgateContactId || it.address?.id == entry.address!!.id }
            if (dbObj != null) {
                dbObj.syncInfoAsJson = entry.syncInfoAsJson
                dbObj.lastSync = entry.lastSync
            } else {
                syncDOList.add(entry)
            }
        }

        override fun delete(entry: SipgateContactSyncDO, em: EntityManager) {
            syncDOList.remove(entry)
        }
    }

    private inner class MockContactService : SipgateContactService() {
        val contactList = createContactList()

        override fun getList(offset: Int, limit: Int, maxNumberOfPages: Int): List<SipgateContact> {
            return contactList
        }

        override fun create(entity: SipgateContact): Boolean {
            contactList.add(entity)
            entity.family = null // Will not returned by Sipgate
            entity.given = null  // Will not returned by Sipgate
            entity.id = "16CC5120-9AEF-11ED-976C-B9402C0419${++contactIdCounter}"
            return true
        }

        override fun update(id: String, entity: SipgateContact): Boolean {
            contactList.removeIf { it.id == id }
            contactList.add(entity)
            return true
        }

        override fun delete(id: String?, entity: SipgateContact): Boolean {
            contactList.removeIf { it.id == id }
            return true
        }
    }

    @Test
    fun syncTest() {
        val mocks = Mocks()
        val addressDao = mocks.addressDao
        val syncService = mocks.syncService
        val contactService = mocks.contactService
        var syncContext = syncService.sync()
        assertSynContext(
            syncContext,
            SipgateContactSyncService.Counter(total = 3, inserted = 2),
            SipgateContactSyncService.Counter(total = 2, inserted = 1),
            3,
        )

        var contact = contactService.contactList.first()
        var address = addressDao.addressList.first()
        log.info { "Modifying address '${address.fullName}', deleting contact '${contact.name}' and syncing again..." }
        address.mobilePhone = "+49 4242424242"
        address.businessPhone = "+49 561 12345"
        contactService.contactList.removeFirst()
        syncContext = syncService.sync()
        assertSynContext(
            syncContext,
            SipgateContactSyncService.Counter(total = 3, inserted = 1, updated = 1),
            SipgateContactSyncService.Counter(total = 3),
            3,
        )

        contact = contactService.contactList.first()
        address = addressDao.addressList.first()
        val address2 = addressDao.addressList.get(2)
        log.info { "Modifying contact '${contact.name}', deleting address '${address.fullName}', marking address '${address2.fullName}' as outdated and syncing again..." }
        contact.cell = "+49 3333333"
        contact.faxWork = "+49 4444444"
        addressDao.addressList.removeFirst()
        address2.addressStatus = AddressStatus.OUTDATED
        syncContext = syncService.sync()
        assertSynContext(
            syncContext,
            SipgateContactSyncService.Counter(total = 2, deleted = 1),
            SipgateContactSyncService.Counter(total = 2, inserted = 1, updated = 1),
            2,
        )
    }

    @Test
    fun forceDeletedAddressTest() {
        val mocks = Mocks()
        mocks.syncService.sync() // Kai and Berta pushed, Paul imported.
        val address = mocks.addressDao.addressList.first()
        val contactId = mocks.forceDelete(address)
        val syncContext = mocks.syncService.sync()
        assertSynContext(
            syncContext,
            SipgateContactSyncService.Counter(total = 2, deleted = 1),
            SipgateContactSyncService.Counter(total = 2),
            2,
        )
        Assertions.assertTrue(mocks.contactService.contactList.none { it.id == contactId }, "remote contact deleted")
        Assertions.assertTrue(mocks.syncService.syncDOList.none { it.sipgateContactId == contactId }, "tombstone deleted")
        Assertions.assertTrue(mocks.addressDao.addressList.none { it.fullName == address.fullName }, "not imported again")
    }

    @Test
    fun forceDeletedAddressWithoutRemoteUpdatesTest() {
        val mocks = Mocks()
        mocks.syncService.sync()
        mocks.syncService.configuration.updateSipgateContacts = false
        val address = mocks.addressDao.addressList.first()
        val contactId = mocks.forceDelete(address)
        val syncContext = mocks.syncService.sync()
        Assertions.assertEquals(0, syncContext.localCounter.inserted, "local inserted")
        Assertions.assertTrue(mocks.contactService.contactList.any { it.id == contactId }, "remote contact kept")
        Assertions.assertTrue(mocks.syncService.syncDOList.any { it.sipgateContactId == contactId }, "tombstone kept")
        Assertions.assertTrue(mocks.addressDao.addressList.none { it.fullName == address.fullName }, "not imported again")
    }

    private fun assertSynContext(
        syncContext: SipgateContactSyncService.SyncContext,
        remoteCounter: SipgateContactSyncService.Counter,
        localCounter: SipgateContactSyncService.Counter,
        expectedSyncSize: Int,
    ) {
        assertCounter(remoteCounter, syncContext.remoteCounter)
        assertCounter(localCounter, syncContext.localCounter)
        Assertions.assertEquals(expectedSyncSize, syncContext.syncDOList.size)
    }

    private fun assertCounter(
        expectedCounter: SipgateContactSyncService.Counter,
        actual: SipgateContactSyncService.Counter
    ) {
        Assertions.assertEquals(expectedCounter.deleted, actual.deleted, "deleted")
        Assertions.assertEquals(expectedCounter.failed, actual.failed, "failed")
        Assertions.assertEquals(expectedCounter.total, actual.total, "total")
        Assertions.assertEquals(expectedCounter.inserted, actual.inserted, "inserted")
        Assertions.assertEquals(expectedCounter.updated, actual.updated, "updated")
        Assertions.assertEquals(expectedCounter.ignored, actual.ignored, "ignored")

    }

    private fun createAddressList(): MutableList<AddressDO> {
        val list = mutableListOf<AddressDO>()
        list.add(
            SipgateSyncServiceTest.createAddress(
                "Reinhard", "Kai", "+49 123456789", organization = "ACME ltd.",
                id = ++addressCounter
            )
        )
        list.add(
            SipgateSyncServiceTest.createAddress(
                "Müller", "Berta", "+49 2222222222", organization = "ACME ltd.",
                id = ++addressCounter
            )
        )
        return list
    }

    private fun createContactList(): MutableList<SipgateContact> {
        val list = mutableListOf<SipgateContact>()
        list.add(
            SipgateSyncServiceTest.createContact(
                "Hurzel",
                "Paul",
                mobilePhone = "+1 3456789",
                organization = "ACME ltd.",
                id = "16CC5120-9AEF-11ED-976C-B9402C0419${++contactIdCounter}"
            )
        )
        return list
    }
}
