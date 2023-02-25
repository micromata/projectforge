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
import org.projectforge.framework.json.JsonUtils

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class SipgateUser(
  var id: String? = null,
  var firstname: String? = null,
  var lastname: String? = null,
  var email: String? = null,
  var defaultDevice: String? = null,
  var busyOnBusy: Boolean? = null,
  var addressId: String? = null,
  var timezone: String? = null,
  var admin: Boolean? = null,
) {
  /**
   * The dial id's are the SipgateNumber id's of the user.
   */
  var directDialIds: List<String>? = null

  val fullname: String
    @JsonIgnore
    get() = "${firstname ?: ""} ${lastname ?: ""}"

  override fun toString(): String {
    return JsonUtils.toJson(this, true)
  }
}
