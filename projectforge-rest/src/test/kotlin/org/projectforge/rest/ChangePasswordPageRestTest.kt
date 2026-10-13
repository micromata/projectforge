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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.core.RestCsrfProtection
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession

/**
 * Only refused changes: a successful one would change the password of a test user shared with other tests.
 */
class ChangePasswordPageRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var changePasswordPageRest: ChangePasswordPageRest

    @Autowired
    private lateinit var changeWlanPasswordPageRest: ChangeWlanPasswordPageRest

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Test
    fun `the init of the own password page`() {
        logon(TEST_USER)
        val init = changePasswordPageRest.getInit(null)
        assertEquals(getUserId(TEST_USER), init.userId)
        assertTrue(init.changeOwn)
        assertTrue(init.supported)
        assertNotNull(init.passwordQualityHint)
        // The default login handler can't change the WLAN password, and its rules aren't checked anyway.
        val wlan = changeWlanPasswordPageRest.getInit(null)
        assertFalse(wlan.supported)
        assertNull(wlan.passwordQualityHint)
    }

    @Test
    fun `only admins may open the page of another user`() {
        logon(TEST_ADMIN_USER)
        val init = changePasswordPageRest.getInit(getUserId(TEST_USER).toString())
        assertFalse(init.changeOwn)
        logon(TEST_USER)
        assertThrows<AccessException> { changePasswordPageRest.getInit(getUserId(TEST_ADMIN_USER).toString()) }
        assertThrows<AccessException> {
            changePasswordPageRest.change(requestWithToken(), postData(userId = getUserId(TEST_ADMIN_USER)))
        }
    }

    @Test
    fun `a change without session or csrf token is refused`() {
        logon(TEST_USER)
        assertEquals(HttpStatus.FORBIDDEN, changePasswordPageRest.change(MockHttpServletRequest(), postData()).statusCode)
        val withoutToken = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        assertEquals(HttpStatus.FORBIDDEN, changePasswordPageRest.change(withoutToken, postData()).statusCode)
        val wrongToken = requestWithToken("x".repeat(30))
        assertEquals(HttpStatus.FORBIDDEN, changePasswordPageRest.change(wrongToken, postData()).statusCode)
    }

    @Test
    fun `the errors are answered on their fields`() {
        logon(TEST_USER)
        assertEquals(
            listOf("loginPassword", "newPassword"),
            fieldIds(changePasswordPageRest.change(requestWithToken(), postData(login = "", new = "", repeat = ""))),
        )
        assertEquals(
            listOf("passwordRepeat"),
            fieldIds(changePasswordPageRest.change(requestWithToken(), postData(repeat = "other-Pass42!"))),
        )
        assertEquals(
            listOf("loginPassword"),
            fieldIds(changePasswordPageRest.change(requestWithToken(), postData(login = "wrong"))),
        )
        // The right old password, but a new one breaking the quality rules (too short, no non-letter): nothing is
        // changed, and every broken rule is on the field.
        assertEquals(
            listOf("newPassword", "newPassword"),
            fieldIds(
                changePasswordPageRest.change(
                    requestWithToken(), postData(login = String(TEST_USER_PASSWORD), new = "a", repeat = "a"),
                )
            ),
        )
    }

    @Test
    fun `a not supported change is refused`() {
        logon(TEST_USER)
        val response = changeWlanPasswordPageRest.change(requestWithToken(), postData(login = String(TEST_USER_PASSWORD)))
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        val error = (response.body as ResponseAction).validationErrors!!.single()
        assertEquals(ChangePasswordPageRest.MESSAGE_KEY_NOT_SUPPORTED, error.messageId)
        assertNull(error.fieldId)
    }

    @Test
    fun `the error keys of the user service belong to their fields`() {
        assertEquals("loginPassword", ChangePasswordPageRest.fieldIdOf("user.changePassword.error.oldPasswordWrong"))
        assertEquals("loginPassword", ChangePasswordPageRest.fieldIdOf("user.changeWlanPassword.error.loginPasswordWrong"))
        assertEquals("newPassword", ChangePasswordPageRest.fieldIdOf("user.changePassword.error.notMinLength"))
        assertEquals("newPassword", ChangePasswordPageRest.fieldIdOf("user.changePassword.error.oldPasswdEqualsNew"))
        assertNull(ChangePasswordPageRest.fieldIdOf("something.else"))
    }

    /**
     * A request of a session with a csrf token, sending that token or [token] instead.
     */
    private fun requestWithToken(token: String? = null): MockHttpServletRequest {
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val sessionToken = sessionCsrfService.createServerData(request as HttpServletRequest).csrfToken!!
        request.addHeader(RestCsrfProtection.CSRF_TOKEN_HEADER, token ?: sessionToken)
        return request
    }

    private fun postData(
        userId: Long? = null,
        login: String = "wrong",
        new: String = "new-Pass42!",
        repeat: String = new,
    ) = PostData(
        ChangePasswordData(
            userId = userId,
            loginPassword = login.toCharArray(),
            newPassword = new.toCharArray(),
            passwordRepeat = repeat.toCharArray(),
        ),
        watchFieldsTriggered = null,
        serverData = null,
    )

    private fun fieldIds(response: ResponseEntity<*>): List<String?> {
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        return (response.body as ResponseAction).validationErrors!!.map { it.fieldId }
    }
}
