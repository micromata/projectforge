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

import org.projectforge.framework.json.JsonUtils

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class SipgateDevice(
  var id: String? = null,
  var alias: String? = null,
  var type: String? = null,
  var online: Boolean? = null,
  /** If true, then not available. */
  var dnd: Boolean? = null,
) {
  data class ActiveRouting(var id: String? = null, var alias: String? = null) {
    override fun toString(): String {
      return "id='$id', alias='$alias'"
    }
  }

  class Credentials(var simId: String? = null, var puk1: String? = null, var puk2: String? = null) {
    override fun toString(): String {
      if (simId.isNullOrBlank() && puk1.isNullOrBlank() && puk2.isNullOrBlank()) {
        return ""
      }
      val sb = StringBuilder()
      if (simId != null) {
        sb.append("simId=[$simId]")
      }
      if (!puk1.isNullOrBlank()) {
        sb.append(", puk1=[***]")
      }
      if (!puk2.isNullOrBlank()) {
        sb.append(", puk2=[***]")
      }
      return sb.toString()
    }
  }

  var activePhonelines: List<ActiveRouting>? = null
  var activeGroups: List<ActiveRouting>? = null
  var credentials: Credentials? = null
  var simState: String? = null
  var esim: Boolean? = null

  fun deleteSecrets() {
    credentials?.let { cred ->
      if (cred.puk1 != null) {
        cred.puk1 = "***"
      }
      if (cred.puk2 != null) {
        cred.puk2 = "***"
      }
    }
  }

  override fun toString(): String {
    return JsonUtils.toJson(this)
  }
}
