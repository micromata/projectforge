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

package org.projectforge.business.sipgate

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.business.address.AddressDO
import org.projectforge.framework.json.JsonUtils
import java.util.*
import javax.persistence.*

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(
  name = "T_SIPGATE_CONTACT_SYNC",
  uniqueConstraints = [
    UniqueConstraint(name = "unique_t_sipgate_contact_id", columnNames = ["sipgate_contact_id"]),
    UniqueConstraint(name = "unique_t_sipgate_address_id", columnNames = ["address_id"]),
  ],
  indexes = [
    javax.persistence.Index(name = "idx_fk_t_sipgate_contact_id", columnList = "sipgate_contact_id"),
    javax.persistence.Index(name = "idx_fk_t_sipgate_address_id", columnList = "address_id"),
  ]
)
@NamedQueries(
  NamedQuery(
    name = SipgateContactSyncDO.FIND_BY_ADDRESS_ID,
    query = "from SipgateContactSyncDO where address_id = :addressId"
  ),
  NamedQuery(
    name = SipgateContactSyncDO.FIND_BY_CONTACT_AND_ADDRESS_ID,
    query = "from SipgateContactSyncDO where sipgate_contact_id = :sipgateContactId or address_id = :addressId"
  ),
  NamedQuery(
    name = SipgateContactSyncDO.LOAD_ALL,
    query = "from SipgateContactSyncDO"
  ),
)
open class SipgateContactSyncDO {
  class SyncInfo {
    var fieldsInfo = mutableMapOf<String, Int?>()

    fun setFieldsInfo(field: String, value: String?) {
      fieldsInfo[field] = hash(value)
    }

    companion object {
      internal fun create(contact: SipgateContact): SyncInfo {
        val info = SyncInfo()
        info.setFieldsInfo("name", contact.name)
        info.setFieldsInfo(AddressDO::organization.name, contact.organization)
        info.setFieldsInfo(AddressDO::division.name, contact.division)
        info.setFieldsInfo(AddressDO::email.name, contact.email)
        info.setFieldsInfo(AddressDO::businessPhone.name, contact.work)
        info.setFieldsInfo(AddressDO::mobilePhone.name, contact.cell)
        info.setFieldsInfo(AddressDO::privatePhone.name, contact.home)
        info.setFieldsInfo(AddressDO::privateMobilePhone.name, contact.other)
        return info
      }

      internal fun create(address: AddressDO): SyncInfo {
        val info = SyncInfo()
        info.setFieldsInfo("name", getName(address))
        // family, given
        // fieldsInfo["picture"] = address
        info.setFieldsInfo(AddressDO::organization.name, address.organization)
        info.setFieldsInfo(AddressDO::division.name, address.division)
        info.setFieldsInfo(AddressDO::email.name, address.email)
        info.setFieldsInfo(AddressDO::businessPhone.name, address.businessPhone)
        info.setFieldsInfo(AddressDO::mobilePhone.name, address.mobilePhone)
        info.setFieldsInfo(AddressDO::privatePhone.name, address.privatePhone)
        info.setFieldsInfo(AddressDO::privateMobilePhone.name, address.privateMobilePhone)
        /* Ignore addresses (synchronize will be pain, because not type of addresses will be given by Sipgate.
        fieldsInfo[AddressDO::addressText.name] = hash(address.addressText)
        fieldsInfo[AddressDO::addressText2.name] = hash(address.addressText2)
        fieldsInfo[AddressDO::zipCode.name] = hash(address.zipCode)
        fieldsInfo[AddressDO::city.name] = hash(address.city)
        fieldsInfo[AddressDO::state.name] = hash(address.state)
        fieldsInfo[AddressDO::country.name] = hash(address.country)
        fieldsInfo["privateAddressText"] = address.privateAddressText?.hashCode()
        fieldsInfo["privateAddressText2"] = address.privateAddressText2?.hashCode()
        fieldsInfo["privateZipCode"] = address.privateZipCode?.hashCode()
        fieldsInfo["privateCity"] = address.privateCity?.hashCode()
        fieldsInfo["privateState"] = address.privateState?.hashCode()
        fieldsInfo["privateCountry"] = address.privateCountry?.hashCode()
        fieldsInfo["organization"] = address.organization?.hashCode()
        fieldsInfo["division"] = address.division?.hashCode()*/
        return info
      }

      fun hash(value: String?): Int {
        // Use 0 instead of null for null strings, for same hash code for null and "" values.
        return value?.trim()?.hashCode() ?: 0
      }
    }
  }

  enum class RemoteStatus {
    /** Remote contact and local address are connected. */
    OK,

    /** Remote contact is created by ProjectForge but contact-id generated by Sipgate not yet known. */
    CREATED_BY_LOCAL,

    /**
     * Remote contact received is unknown by ProjectForge.
     */
    CREATED_BY_REMOTE,

    /**
     * Deleted by local system.
     */
    DELETED_BY_LOCAL,
  }


  /**
   * Id of the remote contact of Sipgate.
   */
  @get:Id
  @get:Column(name = "sipgate_contact_id")
  open var sipgateContactId: String? = null

  /**
   * Id of the local address connected to this remote contact of Sipgate.
   */
  @get:ManyToOne(cascade = [CascadeType.MERGE])
  @get:JoinColumn(name = "address_id")
  open var address: AddressDO? = null

  @get:Column(name = "last_sync")
  open var lastSync: Date? = null

  @get:Enumerated(EnumType.STRING)
  @get:Column(length = 20, nullable = false, name = "remote_status")
  open var remoteStatus: RemoteStatus? = null

  @get:Transient
  @get:JsonIgnore
  open var syncInfo: SyncInfo? = null

  @get:Column(name = "sync_info", length = 10000)
  open var syncInfoAsJson: String? = null

  fun readJson() {
    if (syncInfoAsJson == null) {
      syncInfo = null
    } else {
      try {
        syncInfo = JsonUtils.fromJson(syncInfoAsJson, SyncInfo::class.java)
      } catch (ex: Exception) {
        // Do nothing (version incompability).
        syncInfo = null
      }
    }
  }

  fun updateJson() {
    if (syncInfo == null) {
      syncInfoAsJson = null
    } else {
      syncInfoAsJson = JsonUtils.toJson(syncInfo)
    }
  }

  fun updateJson(contact: SipgateContact?) {
    syncInfo = if (contact != null) {
      SyncInfo.Companion.create(contact)
    } else {
      null
    }
    updateJson()
  }

  override fun toString(): String {
    return JsonUtils.toJson(this, true)
  }

  companion object {
    fun create(
      contact: SipgateContact?,
      address: AddressDO,
      remoteStatus: RemoteStatus = RemoteStatus.OK
    ): SipgateContactSyncDO {
      val syncObject = SipgateContactSyncDO()
      syncObject.sipgateContactId = contact?.id
      syncObject.address = address
      syncObject.lastSync = Date()
      syncObject.remoteStatus = remoteStatus
      return syncObject
    }

    fun getName(address: AddressDO): String {
      val sb = StringBuilder()
      /*address.title?.let {
      sb.append(it.trim()).append(" ")
    }*/
      address.firstName?.let {
        sb.append(it.trim()).append(" ")
      }
      address.name?.let {
        sb.append(it.trim()).append(" ")
      }
      return sb.toString().trim()
    }

    const val FIND_BY_CONTACT_AND_ADDRESS_ID = "SipgateContactSyncDO_findByContactAndAddressId"
    const val FIND_BY_ADDRESS_ID = "SipgateContactSyncDO_findByAddressId"
    const val LOAD_ALL = "SipgateContactSyncDO_loadAll"
  }
}
