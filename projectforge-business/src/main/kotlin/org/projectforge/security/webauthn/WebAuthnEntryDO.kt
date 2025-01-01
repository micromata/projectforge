/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.webauthn4j.authenticator.Authenticator
import com.webauthn4j.authenticator.AuthenticatorImpl
import com.webauthn4j.converter.AttestedCredentialDataConverter
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData
import com.webauthn4j.data.attestation.statement.AttestationStatement
import com.webauthn4j.util.Base64UrlUtil
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*
import jakarta.persistence.*
import org.projectforge.framework.json.IdOnlySerializer

@Entity
@Indexed
@Table(
  name = "T_USER_WEBAUTHN",
  uniqueConstraints = [UniqueConstraint(columnNames = ["owner_fk", "credential_id"])],
  indexes = [jakarta.persistence.Index(
    name = "idx_fk_t_user_webauthn_user",
    columnList = "owner_fk"
  ), jakarta.persistence.Index(name = "t_user_webauthn_pkey", columnList = "pk")]
)
@NamedQueries(
  NamedQuery(
    name = WebAuthnEntryDO.FIND_BY_ID,
    query = "from WebAuthnEntryDO where id = :id"
  ),
  NamedQuery(
    name = WebAuthnEntryDO.FIND_BY_OWNER,
    query = "from WebAuthnEntryDO where owner.id = :ownerId"
  ),
  NamedQuery(
    name = WebAuthnEntryDO.FIND_BY_OWNER_AND_CREDENTIAL_ID,
    query = "from WebAuthnEntryDO where owner.id = :ownerId and credentialId = :credentialId"
  )
)
open class WebAuthnEntryDO {
  @get:Id
  @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
  @get:Column(name = "pk")
  open var id: Long? = null

  @PropertyInfo(i18nKey = "created")
  @get:Basic
  open var created: Date? = null

  @PropertyInfo(i18nKey = "lastUpdate")
  @get:Basic
  @get:Column(name = "last_update")
  open var lastUpdate: Date? = null

  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "owner_fk")
  @JsonSerialize(using = IdOnlySerializer::class)
  open var owner: PFUserDO? = null

  @get:Column(length = 4000, name = "credential_id")
  open var credentialId: String? = null

  @PropertyInfo(i18nKey = "webauthn.entry.displayName")
  @get:Column(length = 1000, name = "display_name")
  open var displayName: String? = null

  @get:Column(length = 10000, name = "attested_credential_data")
  protected open var serializedAttestedCredentialData: String? = null

  @get:Column(length = 10000, name = "attestation_statement")
  protected open var serializedAttestationStatement: String? = null

  @PropertyInfo(i18nKey = "webauthn.entry.signCount", tooltip = "webauthn.entry.signCount.info")
  @get:Column(name = "sign_count")
  open var signCount: Long? = null

  @get:Transient
  @get:JsonIgnore
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
        asByteArray(serializedAttestationStatement)!!,
        WebAuthnStorage.AttestationStatementEnvelope::class.java
      )
      return envelope?.attestationStatement
    }

  @get:Transient
  @get:JsonIgnore
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
        )!!
      )
    }

  @get:Transient
  @get:JsonIgnore
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

  /**
   * Copies all fields except id, user, created and lastUpdate.
   */
  internal fun copyDataFrom(src: WebAuthnEntryDO) {
    this.serializedAttestedCredentialData = src.serializedAttestedCredentialData
    this.serializedAttestationStatement = src.serializedAttestationStatement
    this.credentialId = src.credentialId
    this.signCount = src.signCount
    this.displayName = src.displayName
  }

  companion object {
    fun create(
      credentialId: ByteArray,
      attestedCredentialData: AttestedCredentialData,
      attestationStatement: AttestationStatement,
      signCount: Long,
      displayName: String? = null
    ): WebAuthnEntryDO {
      val entry = create(credentialId, displayName = displayName)
      entry.attestedCredentialData = attestedCredentialData
      entry.attestationStatement = attestationStatement
      entry.signCount = signCount
      return entry
    }

    fun create(credentialId: ByteArray, authenticator: Authenticator, displayName: String? = null): WebAuthnEntryDO {
      val entry = create(credentialId, displayName = displayName)
      entry.attestedCredentialData = authenticator.attestedCredentialData
      entry.attestationStatement = authenticator.attestationStatement
      entry.signCount = authenticator.counter
      return entry
    }

    private fun create(credentialId: ByteArray, displayName: String?): WebAuthnEntryDO {
      val entry = WebAuthnEntryDO()
      entry.credentialId = Base64UrlUtil.encodeToString(credentialId)
      entry.displayName = displayName
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

    internal const val FIND_BY_OWNER = "WebAuthnEntryDO_FindByOwner"

    internal const val FIND_BY_OWNER_AND_CREDENTIAL_ID = "WebAuthnEntryDO_FindByOwnerAndCredentialId"

    internal const val FIND_BY_ID = "WebAuthnEntryDO_FindById"
  }
}
