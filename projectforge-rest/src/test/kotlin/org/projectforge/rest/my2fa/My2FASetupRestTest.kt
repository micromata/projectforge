/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.my2fa

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.calendar.BarcodeServicesRest
import org.projectforge.rest.core.RestCsrfProtection
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.pub.next.TwoFactorRequired
import org.projectforge.security.My2FAService
import org.projectforge.security.dto.WebAuthnFinishRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.util.Base64

/**
 * Only refused changes: a successful one would change the second factors of a test user shared with other tests.
 */
class My2FASetupRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var my2FASetupRest: My2FASetupRest

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Test
    fun `a change without session or csrf token is refused`() {
        logon(TEST_USER)
        ThreadLocalUserContext.userContext!!.updateLastSuccessful2FA() // Only the csrf token is missing.
        val withoutToken = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        listOf(MockHttpServletRequest(), withoutToken, requestWithToken("x".repeat(30))).forEach { request ->
            changes(request).forEach { (name, response) ->
                assertEquals(HttpStatus.FORBIDDEN, response.statusCode, name)
                assertEquals(mapOf("csrfTokenRequired" to true), response.body, name)
            }
        }
    }

    @Test
    fun `the key and a change need a fresh 2FA`() {
        logon(TEST_USER)
        ThreadLocalUserContext.userContext!!.lastSuccessful2FA =
            System.currentTimeMillis() - (My2FAService.SETUP_2FA_MINUTES + 1) * 60_000
        // Renaming a key needs no fresh 2FA. Enabling and disabling the authenticator app depend on the state of the
        // test user (one of them fails before the 2FA check), so they are left out.
        val except = setOf("webAuthnUpdate", "enableAuthenticatorApp", "disableAuthenticatorApp")
        val responses = changes(requestWithToken(), except) +
                ("authenticatorKey" to my2FASetupRest.getAuthenticatorKey())
        responses.forEach { (name, response) ->
            assertEquals(HttpStatus.FORBIDDEN, response.statusCode, name)
            val body = response.body as TwoFactorRequired
            assertEquals(My2FAService.SETUP_2FA_MINUTES * 60_000, body.expiryMillis, name)
        }
        assertEquals(false, my2FASetupRest.getInitialData().fresh2FA)
    }

    @Test
    fun `the qr code of a secret is a data url, not an image url`() {
        val dataUrl = BarcodeServicesRest.getQrCodeDataUrl("otpauth://totp/test?secret=ABCDEF")
        assertTrue(dataUrl.startsWith("data:image/png;base64,"), dataUrl)
        val png = Base64.getDecoder().decode(dataUrl.removePrefix("data:image/png;base64,"))
        assertEquals(listOf(0x89, 'P'.code, 'N'.code, 'G'.code), png.take(4).map { it.toInt() and 0xff })
    }

    /**
     * All changing calls of the page except [except]. None of them may reach the change itself (refused before).
     */
    private fun changes(request: HttpServletRequest, except: Set<String> = emptySet()): Map<String, ResponseEntity<*>> {
        val entry = postData(My2FASetupRest.WebAuthnEntryPostData(id = 1, displayName = "test"))
        val finish = postData(
            My2FASetupRest.WebAuthnEntryPostData(displayName = "test", webAuthnFinishRequest = WebAuthnFinishRequest())
        )
        return mapOf(
            "enableAuthenticatorApp" to { my2FASetupRest.enableAuthenticatorApp(request) },
            "disableAuthenticatorApp" to { my2FASetupRest.disableAuthenticatorApp(request) },
            "saveMobilePhone" to {
                my2FASetupRest.saveMobilePhone(request, postData(My2FASetupRest.MobilePhoneData("+49 123 456789")))
            },
            "webAuthnRegisterFinish" to { my2FASetupRest.webAuthnRegisterFinish(request, finish) },
            "webAuthnUpdate" to { my2FASetupRest.webAuthnUpdate(request, entry) },
            "webAuthnDelete" to { my2FASetupRest.webAuthnDelete(request, entry) },
        ).filterKeys { it !in except }.mapValues { it.value() }
    }

    private fun <T : Any> postData(data: T) = PostData(data, watchFieldsTriggered = null, serverData = null)

    /**
     * A request of a session with a csrf token, sending that token or [token] instead.
     */
    private fun requestWithToken(token: String? = null): MockHttpServletRequest {
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val sessionToken = sessionCsrfService.createServerData(request as HttpServletRequest).csrfToken!!
        request.addHeader(RestCsrfProtection.CSRF_TOKEN_HEADER, token ?: sessionToken)
        return request
    }
}
