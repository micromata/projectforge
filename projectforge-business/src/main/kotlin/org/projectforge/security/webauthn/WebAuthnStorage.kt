/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.bouncycastle.asn1.x500.style.RFC4519Style.owner
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * Stores authenticators of the user's.
 */
@Service
class WebAuthnStorage {
  @Autowired
  private lateinit var webAuthnEntryDao: WebAuthnEntryDao

  fun store(entry: WebAuthnEntryDO) {
    entry.owner = ThreadLocalUserContext.user
    webAuthnEntryDao.upsert(entry)
  }

  fun load(credentialId: ByteArray): WebAuthnEntryDO? {
    return load(WebAuthnEntryDO.asString(credentialId)!!)
  }

  fun load(credentialId: String): WebAuthnEntryDO? {
    val ownerId = ThreadLocalUserContext.user!!.id!!
    return webAuthnEntryDao.getEntry(ownerId, credentialId)
  }

  fun loadAll(ownerId: Int? = null): List<WebAuthnEntryDO> {
    return webAuthnEntryDao.getEntries(ownerId ?: ThreadLocalUserContext.userId)
  }

  fun updateCounter(credentialId: ByteArray, signCount: Long) {
    updateCounter(WebAuthnEntryDO.asString(credentialId)!!, signCount)
  }

  fun updateCounter(credentialId: String, signCount: Long) {
    val ownerId = ThreadLocalUserContext.user!!.id!!
    val entry = webAuthnEntryDao.getEntry(ownerId, credentialId)
    requireNotNull(entry) { "Can't update signCount for webauthn entry, because the entry for the owner with credential-id '$credentialId' doesn't exist." }
    entry.signCount = signCount
    webAuthnEntryDao.upsert(entry)
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
