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

package org.projectforge.rest

import org.projectforge.framework.time.PFDateTime
import org.projectforge.security.My2FAData
import org.projectforge.security.webauthn.WebAuthnSupport
import java.util.*

/**
 * @param webAuthnSupport If given, the WebAuthn entries of the user will be load from the db.
 */
class My2FASetupData(webAuthnSupport: WebAuthnSupport? = null) : My2FAData() {
  class WebAuthnEntry(
    val created: Date?,
    val lastUpdate: Date?,
    val displayName: String?,
    val signCount: Long?,
  )

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

  init {
    if (webAuthnSupport != null) {
      webAuthnEntries = webAuthnSupport.allLoggedInUserCredentials.map {
        WebAuthnEntry(
          it.created,
          it.lastUpdate,
          it.displayName,
          it.signCount,
        )
      }.toMutableList()
    }
  }
}
