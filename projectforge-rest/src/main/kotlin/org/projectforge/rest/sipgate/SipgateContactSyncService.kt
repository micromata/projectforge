/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.business.sipgate.SipgateContactSyncDO
import org.projectforge.business.sipgate.SipgateNumber
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.reflect.KMutableProperty

private val log = KotlinLogging.logger {}

/**
 * Fragen sipgate:
 *   - Warum werden Felder, wie firstName, family, notizen etc. nicht übertragen?
 *   - Warum fehlt der Typ bei Adresse?
 *   - Hilfreich wäre eine Reference-ID (String oder Zahl) zum Verknüpfen von Sipgate und Fremdsystemadressen.
 *   - Hilfreich wäre ansonsten als Antwort nach einem Insert die Contact-id.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
open class SipgateContactSyncService {
  class SyncResult {
    var addressDOOutdated = false // true, if the address has to be updated in ProjectForge.
    var contactOutdated = false   // true, if the contact has to be updated in Sipgate.
  }

  class Counter {
    var inserted = 0
    var updated = 0
    var deleted = 0
    var failed = 0
    var unmodified = 0
    var total = 0

    override fun toString(): String {
      return "total=$total, inserted=$inserted, updated=$updated, deleted=$deleted, failed=$failed, unmodified=$unmodified"
    }
  }

  class SyncContext {
    var remoteContacts = listOf<SipgateContact>()
    var addressList = listOf<AddressDO>()
    var syncDOList = mutableListOf<SipgateContactSyncDO>()
    var localCounter = Counter()
    var remoteCounter = Counter()
  }

  @Autowired
  private lateinit var sipgateContactService: SipgateContactService

  @Autowired
  private lateinit var addressDao: AddressDao

  @PersistenceContext
  private lateinit var em: EntityManager

  companion object {
    internal var countryPrefixForTestcases: String? = null

    internal fun from(address: AddressDO): SipgateContact {
      val contact = SipgateContact()
      // contact.id
      contact.name = SipgateContactSyncDO.getName(address)
      contact.family = address.name
      contact.given = address.firstName
      // var picture: String? = null
      address.email?.let { contact.email = it }
      address.privateEmail?.let { contact.privateEmail = it }
      val numbers = mutableListOf<SipgateNumber>()
      address.businessPhone?.let { numbers.add(SipgateNumber(it).setWork()) }
      address.mobilePhone?.let { numbers.add(SipgateNumber(it).setCell()) }
      address.privatePhone?.let { numbers.add(SipgateNumber(it).setHome()) }
      address.privateMobilePhone?.let { numbers.add(SipgateNumber(it).setOther()) }
      address.fax?.let { numbers.add(SipgateNumber(it).setFaxWork()) }
      contact.numbers = numbers
      /* Ignore addresses (synchronize will be pain, because not type of addresses will be given by Sipgate.
        val addresses = mutableListOf<SipgateAddress>()
        createAddress(
          addressText = address.addressText,
          addressText2 = address.addressText2,
          zipCode = address.zipCode,
          city = address.city,
          state = address.state,
          country = address.country,
        )?.let { addresses.add(it) }
        createAddress(
          addressText = address.privateAddressText,
          addressText2 = address.privateAddressText2,
          zipCode = address.privateZipCode,
          city = address.privateCity,
          state = address.privateState,
          country = address.privateCountry,
        )?.let { addresses.add(it) }
        if (addresses.isNotEmpty()) {
          contact.addresses = addresses.toTypedArray()
        }
    */

      contact.organization = address.organization
      contact.division = address.division
      contact.scope = SipgateContact.Scope.SHARED
      return contact
    }

    internal fun from(contact: SipgateContact): AddressDO {
      val address = extractName(contact.name)
      // contact.id
      // var picture: String? = null
      address.email = contact.email
      address.privateEmail = contact.privateEmail

      address.businessPhone = contact.work
      address.mobilePhone = contact.cell
      address.privatePhone = contact.home
      address.privateMobilePhone = contact.other
      address.fax = contact.faxWork

      /* Ignore addresses (synchronize will be pain, because not type of addresses will be given by Sipgate.
    val addresses = mutableListOf<SipgateAddress>()
    createAddress(
      addressText = address.addressText,
      addressText2 = address.addressText2,
      zipCode = address.zipCode,
      city = address.city,
      state = address.state,
      country = address.country,
    )?.let { addresses.add(it) }
    createAddress(
      addressText = address.privateAddressText,
      addressText2 = address.privateAddressText2,
      zipCode = address.privateZipCode,
      city = address.privateCity,
      state = address.privateState,
      country = address.privateCountry,
    )?.let { addresses.add(it) }
     */

      address.organization = contact.organization
      address.division = contact.division
      return address
    }

    /**
     * @param contact given by Sipgate (will be modified, if to be modified).
     * @param address given by ProjectForge (will be modified, if to be modified).
     * @return result with info whether the objects have to be updated or not.
     */
    internal fun sync(
      contact: SipgateContact,
      address: AddressDO,
      syncInfo: SipgateContactSyncDO.SyncInfo?
    ): SyncResult {
      val result = SyncResult()
      if (contact.name != SipgateContactSyncDO.getName(address)) {
        if (syncInfo != null && syncInfo.fieldsInfo["name"] != SipgateContactSyncDO.SyncInfo.hash(contact.name)) {
          // address to be updated
          val adr = extractName(contact.name)
          address.name = adr.name
          address.firstName = adr.firstName
          result.addressDOOutdated = true
        } else {
          contact.name = SipgateContactSyncDO.getName(address)
          result.contactOutdated = true
        }
      }
      sync(contact, SipgateContact::organization, address, AddressDO::organization, syncInfo, result)
      sync(contact, SipgateContact::division, address, AddressDO::division, syncInfo, result)

      sync(contact, SipgateContact::email, address, AddressDO::email, syncInfo, result)
      sync(contact, SipgateContact::privateEmail, address, AddressDO::privateEmail, syncInfo, result)

      sync(contact, SipgateContact::work, address, AddressDO::businessPhone, syncInfo, result)
      sync(contact, SipgateContact::home, address, AddressDO::privatePhone, syncInfo, result)
      sync(contact, SipgateContact::cell, address, AddressDO::mobilePhone, syncInfo, result)
      sync(contact, SipgateContact::other, address, AddressDO::privateMobilePhone, syncInfo, result)
      sync(contact, SipgateContact::faxWork, address, AddressDO::fax, syncInfo, result)
      return result
    }

    internal fun sync(
      contact: SipgateContact,
      contactField: KMutableProperty<*>,
      address: AddressDO,
      addressField: KMutableProperty<*>,
      syncInfo: SipgateContactSyncDO.SyncInfo?,
      result: SyncResult,
    ) {
      val contactValue = contactField.getter.call(contact) as String?
      val addressValue = addressField.getter.call(address)
      if (contactValue != addressValue) {
        if (syncInfo != null && syncInfo.fieldsInfo[addressField.name] != SipgateContactSyncDO.SyncInfo.hash(
            contactValue
          )
        ) {
          // remote contact was modified, so local address is outdated.
          addressField.setter.call(address, contactValue)
          result.addressDOOutdated = true
        } else {
          // local address was modified, so remote contact is outdated.
          contactField.setter.call(contact, addressValue)
          result.contactOutdated = true
        }
      }
    }

    internal fun extractName(name: String?): AddressDO {
      val address = AddressDO()
      if (name.isNullOrBlank()) {
        return address
      }
      val names = name.split(" ")
      address.name = names.last().trim()
      address.firstName = names.take(names.size - 1).joinToString(" ")
      return address
    }

    /**
     * Tries to find the address with the best match (if address isn't synced and connected to remote contact yet).
     * The returned address (if any) does have a matchScore greater or equal than 1.
     * @param addresses List of all addresses
     * @param contact Contact to match.
     */
    fun findBestMatch(addresses: List<AddressDO>, contact: SipgateContact): AddressDO? {
      val matches = addresses.filter { SipgateContactSyncDO.getName(it).lowercase() == contact.name?.trim()?.lowercase() }
      if (matches.isEmpty()) {
        return null
      }
      val result = matches.maxByOrNull { matchScore(contact, it) }
      return if (result == null || matchScore(contact, result) < 1) {
        null
      } else {
        result
      }
    }

    internal fun matchScore(contact: SipgateContact, address: AddressDO): Int {
      if (contact.name?.trim()?.lowercase() != SipgateContactSyncDO.getName(address).lowercase()) {
        return -1
      }
      var counter = 1
      val numbers = arrayOf(
        extractNumber(address.businessPhone),
        extractNumber(address.mobilePhone),
        extractNumber(address.privateMobilePhone),
        extractNumber(address.privatePhone),
        extractNumber(address.fax),
      )
      contact.numbers?.forEach { number ->
        val extractedNumber = extractNumber(number.number)
        numbers.forEach { if (it != null && extractedNumber == it) ++counter }
      }
      contact.emails?.forEach { email ->
        val str = email.email?.trim()?.lowercase()
        if (str != null && str == address.email?.trim()?.lowercase() || str == address.privateEmail?.trim()
            ?.lowercase()
        ) {
          ++counter
        }
      }
      contact.addresses?.forEach { adr ->
        val str = adr.postalCode?.trim()?.lowercase()
        if (str != null && str == address.zipCode?.trim() || str == address.privateZipCode) {
          ++counter
        }
      }
      if (address.division != null && contact.division?.trim()?.lowercase() == address.division?.trim()?.lowercase()) {
        ++counter
      }
      if (address.organization != null && contact.organization?.trim()?.lowercase() == address.organization?.trim()
          ?.lowercase()
      ) {
        ++counter
      }
      return counter
    }

    private fun extractNumber(number: String?): String? {
      number ?: return null
      if (countryPrefixForTestcases != null) {
        return NumberHelper.extractPhonenumber(number, countryPrefixForTestcases)
      }
      return NumberHelper.extractPhonenumber(number)
    }
  }

  open fun sync(): SyncContext {
    val syncContext = SyncContext()
    syncContext.addressList =
      addressDao.internalLoadAll() // Need all for matching contacts, but only active will be used for syncing to Sipgate.
    updateSyncObjects(syncContext)

    syncContext.localCounter.total = syncContext.addressList.size
    syncContext.addressList.forEach { address ->
      val syncDO = syncContext.syncDOList.find { it.address?.id == address.id }
      val contactId = syncDO?.sipgateContactId
      if (contactId != null) {
        val contact = syncContext.remoteContacts.find { it.id == contactId }
        if (address.contactStatus.isIn(ContactStatus.ACTIVE)) {
          if (contact != null) {
            // Update if active
            val syncResult = sync(contact, address, syncDO.syncInfo)
            if (syncResult.addressDOOutdated) {
              try {
                addressDao.internalUpdate(address)
                syncContext.localCounter.failed++
              } catch (ex: Exception) {
                log.error(ex.message, ex)
                syncContext.localCounter.updated++
              }
            }
            if (syncResult.contactOutdated) {
              updateRemoteContact(contact, syncDO, syncContext)
            }
          } else {
            // Create
            createRemoteContact(address, syncContext)
          }
        } else if (contact != null) {
          // Delete if not active
          deleteRemoteContact(contact, syncDO, syncContext)
        }
      } else if (address.contactStatus.isIn(ContactStatus.ACTIVE)) {
        // Create if active
        createRemoteContact(address, syncContext)
      } else {
        // Ignore if not active
      }
    }
    syncContext.remoteContacts.forEach { contact ->
      syncContext.syncDOList.find { it.sipgateContactId == contact.id }?.sipgateContactId.let { contactId ->
        if (contactId == null) {
          try {
            // Remote contact seems to be a new contact.
            val address = from(contact)
            addressDao.internalSave(address)
            val newSyncDO =
              SipgateContactSyncDO.create(contact, address, SipgateContactSyncDO.RemoteStatus.CREATED_BY_LOCAL)
            upsert(newSyncDO)
            syncContext.localCounter.inserted++
          } catch (ex: Exception) {
            log.error(ex.message, ex)
            syncContext.localCounter.failed++
          }
        }
      }
    }
    updateSyncObjects(syncContext)
    syncContext.remoteCounter.total = syncContext.remoteContacts.size
    // Delete remote contacts (without numbers)?
    return syncContext
  }

  private fun createRemoteContact(address: AddressDO, syncContext: SyncContext) {
    try {
      val contact = from(address)
      sipgateContactService.create(contact)
      val newSyncDO = SipgateContactSyncDO.create(null, address, SipgateContactSyncDO.RemoteStatus.CREATED_BY_LOCAL)
      // Can't set syncDO.sipgateContactId. Will be updated after next updateSyncObjects call.
      upsert(newSyncDO)
      syncContext.remoteCounter.inserted++
    } catch (ex: Exception) {
      log.error(ex.message, ex)
      syncContext.remoteCounter.failed++
    }
  }

  private fun updateRemoteContact(contact: SipgateContact, syncDO: SipgateContactSyncDO, syncContext: SyncContext) {
    contact.id?.let { contactId ->
      try {
        sipgateContactService.update(contactId, contact)
        syncDO.lastSync = Date()
        syncDO.remoteStatus = SipgateContactSyncDO.RemoteStatus.OK
        syncDO.updateJson(contact)
        upsert(syncDO)
        syncContext.remoteCounter.updated++
      } catch (ex: Exception) {
        log.error(ex.message, ex)
        syncContext.remoteCounter.failed++
      }
    }
  }

  private fun deleteRemoteContact(contact: SipgateContact, syncDO: SipgateContactSyncDO, syncContext: SyncContext) {
    contact.id?.let { contactId ->
      try {
        sipgateContactService.delete(contactId, contact)
        syncDO.lastSync = Date()
        syncDO.remoteStatus = SipgateContactSyncDO.RemoteStatus.DELETED_BY_LOCAL
        syncDO.updateJson(null)
        // Can't set syncDO.sipgateContactId. Will be updated after next updateSyncObjects call.
        upsert(syncDO)
        syncContext.remoteCounter.deleted++
      } catch (ex: Exception) {
        log.error(ex.message, ex)
        syncContext.remoteCounter.failed++
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  open fun upsert(entry: SipgateContactSyncDO) {
    val contactId = entry.sipgateContactId
    val addressId = entry.address?.id
    requireNotNull(contactId) { "Contact id must be given for upsert of entry." }
    requireNotNull(addressId) { "Address id must be given." }
    val dbObj = findByContactOrAddressId(contactId, addressId)
    if (dbObj != null) {
      log.info { "Updating sipgateContactSync entry for address id '${entry.address?.id}' and contact id '${entry.sipgateContactId}'." }
      // Ensure correct id and address: (should always an NOP):
      dbObj.sipgateContactId = entry.sipgateContactId
      dbObj.address = entry.address
      // Copy values to merge:
      dbObj.syncInfoAsJson = entry.syncInfoAsJson
      dbObj.lastSync = entry.lastSync
      em.merge(dbObj)
    } else {
      log.info { "Storing new sipgateContactSync entry for address id '${entry.address?.id}' and contact id '${entry.sipgateContactId}'." }
      em.persist(entry)
    }
    em.flush()
  }

  /**
   * Proceeds all given remoteContacts and tries to ensure, that a match is persisted (via SipgateContactSyncDO)
   */
  private fun updateSyncObjects(syncContext: SyncContext) {
    // Get remote contacts and local addresses
    syncContext.remoteContacts = sipgateContactService.getList()
    // Load already matched contacts
    syncContext.syncDOList = loadAll().toMutableList()
    // syncContext. addressList =
    //  addressDao.internalLoadAll() // Need all for matching contacts, but only active will be used for syncing to Sipgate.
    syncContext.remoteContacts.forEach { contact ->
      if (syncContext.syncDOList.none { it.sipgateContactId == contact.id }) {
        val match = findBestMatch(syncContext.addressList, contact)
        if (match != null) {
          val syncDO = SipgateContactSyncDO.create(contact, match, SipgateContactSyncDO.RemoteStatus.OK)
          syncDO.updateJson(contact)
          syncContext.syncDOList.add(syncDO)
          upsert(syncDO)
        }
      }
    }
  }

  private fun findByContactOrAddressId(sipgateContactId: String, addressId: Int): SipgateContactSyncDO? {
    val list =
      em.createNamedQuery(SipgateContactSyncDO.FIND_BY_CONTACT_AND_ADDRESS_ID, SipgateContactSyncDO::class.java)
        .setParameter("sipgateContactId", sipgateContactId)
        .setParameter("addressId", addressId)
        .resultList
    if (list.size == 0) {
      return null
    }
    if (list.size > 1) {
      // Shouldn't occur.
      log.error { "Oups, sync entry for contact-id '$sipgateContactId' and address #$addressId not unique! Found ${list.size} entries with such contact id or address id." }
      list.find { it.address?.id == addressId && it.sipgateContactId == sipgateContactId }?.let {
        // Best match
        return it
      }
      list.find { it.address?.id == addressId }?.let {
        // Try to use local pk if possible.
        return it
      }
    }
    return list.first()
  }

  internal open fun loadAll(): List<SipgateContactSyncDO> {
    return em.createNamedQuery(SipgateContactSyncDO.LOAD_ALL, SipgateContactSyncDO::class.java)
      .resultList
  }
}
