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
import org.projectforge.Constants
import org.projectforge.business.sipgate.SipgateAddress
import org.projectforge.business.sipgate.SipgateNumber
import org.projectforge.business.sipgate.SipgateUser
import org.projectforge.business.sipgate.SipgateUserDevices
import org.projectforge.framework.json.JsonUtils

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

  override fun toString(): String {
    return JsonUtils.toJson(this)
  }
}
