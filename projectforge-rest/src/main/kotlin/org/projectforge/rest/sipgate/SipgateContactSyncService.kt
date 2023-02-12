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
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.business.sipgate.SipgateContactSyncDO
import org.projectforge.business.sipgate.SipgateNumber
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDOChangedListener
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
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
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
open class SipgateContactSyncService : BaseDOChangedListener<AddressDO> {
  /**
   * Holds result of comparison of local address and remote contact.
   */
  class SyncResult {
    var addressDOOutdated = false // true, if the address has to be updated in ProjectForge.
    var contactOutdated = false   // true, if the contact has to be updated in Sipgate.
    var outdatedAddressFields = mutableListOf<String>()
    var outdatedContactFields = mutableListOf<String>()

    fun addOutdatedAddressField(field: String, oldValue: String?, newValue: String?) {
      outdatedAddressFields.add("$field: '$oldValue'->'$newValue'")
    }

    fun addOutdatedContactField(field: String, oldValue: String?, newValue: Any?) {
      outdatedContactFields.add("$field: '$oldValue'->'$newValue'")
    }

    override fun toString(): String {
      val sb = StringBuilder()
      sb.append("addressOutdated=$addressDOOutdated")
      if (outdatedAddressFields.isNotEmpty()) {
        sb.append(outdatedAddressFields.joinToString(prefix = " fields=[", postfix = "]"))
      }
      sb.append(", contactOutdated=$contactOutdated")
      if (outdatedContactFields.isNotEmpty()) {
        sb.append(outdatedContactFields.joinToString(prefix = " fields=[", postfix = "]"))
      }
      return sb.toString()
    }
  }

  /**
   * Modification counter for local and remote objects.
   */
  class Counter(
    var inserted: Int = 0,
    var updated: Int = 0,
    var deleted: Int = 0,
    var failed: Int = 0,
    var ignored: Int = 0,
    var total: Int = 0,
  ) {
    override fun toString(): String {
      return "total=$total, inserted=$inserted, updated=$updated, deleted=$deleted, failed=$failed, ignored=$ignored"
    }
  }

  /**
   * This context is used by sync and holds all used data.
   */
  class SyncContext {
    var remoteContacts = mutableListOf<SipgateContact>()
    var addressList = listOf<AddressDO>()
    var syncDOList = mutableListOf<SipgateContactSyncDO>()
    var localCounter = Counter()
    var remoteCounter = Counter()

    override fun toString(): String {
      return "${remoteContacts.size} remote contacts, ${addressList.size} local addresses, ${syncDOList.size} sync objects, localCounter=$localCounter, remoteCounter=$remoteCounter"
    }
  }

  /**
   * Match score of contact and address to find the best matching pairs.
   */
  class MatchScore(val contactId: String, val addressId: Int, val score: Int) {
    var synced = false
  }

  @Autowired
  internal lateinit var sipgateContactService: SipgateContactService

  @Autowired
  internal lateinit var addressDao: AddressDao

  @PersistenceContext
  internal lateinit var em: EntityManager

  private var lastSyncInEpochMillis: Long? = null

  private fun postConstruct() {
    addressDao.register(this)
  }

