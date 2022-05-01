/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.my2fa

import org.projectforge.framework.time.PFDateTime
import org.projectforge.security.My2FAData
import org.projectforge.security.dto.WebAuthnEntry
import org.projectforge.security.dto.WebAuthnFinishRequest
import org.projectforge.security.webauthn.WebAuthnSupport
import java.util.*

open class My2FASetupData() : My2FAData() {
  var mobilePhone: String? = null

  var authenticatorKey: String? = null

  var authenticatorKeyCreated: String? = null

  var showAuthenticatorKey: Boolean = false

  var webAuthnEntries: MutableList<WebAuthnEntry>? = null

  fun setDate(date: Date?) {
    authenticatorKeyCreated = if (date != null) {
      PFDateTime.from(date).format(withTimeLeftOrAgo = true)
    } else {
      ""
    }
  }

  companion object {
    /**
     * @param webAuthnSupport If given, the WebAuthn entries of the user will be load from the db.
     */
    fun create(webAuthnSupport: WebAuthnSupport): My2FASetupData {
      val webAuthnEntry = My2FASetupData()
      webAuthnEntry.webAuthnEntries = webAuthnSupport.allLoggedInUserCredentials.map {
        WebAuthnEntry.create(it)
      }.toMutableList()
      return webAuthnEntry
    }
  }
}
