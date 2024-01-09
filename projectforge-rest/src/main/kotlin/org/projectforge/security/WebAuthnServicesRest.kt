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
import org.projectforge.rest.my2fa.My2FAServicesRest
import org.projectforge.rest.my2fa.My2FASetupPageRest
import org.projectforge.security.dto.*
import org.projectforge.security.webauthn.WebAuthnSupport
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.ByteBuffer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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

  @Autowired
  private lateinit var my2FAServicesRest: My2FAServicesRest

  @Autowired
  private lateinit var my2FASetupPageRest: My2FASetupPageRest

  /**
   * Step 0 (the client requests the registration).
   * Only available for logged-in-users, so no further info required from client. The info of the logged-in user
   * is taken.
   */
  @GetMapping("register")
  fun register(request: HttpServletRequest): WebAuthnPublicKeyCredentialCreationOptions {
    log.info { "User requested challenge for registration." }
    // https://www.w3.org/TR/webauthn-1/#dictdef-publickeycredentialcreationoptions
    val options = WebAuthnPublicKeyCredentialCreationOptions(
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
    // Add all existing entries for avoiding multiple registration of same tokens:
    options.excludeCredentials = allLoggedInUserCredentials
    return options
  }

  /**
   * Step 5: Browser Creates Final Data, Application sends response to Server
   * Rest service is defined in [WebAuthnEntryPageRest]
   */
  fun doRegisterFinish(
    request: HttpServletRequest,
    webAuthnRequest: WebAuthnFinishRequest,
    displayName: String? = null,
  ): WebAuthnSupport.Result {
    log.info { "User wants to finish registration." }
    if (!my2FASetupPageRest.checkLastSuccessful2FA()) {
      return WebAuthnSupport.Result("user.My2FA.required")
    }
    val credential = webAuthnRequest.credential!!
    val credentialId = Base64.decodeBase64(credential.id)
    val response = credential.response!!
    val attestationObject = Base64.decodeBase64(response.attestationObject)
    val clientDataJSON = Base64.decodeBase64(response.clientDataJSON)
    val clientExtensionJSON = null
    val transports = response.transports
    return webAuthnSupport.registration(
      credentialId,
      attestationObject = attestationObject,
      clientDataJSON = clientDataJSON,
      challenge = getUserChallenge(request)!!,
      clientExtensionJSON = clientExtensionJSON,
      transports = transports,
      displayName = displayName,
    )
  }

  @GetMapping("webAuthn")
  fun webAuthn(request: HttpServletRequest): ResponseEntity<WebAuthnPublicKeyCredentialCreationOptions> {
    log.info { "User requested challenge for authentication." }
    return ResponseEntity.ok(
      WebAuthnPublicKeyCredentialCreationOptions(
        rp = rp,
        user = loggedInUser,
        challenge = createUserChallenge(request), // https://www.w3.org/TR/webauthn-2/
        timeout = WebAuthnSupport.TIMEOUT,
        requestId = requestId,
        sessionToken = request.getSession(false).id,
        pubKeyCredParams = publicKeyCredentialParameters,
        authenticatorSelection = authenticatorSelectionCriteria,
        extensions = extensions,
        allowCredentials = allLoggedInUserCredentials
      )
    )
  }

  /**
   * Step 5: Browser Creates Final Data, Application sends response to Server
   * Rest service is defined in [My2FAServicesRest]
   */
  fun doWebAuthnFinish(
    request: HttpServletRequest,
    httpResponse: HttpServletResponse,
    webAuthnRequest: WebAuthnFinishRequest
  ): WebAuthnSupport.Result {
    log.info { "User wants to finish authentication." }
    val credential = webAuthnRequest.credential!!
    val credentialId = Base64.decodeBase64(credential.id)
    val response = credential.response!!
    val clientDataJSON = Base64.decodeBase64(response.clientDataJSON)
    val authenticatorData = Base64.decodeBase64(response.authenticatorData)
    val signature = Base64.decodeBase64(response.signature)
    val userHandle = if (response.userHandle != null) {
      Base64.decodeBase64(response.userHandle)
    } else {
      null
    }
    val clientExtensionJSON = null
    val result = webAuthnSupport.authenticate(
      credentialId = credentialId,
      signature = signature,
      clientDataJSON = clientDataJSON,
      authenticatorData = authenticatorData,
      challenge = getUserChallenge(request)!!,
      clientExtensionJSON = clientExtensionJSON,
      userHandle = userHandle,
    )
    if (result.success) {
      ThreadLocalUserContext.userContext!!.updateLastSuccessful2FA()
      my2FAServicesRest.updateCookieAndSession(request, httpResponse)
    }
    return result
  }

  private val loggedInUser: WebAuthnUser
    get() {
      val user = ThreadLocalUserContext.user
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

  val allLoggedInUserCredentials: Array<WebAuthnCredential>
    get() = webAuthnSupport.allLoggedInUserCredentials.map { WebAuthnCredential(it.credentialId) }.toTypedArray()

  companion object {
    fun addAuthenticateTranslations(layout: UILayout) {
      layout.addTranslations(
        "webauthn.registration.button.authenticate",
        "webauthn.registration.button.authenticate.info",
      )
    }

    fun addRegisterTranslations(layout: UILayout) {
      layout.addTranslations(
        "webauthn.registration.button.register",
      )
    }

    private val SESSSION_ATTRIBUTE_CHALLENGE = "${WebAuthnServicesRest::class.java.name}:challenge"
  }
}