  companion object {
    internal var countryPrefixForTestcases: String? = null

    /**
     * Create new contact from given address.
     */
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
      address.businessPhone?.let { numbers.add(SipgateNumber(it).setWorkType()) }
      address.mobilePhone?.let { numbers.add(SipgateNumber(it).setCellType()) }
      address.privatePhone?.let { numbers.add(SipgateNumber(it).setHomeType()) }
      address.privateMobilePhone?.let { numbers.add(SipgateNumber(it).setOtherType()) }
      address.fax?.let { numbers.add(SipgateNumber(it).setFaxWorkType()) }
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

    /**
     * Create new address from given contact.
     */
    internal fun from(contact: SipgateContact): AddressDO {
      val address = extractName(contact.name)
      copyFrom(address, contact)
      return address
    }

    /**
     * Copies all fields of the srcContact into the destAddress.
     * @param destAddress The address to modify.
     * @param srcContact The contact as source.
     */
    internal fun copyFrom(destAddress: AddressDO, srcContact: SipgateContact) {
      val address = extractName(srcContact.name)
      destAddress.name = address.name
      destAddress.firstName = address.firstName
      // contact.id
      // var picture: String? = null
      destAddress.email = srcContact.email
      destAddress.privateEmail = srcContact.privateEmail

      destAddress.businessPhone = srcContact.work
      destAddress.mobilePhone = srcContact.cell
      destAddress.privatePhone = srcContact.home
      destAddress.privateMobilePhone = srcContact.other
      destAddress.fax = srcContact.faxWork

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

      destAddress.organization = srcContact.organization
      destAddress.division = srcContact.division
    }

    /**
     * Detects modifications (if any) by comparing given contact and address. Any modification of the contact itself is
     * detected by comparing the hash codes of every field with the hash codes built at the last synchronisation: If
     * any modification was done remote, the hash code of the modified fields would differ?
     * @param contact given by Sipgate (will be modified, if to be modified).
     * @param address given by ProjectForge (will be modified, if to be modified).
     * @return result with info whether the objects have to be updated or not.
     */
    internal fun sync(
      contact: SipgateContact,
      address: AddressDO,
      syncInfo: SipgateContactSyncDO.SyncInfo?,
    ): SyncResult {
      val result = SyncResult()
      if (contact.name != SipgateContactSyncDO.getName(address)) {
        if (syncInfo != null && syncInfo.fieldsInfo["name"] != SipgateContactSyncDO.SyncInfo.hash(contact.name)) {
          // address to be updated
          val adr = extractName(contact.name)
          result.addOutdatedAddressField("name", SipgateContactSyncDO.getName(address), contact.name)
          address.name = adr.name
          address.firstName = adr.firstName
          result.addressDOOutdated = true
        } else {
          contact.name = SipgateContactSyncDO.getName(address)
          result.contactOutdated = true
        }
      }
      if (contact.organization != null && contact.division == null &&
        address.organization.isNullOrBlank() && !address.division.isNullOrBlank() &&
        contact.organization?.trim() == address.division?.trim()
      ) {
        // organization: 'PEV'->'null', division: 'null'->'PEV'
        contact.division = address.division
        contact.organization = null
      }
      sync(contact, SipgateContact::organization, address, AddressDO::organization, syncInfo, result)
      sync(contact, SipgateContact::division, address, AddressDO::division, syncInfo, result)

      sync(contact, SipgateContact::email, address, AddressDO::email, syncInfo, result)
      sync(contact, SipgateContact::privateEmail, address, AddressDO::privateEmail, syncInfo, result)

      sync(contact, SipgateContact::work, address, AddressDO::businessPhone, syncInfo, result, true)
      sync(contact, SipgateContact::home, address, AddressDO::privatePhone, syncInfo, result, true)
      sync(contact, SipgateContact::cell, address, AddressDO::mobilePhone, syncInfo, result, true)
      sync(contact, SipgateContact::other, address, AddressDO::privateMobilePhone, syncInfo, result, true)
      sync(contact, SipgateContact::faxWork, address, AddressDO::fax, syncInfo, result, true)
      return result
    }

    /**
     * Used by sync method above for checking one single field of contact and address.
     */
    internal fun sync(
      contact: SipgateContact,
      contactField: KMutableProperty<*>,
      address: AddressDO,
      addressField: KMutableProperty<*>,
      syncInfo: SipgateContactSyncDO.SyncInfo?,
      result: SyncResult,
      isPhoneNumber: Boolean = false,
    ) {
      val contactValue = contactField.getter.call(contact) as String?
      val addressValue = addressField.getter.call(address) as String?
      var contactValue2Compare = contactValue ?: ""
      var addressValue2Compare = addressValue ?: ""
      if (isPhoneNumber) {
        contactValue2Compare = NumberHelper.extractPhonenumber(contactValue) ?: ""
        addressValue2Compare = NumberHelper.extractPhonenumber(addressValue) ?: ""
      }
      if (contactValue2Compare != addressValue2Compare) {
        if (syncInfo != null
          && syncInfo.fieldsInfo[addressField.name] != SipgateContactSyncDO.SyncInfo.hash(contactValue)
        ) {
          result.addOutdatedAddressField(addressField.name, addressValue, contactValue)
          // remote contact was modified, so local address is outdated.
          addressField.setter.call(address, contactValue)
          result.addressDOOutdated = true
        } else {
          result.addOutdatedContactField(contactField.name, contactValue, addressValue)
          // local address was modified, so remote contact is outdated.
          contactField.setter.call(contact, addressValue)
          result.contactOutdated = true
        }
      }
    }

    /**
     * Sipgate provides only concatenated names (first name - lastname). This method tries to restore the first and
     * family name.
     */
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
     * Builds a list of all contact / adddress pairs with their match score (must be greater or equal 1). Already
     * paired (matched) contacts and addresses (member of syncDOList) will be ignored.
     * @param syncContext list of all addresses, all remote contacts and all already synced contacts (syncDOList)
     * @return List of all match scores greater or equal 1.
     */
    internal fun findMatches(syncContext: SyncContext)
        : List<MatchScore> {
      val addresses = syncContext.addressList
      val contacts = syncContext.remoteContacts
      val syncDOList = syncContext.syncDOList
      // Map key is contact-id.
      val matchScores = mutableListOf<MatchScore>()
      contacts.forEach { contact ->
        if (syncDOList.any { it.sipgateContactId == contact.id }) {
          // Contact is already matched.
          return@forEach
        }
        addresses.filter { SipgateContactSyncDO.getName(it).lowercase() == contact.name?.trim()?.lowercase() }
          .forEach { matchAddress ->
            if (syncDOList.none { it.address?.id == matchAddress.id }) {
              // Address isn't yet matched to any contact.
              contact.id?.let { contactId ->
                val matchScore = matchScore(contact, matchAddress)
                if (matchScore >= 1) {
                  matchScores.add(MatchScore(contactId, matchAddress.id, matchScore))
                }
              }
            }
          }
      }
      if (log.isDebugEnabled) {
        val sb = StringBuilder()
        val map = matchScores.groupBy { it.contactId }
        map.keys.sorted().forEach { contactId ->
          sb.appendLine("$contactId=[${map[contactId]?.sortedByDescending { it.score }?.joinToString { "${it.addressId}:${it.score}" }}]")
        }
        log.debug { sb.toString() }
      }
      return matchScores
    }

    /**
     * Calculates the match score of an address/contact pair.
     */
    internal fun matchScore(contact: SipgateContact, address: AddressDO): Int {
      if (contact.name?.trim()?.lowercase() != SipgateContactSyncDO.getName(address).lowercase()) {
        return -1
      }
      var counter = 1
      if (!address.isDeleted) {
        // Boost undeleted addresses. Deleted addresses with same name (family and first) with lower score.
        counter += 1
        if (isAddressActive(address)) {
          counter += 1
        }
      }
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
      if (address.division != null && contact.division?.trim()?.lowercase() == address.division?.trim()
          ?.lowercase()
      ) {
        ++counter
      }
      if (address.organization != null && contact.organization?.trim()?.lowercase() == address.organization?.trim()
          ?.lowercase()
      ) {
        ++counter
      }
      return counter
    }

    /**
     * Only active addresses will be pushed to Sipgate. In-active addresses will be deleted remote.
     */
    fun isAddressActive(address: AddressDO): Boolean {
      return !address.isDeleted &&
          address.contactStatus.isIn(ContactStatus.ACTIVE) &&
          address.addressStatus.isIn(AddressStatus.UPTODATE)
    }

    /**
     * Only valid remote contacts will be created locally. At least any number
     * and the organization and/or division must exist.
     */
    fun isContactValid(contact: SipgateContact): Boolean {
      return (!contact.organization.isNullOrBlank() || !contact.division.isNullOrBlank()) &&
          !contact.numbers.isNullOrEmpty()
    }

    /**
     * Sipgate will provide numbers in own format (differs from the number strings sent by ProjectForge). This method
     * is used to detect modifications and calculating the match score.
     */
    private fun extractNumber(number: String?): String? {
      number ?: return null
      if (countryPrefixForTestcases != null) {
        return NumberHelper.extractPhonenumber(number, countryPrefixForTestcases)
      }
      return NumberHelper.extractPhonenumber(number)
    }
  }

