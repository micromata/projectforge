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
import com.webauthn4j.data.client.challenge.Challenge
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.util.Base64UrlUtil
import mu.KotlinLogging
import org.apache.commons.codec.binary.Base64
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.dto.PostData
import org.projectforge.security.dto.*
import org.projectforge.security.webauthn.WebAuthnSupport
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
    // https://www.w3.org/TR/webauthn-1/#dictdef-publickeycredentialcreationoptions
    return WebAuthnPublicKeyCredentialCreationOptions(
      rp = rp,
      user = loggedInUser,
      challenge = createUserChallenge(request), // https://www.w3.org/TR/webauthn-2/
      timeout = WebAuthnSupport.TIMEOUT,
      requestId = requestId,
      sessionToken = request.getSession(false).id,
      pubKeyCredParams = publicKeyCredentialParameters,
      authenticatorSelection = authenticatorSelectionCriteria,
      extensions = extensions,
    )
  }

  /**
   * Step 5: Browser Creates Final Data, Application sends response to Server
   */
  @PostMapping("registerFinish")
  fun registerFinish(
    request: HttpServletRequest,
    @RequestBody postData: PostData<WebAuthnFinishRequest>
  ): WebAuthnRegisterResult {
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
      challenge = getUserChallenge(request)!!,
      clientExtensionJSON = clientExtensionJSON,
      transports = transports,
    )
    return WebAuthnRegisterResult(true)
  }

  @GetMapping("authenticate")
  fun authenticate(request: HttpServletRequest): WebAuthnPublicKeyCredentialCreationOptions {
    log.info { "User requested challenge for authentication." }
    val entries = webAuthnSupport.loadAllAllowCredentialsOfUser()
    val allowCredentials = entries.map { WebAuthnCredential.create(it.credentialId) }.toTypedArray()
    return WebAuthnPublicKeyCredentialCreationOptions(
      rp = rp,
      user = loggedInUser,
      challenge = createUserChallenge(request), // https://www.w3.org/TR/webauthn-2/
      timeout = WebAuthnSupport.TIMEOUT,
      requestId = requestId,
      sessionToken = request.getSession(false).id,
      pubKeyCredParams = publicKeyCredentialParameters,
      authenticatorSelection = authenticatorSelectionCriteria,
      extensions = extensions,
      allowCredentials = allowCredentials
    )
  }

  /**
   * Step 5: Browser Creates Final Data, Application sends response to Server
   */
  @PostMapping("authenticateFinish")
  fun authenticateFinish(request: HttpServletRequest, @RequestBody postData: PostData<WebAuthnFinishRequest>): WebAuthnAuthenticateResult {
    log.info { "User wants to finish registration." }
    val webAuthnRequest = postData.data
    val credential = webAuthnRequest.credential!!
    val credentialId = Base64.decodeBase64(credential.id)
    val response = credential.response!!
    val clientDataJSON = Base64.decodeBase64(response.clientDataJSON)
    val authenticatorData = Base64.decodeBase64(response.authenticatorData)
    val signature = Base64.decodeBase64(response.signature)
    val userHandle = if (response.userHandle != null) { Base64.decodeBase64(response.userHandle) } else { null }
    val clientExtensionJSON = null
    webAuthnSupport.authenticate(
      credentialId = credentialId,
      signature = signature,
      clientDataJSON = clientDataJSON,
      authenticatorData = authenticatorData,
      challenge = getUserChallenge(request)!!,
      clientExtensionJSON = clientExtensionJSON,
      userHandle = userHandle,
    )
    return WebAuthnAuthenticateResult(true)
  }

  private val loggedInUser: WebAuthnUser
    get() {
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

  private val rp: WebAuthnRp
    get() = WebAuthnRp(webAuthnSupport.rpId, webAuthnSupport.plainDomain)

  private fun createUserChallenge(request: HttpServletRequest): String {
    val challenge = DefaultChallenge()
    ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_CHALLENGE, challenge, 10)
    return Base64UrlUtil.encodeToString(challenge.value)
  }

  private fun getUserChallenge(request: HttpServletRequest): Challenge? {
    return ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_CHALLENGE, Challenge::class.java)
  }

  private val requestId: String
    get() = NumberHelper.getSecureRandomAlphanumeric(20)

  private val publicKeyCredentialParameters: Array<PublicKeyCredentialParameters>
    get() = arrayOf(
      PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
      PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256),
    )

  // CROSS_PLATFORM: required for support of mobile phones etc.
  private val authenticatorSelectionCriteria: AuthenticatorSelectionCriteria
    get() = AuthenticatorSelectionCriteria(
      AuthenticatorAttachment.CROSS_PLATFORM,
      false,
      UserVerificationRequirement.PREFERRED
    )

  private val extensions: WebAuthnExtensions
    get() = WebAuthnExtensions(webAuthnSupport.domain)

  companion object {
    private val SESSSION_ATTRIBUTE_CHALLENGE = "${WebAuthnServicesRest::class.java.name}:challenge"
  }
}
