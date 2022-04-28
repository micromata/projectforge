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

package org.projectforge.security.webauthn

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.webauthn4j.data.attestation.statement.AttestationStatement
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.stereotype.Service


/**
 * Stores authenticators of the user's.
 */
@Service
class WebAuthnStorage {
  private val testStorage = mutableMapOf<Int, WebAuthnEntry>()

  fun store(entry: WebAuthnEntry) {
    testStorage[ThreadLocalUserContext.getUserId()] = entry
  }

  fun load(credentialId: ByteArray): WebAuthnEntry? {
    return testStorage[ThreadLocalUserContext.getUserId()]
  }

  fun loadAll(): Array<WebAuthnEntry> {
    val entry = testStorage[ThreadLocalUserContext.getUserId()] ?: return emptyArray()
    return arrayOf(entry)
  }

  fun updateCounter(credentialId: ByteArray, signCount: Long) {
    testStorage[ThreadLocalUserContext.getUserId()]?.signCount = signCount
  }

  internal class AttestationStatementEnvelope @JsonCreator constructor(
    @field:JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "fmt"
    ) @field:JsonProperty("attStmt") @param:JsonProperty("attStmt") val attestationStatement: AttestationStatement
  ) {

    @get:JsonProperty("fmt")
    val format: String
      get() = attestationStatement.format
  }
}
