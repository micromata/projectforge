/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.business.sipgate.SipgateContactSyncDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.test.AbstractTestBase
import jakarta.persistence.EntityManager

private val log = KotlinLogging.logger {}

class SipgateSyncServiceMockTest : AbstractTestBase() {
  private var addressCounter = 0L
  var contactIdCounter = 0

  @Test
  fun syncTest() {
    // val configurationService = Mockito.mock(ConfigurationService::class.java)
    // Configuration(configurationService)
    val addressDao = object : AddressDao() {
      val addressList = createAddressList()
      override fun internalLoadAll(): MutableList<AddressDO> {
        return addressList
      }

      override fun internalSave(obj: AddressDO): Long {
        obj.id = ++addressCounter
        addressList.add(obj)
        return obj.id!!
      }

      override fun internalUpdate(obj: AddressDO): EntityCopyStatus {
        // Nothing to do, address values are already updated.
        return EntityCopyStatus.MAJOR
      }
    }
    val syncService = object : SipgateContactSyncService() {
      val syncDOList = mutableListOf<SipgateContactSyncDO>()
      override fun loadAll(): List<SipgateContactSyncDO> {
        return syncDOList
      }

      override fun upsert(entry: SipgateContactSyncDO) {
        val dbObj =
          syncDOList.find { it.sipgateContactId == entry.sipgateContactId || it.address!!.id == entry.address!!.id }
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
    syncService.configuration = SipgateConfiguration()
    syncService.configuration.updateLocalAddresses = true
    val contactService = object : SipgateContactService() {
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
    syncService.persistenceService = persistenceService
    syncService.addressDao = addressDao
    syncService.sipgateContactService = contactService
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
    Assertions.assertEquals(expectedCounter.deleted, actual.deleted)
    Assertions.assertEquals(expectedCounter.failed, actual.failed)
    Assertions.assertEquals(expectedCounter.total, actual.total)
    Assertions.assertEquals(expectedCounter.inserted, actual.inserted)
    Assertions.assertEquals(expectedCounter.updated, actual.updated)
    Assertions.assertEquals(expectedCounter.ignored, actual.ignored)

  }

  private fun createAddressList(): MutableList<AddressDO> {
    val list = mutableListOf<AddressDO>()
    list.add(
      SipgateSyncServiceTest.createAddress(
        "Reinhard", "Kai", "+49123456789", organization = "ACME ltd.",
        id = ++addressCounter
      )
    )
    list.add(
      SipgateSyncServiceTest.createAddress(
        "Müller", "Berta", "+492222222222", organization = "ACME ltd.",
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
