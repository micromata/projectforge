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

import com.webauthn4j.authenticator.Authenticator
import com.webauthn4j.authenticator.AuthenticatorImpl
import com.webauthn4j.converter.AttestedCredentialDataConverter
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData
import com.webauthn4j.data.attestation.statement.AttestationStatement
import com.webauthn4j.util.Base64UrlUtil

class WebAuthnEntry(
  var credentialId: ByteArray,
  var displayName: String? = null,
) {
  private var serializedAttestedCredentialData: String? = null
  private var serializedAttestationStatement: String? = null
  var signCount: Long? = null

  var attestationStatement: AttestationStatement?
    set(value) {
      serializedAttestationStatement = if (value != null) {
        val envelope = WebAuthnStorage.AttestationStatementEnvelope(value);
        val byteArray = objectConverter.cborConverter.writeValueAsBytes(envelope)
        asString(byteArray)
      } else {
        null
      }
    }
    get() {
      serializedAttestationStatement ?: return null
      val envelope = cborConverter.readValue(
        asByteArray(serializedAttestationStatement),
        WebAuthnStorage.AttestationStatementEnvelope::class.java
      )
      return envelope?.attestationStatement
    }

  var attestedCredentialData: AttestedCredentialData?
    set(value) {
      serializedAttestedCredentialData = if (value != null) {
        asString(attestedCredentialDataConverter.convert(value))
      } else {
        null
      }
    }
    get() {
      serializedAttestedCredentialData ?: return null
      return attestedCredentialDataConverter.convert(
        asByteArray(
          serializedAttestedCredentialData
        )
      )
    }

  val authenticator: Authenticator?
    get() {
      val data = attestedCredentialData ?: return null
      val count = signCount ?: 1
      return AuthenticatorImpl(
        // You may create your own Authenticator implementation to save friendly authenticator name
        data,
        attestationStatement,
        count,
      )
    }


  companion object {
    fun create(
      credentialId: ByteArray,
      attestedCredentialData: AttestedCredentialData,
      attestationStatement: AttestationStatement,
      signCount: Long,
      displayName: String? = null
    ): WebAuthnEntry {
      val entry = WebAuthnEntry(credentialId, displayName = displayName)
      entry.attestedCredentialData = attestedCredentialData
      entry.attestationStatement = attestationStatement
      entry.signCount = signCount
      return entry
    }

    fun create(credentialId: ByteArray, authenticator: Authenticator, displayName: String? = null): WebAuthnEntry {
      val entry = WebAuthnEntry(credentialId, displayName = displayName)
      entry.attestedCredentialData = authenticator.attestedCredentialData
      entry.attestationStatement = authenticator.attestationStatement
      entry.signCount = authenticator.counter
      return entry
    }

    internal fun asString(byteArray: ByteArray?): String? {
      byteArray ?: return null
      return Base64UrlUtil.encodeToString(byteArray)
    }

    internal fun asByteArray(str: String?): ByteArray? {
      str ?: return null
      return Base64UrlUtil.decode(str)
    }

    private val objectConverter = ObjectConverter()

    private val attestedCredentialDataConverter = AttestedCredentialDataConverter(objectConverter)

    private val cborConverter = objectConverter.cborConverter
  }
}
