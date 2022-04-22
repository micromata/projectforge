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

package org.projectforge.security

import com.webauthn4j.data.*
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.challenge.DefaultChallenge
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.security.fido2.WebAuthnRegistration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.ByteBuffer

private val log = KotlinLogging.logger {}

/**
 * Does the Webauthn stuff (like fido2, Apple attestation etc.). Uses https://github.com/webauthn4j/webauthn4j
 * Description on how the stuff works: https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API
 */
@RestController
@RequestMapping("${Rest.URL}/webauthn")
class WebAuthnServicesRest {
  @Autowired
  private lateinit var webAuthnRegistration: WebAuthnRegistration

  /**
   * Step 0 (the client requests the registration).
   * Only available for logged-in-users, so no further info required from client. The info of the logged-in user
   * is taken.
   */
  @Autowired
  @GetMapping("registrationInfo")
  fun getRegistrationInfo(): PublicKeyCredentialCreationOptions {
    val user = ThreadLocalUserContext.getUser()
    requireNotNull(user)
    val userId = user.id
    requireNotNull(userId)
    val username = user.username
    require(!username.isNullOrBlank())
    val userDisplayName = user.userDisplayName
    require(!userDisplayName.isNullOrBlank())
    val userIdByteArray = ByteBuffer.allocate(Integer.BYTES).putInt(user.id).array()
    val publicKeyCredentialUserEntity = PublicKeyCredentialUserEntity(userIdByteArray, username, userDisplayName)
    val challenge = DefaultChallenge()
    val publicKeyCredentialParameters = PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)
    // CROSS_PLATFORM: required for support of mobile phones etc.
    val authenticatorSelectionCriteria =
      AuthenticatorSelectionCriteria(AuthenticatorAttachment.CROSS_PLATFORM, true, UserVerificationRequirement.REQUIRED)

    // https://www.w3.org/TR/webauthn-1/#dictdef-publickeycredentialcreationoptions
    return PublicKeyCredentialCreationOptions(
      PublicKeyCredentialRpEntity(webAuthnRegistration.rpId, webAuthnRegistration.plainDomain),
      publicKeyCredentialUserEntity,
      challenge,
      listOf(publicKeyCredentialParameters),
      Constants.MILLIS_PER_MINUTE * 10, // timeout in milliseconds: 10 minutes
      null,
      authenticatorSelectionCriteria,
      AttestationConveyancePreference.NONE,
      null
    )
  }
}
