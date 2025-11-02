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

    /**
     * Sipgate sometimes sends emails without type field.
     * This method tries to assign email types by comparing the email addresses with
     * the corresponding fields in the local address.
     *
     * Example: If Sipgate returns {"email":"test@example.com"} without type,
     * and address.email == "test@example.com", then we set type = WORK.
     * This prevents the sync logic from detecting false changes.
     */
    fun fixEmails(
      contact: SipgateContact,
      address: AddressDO? = null,
      syncInfo: SipgateContactSyncDO.SyncInfo? = null
    ) {
      contact.emails?.filter { it.type == null }?.forEach { emailWithoutType ->
        val email = emailWithoutType.email?.trim()?.lowercase()
        if (email.isNullOrBlank()) {
          return@forEach
        }
        if (checkEmail(syncInfo, AddressDO::email, email, address?.email)) {
          emailWithoutType.type = SipgateContact.EmailType.WORK
          return@forEach
        } else if (checkEmail(syncInfo, AddressDO::privateEmail, email, address?.privateEmail)) {
          emailWithoutType.type = SipgateContact.EmailType.HOME
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

    /**
     * Checks if an email address matches a specific field in the local address.
     *
     * Returns true if:
     * 1. The email matches the corresponding address field (e.g., email matches address.email), OR
     * 2. The syncInfo indicates this email was previously associated with this field
     *
     * This helps identify which type (WORK/HOME) should be assigned to emails without type.
     *
     * @param syncInfo The sync information containing field hashes from the last sync
     * @param field The address field to check (e.g., AddressDO::email or AddressDO::privateEmail)
     * @param email The email address from the contact (normalized: trimmed and lowercase)
     * @param addressEmail The email address from the local address
     * @return true if the email matches this field, false otherwise
     */
    private fun checkEmail(
      syncInfo: SipgateContactSyncDO.SyncInfo?,
      field: KMutableProperty<*>,
      email: String?,
      addressEmail: String?
    ): Boolean {
      return email == addressEmail?.trim()?.lowercase() ||
          syncInfo != null && syncInfo.fieldsInfo[field.name] == SipgateContactSyncDO.SyncInfo.hash(email)
    }
  }
}
