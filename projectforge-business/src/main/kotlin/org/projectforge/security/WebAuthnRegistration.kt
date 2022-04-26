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

package org.projectforge.security.fido2

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.authenticator.Authenticator
import com.webauthn4j.authenticator.AuthenticatorImpl
import com.webauthn4j.converter.exception.DataConversionException
import com.webauthn4j.data.*
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.Challenge
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.validator.exception.ValidationException
import mu.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

@Service
class WebAuthnRegistration {
  @Autowired
  private lateinit var domainService: DomainService

  private val webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager()

  /**
   * ProjectForge's domain: e. g. https://projectforge.acme.com
   */
  lateinit var origin: Origin
    private set

  // See: https://www.w3.org/TR/webauthn-1/#relying-party-identifier
  lateinit var rpId: String // Same as origin url.
    private set

  lateinit var plainDomain: String // projectforge.acme.com (without protocol)
    private set

  @PostConstruct
  private fun postConstruct() {
    origin = Origin.create(domainService.domain)
    rpId = domainService.domain
    plainDomain = domainService.plainDomain
  }

  fun save(authenticator: Authenticator) {
    log.info { "TODO: save authenticator" }
  }

  fun load(credentialId: ByteArray?): Authenticator? {
    log.info { "TODO: load credentialId" }
    return null
  }

  fun updateCounter(credentialId: ByteArray?, signCount: Long) {
    log.info { "TODO: updateCounter" }
  }

  // For challenge, please specify the Challenge issued on WebAuthn JS API call.
  // challenge is a parameter to prevent replay attacks.
  // By issuing the random byte sequence challenge on server side, signing it with WebAuthn JS API,
  // and verifying the signature on server side, users are protected from the replay attack.
  // TreeTraversal.It is the application’s responsibility for retaining the issued Challenge.
  // Parameter for Token binding. If you do not want to use it please specify null:
  fun registration(
    attestationObject: ByteArray,
    clientDataJSON: ByteArray,
    challenge: String,
    clientExtensionJSON: String? = null,
    transports: Set<String>? = null
  ) {
    // Server properties
    val rpId = domainService.domain

    val tokenBindingId: ByteArray? = null
    val challengeObj = DefaultChallenge(challenge)
    val serverProperty = ServerProperty(origin, rpId, challengeObj, tokenBindingId)

    val registrationRequest = RegistrationRequest(attestationObject, clientDataJSON, clientExtensionJSON, transports)
    val registrationData = try {
      webAuthnManager.parse(registrationRequest)
    } catch (ex: DataConversionException) {
      log.error("Error while parsing registration request: ${ex.message}", ex)
      throw ex
    }

    // expectations
    val userVerificationRequired = false
    val userPresenceRequired = true

    val registrationParameters =
      RegistrationParameters(serverProperty, null, userVerificationRequired, userPresenceRequired)
    try {
      webAuthnManager.validate(registrationData, registrationParameters)
    } catch (ex: ValidationException) {
      log.error("Error while validating registration data: ${ex.message}", ex)
      throw ex
    }

    // please persist Authenticator object, which will be used in the authentication process.
    val authenticator: Authenticator =
      AuthenticatorImpl( // You may create your own Authenticator implementation to save friendly authenticator name
        registrationData.attestationObject!!.authenticatorData.attestedCredentialData!!,
        registrationData.attestationObject!!.attestationStatement,
        registrationData.attestationObject!!.authenticatorData.signCount
      )
    save(authenticator) // please persist authenticator in your manner
  }

  fun authenticate() {
    // Client properties
    val credentialId: ByteArray? = null /* set credentialId */
    val userHandle: ByteArray? = null /* set userHandle */
    val authenticatorData: ByteArray? = null /* set authenticatorData */
    val clientDataJSON: ByteArray? = null /* set clientDataJSON */
    val clientExtensionJSON: String? = null /* set clientExtensionJSON */
    val signature: ByteArray? = null /* set signature */

    // Server properties
    val challenge: Challenge? = null /* set challenge */
    val tokenBindingId: ByteArray? = null /* set tokenBindingId */
    val serverProperty = ServerProperty(origin, rpId, challenge, tokenBindingId)

    // expectations
    val allowCredentials: List<ByteArray>? = null
    val userVerificationRequired = true
    val userPresenceRequired = true
    val expectedExtensionIds: List<String> = emptyList()

    val authenticator = load(credentialId)!!

    val authenticationRequest = AuthenticationRequest(
      credentialId,
      userHandle,
      authenticatorData,
      clientDataJSON,
      clientExtensionJSON,
      signature
    )
    val authenticationParameters = AuthenticationParameters(
      serverProperty,
      authenticator,
      allowCredentials,
      userVerificationRequired,
      userPresenceRequired
    )

    val authenticationData: AuthenticationData
    authenticationData = try {
      webAuthnManager.parse(authenticationRequest)
    } catch (ex: DataConversionException) {
      log.error("Error while parsing registration request: ${ex.message}", ex)
      throw ex
    }
    try {
      webAuthnManager.validate(authenticationData, authenticationParameters)
    } catch (ex: ValidationException) {
      log.error("Error while parsing validating request: ${ex.message}", ex)
      throw ex
    }
    // please update the counter of the authenticator record
    updateCounter(
      authenticationData.credentialId,
      authenticationData.authenticatorData!!.signCount
    )
  }
}
