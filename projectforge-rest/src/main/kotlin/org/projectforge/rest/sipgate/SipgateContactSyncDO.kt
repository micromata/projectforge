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

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.business.address.AddressDO
import org.projectforge.framework.json.JsonUtils
import java.util.*

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
/*@Entity
@Indexed
@Table(
  name = "T_SIPGATE_CONTACT_SYNC",
  uniqueConstraints = [UniqueConstraint(name = "unique_t_sipgate_contact_id", columnNames = ["contact_id"])],
  indexes = [javax.persistence.Index(name = "idx_fk_t_sipgate_contact_id", columnList = "contact_id")]
)*/
class SipgateContactSyncDO {
  class SyncInfo {
    var fieldsInfo = mutableMapOf<String, Int?>()
    fun create(address: AddressDO) {
      setFieldsInfo("name", SipgateContactSyncService.getName(address))
      // family, given
      // fieldsInfo["picture"] = address
      setFieldsInfo(AddressDO::organization.name, address.organization)
      setFieldsInfo(AddressDO::division.name, address.division)
      setFieldsInfo(AddressDO::email.name, address.email)
      setFieldsInfo(AddressDO::businessPhone.name, address.businessPhone)
      setFieldsInfo(AddressDO::mobilePhone.name, address.mobilePhone)
      setFieldsInfo(AddressDO::privatePhone.name, address.privatePhone)
      setFieldsInfo(AddressDO::privateMobilePhone.name, address.privateMobilePhone)
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
    }

    internal fun setFieldsInfo(field: String, value: String?) {
      fieldsInfo[field] = hash(value)
    }

    companion object {
      fun hash(value: String?): Int? {
        return value?.trim()?.hashCode()
      }
    }
  }

  //@Id
  //@Column(name = "contact_id")
  var contactId: String? = null

  //@Column(name = "last_sync")
  var lastSync: Date? = null

  @get:JsonIgnore
  var syncInfo: SyncInfo? = null

  //@Column(name = "sync_info")
  var syncInfoAsJson: String? = null

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
      syncInfoAsJson = JsonUtils.toJson(syncInfoAsJson)
    }
  }
}
