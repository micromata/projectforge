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

import org.projectforge.business.address.AddressDO
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.business.sipgate.SipgateContactSyncDO
import org.projectforge.framework.utils.NumberHelper
import org.springframework.stereotype.Service
import kotlin.reflect.KMutableProperty

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
// Must be open for mocking in tests.
@Service
open class SipgateContactService :
  AbstractSipgateEntityService<SipgateContact>("/contacts", "Contact", ContactListData::class.java) {

  override fun setId(obj: SipgateContact, id: String) {
    obj.id = id
  }

  override fun getLogInfo(entity: SipgateContact): String {
    return "contact '${entity.name}' (id=${entity.id})"
  }

  companion object {
    /**
     * Sipgate sends numbers such as fax-home etc. as type other (annoying).
     * This method tries to re-assign numbers from type other by comparing the numbers with
     * the optional given address and/or given syncDO.
     */
    fun fixNumbers(
      contact: SipgateContact,
      address: AddressDO? = null,
      syncInfo: SipgateContactSyncDO.SyncInfo? = null
    ) {
      contact.numbers?.filter { it.isOtherType() }?.forEach { otherNumber ->
        val number = NumberHelper.extractPhonenumber(otherNumber.number)
        if (number.isNullOrBlank()) {
          return@forEach
        }
        if (checkNumber(syncInfo, AddressDO::privatePhone, number, address?.privatePhone)) {
          otherNumber.setHomeType()
          return@forEach
        } else if (checkNumber(syncInfo, AddressDO::mobilePhone, number, address?.mobilePhone)) {
          otherNumber.setCellType()
          return@forEach
        } else if (checkNumber(syncInfo, AddressDO::businessPhone, number, address?.businessPhone)) {
          otherNumber.setWorkType()
          return@forEach
        } else if (checkNumber(syncInfo, AddressDO::fax, number, address?.fax)) {
          otherNumber.setFaxWorkType()
          return@forEach
        } else if (checkNumber(syncInfo, AddressDO::privateMobilePhone, number, address?.privateMobilePhone)) {
          otherNumber.setCellHomeType()
          return@forEach
        }
      }
    }

    private fun checkNumber(
      syncInfo: SipgateContactSyncDO.SyncInfo?,
      field: KMutableProperty<*>,
      number: String?,
      addressNumber: String?
    ): Boolean {
      return number == NumberHelper.extractPhonenumber(addressNumber) ||
          syncInfo != null && syncInfo.fieldsInfo[field.name] == SipgateContactSyncDO.SyncInfo.hash(number)
    }
  }
}