  /**
   * The main sync method: gets all remote contacts and local addresses, find the matching pairs (if not yet paired) and
   * inserts, updates and deletes the remote contacts and local addresses.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  open fun sync(): SyncContext {
    log.info { "Syncing local addresses and remote Sipgate contacts..." }
    synchronized(this) {
      val syncContext = SyncContext()
      syncContext.addressList =
        addressDao.internalLoadAll() // Need all for matching contacts, but only active will be used for syncing to Sipgate.
      updateSyncObjects(syncContext)

      // Handle doublets from Sipgate
      // Gelöschte Adressen in Sipgate?

      syncContext.localCounter.total = syncContext.addressList.size
      syncContext.addressList.forEach { address ->
        val syncDO = syncContext.syncDOList.find { it.address?.id == address.id }
        val contactId = syncDO?.sipgateContactId
        // log.debug { "sync: Processing address #${address.id}: syncObj=$syncDO" }
        if (contactId != null) {
          val contact = syncContext.remoteContacts.find { it.id == contactId }
          if (isAddressActive(address)) {
            // log.debug { "sync: address #${address.id} is active. Remote contact=$contact" }
            if (contact != null) {
              // Update if active
              val oldContact = contact.toString()
              val syncResult = sync(contact, address, syncDO.syncInfo)
              if (syncResult.contactOutdated || syncResult.addressDOOutdated) {
                log.info { "${getLogInfo(address, contact)}: Updating address and/or contact: $syncResult" }
              } else {
                // log.debug { "sync: ${getLogInfo(address, contact)} is up-to-date." }
              }
              if (syncResult.addressDOOutdated) {
                try {
                  log.info { "${getLogInfo(address, contact)}: Updating local address: $address" }
                  copyFrom(address, contact)
                  addressDao.internalUpdate(address)
                  syncContext.localCounter.updated++
                } catch (ex: Exception) {
                  log.error("${getLogInfo(address, contact)}: ${ex.message}", ex)
                  syncContext.localCounter.failed++
                }
              }
              if (syncResult.contactOutdated) {
                log.info { "${getLogInfo(address, contact)}: Updating remote contact: $contact, was: $oldContact" }
                updateRemoteContact(contact, syncDO, syncContext)
              }
            } else {
              // Create
              log.info { "${getLogInfo(address, contact)}: Creating remote contact (doesn't yet exist): $address" }
              createRemoteContact(address, syncContext)
            }
          } else if (contact != null) {
            // Delete if not active
            log.info {
              "${
                getLogInfo(
                  address,
                  contact
                )
              }: Delete remote contact (address is deleted or not active): $contact"
            }
            deleteRemoteContact(contact, syncDO, syncContext)
          }
        } else if (isAddressActive(address)) {
          // Create if active
          log.info { "${getLogInfo(address, null)}: Creating remote contact: ${from(address)}" }
          createRemoteContact(address, syncContext)
        } else {
          log.debug { "sync: address #${address.id} isn't active and no remote contact exists: Nothing to do." }
          // Ignore if not active
        }
      }
      log.debug { "sync: Processing all remote ${syncContext.remoteContacts.size} contacts..." }
      syncContext.remoteContacts.forEach { contact ->
        // log.debug { "sync: Processing remote contact: $contact" }
        syncContext.syncDOList.find { it.sipgateContactId == contact.id }.let { syncDO ->
          // log.debug { "sync: syncDO found: $syncDO" }
          val contactId = syncDO?.sipgateContactId
          if (contactId == null) {
            if (isContactValid(contact)) {
              try {
                // Remote contact seems to be a new contact.
                val address = from(contact)
                log.info { "${getLogInfo(address, contact)}: Creating address: $address" }
                addressDao.internalSave(address)
                syncContext.localCounter.inserted++
              } catch (ex: Exception) {
                log.error("${getLogInfo(null, contact)}: ${ex.message}", ex)
                syncContext.localCounter.failed++
              }
            } else {
              log.info {
                "${
                  getLogInfo(
                    null,
                    contact
                  )
                }; Ignoring contact (not enough information such as numbers, organization and/or division: $contact"
              }
              syncContext.remoteCounter.ignored++
            }
          }
        }
      }
      updateSyncObjects(syncContext)
      syncContext.remoteCounter.total = syncContext.remoteContacts.size
      // Delete remote contacts (without numbers)?
      lastSyncInEpochMillis = System.currentTimeMillis()
      log.info { "Syncing of local addresses and remote Sipgate contacts finished: $syncContext" }
      return syncContext
    }
  }

  private fun createRemoteContact(address: AddressDO, syncContext: SyncContext? = null) {
    try {
      val contact = from(address)
      sipgateContactService.create(contact)
      syncContext?.let {
        it.remoteCounter.inserted++
      }
    } catch (ex: Exception) {
      log.error("${getLogInfo(address, null)}: ${ex.message}", ex)
      syncContext?.let {
        it.remoteCounter.failed++
      }
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
        log.error("${getLogInfo(null, contact)}: ${ex.message}", ex)
        syncContext.remoteCounter.failed++
      }
    }
  }

  private fun deleteRemoteContact(contact: SipgateContact, syncDO: SipgateContactSyncDO, syncContext: SyncContext) {
    contact.id?.let { contactId ->
      try {
        sipgateContactService.delete(contactId, contact)
        delete(syncDO)
        syncContext.syncDOList.remove(syncDO)
        syncContext.remoteContacts.remove(contact)
        syncContext.remoteCounter.deleted++
      } catch (ex: Exception) {
        log.error("${getLogInfo(null, contact)}: ${ex.message}", ex)
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
      require(dbObj.sipgateContactId == entry.sipgateContactId)
      require(dbObj.address?.id == entry.address?.id)
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  open fun delete(entry: SipgateContactSyncDO) {
    log.info { "Deleting syncObj: $entry" }
    em.remove(entry)
    em.flush()
  }

  /**
   * Proceeds all given remoteContacts and tries to ensure, that a match is persisted (via SipgateContactSyncDO)
   */
  private fun updateSyncObjects(syncContext: SyncContext) {
    // Get remote contacts and local addresses
    syncContext.remoteContacts =
      sipgateContactService.getList().filter { it.scope == SipgateContact.Scope.SHARED }.toMutableList()
    log.info { "Trying to match ${syncContext.remoteContacts.size} remote contacts." }
    // Load already matched contacts
    syncContext.syncDOList = loadAll().toMutableList()
    log.debug { "updateSyncObjects: synContext=$syncContext" }
    // Find deleted remote contacts and deleted addresses for removing them from the syncDOList (for rematching):
    var deleted = false
    syncContext.syncDOList.forEach { syncDO ->
      log.debug { "updateSyncObjects: syncDO=$syncDO" }
      var deleteIt = false
      if (syncContext.remoteContacts.none { it.id == syncDO.sipgateContactId }) {
        log.info { "Deleting syncDO (because contact id '${syncDO.sipgateContactId}' doesn't exist anymore." }
        deleteIt = true
      }
      if (!deleteIt && syncContext.addressList.none { it.pk == syncDO.address?.id }) {
        log.info { "Deleting syncDO (because address id '${syncDO.address?.id}' doesn't exist anymore." }
        deleteIt = true
      }
      if (deleteIt) {
        delete(syncDO)
        deleted = true
      }
    }
    if (deleted) {
      // Reload list:
      val listSize = syncContext.syncDOList.size
      syncContext.syncDOList = loadAll().toMutableList()
      log.debug { "updateSyncObjects: sync objects reloaded (some were deleted): $listSize sync objects before and now ${syncContext.syncDOList.size}" }
    } else {
      log.debug { "updateSyncObjects: no sync objects were. Reload of the sync objects not needed." }
    }
    // syncContext. addressList =
    //  addressDao.internalLoadAll() // Need all for matching contacts, but only active will be used for syncing to Sipgate.
    val matchScores = findMatches(syncContext).sortedByDescending { it.score }
    matchScores.forEach { matchScore ->
      if (matchScore.synced) {
        return@forEach
      }
      if (syncContext.syncDOList.any { it.sipgateContactId == matchScore.contactId || it.address?.id == matchScore.addressId }) {
        // contact or address is already synced: don't try it anymore:
        matchScore.synced = true
        return@forEach
      }
      val contact = syncContext.remoteContacts.find { it.id == matchScore.contactId }
      if (contact == null) {
        log.error { "oups, shouldn't occur. Can't find contact '${matchScore.contactId}' in contacts." }
      }
      val address = syncContext.addressList.find { it.id == matchScore.addressId }
      if (address == null) {
        log.error { "oups, shouldn't occur. Can't find address #${matchScore.addressId} in addresses." }
      }
      if (address != null && contact != null) {
        val syncDO = SipgateContactSyncDO.create(contact, address, SipgateContactSyncDO.RemoteStatus.OK)
        syncDO.updateJson(contact)
        syncContext.syncDOList.add(syncDO)
        upsert(syncDO)
      }
      matchScores.filter { it.contactId == matchScore.contactId || it.addressId == matchScore.addressId }
        .forEach { it.synced = true }
    }
    val nomatch =
      syncContext.remoteContacts.count { contact -> syncContext.syncDOList.none { it.sipgateContactId == contact.id } }
    log.info { "updateSyncObjects: ${syncContext.remoteContacts.size} remote contacts processed. $nomatch remote contacts without local matched address." }
  }

  private fun findByContactOrAddressId(sipgateContactId: String?, addressId: Int): SipgateContactSyncDO? {
    val list = if (sipgateContactId != null) {
      em.createNamedQuery(SipgateContactSyncDO.FIND_BY_CONTACT_AND_ADDRESS_ID, SipgateContactSyncDO::class.java)
        .setParameter("sipgateContactId", sipgateContactId)
        .setParameter("addressId", addressId)
        .resultList
    } else {
      em.createNamedQuery(SipgateContactSyncDO.FIND_BY_ADDRESS_ID, SipgateContactSyncDO::class.java)
        .setParameter("addressId", addressId)
        .resultList
    }
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

  private fun getLogInfo(address: AddressDO?, contact: SipgateContact?): String {
    if (address == null) {
      return "contact '${contact?.name}' (id=${contact?.id})"
    }
    if (contact == null) {
      return "address '${SipgateContactSyncDO.getName(address)}' (id=${address.id})"
    }
    return "address '${SipgateContactSyncDO.getName(address)}' (id=${address.id}, contact-id=${contact.id})"
  }

  override fun afterSaveOrModify(changedObject: AddressDO, operationType: OperationType) {
    sync()
  }
}
