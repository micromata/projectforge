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

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.Constants
import org.projectforge.business.sipgate.*
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class SipgateDataStorage {
  val lastSyncInEpochMillis = System.currentTimeMillis()

  var userDevices: List<SipgateUserDevices>? = null
    internal set
  var users: List<SipgateUser>? = null
    internal set
  var numbers: List<SipgateNumber>? = null
    internal set
  var addresses: List<SipgateAddress>? = null
    internal set

  val uptodate: Boolean
    @JsonIgnore
    get() = System.currentTimeMillis() - lastSyncInEpochMillis < Constants.MILLIS_PER_DAY

  init {
    lastSyncInEpochMillis
  }

  fun getUsersByDialId(dialId: String?): List<SipgateUser> {
    dialId ?: return emptyList()
    return users?.filter { it.directDialIds?.contains(dialId) == true } ?: emptyList()
  }

  fun getUserDevices(user: PFUserDO): SipgateUserDevices? {
    val sipgateUserId = getUser(user)?.id ?: return null
    return userDevices?.firstOrNull { it.userId == sipgateUserId }
  }

  fun getUser(user: PFUserDO): SipgateUser? {
    val email = user.email?.trim { it <= ' ' }
    val firstname = user.firstname?.trim { it <= ' ' }
    val lastname = user.lastname?.trim { it <= ' ' }
    // Get matching user by firstname and lastname or by e-mail:
    return users?.firstOrNull {
      (it.firstname?.trim()?.equals(firstname) == true && it.lastname?.trim()?.equals(lastname) == true) ||
          it.email?.trim()?.equals(email, ignoreCase = true) == true
    }
  }

  fun getActivePhoneLines(number: SipgateNumber): Set<SipgateDevice.ActiveRouting> {
    val set = mutableSetOf<SipgateDevice.ActiveRouting>()
    userDevices?.forEach { entry ->
      entry.devices?.forEach { device ->
        device.activePhonelines?.forEach { activeRouting ->
          if (activeRouting.id == number.endpointId) {
            val alias = activeRouting.alias
            if (!alias.isNullOrBlank()) {
              set.add(activeRouting)
            }
          }
        }
      }
    }
    return set
  }

  override fun toString(): String {
    return JsonUtils.toJson(this)
  }
}
