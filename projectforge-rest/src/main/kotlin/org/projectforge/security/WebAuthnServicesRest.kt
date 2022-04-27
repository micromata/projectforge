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
import com.webauthn4j.util.Base64UrlUtil
import mu.KotlinLogging
import org.apache.commons.codec.binary.Base64
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.PostData
import org.projectforge.security.dto.*
import org.projectforge.security.fido2.WebAuthnSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.nio.ByteBuffer
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Does the Webauthn stuff (like fido2, Apple attestation etc.). Uses https://github.com/webauthn4j/webauthn4j
 * Description on how the stuff works: https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API
 */
@RestController
@RequestMapping("${Rest.URL}/webauthn")
class WebAuthnServicesRest {
  @Autowired
  private lateinit var webAuthnSupport: WebAuthnSupport

  /**
   * Step 0 (the client requests the registration).
   * Only available for logged-in-users, so no further info required from client. The info of the logged-in user
   * is taken.
   */
  @GetMapping("register")
  fun register(request: HttpServletRequest): WebAuthnPublicKeyCredentialCreationOptions {
    log.info { "User requested challenge for registration." }
    val user = getLoggedInUser()
    // https://www.w3.org/TR/webauthn-1/#dictdef-publickeycredentialcreationoptions
    return WebAuthnPublicKeyCredentialCreationOptions(
      rp = getRp(),
      user = user,
      challenge = getChallenge(), // https://www.w3.org/TR/webauthn-2/
      timeout = WebAuthnSupport.TIMEOUT,
      requestId = getRequestId(),
      sessionToken = request.getSession(false).id,
      pubKeyCredParams = getPublicKeyCredentialParameters(),
      authenticatorSelection = getAuthenticatorSelectionCriteria(),
      extensions = WebAuthnExtensions(webAuthnSupport.rpId),
    )
  }

  /**
   * Step 5: Browser Creates Final Data, Application sends response to Server
   */
  @PostMapping("registerFinish")
  fun registerFinish(@RequestBody postData: PostData<WebAuthnFinishRequest>): WebAuthnFinishResult {
    log.info { "User wants to finish registration." }
    val webAuthnRequest = postData.data
    val credential = webAuthnRequest.credential!!
    val credentialId = Base64.decodeBase64(credential.id)
    val response = credential.response!!
    val attestationObject = Base64.decodeBase64(response.attestationObject)
    val clientDataJSON = Base64.decodeBase64(response.clientDataJSON)
    val clientExtensionJSON = null
    val transports = response.transports
    webAuthnSupport.registration(
      credentialId,
      attestationObject = attestationObject,
      clientDataJSON = clientDataJSON,
      challenge = webAuthnRequest.challenge!!,
      clientExtensionJSON = clientExtensionJSON,
      transports = transports,
    )
    return WebAuthnFinishResult(true)
  }

  @GetMapping("authenticate")
  fun authenticate(request: HttpServletRequest): WebAuthnPublicKeyCredentialCreationOptions {
    log.info { "User requested challenge for authentication." }
    val user = getLoggedInUser()
    return WebAuthnPublicKeyCredentialCreationOptions(
      rp = getRp(),
      user = user,
      challenge = getChallenge(), // https://www.w3.org/TR/webauthn-2/
      timeout = WebAuthnSupport.TIMEOUT,
      requestId = getRequestId(),
      sessionToken = request.getSession(false).id,
      pubKeyCredParams = getPublicKeyCredentialParameters(),
      authenticatorSelection = getAuthenticatorSelectionCriteria(),
      extensions = WebAuthnExtensions(webAuthnSupport.rpId),
    )
  }

  /**
   * Step 5: Browser Creates Final Data, Application sends response to Server
   */
  @PostMapping("authenticateFinish")
  fun authenticateFinish(@RequestBody postData: PostData<WebAuthnFinishRequest>): WebAuthnFinishResult {
    log.info { "User wants to finish registration." }
    val webAuthnRequest = postData.data
    val credential = webAuthnRequest.credential!!
    val credentialId = Base64.decodeBase64(credential.id)
    val response = credential.response!!
    log.info { "User wants to finish registration." }
    val attestationObject = Base64.decodeBase64(response.attestationObject)
    val clientDataJSON = Base64.decodeBase64(response.clientDataJSON)
    val clientExtensionJSON = null
    val transports = response.transports
    webAuthnSupport.registration(
      credentialId,
      attestationObject = attestationObject,
      clientDataJSON = clientDataJSON,
      challenge = webAuthnRequest.challenge!!,
      clientExtensionJSON = clientExtensionJSON,
      transports = transports,
    )
    return WebAuthnFinishResult(true)
  }

  private fun getLoggedInUser(): WebAuthnUser {
    val user = ThreadLocalUserContext.getUser()
    requireNotNull(user)
    val userId = user.id
    requireNotNull(userId)
    val username = user.username
    require(!username.isNullOrBlank())
    val userDisplayName = user.userDisplayName
    require(!userDisplayName.isNullOrBlank())
    val userIdByteArray = ByteBuffer.allocate(Integer.BYTES).putInt(user.id).array()
    return WebAuthnUser(userIdByteArray, username, userDisplayName)
  }

  private fun getRp(): WebAuthnRp {
    return WebAuthnRp(webAuthnSupport.rpId, webAuthnSupport.plainDomain)
  }

  private fun getChallenge(): String {
    return Base64UrlUtil.encodeToString(DefaultChallenge().value)
  }

  private fun getRequestId(): String {
    return NumberHelper.getSecureRandomAlphanumeric(20)
  }

  private fun getPublicKeyCredentialParameters(): Array<PublicKeyCredentialParameters> {
    return arrayOf(
      PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
      PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256),
    )
  }

  private fun getAuthenticatorSelectionCriteria(): AuthenticatorSelectionCriteria {
    // CROSS_PLATFORM: required for support of mobile phones etc.
    return AuthenticatorSelectionCriteria(
      AuthenticatorAttachment.CROSS_PLATFORM,
      false,
      UserVerificationRequirement.PREFERRED
    )

  }
}
