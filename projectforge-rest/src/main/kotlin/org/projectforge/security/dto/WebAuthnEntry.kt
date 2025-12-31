/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.security.dto

import org.projectforge.security.webauthn.WebAuthnEntryDO
import java.util.*

open class WebAuthnEntry(
  var id: Long? = null,
  var created: Date? = null,
  var lastUpdate: Date? = null,
  var displayName: String? = null,
  var signCount: Long? = null,
) {
  companion object {
    fun create(entryDO: WebAuthnEntryDO): WebAuthnEntry {
      val result = WebAuthnEntry()
      result.id = entryDO.id
      result.created = entryDO.created
      result.lastUpdate = entryDO.lastUpdate
      result.displayName = entryDO.displayName
      result.signCount = entryDO.signCount
      return result
    }
  }
}
