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

import org.projectforge.business.address.AddressDO
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
    var lastSync: Date? = null
    var fieldsInfo = mutableMapOf<String, Int?>()
    fun create(address: AddressDO) {
      fieldsInfo["name"] = SipgateContactSyncService.getName(address).hashCode()
      // family, given
      // fieldsInfo["picture"] = address
      fieldsInfo["organization"] = address.organization?.hashCode()
      fieldsInfo["division"] = address.division?.hashCode()
      fieldsInfo["email"] = address.email?.hashCode()
      fieldsInfo["businessPhone"] = address.businessPhone?.hashCode()
      fieldsInfo["mobilePhone"] = address.mobilePhone?.hashCode()
      fieldsInfo["privatePhone"] = address.privatePhone?.hashCode()
      fieldsInfo["privateMobilePhone"] = address.privateMobilePhone?.hashCode()
      fieldsInfo["addressText"] = address.addressText?.hashCode()
      fieldsInfo["addressText2"] = address.addressText2?.hashCode()
      fieldsInfo["zipCode"] = address.zipCode?.hashCode()
      fieldsInfo["city"] = address.city?.hashCode()
      fieldsInfo["state"] = address.state?.hashCode()
      fieldsInfo["country"] = address.country?.hashCode()
      fieldsInfo["privateAddressText"] = address.privateAddressText?.hashCode()
      fieldsInfo["privateAddressText2"] = address.privateAddressText2?.hashCode()
      fieldsInfo["privateZipCode"] = address.privateZipCode?.hashCode()
      fieldsInfo["privateCity"] = address.privateCity?.hashCode()
      fieldsInfo["privateState"] = address.privateState?.hashCode()
      fieldsInfo["privateCountry"] = address.privateCountry?.hashCode()
      fieldsInfo["organization"] = address.organization?.hashCode()
      fieldsInfo["division"] = address.division?.hashCode()
    }
  }

  //@Id
  //@Column(name = "contact_id")
  var contactId: String? = null

  var lastSync: Date? = null
}
