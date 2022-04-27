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

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.authenticator.Authenticator
import com.webauthn4j.authenticator.AuthenticatorImpl
import com.webauthn4j.converter.exception.DataConversionException
import com.webauthn4j.data.*
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.Challenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.validator.exception.ValidationException
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.configuration.DomainService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

@Service
class WebAuthnSupport {
  @Autowired
  private lateinit var domainService: DomainService

  private val testStorage = mutableMapOf<Int, WebAuthnEntry>()

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

  lateinit var domain: String // projectforge.acme.com (without protocol)
    private set

  @PostConstruct
  private fun postConstruct() {
    origin = Origin.create(domainService.domain)
    rpId = domainService.plainDomain // Use plain domain (for working also in development mode: http://localhost:3000)
    plainDomain = domainService.plainDomain
    domain = domainService.domain
  }

  fun save(credentialId: ByteArray, authenticator: Authenticator) {
    log.info { "TODO: save authenticator" }
    testStorage.put(ThreadLocalUserContext.getUserId(), WebAuthnEntry(credentialId, authenticator))
  }

  fun load(): WebAuthnEntry? {
    return testStorage[ThreadLocalUserContext.getUserId()]
  }

  fun updateCounter(credentialId: ByteArray, signCount: Long) {
    log.info { "TODO: updateCounter" }
    load()!!.authenticator.counter++
  }

  // For challenge, please specify the Challenge issued on WebAuthn JS API call.
  // challenge is a parameter to prevent replay attacks.
  // By issuing the random byte sequence challenge on server side, signing it with WebAuthn JS API,
  // and verifying the signature on server side, users are protected from the replay attack.
  // TreeTraversal.It is the application’s responsibility for retaining the issued Challenge.
  // Parameter for Token binding. If you do not want to use it please specify null:
  fun registration(
    credentialId: ByteArray,
    attestationObject: ByteArray,
    clientDataJSON: ByteArray,
    challenge: Challenge,
    clientExtensionJSON: String? = null,
    transports: Set<String>? = null
  ) {
    // Server properties
    val tokenBindingId: ByteArray? = null // Not yet supported
    val serverProperty = ServerProperty(origin, rpId, challenge, tokenBindingId)

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
    save(credentialId, authenticator) // please persist authenticator in your manner
  }

  fun authenticate(
    credentialId: ByteArray,
    authenticatorData: ByteArray, /* set authenticatorData */
    clientDataJSON: ByteArray, /* set clientDataJSON */
    clientExtensionJSON: String? = null, /* set clientExtensionJSON */
    signature: ByteArray? = null, /* set signature */
    // Server properties
    challenge: Challenge? = null, /* set challenge */
    userHandle: ByteArray? = null, /* set userHandle */
  ) {
    val tokenBindingId: ByteArray? = null // Not yet supported.
    val serverProperty = ServerProperty(origin, rpId, challenge, tokenBindingId)

    // expectations
    val allowCredentials: List<ByteArray>? = null
    val userVerificationRequired = false
    val userPresenceRequired = true

    val authenticator = load()!!.authenticator

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

  companion object {
    /**
     * Time out for registration, the server is willing to wait for response.
     */
    const val TIMEOUT = 10 * Constants.MILLIS_PER_MINUTE
  }
}
