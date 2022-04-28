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

package org.projectforge.security.dto

import com.webauthn4j.util.Base64UrlUtil


class WebAuthnCredential(
  /** The credential identifier. */
  var id: String? = null,
  var rawId: String? = null,
  var userHandle: String? = null,
  var publicKeyCose: String? = null,
  var signatureCount: Long? = null,
) {
  var response: WebAuthnResponse? = null
  var type: String? = "public-key"

  companion object {
    fun create(id: ByteArray): WebAuthnCredential {
      val idString = Base64UrlUtil.encodeToString(id)
      return WebAuthnCredential(idString, idString)
    }
  }
}
