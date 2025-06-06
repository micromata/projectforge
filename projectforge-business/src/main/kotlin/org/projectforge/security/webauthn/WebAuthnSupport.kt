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

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.exception.DataConversionException
import com.webauthn4j.data.*
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.Challenge
import com.webauthn4j.server.ServerProperty
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.configuration.DomainService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class WebAuthnSupport {
    class Result(val errorMessage: String? = null) {
        val success = errorMessage == null
    }

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var webAuthnStorage: WebAuthnStorage

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
        rpId =
            domainService.plainDomain // Use plain domain (for working also in development mode: http://localhost:3000)
        plainDomain = domainService.plainDomain
        domain = domainService.domain
    }

    // For challenge, please specify the Challenge issued on WebAuthn JS API call.
    // challenge is a parameter to prevent replay attacks.
    // By issuing the random byte sequence challenge on server side, signing it with WebAuthn JS API,
    // and verifying the signature on server side, users are protected from the replay attack.
    // TreeTraversal.It is the application’s responsibility for retaining the issued Challenge.
    // Parameter for Token binding. If you do not want to use it please specify null:
    /**
     * @param displayName Usable by user for identifying multiple tokens.
     */
    fun registration(
        credentialId: ByteArray,
        attestationObject: ByteArray,
        clientDataJSON: ByteArray,
        challenge: Challenge,
        clientExtensionJSON: String? = null,
        transports: Set<String>? = null,
        displayName: String? = null,
    ): Result {
        // Server properties
        val tokenBindingId: ByteArray? = null // Not yet supported
        val serverProperty = ServerProperty(origin, rpId, challenge, tokenBindingId)

        val registrationRequest =
            RegistrationRequest(attestationObject, clientDataJSON, clientExtensionJSON, transports)
        val registrationData = try {
            webAuthnManager.parse(registrationRequest)
        } catch (ex: DataConversionException) {
            log.error("Error while parsing registration request: ${ex.message}", ex)
            return Result("webauthn.error.process")
        }

        // expectations
        val userVerificationRequired = false
        val userPresenceRequired = true

        val registrationParameters =
            RegistrationParameters(serverProperty, null, userVerificationRequired, userPresenceRequired)
        try {
            //webAuthnManager.validate(registrationData, registrationParameters)
            webAuthnManager.verify(registrationData, registrationParameters)
        } catch (ex: Exception) {
            log.error("Error while validating registration data: ${ex.message}", ex)
            return Result("webauthn.error.validate")
        }
        val attestationObject = registrationData.attestationObject!!
        val authenticatorData = attestationObject.authenticatorData
        val webAuthnEntry = WebAuthnEntryDO.create(
            credentialId,
            authenticatorData.attestedCredentialData!!,
            attestationObject.attestationStatement,
            authenticatorData.signCount,
            displayName = displayName
        )
        webAuthnStorage.store(webAuthnEntry) // please persist authenticator in your manner
        return Result()
    }


    fun authenticate(
        credentialId: ByteArray,
        authenticatorData: ByteArray, /* set authenticatorData */
        clientDataJSON: ByteArray, /* set clientDataJSON */
        clientExtensionJSON: String? = null, /* set clientExtensionJSON */
        signature: ByteArray? = null,
        /* set signature */
        // Server properties
        challenge: Challenge? = null, /* set challenge */
        userHandle: ByteArray? = null, /* set userHandle */
    ): Result {
        val tokenBindingId: ByteArray? = null // Not yet supported.
        val serverProperty = ServerProperty(origin, rpId, challenge, tokenBindingId)

        // expectations
        val allowCredentials: List<ByteArray>? = null
        val userVerificationRequired = false
        val userPresenceRequired = true

        val authenticator = webAuthnStorage.load(credentialId)!!.authenticator!!

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
            return Result("webauthn.error.process")
        }
        try {
            //webAuthnManager.validate(authenticationData, authenticationParameters)
            webAuthnManager.verify(authenticationData, authenticationParameters)
        } catch (ex: Exception) {
            log.error("Error while parsing validating request: ${ex.message}", ex)
            return Result("webauthn.error.validate")
        }
        authenticationData.credentialId?.let { credentialId ->
            webAuthnStorage.updateCounter(credentialId, authenticationData.authenticatorData!!.signCount)
        }
        return Result()
    }

    val allLoggedInUserCredentials: List<WebAuthnEntryDO>
        get() = webAuthnStorage.loadAll()

    val availableForLoggedInUser: Boolean
        get() = webAuthnStorage.loadAll().isNotEmpty()

    fun isAvailableForUser(ownerId: Long?): Boolean {
        return webAuthnStorage.loadAll(ownerId).isNotEmpty()
    }

    companion object {
        /**
         * Time out for registration, the server is willing to wait for response.
         */
        const val TIMEOUT = 10 * Constants.MILLIS_PER_MINUTE
    }
}
